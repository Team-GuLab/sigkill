package com.gulab.sigkillserver.domain.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameLoadEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndReason;
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
import com.gulab.sigkillserver.domain.game.service.GameFlowOrchestrator;
import com.gulab.sigkillserver.domain.game.service.GameService;
import com.gulab.sigkillserver.domain.lock.RoomLockManager;
import com.gulab.sigkillserver.domain.room.dto.shared.PlayerRole;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.exception.RoomErrorCode;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;

class BotOrchestratorTest {

    private UserRepository userRepository;
    private GameRepository gameRepository;
    private QuizRepository quizRepository;
    private PlayerRepository playerRepository;
    private RoomRepository roomRepository;
    private SelectedChoiceRepository selectedChoiceRepository;
    private QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private GamePlayerRepository gamePlayerRepository;
    private RoomLockManager roomLockManager;
    private RoomService roomService;
    private GameService gameService;
    private TaskScheduler botTaskScheduler;
    private SimpMessagingTemplate messagingTemplate;
    private GameFlowOrchestrator gameFlowOrchestrator;
    private WaitingBotRoomCleanupService waitingBotRoomCleanupService;
    private BotUserService botUserService;
    private BotOrchestrator botOrchestrator;

    @BeforeEach
    void setUp() {
        userRepository = new UserMemoryRepository();
        gameRepository = new GameMemoryRepository();
        quizRepository = new QuizMemoryRepository(new ObjectMapper(), new ClassPathResource("quiz/quiz.json"));
        playerRepository = new PlayerMemoryRepository();
        roomRepository = new RoomMemoryRepository();
        selectedChoiceRepository = new SelectedChoiceMemoryRepository();
        quizChoiceNumberMappingRepository = new QuizChoiceNumberMappingMemoryRepository();
        gamePlayerRepository = new GamePlayerMemoryRepository();
        roomLockManager = new RoomLockManager();

        gameService = new GameService(
                userRepository,
                gameRepository,
                quizRepository,
                playerRepository,
                roomRepository,
                selectedChoiceRepository,
                quizChoiceNumberMappingRepository,
                gamePlayerRepository,
                new com.gulab.sigkillserver.domain.game.service.GameEventBuilder(),
                roomLockManager
        );
        roomService = new RoomService(
                roomRepository,
                userRepository,
                playerRepository,
                roomLockManager,
                gameService
        );
        botTaskScheduler = mock(TaskScheduler.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        gameFlowOrchestrator = mock(GameFlowOrchestrator.class);
        waitingBotRoomCleanupService = mock(WaitingBotRoomCleanupService.class);
        botUserService = new BotUserService(userRepository);
        botOrchestrator = new BotOrchestrator(
                botUserService,
                roomService,
                roomRepository,
                playerRepository,
                roomLockManager,
                gameService,
                gameFlowOrchestrator,
                quizRepository,
                quizChoiceNumberMappingRepository,
                gamePlayerRepository,
                waitingBotRoomCleanupService,
                messagingTemplate,
                botTaskScheduler
        );
    }

    private User createGuestUser(String sessionId, String nickname) {
        return userRepository.save(User.create(sessionId, nickname, UserRole.GUEST));
    }

    private String createActiveRoom(User host) {
        String roomId = roomService.createRoom("테스트 방", 6, host.getUserId()).roomId();
        roomService.confirmJoin(roomId, host.getUserId());
        return roomId;
    }

    private User createReadyBot(String roomId) {
        User botUser = botUserService.createBotUser();
        roomService.joinRoom(roomId, botUser.getUserId());
        roomService.confirmJoin(roomId, botUser.getUserId());
        roomService.readyPlayer(roomId, botUser.getUserId());
        return botUser;
    }

    @SuppressWarnings("unchecked")
    private ScheduledFuture<Object> mockScheduledFuture() {
        return (ScheduledFuture<Object>) mock(ScheduledFuture.class);
    }

    @Nested
    class AddBotTests {

        @Test
        void app_room_bot_호출시_PLAYER_JOIN후_지연된_PLAYER_READY를_브로드캐스트한다() {
            // given
            User host = createGuestUser("host-session", "호스트");
            String roomId = createActiveRoom(host);
            List<Runnable> scheduledTasks = new ArrayList<>();
            when(botTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> {
                        scheduledTasks.add(invocation.getArgument(0));
                        return mockScheduledFuture();
                    });

            // when
            botOrchestrator.addBot(roomId, host.getUserId());

            // then
            User botUser = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == UserRole.BOT)
                    .findFirst()
                    .orElseThrow();
            Player botPlayer = playerRepository.findById(botUser.getUserId()).orElseThrow();
            assertThat(botPlayer.isActive()).isTrue();
            assertThat(botPlayer.isReady()).isFalse();

            scheduledTasks.getFirst().run();

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/room/" + roomId), eventCaptor.capture());
            assertThat(eventCaptor.getAllValues().get(0)).isInstanceOf(PlayerJoinEvent.class);
            assertThat(eventCaptor.getAllValues().get(1)).isInstanceOf(PlayerReadyEvent.class);

            Player updatedBotPlayer = playerRepository.findById(botUser.getUserId()).orElseThrow();
            assertThat(updatedBotPlayer.isReady()).isTrue();
        }

        @Test
        void 방장이_아닌_사용자는_봇을_추가할_수_없고_봇_데이터도_남지_않는다() {
            // given
            User host = createGuestUser("host-session", "호스트");
            User guest = createGuestUser("guest-session", "게스트");
            String roomId = createActiveRoom(host);
            roomService.joinRoom(roomId, guest.getUserId());
            roomService.confirmJoin(roomId, guest.getUserId());
            int userCountBefore = userRepository.findAll().size();
            int playerCountBefore = playerRepository.countByRoomId(roomId);

            // when then
            assertThatThrownBy(() -> botOrchestrator.addBot(roomId, guest.getUserId()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(throwable -> assertThat(((CustomException) throwable).getErrorCode().getCode())
                            .isEqualTo(RoomErrorCode.ONLY_HOST_CAN_ADD_BOT.name()));

            assertThat(userRepository.findAll()).hasSize(userCountBefore);
            assertThat(playerRepository.countByRoomId(roomId)).isEqualTo(playerCountBefore);
        }

        @Test
        void roomId가_4자리_정수가_아니면_ROOM_NUMBER_ERROR를_반환하고_봇_데이터를_남기지_않는다() {
            // given
            User host = createGuestUser("host-session", "호스트");
            createActiveRoom(host);
            int userCountBefore = userRepository.findAll().size();

            // when then
            assertThatThrownBy(() -> botOrchestrator.addBot("abc", host.getUserId()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(throwable -> assertThat(((CustomException) throwable).getErrorCode().getCode())
                            .isEqualTo(RoomErrorCode.ROOM_NUMBER_ERROR.name()));

            assertThatThrownBy(() -> botOrchestrator.addBot("999", host.getUserId()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(throwable -> assertThat(((CustomException) throwable).getErrorCode().getCode())
                            .isEqualTo(RoomErrorCode.ROOM_NUMBER_ERROR.name()));

            assertThat(userRepository.findAll()).hasSize(userCountBefore);
            assertThat(userRepository.findAll())
                    .allMatch(user -> user.getRole() != UserRole.BOT);
        }

        @Test
        void closing_상태_방에는_봇을_추가하지_않고_ROOM_CLOSING으로_실패한다() {
            // given
            User host = createGuestUser("host-session", "호스트");
            String roomId = createActiveRoom(host);
            roomRepository.findById(roomId).orElseThrow().markClosing();
            int userCountBefore = userRepository.findAll().size();
            int playerCountBefore = playerRepository.countByRoomId(roomId);

            // when then
            assertThatThrownBy(() -> botOrchestrator.addBot(roomId, host.getUserId()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(throwable -> assertThat(((CustomException) throwable).getErrorCode().getCode())
                            .isEqualTo(RoomErrorCode.ROOM_CLOSING.name()));

            assertThat(userRepository.findAll()).hasSize(userCountBefore);
            assertThat(playerRepository.countByRoomId(roomId)).isEqualTo(playerCountBefore);
            assertThat(userRepository.findAll())
                    .allMatch(user -> user.getRole() != UserRole.BOT);
        }
    }

    @Nested
    class GameHookTests {

        @Test
        void 마지막_사람이_나가도_봇_loadGame_호출만으로_allLoaded가_true가된다() {
            // given
            User host = createGuestUser("host-session", "호스트");
            String roomId = createActiveRoom(host);
            User botUser = createReadyBot(roomId);
            List<Runnable> scheduledTasks = new ArrayList<>();
            when(botTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> {
                        scheduledTasks.add(invocation.getArgument(0));
                        return mockScheduledFuture();
                    });

            GameStartEvent gameStartEvent = roomService.startGame(roomId, host.getUserId());

            // when
            botOrchestrator.onGameStarted(gameStartEvent);
            playerRepository.deleteById(host.getUserId());
            scheduledTasks.getFirst().run();

            // then
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameStartEvent.gameId()), eventCaptor.capture());
            GameLoadEvent gameLoadEvent = (GameLoadEvent) eventCaptor.getValue();
            assertThat(gameLoadEvent.payload().allLoaded()).isTrue();
            assertThat(gameLoadEvent.payload().players())
                    .extracting(player -> player.userId())
                    .containsExactly(botUser.getUserId());
            verify(gameFlowOrchestrator).onAllPlayersLoaded(roomId, gameStartEvent.gameId());
        }

        @Test
        void QUIZ_START후_살아있는_봇은_지연_후_기존_submitChoice를_호출한다() {
            // given
            User host = createGuestUser("host-session", "호스트");
            String roomId = createActiveRoom(host);
            User botUser = createReadyBot(roomId);
            GameStartEvent gameStartEvent = roomService.startGame(roomId, host.getUserId());
            QuizStartEvent quizStartEvent = gameService.startQuiz(host.getUserId(), roomId, gameStartEvent.gameId());

            List<Runnable> scheduledTasks = new ArrayList<>();
            List<Instant> scheduledInstants = new ArrayList<>();
            when(botTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> {
                        scheduledTasks.add(invocation.getArgument(0));
                        scheduledInstants.add(invocation.getArgument(1));
                        return mockScheduledFuture();
                    });

            // when
            botOrchestrator.onQuizStarted(quizStartEvent);
            scheduledTasks.getFirst().run();

            // then
            long scheduledAtMillis = scheduledInstants.getFirst().toEpochMilli();
            assertThat(scheduledAtMillis)
                    .isBetween(
                            quizStartEvent.payload().quiz().startTime() + 2_000L,
                            quizStartEvent.payload().quiz().endTime() - 200L
                    );

            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/game/" + gameStartEvent.gameId()), eventCaptor.capture());
            ChoiceSubmitEvent choiceSubmitEvent = (ChoiceSubmitEvent) eventCaptor.getValue();
            assertThat(choiceSubmitEvent.payload().actor().userId()).isEqualTo(botUser.getUserId());
            assertThat(selectedChoiceRepository.findByGameIdAndQuizId(
                    gameStartEvent.gameId(),
                    quizStartEvent.payload().quiz().quizId()
            )).hasSize(1);
        }

        @Test
        void GAME_END후_사람이_남아있으면_봇이_정상_PLAYER_READY를_브로드캐스트한다() {
            // given
            User host = createGuestUser("host-session", "호스트");
            String roomId = createActiveRoom(host);
            User botUser = createReadyBot(roomId);
            playerRepository.findById(botUser.getUserId()).orElseThrow().unready();

            List<Runnable> scheduledTasks = new ArrayList<>();
            when(botTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> {
                        scheduledTasks.add(invocation.getArgument(0));
                        return mockScheduledFuture();
                    });

            GameEndEvent gameEndEvent = GameEndEvent.of(
                    roomId,
                    1L,
                    Instant.now().toEpochMilli(),
                    new GameEndPayload(GameEndReason.ONE_SURVIVOR, List.of())
            );

            // when
            botOrchestrator.onGameEnded(gameEndEvent);
            scheduledTasks.getFirst().run();

            // then
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId), eventCaptor.capture());
            PlayerReadyEvent playerReadyEvent = (PlayerReadyEvent) eventCaptor.getValue();
            assertThat(playerReadyEvent.player().userId()).isEqualTo(botUser.getUserId());
            assertThat(playerReadyEvent.player().role()).isEqualTo(PlayerRole.GUEST);
            assertThat(playerRepository.findById(botUser.getUserId()).orElseThrow().isReady()).isTrue();
        }
    }
}
