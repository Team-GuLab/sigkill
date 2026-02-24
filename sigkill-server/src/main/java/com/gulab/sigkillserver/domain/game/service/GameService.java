package com.gulab.sigkillserver.domain.game.service;

import static com.gulab.sigkillserver.domain.game.exception.GameErrorCode.GAME_NOT_FOUND;
import static com.gulab.sigkillserver.domain.game.exception.GameErrorCode.SUBMIT_CHOICE_IS_AFTER_DEADLINE;
import static com.gulab.sigkillserver.domain.game.exception.GameErrorCode.SUBMIT_CHOICE_NOT_CURRENT_QUIZ;
import static com.gulab.sigkillserver.domain.game.exception.QuizErrorCode.QUIZ_CHOICE_NUMBER_MAPPING_ERROR;
import static com.gulab.sigkillserver.domain.game.exception.QuizErrorCode.QUIZ_CHOICE_NUMBER_MAPPING_NOT_FOUND;
import static com.gulab.sigkillserver.domain.game.exception.QuizErrorCode.QUIZ_INDEX_OUT_OF_BOUNDS;
import static com.gulab.sigkillserver.domain.game.exception.QuizErrorCode.QUIZ_NOT_FOUND;
import static com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM;
import static com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode.PLAYER_NOT_IN_ROOM;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_ALREADY_STARTED;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NOT_FOUND;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NOT_STARTED;
import static com.gulab.sigkillserver.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizChoiceInfo;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.model.SelectedChoice;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoice;
import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceRepository;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;
    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
    private final SelectedChoiceRepository selectedChoiceRepository;
    private final QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private final GameEventBuilder gameEventBuilder;

    /**
     * 게임 시작. RoomService 에서 호출
     */
    public GameStartEvent startGame(Room room) {
        validateGameNotInProgress(room);
        List<Long> quizIds = quizRepository.findByCategoryId(
                        GameConstants.DEFAULT_CATEGORY_ID,
                        GameConstants.QUIZ_COUNT)
                .stream()
                .map(Quiz::quizId)
                .toList();
        Game game = Game.create(room.getRoomId(), quizIds);
        game = gameRepository.save(game);
        room.startGame();

        return gameEventBuilder.toGameStartEvent(room, game);
    }

    /**
     * 게임 중 다음 퀴즈 시작. 클라이언트에서 호출
     */
    public QuizStartEvent startQuiz(Long userId, String roomId, Long gameId) {
        getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);

        if (game.isQuizIndexOutOfBounds()) {
            throw new CustomException(QUIZ_INDEX_OUT_OF_BOUNDS);
        }

        long quizStartTime = Instant.now().toEpochMilli();
        long quizId = game.startNextQuiz(quizStartTime);
        Quiz quiz = getQuizOrThrow(quizId);

        List<QuizChoice> shuffled = new ArrayList<>(quiz.choices());
        Collections.shuffle(shuffled);
        Map<Integer, Long> numberToChoiceId = new LinkedHashMap<>();
        List<QuizChoiceInfo> quizChoiceInfos = new ArrayList<>();

        for (int i = 0; i < shuffled.size(); i++) {
            int number = i + 1;
            QuizChoice c = shuffled.get(i);
            numberToChoiceId.put(number, c.choiceId());
            quizChoiceInfos.add(new QuizChoiceInfo(number, c.text()));
        }

        quizChoiceNumberMappingRepository.save(
                QuizChoiceNumberMapping.create(gameId, quizId, numberToChoiceId)
        );

        return gameEventBuilder.toQuizStartEvent(room, game, quiz, quizStartTime, quizChoiceInfos);
    }

    /**
     * 퀴즈에 대한 선택지 제출. 클라이언트에서 호출
     */
    public ChoiceSubmitEvent submitChoice(Long userId, String roomId, Long gameId, Long quizId, Integer choiceNumber) {
        long submitTime = Instant.now().toEpochMilli();
        Player player = getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);
        validateSubmitChoiceIsForCurrentQuiz(game, quizId);
        validateDeadline(game, submitTime);

        long choiceId = getChoiceId(gameId, quizId, choiceNumber);
        Quiz quiz = getQuizOrThrow(quizId);

        SelectedChoice selectedChoice = SelectedChoice.create(gameId, quizId, userId, choiceId, submitTime);
        selectedChoiceRepository.save(selectedChoice);
        return gameEventBuilder.toChoiceSubmitEvent(room, game, quiz, player, choiceNumber, submitTime);
    }

    private void validateDeadline(Game game, long submitTime) {
        if (game.hasExceededDeadline(submitTime)) {
            throw new CustomException(SUBMIT_CHOICE_IS_AFTER_DEADLINE);
        }
    }

    private long getChoiceId(Long gameId, Long quizId, Integer choiceNumber) {
        return quizChoiceNumberMappingRepository.findByGameIdAndQuizId(gameId, quizId)
                .orElseThrow(() -> new CustomException(QUIZ_CHOICE_NUMBER_MAPPING_NOT_FOUND))
                .findChoiceIdByNumber(choiceNumber)
                .orElseThrow(() -> new CustomException(QUIZ_CHOICE_NUMBER_MAPPING_ERROR));
    }

    private void validateSubmitChoiceIsForCurrentQuiz(Game game, Long quizId) {
        if (game.getCurrentQuizId() != quizId) {
            throw new CustomException(SUBMIT_CHOICE_NOT_CURRENT_QUIZ);
        }
    }

    /**
     * 퀴즈 종료. 서버 에서 호출
     */
    public QuizEndEvent endQuiz(Long userId, String roomId, Long gameId, Long quizId) {
        Player player = getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);

        quizChoiceNumberMappingRepository.deleteByGameIdAndQuizId(gameId, quizId);
        return null;
    }

    /**
     * 게임 종료. 서버 에서 호출
     */
    public GameEndEvent endGame(Long userId, String roomId, Long gameId) {
        Player player = getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);

        quizChoiceNumberMappingRepository.deleteByGameId(gameId);
        return null;
    }

    private Player getPlayerInRoomOrThrow(Long userId, String roomId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        Player player = playerRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(PLAYER_NOT_IN_ANY_ROOM));

        if (!player.getRoomId().equals(roomId)) {
            throw new CustomException(PLAYER_NOT_IN_ROOM);
        }

        return player;
    }

    private Room getRoomOrThrow(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOM_NOT_FOUND));
    }

    private Game getGameInRoomOrThrow(Long gameId, Room room) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(GAME_NOT_FOUND));

        if (!game.getRoomId().equals(room.getRoomId())) {
            throw new CustomException(GAME_NOT_FOUND);
        }

        return game;
    }

    private Quiz getQuizOrThrow(Long quizId) {
        return quizRepository.findById(quizId).orElseThrow(() -> new CustomException(QUIZ_NOT_FOUND));
    }

    private void validateGameInProgress(Room room, Game game) {
        if (!room.isInGame()) {
            throw new CustomException(ROOM_NOT_STARTED);
        }
    }

    private void validateGameNotInProgress(Room room) {
        if (room.isInGame()) {
            throw new CustomException(ROOM_ALREADY_STARTED);
        }
    }
}
