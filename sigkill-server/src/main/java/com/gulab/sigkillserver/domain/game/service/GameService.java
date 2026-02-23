package com.gulab.sigkillserver.domain.game.service;

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
import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final QuizRepository quizRepository;

    /**
     * 게임 시작. RoomService 에서 호출
     */
    public GameStartEvent startGame(Room room) {
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

    public QuizStartEvent startQuiz(String roomId) {
        return null;
    }

    public ChoiceSubmitEvent submitChoice(Long gameId, Long quizId, Integer choiceNumber) {
        return null;
    }

    public QuizEndEvent endQuiz(Long gameId, Long quizId) {
        return null;
    }

    public GameEndEvent endGame(Long gameId) {
        return null;
    }
}
