package com.gulab.sigkillserver.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.EndQuizOrGameEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameResponseType;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndReason;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPlayerInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizResult;
import com.gulab.sigkillserver.domain.game.exception.GameErrorCode;
import com.gulab.sigkillserver.domain.game.exception.QuizErrorCode;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.model.GamePlayerStatus;
import com.gulab.sigkillserver.domain.game.model.SelectedChoice;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import com.gulab.sigkillserver.domain.game.repository.GameMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerRepository;
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
    private GamePlayerRepository gamePlayerRepository;
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
        gamePlayerRepository = new GamePlayerMemoryRepository();
        gameEventBuilder = new GameEventBuilder();

        gameService = new GameService(
                userRepository,
                gameRepository,
                quizRepository,
                playerRepository,
                roomRepository,
                selectedChoiceRepository,
                quizChoiceNumberMappingRepository,
                gamePlayerRepository,
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
            User user1 = saveUser("start-game-session-1", "start-game-user-1");
            User user2 = saveUser("start-game-session-2", "start-game-user-2");
            playerRepository.create(Player.create(user1.getUserId(), room.getRoomId(), user1.getNickname()));
            playerRepository.create(Player.create(user2.getUserId(), room.getRoomId(), user2.getNickname()));

            // when
            GameStartEvent result = gameService.startGame(room);

            // then
            assertThat(result.type()).isEqualTo(GameResponseType.GAME_START);
            assertThat(result.roomId()).isEqualTo("1234");
            assertThat(result.gameId()).isNotNull();
            assertThat(result.payload().quiz().currentQuizIndex()).isZero();
            assertThat(result.payload().quiz().totalQuizCount()).isPositive();
            assertThat(result.payload().players()).hasSize(2);
            assertThat(result.payload().players())
                    .extracting(actor -> actor.userId())
                    .containsExactlyInAnyOrder(user1.getUserId(), user2.getUserId());
            assertThat(result.payload().players())
                    .extracting(actor -> actor.nickname())
                    .containsExactlyInAnyOrder(user1.getNickname(), user2.getNickname());
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
        void 플레이어별_제출결과대로_정답_오답_미제출이_채점된다() {
            // given
            EndQuizFixture fixture = prepareEndQuizFixture("2034");
            gameService.submitChoice(
                    fixture.host().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    fixture.quiz().quizId(),
                    fixture.correctNumber()
            );
            gameService.submitChoice(
                    fixture.secondPlayer().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    fixture.quiz().quizId(),
                    fixture.wrongNumber()
            );

            // when
            QuizEndEvent result = gameService.endQuiz(
                    fixture.host().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    fixture.quiz().quizId()
            ).quizEndEvent();

            // then
            assertThat(result.type()).isEqualTo(GameResponseType.QUIZ_END);
            assertThat(result.payload().quiz().quizId()).isEqualTo(fixture.quiz().quizId());
            assertThat(result.payload().answer().correctChoiceNumber()).isEqualTo(fixture.correctNumber());

            Map<Long, QuizEndPlayerInfo> playersByUserId = result.payload().players().stream()
                    .collect(Collectors.toMap(QuizEndPlayerInfo::userId, info -> info));
            assertThat(playersByUserId).hasSize(3);

            assertThat(playersByUserId.get(fixture.host().getUserId()).status()).isEqualTo(GamePlayerStatus.ALIVE);
            assertThat(playersByUserId.get(fixture.host().getUserId()).quizResult()).isEqualTo(QuizResult.CORRECT);
            assertThat(playersByUserId.get(fixture.host().getUserId()).score()).isEqualTo(1);

            assertThat(playersByUserId.get(fixture.secondPlayer().getUserId()).status()).isEqualTo(
                    GamePlayerStatus.DEAD);
            assertThat(playersByUserId.get(fixture.secondPlayer().getUserId()).quizResult()).isEqualTo(
                    QuizResult.WRONG);
            assertThat(playersByUserId.get(fixture.secondPlayer().getUserId()).score()).isZero();

            assertThat(playersByUserId.get(fixture.thirdPlayer().getUserId()).status()).isEqualTo(
                    GamePlayerStatus.DEAD);
            assertThat(playersByUserId.get(fixture.thirdPlayer().getUserId()).quizResult())
                    .isEqualTo(QuizResult.NO_SUBMISSION);
            assertThat(playersByUserId.get(fixture.thirdPlayer().getUserId()).score()).isZero();
        }

        @Test
        void 같은_플레이어가_여러번_제출하면_마지막_제출로_채점된다() {
            // given
            EndQuizFixture fixture = prepareEndQuizFixture("2134");
            gameService.submitChoice(
                    fixture.host().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    fixture.quiz().quizId(),
                    fixture.wrongNumber()
            );
            gameService.submitChoice(
                    fixture.host().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    fixture.quiz().quizId(),
                    fixture.correctNumber()
            );

            // when
            QuizEndEvent result = gameService.endQuiz(
                    fixture.host().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    fixture.quiz().quizId()
            ).quizEndEvent();

            // then
            assertThat(result.payload().players())
                    .filteredOn(player -> player.userId().equals(fixture.host().getUserId()))
                    .singleElement()
                    .satisfies(player -> {
                        assertThat(player.status()).isEqualTo(GamePlayerStatus.ALIVE);
                        assertThat(player.quizResult()).isEqualTo(QuizResult.CORRECT);
                        assertThat(player.score()).isEqualTo(1);
                    });
        }

        @Test
        void 게임이_진행중이지_않은_방에서_퀴즈를_종료하지_못한다() {
            // given
            User user = saveUser("end-quiz-not-started-session", "end-quiz-not-started");
            Room room = saveRoom("2234");
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);
            Long quizId = game.getQuizIds().get(0);

            // when
            Runnable call = () -> gameService.endQuiz(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    quizId
            );

            // then
            assertThrowsCustomExceptionWithCode(call, RoomErrorCode.ROOM_NOT_STARTED.name());
        }

        @Test
        void 게임이_종료된_방에서_퀴즈를_종료하지_못한다() {
            // given
            EndQuizFixture fixture = prepareEndQuizFixture("2334");
            fixture.room().endGame();

            // when
            Runnable call = () -> gameService.endQuiz(
                    fixture.host().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    fixture.quiz().quizId()
            );

            // then
            assertThrowsCustomExceptionWithCode(call, RoomErrorCode.ROOM_NOT_STARTED.name());
        }

        @Test
        void 현재_퀴즈가_아닌_quizId로_퀴즈를_종료하지_못한다() {
            // given
            EndQuizFixture fixture = prepareEndQuizFixture("2434");
            Long nonCurrentQuizId = fixture.game().getQuizIds().stream()
                    .filter(id -> !id.equals(fixture.quiz().quizId()))
                    .findFirst()
                    .orElseThrow();

            // when
            Runnable call = () -> gameService.endQuiz(
                    fixture.host().getUserId(),
                    fixture.room().getRoomId(),
                    fixture.game().getGameId(),
                    nonCurrentQuizId
            );

            // then
            assertThrowsCustomExceptionWithCode(call, GameErrorCode.SUBMIT_CHOICE_NOT_CURRENT_QUIZ.name());
        }

        @Test
        void 마지막_퀴즈가_끝나면_생존자가_여러명이어도_endGame을_호출한다() {
            // given
            User host = saveUser("last-quiz-host-session", "last-quiz-host");
            User second = saveUser("last-quiz-second-session", "last-quiz-second");
            User third = saveUser("last-quiz-third-session", "last-quiz-third");
            Room room = Room.create("2454", "테스트 방", host.getUserId(), 6);
            roomRepository.save(room);
            playerRepository.create(Player.create(host.getUserId(), room.getRoomId(), host.getNickname()));
            playerRepository.create(Player.create(second.getUserId(), room.getRoomId(), second.getNickname()));
            playerRepository.create(Player.create(third.getUserId(), room.getRoomId(), third.getNickname()));
            gameService.startGame(room);

            Game game = gameRepository.findByRoomId(room.getRoomId()).orElseThrow();
            long lastQuizId = -1L;
            for (int i = 0; i < game.getTotalQuizCount(); i++) {
                lastQuizId = game.startNextQuiz(Instant.now().toEpochMilli());
            }
            Quiz lastQuiz = quizRepository.findById(lastQuizId).orElseThrow();
            Map<Integer, Long> numberToChoiceId = createNumberToChoiceIdMap(lastQuiz);
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(game.getGameId(), lastQuiz.quizId(), numberToChoiceId)
            );
            int correctNumber = findChoiceNumber(numberToChoiceId, lastQuiz.correctChoiceId());

            GameService spyGameService = spy(gameService);
            spyGameService.submitChoice(host.getUserId(), room.getRoomId(), game.getGameId(), lastQuiz.quizId(), correctNumber);
            spyGameService.submitChoice(second.getUserId(), room.getRoomId(), game.getGameId(), lastQuiz.quizId(), correctNumber);
            spyGameService.submitChoice(third.getUserId(), room.getRoomId(), game.getGameId(), lastQuiz.quizId(), correctNumber);

            // when
            EndQuizOrGameEvent result = spyGameService.endQuiz(
                    host.getUserId(),
                    room.getRoomId(),
                    game.getGameId(),
                    lastQuiz.quizId()
            );

            // then
            verify(spyGameService).endGame(host.getUserId(), room.getRoomId(), game.getGameId());
            assertThat(result.quizEndEvent()).isNotNull();
        }

        private EndQuizFixture prepareEndQuizFixture(String roomId) {
            User host = saveUser(roomId + "-host-session", roomId + "-host");
            User secondPlayer = saveUser(roomId + "-second-session", roomId + "-second");
            User thirdPlayer = saveUser(roomId + "-third-session", roomId + "-third");

            Room room = Room.create(roomId, "테스트 방", host.getUserId(), 6);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), room.getRoomId(), host.getNickname()));
            playerRepository.create(
                    Player.create(secondPlayer.getUserId(), room.getRoomId(), secondPlayer.getNickname()));
            playerRepository.create(
                    Player.create(thirdPlayer.getUserId(), room.getRoomId(), thirdPlayer.getNickname()));

            gameService.startGame(room);

            Game game = gameRepository.findByRoomId(roomId).orElseThrow();
            long quizId = game.startNextQuiz(Instant.now().toEpochMilli());
            Quiz quiz = quizRepository.findById(quizId).orElseThrow();

            Map<Integer, Long> numberToChoiceId = createNumberToChoiceIdMap(quiz);
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(game.getGameId(), quiz.quizId(), numberToChoiceId)
            );

            int correctNumber = findChoiceNumber(numberToChoiceId, quiz.correctChoiceId());
            int wrongNumber = findWrongChoiceNumber(numberToChoiceId, quiz.correctChoiceId());

            return new EndQuizFixture(host, secondPlayer, thirdPlayer, room, game, quiz, correctNumber, wrongNumber);
        }

        private Map<Integer, Long> createNumberToChoiceIdMap(Quiz quiz) {
            Map<Integer, Long> numberToChoiceId = new LinkedHashMap<>();
            for (int i = 0; i < quiz.choices().size(); i++) {
                numberToChoiceId.put(i + 1, quiz.choices().get(i).choiceId());
            }
            return numberToChoiceId;
        }

        private int findChoiceNumber(Map<Integer, Long> numberToChoiceId, long choiceId) {
            return numberToChoiceId.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(choiceId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow();
        }

        private int findWrongChoiceNumber(Map<Integer, Long> numberToChoiceId, long correctChoiceId) {
            return numberToChoiceId.entrySet().stream()
                    .filter(entry -> !entry.getValue().equals(correctChoiceId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow();
        }

        private record EndQuizFixture(
                User host,
                User secondPlayer,
                User thirdPlayer,
                Room room,
                Game game,
                Quiz quiz,
                int correctNumber,
                int wrongNumber
        ) {
        }
    }

    @Nested
    class EndGameTests {
        @Test
        void 게임이_종료된다() {
            // given
            User host = saveUser("end-game-host-session", "end-game-host");
            User second = saveUser("end-game-second-session", "end-game-second");
            User third = saveUser("end-game-third-session", "end-game-third");
            Room room = Room.create("2534", "테스트 방", host.getUserId(), 6);
            roomRepository.save(room);
            playerRepository.create(Player.create(host.getUserId(), room.getRoomId(), host.getNickname()));
            playerRepository.create(Player.create(second.getUserId(), room.getRoomId(), second.getNickname()));
            playerRepository.create(Player.create(third.getUserId(), room.getRoomId(), third.getNickname()));
            gameService.startGame(room);

            Game game = gameRepository.findByRoomId(room.getRoomId()).orElseThrow();
            List<Long> quizIds = game.getQuizIds();
            Long quizId = quizIds.get(0);
            quizChoiceNumberMappingRepository.save(
                    QuizChoiceNumberMapping.create(game.getGameId(), quizId, Map.of(1, 1L))
            );
            selectedChoiceRepository.save(
                    SelectedChoice.create(game.getGameId(), quizId, host.getUserId(), 1L, Instant.now().toEpochMilli())
            );

            gamePlayerRepository.getByGameId(game.getGameId()).forEach(gp -> {
                if (gp.getUserId() == host.getUserId()) {
                    gp.addScore(5);
                    return;
                }
                if (gp.getUserId() == second.getUserId()) {
                    gp.addScore(2);
                    gp.kill();
                    return;
                }
                gp.addScore(1);
                gp.kill();
            });

            // when
            GameEndEvent result = gameService.endGame(host.getUserId(), room.getRoomId(), game.getGameId());

            // then
            assertThat(result.type()).isEqualTo(GameResponseType.GAME_END);
            assertThat(result.roomId()).isEqualTo(room.getRoomId());
            assertThat(result.gameId()).isEqualTo(game.getGameId());
            assertThat(result.payload().reason()).isEqualTo(GameEndReason.ONE_SURVIVOR);
            assertThat(result.payload().rankings())
                    .extracting(ranking -> ranking.userId())
                    .containsExactly(host.getUserId(), second.getUserId(), third.getUserId());

            assertThat(room.isInGame()).isFalse();
            assertThat(gameRepository.findById(game.getGameId())).isEmpty();
            assertThat(gamePlayerRepository.getByGameId(game.getGameId())).isEmpty();
            assertThat(quizChoiceNumberMappingRepository.findByGameIdAndQuizId(game.getGameId(), quizId)).isEmpty();
            assertThat(selectedChoiceRepository.findByGameIdAndQuizId(game.getGameId(), quizId)).isEmpty();
        }

        @Test
        void 게임이_진행중이지_않은_방에서_게임을_종료하지_못한다() {
            // given
            User user = saveUser("end-game-not-started-session", "end-game-not-started");
            Room room = saveRoom("2634");
            playerRepository.create(Player.create(user.getUserId(), room.getRoomId(), user.getNickname()));
            Game game = saveGameWithQuizIds(room.getRoomId(), 3);

            // when
            Runnable call = () -> gameService.endGame(
                    user.getUserId(),
                    room.getRoomId(),
                    game.getGameId()
            );

            // then
            assertThrowsCustomExceptionWithCode(call, RoomErrorCode.ROOM_NOT_STARTED.name());
        }

        @Test
        void 게임이_종료된_방에서_게임을_종료하지_못한다() {
            // given
            User host = saveUser("end-game-finished-host-session", "end-game-finished-host");
            User second = saveUser("end-game-finished-second-session", "end-game-finished-second");
            Room room = Room.create("2734", "테스트 방", host.getUserId(), 6);
            roomRepository.save(room);
            playerRepository.create(Player.create(host.getUserId(), room.getRoomId(), host.getNickname()));
            playerRepository.create(Player.create(second.getUserId(), room.getRoomId(), second.getNickname()));
            gameService.startGame(room);
            Game game = gameRepository.findByRoomId(room.getRoomId()).orElseThrow();
            room.endGame();

            // when
            Runnable call = () -> gameService.endGame(
                    host.getUserId(),
                    room.getRoomId(),
                    game.getGameId()
            );

            // then
            assertThrowsCustomExceptionWithCode(call, RoomErrorCode.ROOM_NOT_STARTED.name());
        }

        @Test
        void 생존자가_둘_이상이고_마지막_퀴즈이면_QUIZ_EXHAUSTED_사유로_종료된다() {
            // given
            User host = saveUser("end-game-exhaust-host-session", "end-game-exhaust-host");
            User second = saveUser("end-game-exhaust-second-session", "end-game-exhaust-second");
            Room room = Room.create("2834", "테스트 방", host.getUserId(), 6);
            roomRepository.save(room);
            playerRepository.create(Player.create(host.getUserId(), room.getRoomId(), host.getNickname()));
            playerRepository.create(Player.create(second.getUserId(), room.getRoomId(), second.getNickname()));
            gameService.startGame(room);
            Game game = gameRepository.findByRoomId(room.getRoomId()).orElseThrow();

            for (int i = 0; i < game.getTotalQuizCount(); i++) {
                game.startNextQuiz(Instant.now().toEpochMilli());
            }

            // when
            GameEndEvent result = gameService.endGame(host.getUserId(), room.getRoomId(), game.getGameId());

            // then
            assertThat(result.payload().reason()).isEqualTo(GameEndReason.QUIZ_EXHAUSTED);
        }

        @Test
        void 종료_조건이_충족되지_않으면_게임을_종료하지_못한다() {
            // given
            User host = saveUser("end-game-guard-host-session", "end-game-guard-host");
            User second = saveUser("end-game-guard-second-session", "end-game-guard-second");
            Room room = Room.create("2844", "테스트 방", host.getUserId(), 6);
            roomRepository.save(room);
            playerRepository.create(Player.create(host.getUserId(), room.getRoomId(), host.getNickname()));
            playerRepository.create(Player.create(second.getUserId(), room.getRoomId(), second.getNickname()));
            gameService.startGame(room);
            Game game = gameRepository.findByRoomId(room.getRoomId()).orElseThrow();

            // when
            Runnable call = () -> gameService.endGame(host.getUserId(), room.getRoomId(), game.getGameId());

            // then
            assertThrowsCustomExceptionWithCode(call, GameErrorCode.GAME_IN_PROGRESS.name());
        }

        @Test
        void 동점자는_같은_rank를_부여한다() {
            // given
            User host = saveUser("end-game-tie-host-session", "end-game-tie-host");
            User second = saveUser("end-game-tie-second-session", "end-game-tie-second");
            User third = saveUser("end-game-tie-third-session", "end-game-tie-third");
            Room room = Room.create("2934", "테스트 방", host.getUserId(), 6);
            roomRepository.save(room);
            playerRepository.create(Player.create(host.getUserId(), room.getRoomId(), host.getNickname()));
            playerRepository.create(Player.create(second.getUserId(), room.getRoomId(), second.getNickname()));
            playerRepository.create(Player.create(third.getUserId(), room.getRoomId(), third.getNickname()));
            gameService.startGame(room);
            Game game = gameRepository.findByRoomId(room.getRoomId()).orElseThrow();

            gamePlayerRepository.getByGameId(game.getGameId()).forEach(gp -> {
                if (gp.getUserId() == host.getUserId()) {
                    gp.addScore(5);
                    return;
                }
                if (gp.getUserId() == second.getUserId()) {
                    gp.addScore(5);
                    gp.kill();
                    return;
                }
                gp.addScore(1);
                gp.kill();
            });

            // when
            GameEndEvent result = gameService.endGame(host.getUserId(), room.getRoomId(), game.getGameId());

            // then
            assertThat(result.payload().rankings())
                    .extracting(ranking -> ranking.rank())
                    .containsExactly(1, 1, 3);
        }
    }
}
