package com.gulab.sigkillserver.domain.game.service;

import static com.gulab.sigkillserver.domain.game.exception.GameErrorCode.GAME_NOT_FOUND;
import static com.gulab.sigkillserver.domain.game.exception.GameErrorCode.GAME_NOT_IN_PROGRESS;
import static com.gulab.sigkillserver.domain.game.exception.GameErrorCode.INVALID_GAME_STATE;
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
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartQuizInfo;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.util.List;
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

        return GameStartEvent.of(room.getRoomId(), game.getGameId(),
                new GameStartPayload(new GameStartQuizInfo(0, quizIds.size()))
        );
    }

    public QuizStartEvent startQuiz(Long userId, String roomId, Long gameId) {
        Player player = getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);

        return null;
    }

    public ChoiceSubmitEvent submitChoice(Long userId, String roomId, Long gameId, Long quizId, Integer choiceNumber) {
        Player player = getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);

        return null;
    }

    public QuizEndEvent endQuiz(Long userId, String roomId, Long gameId, Long quizId) {
        Player player = getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);

        return null;
    }

    public GameEndEvent endGame(Long userId, String roomId, Long gameId) {
        Player player = getPlayerInRoomOrThrow(userId, roomId);
        Room room = getRoomOrThrow(roomId);
        Game game = getGameInRoomOrThrow(gameId, room);
        validateGameInProgress(room, game);

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
        if (room.isInGame() != game.isInProgress()) {
            throw new CustomException(INVALID_GAME_STATE);
        }
        if (!room.isInGame()) {
            throw new CustomException(ROOM_NOT_STARTED);
        }
        if (!game.isInProgress()) {
            throw new CustomException(GAME_NOT_IN_PROGRESS);
        }
    }

    private void validateGameNotInProgress(Room room) {
        if (room.isInGame()) {
            throw new CustomException(ROOM_ALREADY_STARTED);
        }
    }
}
