package com.gulab.sigkillserver.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.domain.game.repository.GameMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceMemoryRepository;
import com.gulab.sigkillserver.domain.game.service.GameEventBuilder;
import com.gulab.sigkillserver.domain.game.service.GameService;
import com.gulab.sigkillserver.domain.lock.RoomLockManager;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.bot.service.WaitingBotRoomCleanupService;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.TaskScheduler;

class PendingRoomJoinOrchestratorTest {

    private UserRepository userRepository;
    private PlayerMemoryRepository playerRepository;
    private RoomMemoryRepository roomRepository;
    private RoomService roomService;
    private TaskScheduler taskScheduler;
    private WaitingBotRoomCleanupService waitingBotRoomCleanupService;
    private PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator;

    @BeforeEach
    void setup() {
        userRepository = new UserMemoryRepository();
        playerRepository = new PlayerMemoryRepository();
        roomRepository = new RoomMemoryRepository();
        RoomLockManager roomLockManager = new RoomLockManager();

        GameService gameService = new GameService(
                userRepository,
                new GameMemoryRepository(),
                new QuizMemoryRepository(new ObjectMapper(), new ClassPathResource("quiz/quiz.json")),
                playerRepository,
                roomRepository,
                new SelectedChoiceMemoryRepository(),
                new QuizChoiceNumberMappingMemoryRepository(),
                new GamePlayerMemoryRepository(),
                new GameEventBuilder(),
                roomLockManager
        );
        roomService = new RoomService(
                roomRepository,
                userRepository,
                playerRepository,
                roomLockManager,
                gameService
        );
        taskScheduler = mock(TaskScheduler.class);
        waitingBotRoomCleanupService = mock(WaitingBotRoomCleanupService.class);
        pendingRoomJoinOrchestrator = new PendingRoomJoinOrchestrator(
                taskScheduler,
                roomService,
                waitingBotRoomCleanupService
        );
    }

    private Player activePlayer(Long userId, String roomId, String nickname) {
        Player player = Player.create(userId, roomId, nickname);
        player.activate();
        return player;
    }

    @Test
    void pending_join_timeout_실행시_아직_pending인_플레이어를_정리한다() {
        // given
        User host = userRepository.save(User.create("pending-host-session", "호스트", UserRole.GUEST));
        String roomId = roomService.createRoom("방", 6, host.getUserId()).roomId();
        AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            scheduledTask.set(invocation.getArgument(0));
            return scheduledFuture;
        });

        // when
        pendingRoomJoinOrchestrator.schedulePendingJoinTimeout(roomId, host.getUserId());
        scheduledTask.get().run();

        // then
        assertThat(playerRepository.findById(host.getUserId())).isEmpty();
        assertThat(roomRepository.findById(roomId)).isEmpty();
        verify(waitingBotRoomCleanupService).scheduleDrainIfWaitingBotOnly(roomId);
    }

    @Test
    void pending_join_timeout_실행시_이미_active면_아무것도_하지_않는다() {
        // given
        User host = userRepository.save(User.create("active-host-session", "호스트", UserRole.GUEST));
        String roomId = roomService.createRoom("방", 6, host.getUserId()).roomId();
        AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            scheduledTask.set(invocation.getArgument(0));
            return mock(ScheduledFuture.class);
        });

        pendingRoomJoinOrchestrator.schedulePendingJoinTimeout(roomId, host.getUserId());
        roomService.confirmJoin(roomId, host.getUserId());

        // when
        scheduledTask.get().run();

        // then
        assertThat(playerRepository.findById(host.getUserId())).isPresent();
        assertThat(roomRepository.findById(roomId)).isPresent();
    }

    @Test
    void expirePendingJoin은_이미_active로_전환된_플레이어를_정리하지_않는다() {
        // given
        User host = userRepository.save(User.create("confirmed-host-session", "호스트", UserRole.GUEST));
        String roomId = roomService.createRoom("방", 6, host.getUserId()).roomId();
        roomService.confirmJoin(roomId, host.getUserId());

        // when
        boolean expired = roomService.expirePendingJoin(roomId, host.getUserId());

        // then
        assertThat(expired).isFalse();
        assertThat(playerRepository.findById(host.getUserId())).isPresent();
        assertThat(roomRepository.findById(roomId)).isPresent();
    }

    @Test
    void clearAllPendingJoinTimeouts_호출시_예약된_작업을_모두_취소한다() {
        // given
        User host = userRepository.save(User.create("cleanup-host-session", "호스트", UserRole.GUEST));
        String roomId = roomService.createRoom("방", 6, host.getUserId()).roomId();
        ScheduledFuture<Object> scheduledFuture = mock(ScheduledFuture.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> scheduledFuture);
        pendingRoomJoinOrchestrator.schedulePendingJoinTimeout(roomId, host.getUserId());

        // when
        PendingRoomJoinOrchestrator.PendingJoinCleanupResult result =
                pendingRoomJoinOrchestrator.clearAllPendingJoinTimeouts();

        // then
        assertThat(result.canceledScheduledTaskCount()).isEqualTo(1);
        verify(scheduledFuture).cancel(false);
    }

    @Test
    void pending_join_timeout으로_마지막_사람이_빠지면_bot_cleanup을_트리거한다() {
        // given
        User host = userRepository.save(User.create("host-session", "호스트", UserRole.GUEST));
        User pendingGuest = userRepository.save(User.create("pending-session", "대기손님", UserRole.GUEST));
        User bot = userRepository.save(User.create(null, "[봇] 손님", UserRole.BOT));
        String roomId = roomService.createRoom("방", 6, host.getUserId()).roomId();
        roomService.confirmJoin(roomId, host.getUserId());
        playerRepository.create(activePlayer(bot.getUserId(), roomId, bot.getNickname()));
        roomService.joinRoom(roomId, pendingGuest.getUserId());
        roomService.leaveRoom(roomId, host.getUserId());

        AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            scheduledTask.set(invocation.getArgument(0));
            return mock(ScheduledFuture.class);
        });

        // when
        pendingRoomJoinOrchestrator.schedulePendingJoinTimeout(roomId, pendingGuest.getUserId());
        scheduledTask.get().run();

        // then
        assertThat(roomRepository.findById(roomId)).isPresent();
        assertThat(roomRepository.findById(roomId).orElseThrow().isClosing()).isTrue();
        verify(waitingBotRoomCleanupService).scheduleDrainIfWaitingBotOnly(roomId);
    }
}
