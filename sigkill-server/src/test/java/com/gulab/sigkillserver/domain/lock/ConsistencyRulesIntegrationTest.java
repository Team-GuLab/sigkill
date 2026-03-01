package com.gulab.sigkillserver.domain.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.EndQuizOrGameEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameLoadEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPlayerInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizResult;
import com.gulab.sigkillserver.domain.game.model.GamePlayer;
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
import com.gulab.sigkillserver.domain.game.service.GameEventBuilder;
import com.gulab.sigkillserver.domain.game.service.GameService;
import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import com.gulab.sigkillserver.domain.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;

class ConsistencyRulesIntegrationTest {
    private UserRepository userRepository;
    private GameRepository gameRepository;
    private QuizRepository quizRepository;
    private PlayerRepository playerRepository;
    private RoomRepository roomRepository;
    private SelectedChoiceRepository selectedChoiceRepository;
    private QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private GamePlayerRepository gamePlayerRepository;
    private GameEventBuilder gameEventBuilder;

    private UserService userService;
    private GameService gameService;
    private RoomService roomService;

    @BeforeEach
    void initObjects() {
        userRepository = new UserMemoryRepository();
        gameRepository = new GameMemoryRepository();
        quizRepository = new QuizMemoryRepository(new ObjectMapper(), new ClassPathResource("quiz/quiz.json"));
        playerRepository = new PlayerMemoryRepository();
        roomRepository = new RoomMemoryRepository();
        selectedChoiceRepository = new SelectedChoiceMemoryRepository();
        quizChoiceNumberMappingRepository = new QuizChoiceNumberMappingMemoryRepository();
        gamePlayerRepository = new GamePlayerMemoryRepository();
        gameEventBuilder = new GameEventBuilder();

        userService = new UserService(userRepository);

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
        roomService = new RoomService(roomRepository, userRepository, playerRepository, gameService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private LoginResponse loginGuest(String sessionId) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(sessionId);

        return userService.loginAsGuest(session);
    }

    private List<Throwable> runConcurrently(ThrowingRunnable... actions) throws InterruptedException {
        int threadCount = actions.length;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        try {
            for (ThrowingRunnable action : actions) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        action.run();
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            done.await();
            return errors;
        } finally {
            pool.shutdown();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class SnapshotRacePlayerRepository extends PlayerMemoryRepository {
        private final Long lateJoinerUserId;
        private final CountDownLatch lateJoinCreateAttempted = new CountDownLatch(1);
        private final CountDownLatch gameStartSnapshotTaken = new CountDownLatch(1);
        private volatile String roomIdForCoordination;

        private SnapshotRacePlayerRepository(Long lateJoinerUserId) {
            this.lateJoinerUserId = lateJoinerUserId;
        }

        private void coordinateForRoom(String roomId) {
            this.roomIdForCoordination = roomId;
        }

        private boolean awaitLateJoinCreateAttempted() throws InterruptedException {
            return lateJoinCreateAttempted.await(3, TimeUnit.SECONDS);
        }

        @Override
        public Player create(Player player) {
            if (Objects.equals(player.getUserId(), lateJoinerUserId)
                    && Objects.equals(player.getRoomId(), roomIdForCoordination)) {
                lateJoinCreateAttempted.countDown();
                try {
                    boolean released = gameStartSnapshotTaken.await(3, TimeUnit.SECONDS);
                    if (!released) {
                        throw new IllegalStateException("게임 시작 스냅샷 대기 타임아웃");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("late join create 대기 중 인터럽트", e);
                }
            }
            return super.create(player);
        }

        @Override
        public List<Player> findAllByRoomId(String roomId) {
            List<Player> players = super.findAllByRoomId(roomId);
            if (Objects.equals(roomId, roomIdForCoordination)
                    && lateJoinCreateAttempted.getCount() == 0
                    && isGameServiceStartGameInCallStack()) {
                gameStartSnapshotTaken.countDown();
            }
            return players;
        }

        private boolean isGameServiceStartGameInCallStack() {
            for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
                if (GameService.class.getName().equals(frame.getClassName())
                        && "startGame".equals(frame.getMethodName())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    class RoomCreateJoinConcurrencyTests {
        //        @RepeatedTest(10000)
        @Test
        void 동시에_여러_사용자가_방을_만들어도_서로_다른_방_번호가_발급된다() throws InterruptedException {
            // given
            LoginResponse user1 = loginGuest("session1");
            LoginResponse user2 = loginGuest("session2");
            LoginResponse user3 = loginGuest("session3");
            LoginResponse user4 = loginGuest("session4");
            LoginResponse user5 = loginGuest("session5");
            LoginResponse user6 = loginGuest("session6");
            List<Long> userIds = List.of(
                    user1.userId(),
                    user2.userId(),
                    user3.userId(),
                    user4.userId(),
                    user5.userId(),
                    user6.userId()
            );
            Set<String> roomIds = ConcurrentHashMap.newKeySet();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> {
                        RoomCreateResponse res = roomService.createRoom("테스트 방", 6, user1.userId());
                        roomIds.add(res.roomId());
                    },
                    () -> {
                        RoomCreateResponse res = roomService.createRoom("테스트 방", 6, user2.userId());
                        roomIds.add(res.roomId());
                    },
                    () -> {
                        RoomCreateResponse res = roomService.createRoom("테스트 방", 6, user3.userId());
                        roomIds.add(res.roomId());
                    },
                    () -> {
                        RoomCreateResponse res = roomService.createRoom("테스트 방", 6, user4.userId());
                        roomIds.add(res.roomId());
                    },
                    () -> {
                        RoomCreateResponse res = roomService.createRoom("테스트 방", 6, user5.userId());
                        roomIds.add(res.roomId());
                    },
                    () -> {
                        RoomCreateResponse res = roomService.createRoom("테스트 방", 6, user6.userId());
                        roomIds.add(res.roomId());
                    }
            );

            // then
            assertThat(errors).isEmpty();
            assertThat(roomIds).hasSize(userIds.size());
            assertThat(roomIds).doesNotContainNull();
            var createdRooms = roomRepository.findAll();
            assertThat((long) createdRooms.size()).isEqualTo(userIds.size());
            assertThat(createdRooms).extracting(Room::getHostId)
                    .containsExactlyInAnyOrderElementsOf(userIds);
        }

        @Test
        void 같은_사용자가_동시에_여러_번_방_만들기를_눌러도_방은_하나만_만들어진다() throws InterruptedException {
            // given
            LoginResponse rs = loginGuest("session1");

            // when
            List<Throwable> errors = runConcurrently(
                    () -> roomService.createRoom("테스트 방", 6, rs.userId()),
                    () -> roomService.createRoom("테스트 방", 6, rs.userId())
            );

            // then
            assertThat(errors).hasSize(1);
            assertThat(roomRepository.findAll()).hasSize(1);
            assertThat(playerRepository.findAll()).hasSize(1);
            assertThat(playerRepository.findAll().getFirst().getUserId()).isEqualTo(rs.userId());
        }

        @Test
        void 동시에_여러_사용자가_입장해도_방_정원을_넘겨_입장되지_않는다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();

            LoginResponse user1 = loginGuest("session1");
            LoginResponse user2 = loginGuest("session2");
            LoginResponse user3 = loginGuest("session3");
            LoginResponse user4 = loginGuest("session4");
            LoginResponse user5 = loginGuest("session5");

            RoomCreateResponse rcr = roomService.createRoom("테스트 방", 2, hostUserId);
            String roomId = rcr.roomId();
            Set<Long> joinedUserIds = ConcurrentHashMap.newKeySet();

            // when
            runConcurrently(
                    () -> {
                        roomService.joinRoom(roomId, user1.userId());
                        joinedUserIds.add(user1.userId());
                    },
                    () -> {
                        roomService.joinRoom(roomId, user2.userId());
                        joinedUserIds.add(user2.userId());
                    },
                    () -> {
                        roomService.joinRoom(roomId, user3.userId());
                        joinedUserIds.add(user3.userId());
                    },
                    () -> {
                        roomService.joinRoom(roomId, user4.userId());
                        joinedUserIds.add(user4.userId());
                    },
                    () -> {
                        roomService.joinRoom(roomId, user5.userId());
                        joinedUserIds.add(user5.userId());
                    }
            );

            // then
            List<Player> players = playerRepository.findAllByRoomId(roomId);
            assertThat(players).hasSize(2);
            assertThat(players).extracting(Player::getUserId).containsAll(joinedUserIds);
        }

        @Test
        void 게임_시작_요청과_입장_요청이_동시에_도착해도_시작_시점의_참가자_스냅샷과_실제_방_인원이_일치한다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long readyGuestUserId = loginGuest("readyGuestSession").userId();
            long lateJoinerUserId = loginGuest("lateJoinerSession").userId();

            SnapshotRacePlayerRepository snapshotRacePlayerRepository = new SnapshotRacePlayerRepository(
                    lateJoinerUserId);
            playerRepository = snapshotRacePlayerRepository;
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
            roomService = new RoomService(roomRepository, userRepository, playerRepository, gameService);

            RoomCreateResponse createdRoom = roomService.createRoom("테스트 방", 3, hostUserId);
            String roomId = createdRoom.roomId();
            roomService.joinRoom(roomId, readyGuestUserId);
            roomService.readyPlayer(roomId, readyGuestUserId);
            snapshotRacePlayerRepository.coordinateForRoom(roomId);

            AtomicReference<GameStartEvent> gameStartEventRef = new AtomicReference<>();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> {
                        if (!snapshotRacePlayerRepository.awaitLateJoinCreateAttempted()) {
                            throw new IllegalStateException("late join create 시도 대기 타임아웃");
                        }
                        gameStartEventRef.set(roomService.startGame(roomId, hostUserId));
                    },
                    () -> roomService.joinRoom(roomId, lateJoinerUserId)
            );

            // then
            assertThat(errors).isEmpty();

            GameStartEvent gameStartEvent = gameStartEventRef.get();
            assertThat(gameStartEvent).isNotNull();

            List<Long> snapshotUserIds = gameStartEvent.payload().players().stream()
                    .map(QuizEndPlayerInfo::userId)
                    .toList();
            List<Long> roomUserIds = playerRepository.findAllByRoomId(roomId).stream()
                    .map(Player::getUserId)
                    .toList();

            assertThat(snapshotUserIds).containsExactlyInAnyOrderElementsOf(roomUserIds);
            assertThat(gamePlayerRepository.getByGameId(gameStartEvent.gameId()))
                    .extracting(GamePlayer::getUserId)
                    .containsExactlyInAnyOrderElementsOf(roomUserIds);
        }
    }

    @Nested
    class RoomLeaveHostTransitionConcurrencyTests {
        @Test
        void 나가는_사용자와_들어오는_사용자가_겹쳐도_방_참가자_목록이_깨지지_않는다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long leavingGuestUserId = loginGuest("leavingGuestSession").userId();
            long joiningGuestUserId = loginGuest("joiningGuestSession").userId();
            RoomCreateResponse createdRoom = roomService.createRoom("테스트 방", 3, hostUserId);
            String roomId = createdRoom.roomId();
            roomService.joinRoom(roomId, leavingGuestUserId);

            // when
            List<Throwable> errors = runConcurrently(
                    () -> roomService.leaveRoom(roomId, leavingGuestUserId),
                    () -> roomService.joinRoom(roomId, joiningGuestUserId)
            );

            // then
            assertThat(errors).isEmpty();
            List<Player> players = playerRepository.findAllByRoomId(roomId);
            assertThat(players).hasSize(2);
            assertThat(players).extracting(Player::getUserId).
                    containsExactlyInAnyOrder(hostUserId, joiningGuestUserId)
                    .doesNotContain(leavingGuestUserId);
        }

        @RepeatedTest(100)
        void 방_나가기_요청과_게임_시작_요청이_동시에_도착해도_게임_참가자와_방_인원_정보가_일치한다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long leavingGuestUserId = loginGuest("leavingGuestSession").userId();
            long stayingGuestUserId = loginGuest("stayingGuestSession").userId();

            RoomCreateResponse createdRoom = roomService.createRoom("테스트 방", 3, hostUserId);
            String roomId = createdRoom.roomId();
            roomService.joinRoom(roomId, leavingGuestUserId);
            roomService.joinRoom(roomId, stayingGuestUserId);
            roomService.readyPlayer(roomId, leavingGuestUserId);
            roomService.readyPlayer(roomId, stayingGuestUserId);

            AtomicReference<GameStartEvent> gameStartEventRef = new AtomicReference<>();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> gameStartEventRef.set(roomService.startGame(roomId, hostUserId)),
                    () -> roomService.leaveRoom(roomId, leavingGuestUserId)
            );

            // then
            assertThat(errors).isEmpty();

            GameStartEvent gameStartEvent = gameStartEventRef.get();
            assertThat(gameStartEvent).isNotNull();

            List<Long> gamePlayerUserIds = gamePlayerRepository.getByGameId(gameStartEvent.gameId()).stream()
                    .map(GamePlayer::getUserId)
                    .toList();
            List<Long> roomUserIds = playerRepository.findAllByRoomId(roomId).stream()
                    .map(Player::getUserId)
                    .toList();

            assertThat(gamePlayerUserIds).containsExactlyInAnyOrderElementsOf(roomUserIds);
            assertThat(gameStartEvent.payload().players())
                    .extracting(QuizEndPlayerInfo::userId)
                    .containsExactlyInAnyOrderElementsOf(roomUserIds);
        }

        @Test
        void 방장이_나가는_순간_게임_시작_요청이_겹쳐도_새_방장이_정상적으로_결정된다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long guest1UserId = loginGuest("guest1Session").userId();
            long guest2UserId = loginGuest("guest2Session").userId();

            RoomCreateResponse createdRoom = roomService.createRoom("테스트 방", 3, hostUserId);
            String roomId = createdRoom.roomId();
            roomService.joinRoom(roomId, guest1UserId);
            roomService.joinRoom(roomId, guest2UserId);
            roomService.readyPlayer(roomId, guest1UserId);
            roomService.readyPlayer(roomId, guest2UserId);

            AtomicReference<GameStartEvent> gameStartEventRef = new AtomicReference<>();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> {
                        Thread.sleep(100);
                        roomService.leaveRoom(roomId, hostUserId);
                    },
                    () -> gameStartEventRef.set(roomService.startGame(roomId, hostUserId))
            );

            // then
            assertThat(errors).isEmpty();

            Room room = roomRepository.findById(roomId).orElseThrow();
            List<Player> players = playerRepository.findAllByRoomId(roomId);
            List<Long> remainingUserIds = players.stream()
                    .map(Player::getUserId)
                    .toList();
            GameStartEvent gameStartEvent = gameStartEventRef.get();

            assertThat(remainingUserIds).hasSize(2);
            assertThat(remainingUserIds).doesNotContain(hostUserId);
            assertThat(remainingUserIds).contains(room.getHostId());
            assertThat(room.getHostId()).isNotEqualTo(hostUserId);
            assertThat(gamePlayerRepository.getByGameId(gameStartEvent.gameId()))
                    .extracting(GamePlayer::getUserId)
                    .containsExactlyInAnyOrderElementsOf(remainingUserIds);
        }

        @Test
        void 방장_퇴장과_자동_퇴장이_동시에_발생해도_방장_변경_안내는_한_번만_전달된다() throws InterruptedException {
            // given
            long hostId = loginGuest("host").userId();
            long guest1 = loginGuest("g1").userId();
            long guest2 = loginGuest("g2").userId();

            String roomId = roomService.createRoom("방", 4, hostId).roomId();
            roomService.joinRoom(roomId, guest1);
            roomService.joinRoom(roomId, guest2);

            List<LeaveRoomResult> results = Collections.synchronizedList(new ArrayList<>());

            // when
            List<Throwable> errors = runConcurrently(
                    () -> results.add(roomService.leaveRoom(roomId, hostId)),
                    () -> results.add(roomService.leaveRoom(roomId, hostId))
            );

            // then
            assertThat(errors).hasSize(1);
            long hostChangedCount = results.stream().filter(LeaveRoomResult::hasHostChangedEvent).count();
            assertThat(hostChangedCount).isEqualTo(1);
        }
    }

    @Nested
    class ReadyStartBoundaryTests {
        @Test
        void 준비_완료와_퇴장이_동시에_일어나도_시작_가능_여부는_최종_참가자_기준으로_계산된다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long readyGuestUserId = loginGuest("readyGuestSession").userId();
            long leavingGuestUserId = loginGuest("leavingGuestSession").userId();

            RoomCreateResponse createdRoom = roomService.createRoom("테스트 방", 3, hostUserId);
            String roomId = createdRoom.roomId();
            roomService.joinRoom(roomId, readyGuestUserId);
            roomService.joinRoom(roomId, leavingGuestUserId);
            AtomicReference<PlayerReadyEvent> readyEventRef = new AtomicReference<>();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> readyEventRef.set(roomService.readyPlayer(roomId, readyGuestUserId)),
                    () -> roomService.leaveRoom(roomId, leavingGuestUserId)
            );

            // then
            assertThat(errors).isEmpty();
            PlayerReadyEvent readyEvent = readyEventRef.get();
            assertThat(readyEvent).isNotNull();
            assertThat(readyEvent.allReady()).isFalse();

            List<Player> players = playerRepository.findAllByRoomId(roomId);
            assertThat(players).extracting(Player::getUserId)
                    .containsExactlyInAnyOrder(hostUserId, readyGuestUserId)
                    .doesNotContain(leavingGuestUserId);

            GameStartEvent gameStartEvent = roomService.startGame(roomId, hostUserId);
            assertThat(gameStartEvent.payload().players())
                    .extracting(QuizEndPlayerInfo::userId)
                    .containsExactlyInAnyOrder(hostUserId, readyGuestUserId);
        }

        @RepeatedTest(1000)
        void 준비_취소와_게임_시작_요청이_동시에_일어나면_준비_취소가_반영된_경우_게임_시작이_거부된다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long readyGuest1UserId = loginGuest("readyGuest1Session").userId();
            long readyGuest2UserId = loginGuest("readyGuest2Session").userId();

            RoomCreateResponse createdRoom = roomService.createRoom("테스트 방", 3, hostUserId);
            String roomId = createdRoom.roomId();
            roomService.joinRoom(roomId, readyGuest1UserId);
            roomService.joinRoom(roomId, readyGuest2UserId);
            roomService.readyPlayer(roomId, readyGuest1UserId);
            roomService.readyPlayer(roomId, readyGuest2UserId);

            AtomicReference<GameStartEvent> gameStartEventRef = new AtomicReference<>();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> roomService.unreadyPlayer(roomId, readyGuest1UserId),
                    () -> gameStartEventRef.set(roomService.startGame(roomId, hostUserId))
            );

            // then
            assertThat(roomRepository.findById(roomId).orElseThrow().isInGame()).isFalse();
        }
    }

    @Nested
    class GameLoadEndBoundaryTests {
        @RepeatedTest(10)
        void 참가자들이_동시에_게임_화면_로딩을_완료해도_전체_로딩_완료는_한_번만_확정된다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long guest1UserId = loginGuest("guest1Session").userId();
            long guest2UserId = loginGuest("guest2Session").userId();

            String roomId = roomService.createRoom("테스트 방", 3, hostUserId).roomId();
            roomService.joinRoom(roomId, guest1UserId);
            roomService.joinRoom(roomId, guest2UserId);
            roomService.readyPlayer(roomId, guest1UserId);
            roomService.readyPlayer(roomId, guest2UserId);
            Long gameId = roomService.startGame(roomId, hostUserId).gameId();

            List<GameLoadEvent> loadEvents = Collections.synchronizedList(new ArrayList<>());

            // when
            List<Throwable> errors = runConcurrently(
                    () -> loadEvents.add(gameService.loadGame(hostUserId, gameId)),
                    () -> loadEvents.add(gameService.loadGame(guest1UserId, gameId)),
                    () -> loadEvents.add(gameService.loadGame(guest2UserId, gameId))
            );

            // then
            assertThat(errors).isEmpty();
            long allLoadedTrueCount = loadEvents.stream()
                    .filter(event -> event.payload().allLoaded())
                    .count();
            assertThat(allLoadedTrueCount).isEqualTo(1);
        }
    }

    @Nested
    class RoundTransitionConcurrencyTests {
        @Test
        void 라운드_종료와_다음_라운드_시작이_겹쳐도_문제_순서가_중복되거나_건너뛰지_않는다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long guest1UserId = loginGuest("guest1Session").userId();
            long guest2UserId = loginGuest("guest2Session").userId();

            String roomId = roomService.createRoom("테스트 방", 3, hostUserId).roomId();
            roomService.joinRoom(roomId, guest1UserId);
            roomService.joinRoom(roomId, guest2UserId);
            roomService.readyPlayer(roomId, guest1UserId);
            roomService.readyPlayer(roomId, guest2UserId);

            Long gameId = roomService.startGame(roomId, hostUserId).gameId();
            gameService.startQuiz(hostUserId, roomId, gameId);

            long firstQuizId = gameRepository.findById(gameId).orElseThrow().getCurrentQuizId();
            var firstQuiz = quizRepository.findById(firstQuizId).orElseThrow();
            var choiceMapping = quizChoiceNumberMappingRepository.findByGameIdAndQuizId(gameId, firstQuizId)
                    .orElseThrow();
            int correctChoiceNumber = choiceMapping.getNumberToChoiceId().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(firstQuiz.correctChoiceId()))
                    .findFirst()
                    .orElseThrow()
                    .getKey();

            gameService.submitChoice(hostUserId, gameId, firstQuizId, correctChoiceNumber);
            gameService.submitChoice(guest1UserId, gameId, firstQuizId, correctChoiceNumber);
            gameService.submitChoice(guest2UserId, gameId, firstQuizId, correctChoiceNumber);

            AtomicReference<QuizStartEvent> nextQuizStartEventRef = new AtomicReference<>();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> gameService.endQuiz(hostUserId, roomId, gameId, firstQuizId),
                    () -> nextQuizStartEventRef.set(gameService.startQuiz(hostUserId, roomId, gameId))
            );

            // then
            assertThat(errors.size()).isLessThanOrEqualTo(1);
            assertThat(nextQuizStartEventRef.get()).isNotNull();

            var gameAfter = gameRepository.findById(gameId).orElseThrow();
            assertThat(gameAfter.getCurrentQuizIndex()).isEqualTo(1);
            assertThat(gameAfter.getCurrentQuizId()).isNotEqualTo(firstQuizId);
            assertThat(nextQuizStartEventRef.get().payload().quiz().quizId()).isEqualTo(gameAfter.getCurrentQuizId());
        }

        @Test
        void 같은_라운드의_종료_처리_요청이_중복되어도_결과_집계는_한_번만_수행된다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long guest1UserId = loginGuest("guest1Session").userId();
            long guest2UserId = loginGuest("guest2Session").userId();

            String roomId = roomService.createRoom("테스트 방", 3, hostUserId).roomId();
            roomService.joinRoom(roomId, guest1UserId);
            roomService.joinRoom(roomId, guest2UserId);
            roomService.readyPlayer(roomId, guest1UserId);
            roomService.readyPlayer(roomId, guest2UserId);

            Long gameId = roomService.startGame(roomId, hostUserId).gameId();
            gameService.startQuiz(hostUserId, roomId, gameId);
            long quizId = gameRepository.findById(gameId).orElseThrow().getCurrentQuizId();

            var quiz = quizRepository.findById(quizId).orElseThrow();
            var mapping = quizChoiceNumberMappingRepository.findByGameIdAndQuizId(gameId, quizId).orElseThrow();
            int correctChoiceNumber = mapping.getNumberToChoiceId().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(quiz.correctChoiceId()))
                    .findFirst()
                    .orElseThrow()
                    .getKey();

            gameService.submitChoice(hostUserId, gameId, quizId, correctChoiceNumber);
            gameService.submitChoice(guest1UserId, gameId, quizId, correctChoiceNumber);
            gameService.submitChoice(guest2UserId, gameId, quizId, correctChoiceNumber);
            List<EndQuizOrGameEvent> successEvents = Collections.synchronizedList(new ArrayList<>());

            // when
            List<Throwable> errors = runConcurrently(
                    () -> successEvents.add(gameService.endQuiz(hostUserId, roomId, gameId, quizId)),
                    () -> successEvents.add(gameService.endQuiz(hostUserId, roomId, gameId, quizId))
            );

            // then
            assertThat(successEvents).hasSize(1);
            assertThat(errors).hasSize(1);
            assertThat(gamePlayerRepository.getByGameId(gameId))
                    .extracting(GamePlayer::getScore)
                    .containsOnly(1);
        }
    }

    @Nested
    class SubmitScoringEndBoundaryTests {
        @RepeatedTest(50)
        void 한_사용자가_답을_연속으로_제출하면_가장_마지막_제출만_최종_답으로_인정된다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long guestUserId = loginGuest("guestSession").userId();

            String roomId = roomService.createRoom("테스트 방", 2, hostUserId).roomId();
            roomService.joinRoom(roomId, guestUserId);
            roomService.readyPlayer(roomId, guestUserId);

            Long gameId = roomService.startGame(roomId, hostUserId).gameId();
            gameService.startQuiz(hostUserId, roomId, gameId);
            long quizId = gameRepository.findById(gameId).orElseThrow().getCurrentQuizId();

            var quiz = quizRepository.findById(quizId).orElseThrow();
            var mapping = quizChoiceNumberMappingRepository.findByGameIdAndQuizId(gameId, quizId).orElseThrow();
            int correctChoiceNumber = mapping.getNumberToChoiceId().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(quiz.correctChoiceId()))
                    .findFirst()
                    .orElseThrow()
                    .getKey();
            int wrongChoiceNumber = mapping.getNumberToChoiceId().keySet().stream()
                    .filter(number -> number != correctChoiceNumber)
                    .findFirst()
                    .orElseThrow();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> gameService.submitChoice(hostUserId, gameId, quizId, wrongChoiceNumber),
                    () -> {
                        Thread.sleep(20);
                        gameService.submitChoice(hostUserId, gameId, quizId, correctChoiceNumber);
                    }
            );
            EndQuizOrGameEvent endQuizResult = gameService.endQuiz(hostUserId, roomId, gameId, quizId);

            // then
            assertThat(errors).isEmpty();
            QuizEndPlayerInfo hostResult = endQuizResult.quizEndEvent().payload().players().stream()
                    .filter(info -> info.userId().equals(hostUserId))
                    .findFirst()
                    .orElseThrow();
            assertThat(hostResult.quizResult()).isEqualTo(QuizResult.CORRECT);
        }

        @RepeatedTest(100)
        void 답_제출과_라운드_종료가_동시에_발생해도_채점_결과는_요청_순서에_따라_흔들리지_않는다() throws InterruptedException {
            // given
            long hostUserId = loginGuest("hostSession").userId();
            long guestUserId = loginGuest("guestSession").userId();

            String roomId = roomService.createRoom("테스트 방", 2, hostUserId).roomId();
            roomService.joinRoom(roomId, guestUserId);
            roomService.readyPlayer(roomId, guestUserId);

            Long gameId = roomService.startGame(roomId, hostUserId).gameId();
            gameService.startQuiz(hostUserId, roomId, gameId);
            long quizId = gameRepository.findById(gameId).orElseThrow().getCurrentQuizId();

            var quiz = quizRepository.findById(quizId).orElseThrow();
            var mapping = quizChoiceNumberMappingRepository.findByGameIdAndQuizId(gameId, quizId).orElseThrow();
            int correctChoiceNumber = mapping.getNumberToChoiceId().entrySet().stream()
                    .filter(entry -> entry.getValue().equals(quiz.correctChoiceId()))
                    .findFirst()
                    .orElseThrow()
                    .getKey();

            AtomicReference<Throwable> submitErrorRef = new AtomicReference<>();
            AtomicReference<EndQuizOrGameEvent> endQuizResultRef = new AtomicReference<>();

            // when
            List<Throwable> errors = runConcurrently(
                    () -> {
                        try {
                            gameService.submitChoice(hostUserId, gameId, quizId, correctChoiceNumber);
                        } catch (Throwable t) {
                            submitErrorRef.set(t);
                            throw t;
                        }
                    },
                    () -> endQuizResultRef.set(gameService.endQuiz(hostUserId, roomId, gameId, quizId))
            );

            // then
            assertThat(errors).hasSizeLessThanOrEqualTo(1);
            EndQuizOrGameEvent endQuizResult = endQuizResultRef.get();
            assertThat(endQuizResult).isNotNull();

            QuizEndPlayerInfo hostResult = endQuizResult.quizEndEvent().payload().players().stream()
                    .filter(info -> info.userId().equals(hostUserId))
                    .findFirst()
                    .orElseThrow();

            if (submitErrorRef.get() == null) {
                assertThat(hostResult.quizResult()).isEqualTo(QuizResult.CORRECT);
            }
        }

        @Test
        void 라운드_종료_시점과_제출_요청이_경계에서_겹쳐도_종료_이후_제출은_채점에_반영되지_않는다() {
            // given

            // when

            // then
        }

        @Test
        void 게임_종료와_답_제출이_동시에_발생해도_종료된_gameId에_제출_데이터가_남지_않는다() {
            // given

            // when

            // then
        }
    }
}
