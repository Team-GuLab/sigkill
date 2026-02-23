package com.gulab.sigkillserver.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameResponseType;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.repository.GameMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.room.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GameServiceTest {

    private GameRepository gameRepository;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameRepository = new GameMemoryRepository();
        QuizRepository quizRepository = new QuizMemoryRepository(new ObjectMapper(), new ClassPathResource("quiz/quiz.json"));
        gameService = new GameService(gameRepository, quizRepository);
    }

    @Nested
    class StartGameTests {
        @Test
        void 메모리_저장소_기반으로_GAME_START를_생성하고_게임을_저장한다() {
            // given
            Room room = Room.create("1234", "테스트 방", 1L, 6);

            // when
            GameStartEvent result = gameService.startGame(room);

            // then
            assertThat(result.type()).isEqualTo(GameResponseType.GAME_START);
            assertThat(result.roomId()).isEqualTo("1234");
            assertThat(result.gameId()).isNotNull();
            assertThat(result.payload().quiz().currentQuizIndex()).isZero();
            assertThat(result.payload().quiz().totalQuizCount()).isPositive();
            assertThat(room.isInGame()).isTrue();

            Game savedGame = gameRepository.findByRoomId("1234").orElseThrow();
            assertThat(savedGame.getGameId()).isEqualTo(result.gameId());
            assertThat(savedGame.getRoomId()).isEqualTo("1234");
            assertThat(savedGame.getQuizIds()).hasSize(result.payload().quiz().totalQuizCount());
        }
    }
}
