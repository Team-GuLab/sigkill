package com.gulab.sigkillserver.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameResponseType;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoice;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class GameServiceTest {

    private GameRepository gameRepository;
    private QuizRepository quizRepository;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameRepository = Mockito.mock(GameRepository.class);
        quizRepository = Mockito.mock(QuizRepository.class);
        gameService = new GameService(gameRepository, quizRepository);
    }

    @Nested
    class StartGameTests {
        @Test
        void 저장된_gameId로_GAME_START를_생성하고_방상태를_INGAME으로_변경한다() {
            // given
            Room room = Room.create("1234", "테스트 방", 1L, 6);
            List<Quiz> quizzes = List.of(
                    new Quiz(11L, "CS", "q1", "e1", 101L, 1, List.of(new QuizChoice(101L, "a1"))),
                    new Quiz(12L, "CS", "q2", "e2", 201L, 2, List.of(new QuizChoice(201L, "a2")))
            );
            when(quizRepository.findByCategoryId(GameConstants.DEFAULT_CATEGORY_ID, GameConstants.QUIZ_COUNT))
                    .thenReturn(quizzes);
            when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
                Game unsaved = invocation.getArgument(0);
                return unsaved.withGameId(77L);
            });

            // when
            GameStartEvent result = gameService.startGame(room);

            // then
            assertThat(result.type()).isEqualTo(GameResponseType.GAME_START);
            assertThat(result.roomId()).isEqualTo("1234");
            assertThat(result.gameId()).isEqualTo(77L);
            assertThat(result.payload().quiz().currentQuizIndex()).isZero();
            assertThat(result.payload().quiz().totalQuizCount()).isEqualTo(2);
            assertThat(room.isInGame()).isTrue();

            ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
            verify(gameRepository).save(captor.capture());
            Game savedTarget = captor.getValue();
            assertThat(savedTarget.getRoomId()).isEqualTo("1234");
            assertThat(savedTarget.getQuizIds()).containsExactly(11L, 12L);
            assertThat(savedTarget.getGameId()).isNull();
        }
    }
}
