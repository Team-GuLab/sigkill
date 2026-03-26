package com.gulab.sigkillserver.domain.bot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.repository.GameMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceMemoryRepository;
import com.gulab.sigkillserver.domain.game.service.GameEventBuilder;
import com.gulab.sigkillserver.domain.game.service.GameService;
import com.gulab.sigkillserver.domain.lock.RoomLockManager;
import com.gulab.sigkillserver.domain.room.exception.RoomErrorCode;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.service.PendingRoomJoinOrchestrator;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;

class WaitingBotRoomCleanupServiceTest {

    private UserMemoryRepository userRepository;
    private PlayerMemoryRepository playerRepository;
    private RoomMemoryRepository roomRepository;
    private RoomService roomService;
    private RoomLockManager roomLockManager;
    private TaskScheduler botTaskScheduler;
    private BotUserService botUserService;
    private WaitingBotRoomCleanupService waitingBotRoomCleanupService;

    @BeforeEach
    void setUp() {
        userRepository = new UserMemoryRepository();
        playerRepository = new PlayerMemoryRepository();
        roomRepository = new RoomMemoryRepository();
        roomLockManager = new RoomLockManager();

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
        botTaskScheduler = mock(TaskScheduler.class);
        botUserService = new BotUserService(userRepository);
        waitingBotRoomCleanupService = new WaitingBotRoomCleanupService(
                roomRepository,
                playerRepository,
                botUserService,
                roomService,
                roomLockManager,
                mock(PendingRoomJoinOrchestrator.class),
                mock(SimpMessagingTemplate.class),
                botTaskScheduler
        );
    }

    @SuppressWarnings("unchecked")
    private ScheduledFuture<Object> mockScheduledFuture() {
        return (ScheduledFuture<Object>) mock(ScheduledFuture.class);
    }

    private Player activePlayer(Long userId, String roomId, String nickname) {
        Player player = Player.create(userId, roomId, nickname);
        player.activate();
        return player;
    }

    private User createGuestUser(String sessionId, String nickname) {
        return userRepository.save(User.create(sessionId, nickname, UserRole.GUEST));
    }

    @Test
    void waiting_bot_only_room은_leaveRoom_경로로_끝까지_정리된다() {
        // given
        var botHost = botUserService.createBotUser();
        var botGuest = botUserService.createBotUser();
        Room room = Room.create("1456", "봇 방", botHost.getUserId(), 6);
        roomRepository.save(room);
        playerRepository.create(activePlayer(botHost.getUserId(), room.getRoomId(), botHost.getNickname()));
        playerRepository.create(activePlayer(botGuest.getUserId(), room.getRoomId(), botGuest.getNickname()));

        List<Runnable> scheduledTasks = new ArrayList<>();
        when(botTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenAnswer(invocation -> {
                    scheduledTasks.add(invocation.getArgument(0));
                    return mockScheduledFuture();
                });

        // when
        waitingBotRoomCleanupService.scheduleDrainIfWaitingBotOnly(room.getRoomId());
        scheduledTasks.get(0).run();
        scheduledTasks.get(1).run();

        // then
        assertThat(roomRepository.findById(room.getRoomId())).isEmpty();
        assertThat(playerRepository.findAllByRoomId(room.getRoomId())).isEmpty();
        assertThat(userRepository.findById(botHost.getUserId())).isEmpty();
        assertThat(userRepository.findById(botGuest.getUserId())).isEmpty();
    }

    @Test
    void waiting_bot_only_room이_드레인_예약되면_closing_상태가_되어_새_입장이_거부된다() {
        // given
        var botHost = botUserService.createBotUser();
        var botGuest = botUserService.createBotUser();
        User human = createGuestUser("human-session", "사람");
        Room room = Room.create("1457", "닫히는 봇 방", botHost.getUserId(), 6);
        roomRepository.save(room);
        playerRepository.create(activePlayer(botHost.getUserId(), room.getRoomId(), botHost.getNickname()));
        playerRepository.create(activePlayer(botGuest.getUserId(), room.getRoomId(), botGuest.getNickname()));

        when(botTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenAnswer(invocation -> mockScheduledFuture());

        // when
        waitingBotRoomCleanupService.scheduleDrainIfWaitingBotOnly(room.getRoomId());

        // then
        assertThat(roomRepository.findById(room.getRoomId()).orElseThrow().isClosing()).isTrue();
        assertThatThrownBy(() -> roomService.joinRoom(room.getRoomId(), human.getUserId()))
                .isInstanceOf(CustomException.class)
                .satisfies(throwable -> assertThat(((CustomException) throwable).getErrorCode().getCode())
                        .isEqualTo(RoomErrorCode.ROOM_CLOSING.name()));
    }
}
