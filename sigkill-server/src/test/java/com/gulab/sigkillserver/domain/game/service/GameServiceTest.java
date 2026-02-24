package com.gulab.sigkillserver.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameResponseType;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.exception.GameErrorCode;
import com.gulab.sigkillserver.domain.game.exception.QuizErrorCode;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import com.gulab.sigkillserver.domain.game.repository.GameMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceRepository;
import com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode;
import com.gulab.sigkillserver.domain.room.exception.RoomErrorCode;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private SelectedChoiceRepository selectedChoiceRepository;
    private QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private GameEventBuilder gameEventBuilder;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        userRepository = new UserMemoryRepository();
        gameRepository = new GameMemoryRepository();
        quizRepository = new QuizMemoryRepository(new ObjectMapper(),
                new ClassPathResource("quiz/quiz.json"));
        playerRepository = new PlayerMemoryRepository();
        roomRepository = new RoomMemoryRepository();
        selectedChoiceRepository = new SelectedChoiceMemoryRepository();
        quizChoiceNumberMappingRepository = new QuizChoiceNumberMappingMemoryRepository();
        gameEventBuilder = new GameEventBuilder();

        gameService = new GameService(
                userRepository,
                gameRepository,
                quizRepository,
                playerRepository,
                roomRepository,
                selectedChoiceRepository,
                quizChoiceNumberMappingRepository,
                gameEventBuilder
        );
    }

    private User saveUser(String sessionId, String nickname) {
        return userRepository.save(User.create(sessionId, nickname, UserRole.GUEST));
    }

    private Room saveRoom(String roomId) {
        Room room = Room.create(roomId, "테스트 방", 1L, 6);
        roomRepository.save(room);
        return room;
    }

    private Game saveGameWithQuizIds(String roomId, int quizCount) {
        List<Long> quizIds = quizRepository.findByCategoryId(GameConstants.DEFAULT_CATEGORY_ID, quizCount)
                .stream()
                .map(Quiz::quizId)
                .toList();
        return gameRepository.save(Game.create(roomId, quizIds));
    }

    private void assertThrowsCustomExceptionWithCode(Runnable call, String code) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .satisfies(throwable ->
                        assertThat(((CustomException) throwable).getErrorCode().getCode()).isEqualTo(code));
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

        @Test
        void 이미_게임이_시작된_방에서_startGame을_호출하면_ROOM_ALREADY_STARTED_예외가_발생한다() {
            // given
            Room room = Room.create("1234", "테스트 방", 1L, 6);
            room.startGame();

            // when then
            assertThatThrownBy(() -> gameService.startGame(room))
                    .isInstanceOf(CustomException.class)
                    .satisfies(throwable -> assertThat(((CustomException) throwable).getErrorCode().getCode())
                            .isEqualTo(RoomErrorCode.ROOM_ALREADY_STARTED.name()));
        }
    }

    @Nested
    class StartQuizTests {
        @Test
        void 퀴즈가_시작되면_퀴즈정보와_번호매핑이_생성된다() {
            // given
            User user = saveUser("session-1", "tester");
            Room room = saveRoom("1234");
            room.startGame();

            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            game.startNextQuiz(Instant.now().toEpochMilli());

            // when
            QuizStartEvent result = gameService.startQuiz(user.getUserId(), room.getRoomId(), game.getGameId());

            // then
            assertThat(result.type()).isEqualTo(GameResponseType.QUIZ_START);
            assertThat(result.roomId()).isEqualTo(room.getRoomId());
            assertThat(result.gameId()).isEqualTo(game.getGameId());

            assertThat(result.payload().quiz().currentQuizIndex()).isEqualTo(2);
            assertThat(result.payload().quiz().totalQuizCount()).isEqualTo(game.getTotalQuizCount());
            assertThat(result.payload().quiz().endTime() - result.payload().quiz().startTime())
                    .isEqualTo(GameConstants.QUIZ_COUNTDOWN_MILLIS);

            Quiz quiz = quizRepository.findById(result.payload().quiz().quizId()).orElseThrow();
            assertThat(result.payload().quiz().choices()).hasSize(quiz.choices().size());
            assertThat(result.payload().quiz().choices())
                    .extracting(choice -> choice.number())
                    .containsExactlyInAnyOrder(1, 2, 3, 4);

            QuizChoiceNumberMapping mapping = quizChoiceNumberMappingRepository
                    .findByGameIdAndQuizId(game.getGameId(), quiz.quizId())
                    .orElseThrow();

            assertThat(mapping.getNumberToChoiceId().keySet()).containsExactlyInAnyOrder(1, 2, 3, 4);
            assertThat(mapping.getNumberToChoiceId().values())
                    .containsExactlyInAnyOrderElementsOf(
                            quiz.choices().stream().map(choice -> choice.choiceId()).toList()
                    );
        }

        @Test
        void 게임이_진행중이지_않은_방에서_퀴즈를_시작하지_못한다() {
            // given
            User user = saveUser("session-2", "tester2");
            Room room = saveRoom("2234");
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);

            // when
            Throwable thrown = catchThrowable(
                    () -> gameService.startQuiz(user.getUserId(), room.getRoomId(), game.getGameId()));

            // then
            assertCustomErrorCode(thrown, RoomErrorCode.ROOM_NOT_STARTED.name());
        }

        @Test
        void 플레이어가_어떤_방에도_없으면_퀴즈를_시작하지_못한다() {
            // given
            User user = saveUser("session-3", "tester3");
            Room room = saveRoom("3234");
            room.startGame();
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);

            // when
            Throwable thrown = catchThrowable(
                    () -> gameService.startQuiz(user.getUserId(), room.getRoomId(), game.getGameId()));

            // then
            assertCustomErrorCode(thrown, PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM.name());
        }

        @Test
        void 플레이어가_다른_방에_있으면_퀴즈를_시작하지_못한다() {
            // given
            User user = saveUser("session-4", "tester4");
            Room room = saveRoom("4234");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), "9999", user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);

            // when
            Throwable thrown = catchThrowable(
                    () -> gameService.startQuiz(user.getUserId(), room.getRoomId(), game.getGameId()));

            // then
            assertCustomErrorCode(thrown, PlayerErrorCode.PLAYER_NOT_IN_ROOM.name());
        }

        @Test
        void 퀴즈_인덱스가_범위를_벗어나면_예외가_발생한다() {
            // given
            User user = saveUser("session-5", "tester5");
            Room room = saveRoom("5234");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);

            // when
            Throwable thrown = catchThrowable(
                    () -> gameService.startQuiz(user.getUserId(), room.getRoomId(), game.getGameId()));

            // then
            assertCustomErrorCode(thrown, QuizErrorCode.QUIZ_INDEX_OUT_OF_BOUNDS.name());
        }

        private Throwable catchThrowable(ThrowingCall call) {
            try {
                call.run();
                return null;
            } catch (Throwable throwable) {
                return throwable;
            }
        }

        private void assertCustomErrorCode(Throwable thrown, String code) {
            assertThat(thrown).isInstanceOf(CustomException.class);
            assertThat(((CustomException) thrown).getErrorCode().getCode()).isEqualTo(code);
        }

        @FunctionalInterface
        private interface ThrowingCall {
            void run();
        }
    }

    @Nested
    class SubmitChoiceTests {
        @Test
        void 퀴즈_시작시_생성된_번호와_실제_choiceId_매핑이_정합하다() {
            // given
            User user = saveUser("submit-map-session-1", "submit-map-user");
            Room room = saveRoom("6134");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 4);

            // startQuiz 사전 조건 충족(현재 구현 기준)
            game.startNextQuiz(Instant.now().toEpochMilli());

            // when
            QuizStartEvent startEvent = gameService.startQuiz(user.getUserId(), room.getRoomId(), game.getGameId());
            Long startedQuizId = startEvent.payload().quiz().quizId();
            QuizChoiceNumberMapping mapping = quizChoiceNumberMappingRepository
                    .findByGameIdAndQuizId(game.getGameId(), startedQuizId)
                    .orElseThrow();
            Quiz startedQuiz = quizRepository.findById(startedQuizId).orElseThrow();

            // then
            Map<Long, String> choiceIdToText = startedQuiz.choices().stream()
                    .collect(Collectors.toMap(choice -> choice.choiceId(), choice -> choice.text()));

            Set<Long> mappedChoiceIds = startEvent.payload().quiz().choices().stream()
                    .map(choiceInfo -> mapping.findChoiceIdByNumber(choiceInfo.number()).orElseThrow())
                    .collect(Collectors.toSet());

            // number -> choiceId -> text가 이벤트의 number/text와 1:1로 일치해야 한다.
            for (var choiceInfo : startEvent.payload().quiz().choices()) {
                Long mappedChoiceId = mapping.findChoiceIdByNumber(choiceInfo.number()).orElseThrow();
                assertThat(choiceIdToText.get(mappedChoiceId)).isEqualTo(choiceInfo.text());
            }

            assertThat(mappedChoiceIds)
                    .containsExactlyInAnyOrderElementsOf(
                            startedQuiz.choices().stream().map(choice -> choice.choiceId()).toList()
                    );
        }

        @Test
        void 퀴즈_시작시_생성된_모든_번호로_submitChoice가_성공한다() {
            // given
            User user = saveUser("submit-map-session-2", "submit-map-user-2");
            Room room = saveRoom("6144");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 4);

            // startQuiz 사전 조건 충족(현재 구현 기준)
            game.startNextQuiz(Instant.now().toEpochMilli());
            QuizStartEvent startEvent = gameService.startQuiz(user.getUserId(), room.getRoomId(), game.getGameId());
            Long startedQuizId = startEvent.payload().quiz().quizId();

            // when then
            for (var choiceInfo : startEvent.payload().quiz().choices()) {
                ChoiceSubmitEvent submitEvent = gameService.submitChoice(
                        user.getUserId(),
                        room.getRoomId(),
                        game.getGameId(),
                        startedQuizId,
                        choiceInfo.number()
                );

                assertThat(submitEvent.type()).isEqualTo(GameResponseType.CHOICE_SUBMIT);
                assertThat(submitEvent.payload().quiz().quizId()).isEqualTo(startedQuizId);
                assertThat(submitEvent.payload().choiceNumber()).isEqualTo(choiceInfo.number());
            }
        }

        @Test
        void 플레이어가_현재_퀴즈의_선택지를_정상적으로_제출한다() {
            // given
            User user = saveUser("submit-session-1", "submitter");
            Room room = saveRoom("6234");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            long currentQuizId = game.startNextQuiz(Instant.now().toEpochMilli());
            Quiz quiz = quizRepository.findById(currentQuizId).orElseThrow();
            Map<Integer, Long> numberToChoiceId = new LinkedHashMap<>();
            for (int i = 0; i < quiz.choices().size(); i++) {
                numberToChoiceId.put(i + 1, quiz.choices().get(i).choiceId());
            }
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(game.getGameId(), quiz.quizId(), numberToChoiceId)
            );

            // when
            ChoiceSubmitEvent result = gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    quiz.quizId(),
                    1
            );

            // then
            assertThat(result.type()).isEqualTo(GameResponseType.CHOICE_SUBMIT);
            assertThat(result.roomId()).isEqualTo(room.getRoomId());
            assertThat(result.gameId()).isEqualTo(game.getGameId());
            assertThat(result.occurredAt()).isPositive();
            assertThat(result.payload().quiz().quizId()).isEqualTo(quiz.quizId());
            assertThat(result.payload().quiz().currentQuizIndex()).isEqualTo(game.getCurrentQuizIndex() + 1);
            assertThat(result.payload().quiz().totalQuizCount()).isEqualTo(game.getTotalQuizCount());
            assertThat(result.payload().actor().userId()).isEqualTo(user.getUserId());
            assertThat(result.payload().actor().nickname()).isEqualTo(user.getNickname());
            assertThat(result.payload().choiceNumber()).isEqualTo(1);
        }

        @Test
        void 플레이어가_현재_퀴즈의_선택지를_여러개_제출한다() {
            // given
            User user = saveUser("submit-session-multi", "submitter-multi");
            Room room = saveRoom("6334");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            long currentQuizId = game.startNextQuiz(Instant.now().toEpochMilli());
            Quiz quiz = quizRepository.findById(currentQuizId).orElseThrow();
            Map<Integer, Long> numberToChoiceId = new LinkedHashMap<>();
            for (int i = 0; i < quiz.choices().size(); i++) {
                numberToChoiceId.put(i + 1, quiz.choices().get(i).choiceId());
            }
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(game.getGameId(), quiz.quizId(), numberToChoiceId)
            );

            // when
            ChoiceSubmitEvent firstSubmit = gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    quiz.quizId(),
                    1
            );
            ChoiceSubmitEvent secondSubmit = gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    quiz.quizId(),
                    2
            );

            // then
            assertThat(firstSubmit.payload().choiceNumber()).isEqualTo(1);
            assertThat(secondSubmit.payload().choiceNumber()).isEqualTo(2);
            assertThat(secondSubmit.payload().quiz().quizId()).isEqualTo(quiz.quizId());
            assertThat(secondSubmit.payload().actor().userId()).isEqualTo(user.getUserId());
        }

        @Test
        void 제출_마감시간이_지나면_선택지를_제출하지_못한다() {
            // given
            User user = saveUser("submit-session-deadline", "submitter-deadline");
            Room room = saveRoom("7334");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);

            long startedAt = Instant.now().toEpochMilli()
                    - GameConstants.QUIZ_COUNTDOWN_MILLIS
                    - GameConstants.QUIZ_ANSWER_ALLOWANCE_MILLIS
                    - 1L;

            long currentQuizId = game.startNextQuiz(startedAt);
            Quiz quiz = quizRepository.findById(currentQuizId).orElseThrow();
            Map<Integer, Long> numberToChoiceId = new LinkedHashMap<>();
            for (int i = 0; i < quiz.choices().size(); i++) {
                numberToChoiceId.put(i + 1, quiz.choices().get(i).choiceId());
            }
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(game.getGameId(), quiz.quizId(), numberToChoiceId)
            );

            // when
            Runnable call = () -> gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    quiz.quizId(),
                    1
            );

            // then
            assertThrowsCustomExceptionWithCode(call, GameErrorCode.SUBMIT_CHOICE_IS_AFTER_DEADLINE.name());
        }

        @Test
        void 게임이_진행중이지_않은_방에서_선택지를_제출하지_못한다() {
            // given
            User user = saveUser("submit-session-2", "submitter2");
            Room room = saveRoom("7234");
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            long currentQuizId = game.startNextQuiz(Instant.now().toEpochMilli());

            // when
            Runnable call = () -> gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    currentQuizId,
                    1
            );

            // then
            assertThrowsCustomExceptionWithCode(call, RoomErrorCode.ROOM_NOT_STARTED.name());
        }

        @Test
        void 현재_퀴즈가_아닌_quizId를_제출하면_예외가_발생한다() {
            // given
            User user = saveUser("submit-session-3", "submitter3");
            Room room = saveRoom("8234");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            long currentQuizId = game.startNextQuiz(Instant.now().toEpochMilli());
            Quiz currentQuiz = quizRepository.findById(currentQuizId).orElseThrow();
            Map<Integer, Long> numberToChoiceId = new LinkedHashMap<>();
            for (int i = 0; i < currentQuiz.choices().size(); i++) {
                numberToChoiceId.put(i + 1, currentQuiz.choices().get(i).choiceId());
            }
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(game.getGameId(), currentQuiz.quizId(), numberToChoiceId)
            );
            Long nonCurrentQuizId = game.getQuizIds().stream()
                    .filter(id -> !id.equals(currentQuizId))
                    .findFirst()
                    .orElseThrow();

            // when
            Runnable call = () -> gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    nonCurrentQuizId,
                    1
            );

            // then
            assertThrowsCustomExceptionWithCode(call, GameErrorCode.SUBMIT_CHOICE_NOT_CURRENT_QUIZ.name());
        }

        @Test
        void 선택지_번호_매핑이_없으면_예외가_발생한다() {
            // given
            User user = saveUser("submit-session-4", "submitter4");
            Room room = saveRoom("9234");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            long currentQuizId = game.startNextQuiz(Instant.now().toEpochMilli());

            // when
            Runnable call = () -> gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    currentQuizId,
                    1
            );

            // then
            assertThrowsCustomExceptionWithCode(call, QuizErrorCode.QUIZ_CHOICE_NUMBER_MAPPING_NOT_FOUND.name());
        }

        @Test
        void 매핑에_없는_선택지_번호를_제출하면_예외가_발생한다() {
            // given
            User user = saveUser("submit-session-5", "submitter5");
            Room room = saveRoom("1034");
            room.startGame();
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            long currentQuizId = game.startNextQuiz(Instant.now().toEpochMilli());
            Quiz quiz = quizRepository.findById(currentQuizId).orElseThrow();
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(
                            game.getGameId(),
                            quiz.quizId(),
                            Map.of(1, quiz.choices().get(0).choiceId())
                    )
            );

            // when
            Runnable call = () -> gameService.submitChoice(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    quiz.quizId(),
                    999
            );

            // then
            assertThrowsCustomExceptionWithCode(call, QuizErrorCode.QUIZ_CHOICE_NUMBER_MAPPING_ERROR.name());
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
