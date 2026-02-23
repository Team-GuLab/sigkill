package com.gulab.sigkillserver.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameResponseType;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.repository.GameMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GameServiceTest {

    private UserRepository userRepository;
    private GameRepository gameRepository;
    private QuizRepository quizRepository;
    private PlayerRepository playerRepository;
    private RoomRepository roomRepository;
    private QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        userRepository = new UserMemoryRepository();
        gameRepository = new GameMemoryRepository();
        quizRepository = new QuizMemoryRepository(new ObjectMapper(),
                new ClassPathResource("quiz/quiz.json"));
        playerRepository = new PlayerMemoryRepository();
        roomRepository = new RoomMemoryRepository();
        quizChoiceNumberMappingRepository = new QuizChoiceNumberMappingMemoryRepository();

        gameRepository = new GameMemoryRepository();
        QuizRepository quizRepository = new QuizMemoryRepository(new ObjectMapper(),
                new ClassPathResource("quiz/quiz.json"));
        gameService = new GameService(
                userRepository,
                gameRepository,
                quizRepository,
                playerRepository,
                roomRepository,
                quizChoiceNumberMappingRepository
        );
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

    @Nested
    class StartQuizTests {
        @Test
        void 퀴즈가_시작된다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_진행중이지_않은_방에서_퀴즈를_시작하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_종료된_방에서_퀴즈를_시작하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 모든_퀴즈가_끝난_방에서_퀴즈를_시작하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 플레이어가_1명_이하인_게임에서_퀴즈를_시작하지_못한다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class SubmitChoiceTests {
        @Test
        void 플레이어가_선택지를_제출한다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_진행중이지_않은_방에서_선택지를_제출하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_종료된_방에서_선택지를_제출하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 모든_퀴즈가_끝난_방에서_선택지를_제출하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 플레이어가_1명_이하인_게임에서_선택지를_제출하지_못한다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class EndQuizTests {
        @Test
        void 퀴즈가_종료된다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_진행중이지_않은_방에서_퀴즈를_종료하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_종료된_방에서_퀴즈를_종료하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 모든_퀴즈가_끝난_방에서_퀴즈를_종료하지_못한다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class EndGameTests {
        @Test
        void 게임이_종료된다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_진행중이지_않은_방에서_게임을_종료하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 게임이_종료된_방에서_게임을_종료하지_못한다() {
            // given

            // when

            // then
        }

        @Test
        void 모든_퀴즈가_끝난_방에서_게임을_종료하지_못한다() {
            // given

            // when

            // then
        }
    }
}
