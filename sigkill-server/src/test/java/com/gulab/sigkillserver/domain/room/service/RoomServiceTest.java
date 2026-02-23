package com.gulab.sigkillserver.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomResponse;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.RoomResponseType;
import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerRole;
import com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode;
import com.gulab.sigkillserver.domain.room.exception.RoomErrorCode;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.ReadyStatus;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.model.RoomStatus;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.exception.UserErrorCode;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import java.util.HashSet;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoomServiceTest {

    // 테스트용 고정 데이터
    private static final String TEST_ROOM_ID = "1234";
    private static final String TEST_ROOM_TITLE = "테스트 방";
    private static final Integer TEST_CAPACITY = 10;
    private static final Long NON_EXISTENT_USER_ID = -1L;
    private RoomService roomService;
    private RoomRepository roomRepository;
    private UserMemoryRepository userRepository;
    private PlayerRepository playerRepository;

    @BeforeEach
    void setup() {
        roomRepository = new RoomMemoryRepository();
        userRepository = new UserMemoryRepository();
        playerRepository = new PlayerMemoryRepository();
        roomService = new RoomService(roomRepository, userRepository, playerRepository);
    }

    /**
     * User 생성 및 저장 헬퍼 메서드
     */
    private User createAndSaveUser(String sessionId, String nickname) {
        User user = User.create(sessionId, nickname, UserRole.GUEST);
        return userRepository.save(user);
    }

    /**
     * Room 생성 및 저장 + 호스트 Player 참가 헬퍼 메서드
     */
    private Room createAndSaveRoomWithHost(String roomId, String roomTitle, Integer capacity, User host) {
        Room room = Room.create(roomId, roomTitle, host.getUserId(), capacity);
        roomRepository.save(room);
        playerRepository.create(Player.create(host.getUserId(), roomId, host.getNickname()));
        return room;
    }

    private void assertThrowsCustomExceptionWithCode(ThrowingCallable callable, String expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .satisfies(throwable ->
                        assertThat(((CustomException) throwable).getErrorCode().getCode()).isEqualTo(expectedCode));
    }

    @Nested
    class FetchRoomsTests {

        @Test
        void 방_목록을_정상적으로_조회한다() {
            // given
            User host1 = createAndSaveUser("session-1", "호스트1");
            User host2 = createAndSaveUser("session-2", "호스트2");
            createAndSaveRoomWithHost("1111", "방1", 6, host1);
            createAndSaveRoomWithHost("2222", "방2", 6, host2);

            int page = 0;
            int size = 10;

            // when
            RoomListResponse response = roomService.fetchRooms(page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.rooms()).hasSize(2);
            assertThat(response.totalPages()).isEqualTo(1);
            assertThat(response.totalElements()).isEqualTo(2);
            assertThat(response.hasNext()).isFalse();
            assertThat(response.page()).isEqualTo(page);
            assertThat(response.size()).isEqualTo(size);

            RoomResponse roomResponse = response.rooms().getFirst();
            assertThat(roomResponse.roomId()).isIn("1111", "2222");
            assertThat(roomResponse.roomTitle()).isIn("방1", "방2");
            assertThat(roomResponse.canJoin()).isTrue();
            assertThat(roomResponse.capacity()).isEqualTo(6);
            assertThat(roomResponse.status()).isEqualTo("WAITING");
            assertThat(roomResponse.playerCount()).isOne();
        }

        @Test
        void 방이_없을_때_빈_리스트를_반환한다() {
            // given
            int page = 0;
            int size = 10;

            // when
            RoomListResponse response = roomService.fetchRooms(page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.rooms()).isEmpty();
            assertThat(response.totalPages()).isZero();
            assertThat(response.totalElements()).isZero();
            assertThat(response.hasNext()).isFalse();
            assertThat(response.page()).isEqualTo(page);
            assertThat(response.size()).isEqualTo(size);
        }

        @Test
        void 페이징이_정상적으로_작동한다() {
            // given
            for (int i = 0; i < 25; i++) {
                User host = createAndSaveUser("session-" + i, "호스트" + i);
                createAndSaveRoomWithHost(String.format("%04d", 1000 + i), "방" + i, 6, host);
            }
            int page = 0;
            int size = 10;

            // when
            RoomListResponse response = roomService.fetchRooms(page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.rooms()).hasSize(10);
            assertThat(response.totalPages()).isEqualTo(3);
            assertThat(response.totalElements()).isEqualTo(25);
            assertThat(response.hasNext()).isTrue();

            Set<String> roomIds = new HashSet<>();
            response.rooms().forEach(room -> roomIds.add(room.roomId()));

            page = 1;
            response = roomService.fetchRooms(page, size);
            response.rooms().forEach(room -> roomIds.add(room.roomId()));

            page = 2;
            response = roomService.fetchRooms(page, size);
            assertThat(response.hasNext()).isFalse();
            response.rooms().forEach(room -> roomIds.add(room.roomId()));
            assertThat(roomIds).hasSize(25);
        }

        @Test
        void 입장_가능한_방이_먼저_정렬되고_최신순으로_조회된다() {
            // given
            User host1 = createAndSaveUser("session-1", "호스트1");
            User host2 = createAndSaveUser("session-2", "호스트2");
            User host3 = createAndSaveUser("session-3", "호스트3");
            User host4 = createAndSaveUser("session-4", "호스트4");

            createAndSaveRoomWithHost("1111", "방1", 6, host1); // 입장 가능

            Room room2 = createAndSaveRoomWithHost("2222", "방2", 6, host2); // 입장 불가 (풀)
            // 호스트 포함 총 6명으로 방을 가득 채움
            for (int i = 0; i < 5; i++) {
                User player = createAndSaveUser("player-2-" + i, "플레이어" + i);
                playerRepository.create(Player.create(player.getUserId(), room2.getRoomId(), player.getNickname()));
            }

            createAndSaveRoomWithHost("3333", "방3", 6, host3); // 입장 가능

            Room room4 = createAndSaveRoomWithHost("4444", "방4", 6, host4); // 입장 불가 (게임 중)
            room4.startGame();

            int page = 0;
            int size = 10;

            // when
            RoomListResponse response = roomService.fetchRooms(page, size);

            // then
            assertThat(response).isNotNull();
            assertThat(response.rooms()).hasSize(4);
            assertThat(response.rooms().get(0).roomId()).isEqualTo("3333"); // 입장 가능한 방 중에 최신 방이 먼저 옴
            assertThat(response.rooms().get(1).roomId()).isEqualTo("1111");
            assertThat(response.rooms().get(2).roomId()).isEqualTo("4444"); // 그 다음 입장 불가 방
            assertThat(response.rooms().get(3).roomId()).isEqualTo("2222");
        }

        @Test
        void 페이징_변수가_유효하지_않을_경우_예외를_발생한다() {
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.fetchRooms(-1, 10),
                    RoomErrorCode.ROOM_PAGING_PARAMETER_INVALID.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.fetchRooms(0, 0),
                    RoomErrorCode.ROOM_PAGING_PARAMETER_INVALID.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.fetchRooms(0, -1),
                    RoomErrorCode.ROOM_PAGING_PARAMETER_INVALID.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.fetchRooms(0, 101),
                    RoomErrorCode.ROOM_PAGING_PARAMETER_INVALID.name());
        }

        @Test
        void 페이징_경계값은_정상적으로_조회된다() {
            assertThatCode(() -> roomService.fetchRooms(0, 1))
                    .doesNotThrowAnyException();
            assertThatCode(() -> roomService.fetchRooms(0, 100))
                    .doesNotThrowAnyException();
        }

        @Test
        void 페이지_범위를_초과하면_빈_리스트를_반환한다() {
            // given
            for (int i = 0; i < 3; i++) {
                User host = createAndSaveUser("overflow-session-" + i, "호스트" + i);
                createAndSaveRoomWithHost(String.format("%04d", 1000 + i), "방" + i, 6, host);
            }

            // when
            RoomListResponse response = roomService.fetchRooms(2, 2);

            // then
            assertThat(response.rooms()).isEmpty();
            assertThat(response.page()).isEqualTo(2);
            assertThat(response.size()).isEqualTo(2);
            assertThat(response.totalElements()).isEqualTo(3);
            assertThat(response.totalPages()).isEqualTo(2);
            assertThat(response.hasNext()).isFalse();
        }
    }

    @Nested
    class CreateRoomTests {

        @Test
        void 방을_정상적으로_생성한다() {
            // given
            User host = createAndSaveUser("test-session", "호스트");

            // when
            var response = roomService.createRoom("방1", 6, host.getUserId());

            // then
            assertThat(roomRepository.findAll()).hasSize(1);
            Room room = roomRepository.findAll().getFirst();
            assertThat(room.getRoomId()).matches("\\d{4}");
            assertThat(room.getRoomTitle()).isEqualTo("방1");
            assertThat(room.getCapacity()).isEqualTo(6);
            assertThat(room.getHostId()).isEqualTo(host.getUserId());
            assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);

            // 호스트가 Player로 자동 참가되었는지 확인
            assertThat(playerRepository.countByRoomId(room.getRoomId())).isEqualTo(1);
            assertThat(playerRepository.existsByRoomIdAndUserId(room.getRoomId(), host.getUserId())).isTrue();

            // 응답 DTO 확인
            assertThat(response.room().roomId()).isEqualTo(room.getRoomId());
            assertThat(response.room().roomTitle()).isEqualTo("방1");
            assertThat(response.room().hostId()).isEqualTo(host.getUserId());
            assertThat(response.room().capacity()).isEqualTo(6);
            assertThat(response.room().status()).isEqualTo(RoomStatus.WAITING);
            assertThat(response.players()).hasSize(1);
            assertThat(response.players().getFirst().userId()).isEqualTo(host.getUserId());
            assertThat(response.players().getFirst().status()).isEqualTo(ReadyStatus.NOT_READY);
            assertThat(response.players().getFirst().role()).isEqualTo(PlayerRole.HOST);
        }

        @Test
        void 수용_인원수가_null이면_기본값으로_방을_생성한다() {
            // given
            User host = createAndSaveUser("test-session-default-capacity", "호스트");

            // when
            var response = roomService.createRoom("기본 정원 방", null, host.getUserId());

            // then
            Room room = roomRepository.findAll().getFirst();
            assertThat(room.getCapacity()).isEqualTo(6);
            assertThat(response.room().capacity()).isEqualTo(6);
            assertThat(response.players()).hasSize(1);
        }

        @Test
        void 제목_앞뒤_공백은_제거되어_저장된다() {
            // given
            User host = createAndSaveUser("test-session-title-trim", "호스트");

            // when
            var response = roomService.createRoom("   공백 포함 제목   ", TEST_CAPACITY, host.getUserId());

            // then
            Room room = roomRepository.findAll().getFirst();
            assertThat(room.getRoomTitle()).isEqualTo("공백 포함 제목");
            assertThat(response.room().roomTitle()).isEqualTo("공백 포함 제목");
        }

        @Test
        void 존재하지_않는_유저로_방을_생성할_경우_예외를_발생한다() {
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom(TEST_ROOM_TITLE, TEST_CAPACITY, NON_EXISTENT_USER_ID),
                    UserErrorCode.USER_NOT_FOUND.name());
            assertThat(roomRepository.findAll()).isEmpty();
            assertThat(playerRepository.findAll()).isEmpty();
        }

        @Test
        void 이미_방에_참가중인_유저로_방을_생성할_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("test-session-already-in-room", "호스트");
            createAndSaveRoomWithHost("9999", "기존 방", TEST_CAPACITY, host);

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom(TEST_ROOM_TITLE, TEST_CAPACITY, host.getUserId()),
                    RoomErrorCode.USER_ALREADY_IN_ROOM.name());

            // 기존 데이터 유지
            assertThat(roomRepository.findAll()).hasSize(1);
            assertThat(playerRepository.findAll()).hasSize(1);
            assertThat(playerRepository.existsByRoomIdAndUserId("9999", host.getUserId())).isTrue();
        }

        @Test
        void 제목_경계값을_검증한다() {
            User host = createAndSaveUser("test-session", "호스트");
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom("   ", TEST_CAPACITY, host.getUserId()),
                    RoomErrorCode.ROOM_TITLE_INVALID.name());

            String maxLengthTitle = "A".repeat(20);
            User boundaryHost = createAndSaveUser("test-session-boundary", "호스트2");
            assertThatCode(() -> roomService.createRoom(maxLengthTitle, TEST_CAPACITY, boundaryHost.getUserId()))
                    .doesNotThrowAnyException();

            String overMaxLengthTitle = "A".repeat(21);
            User overLimitHost = createAndSaveUser("test-session-over-limit", "호스트3");
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom(overMaxLengthTitle, TEST_CAPACITY, overLimitHost.getUserId()),
                    RoomErrorCode.ROOM_TITLE_INVALID.name());

            String overMaxLengthTitleWithPadding = "   " + "A".repeat(21) + "   ";
            User paddedOverLimitHost = createAndSaveUser("test-session-padded-over-limit", "호스트4");
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom(overMaxLengthTitleWithPadding, TEST_CAPACITY,
                            paddedOverLimitHost.getUserId()),
                    RoomErrorCode.ROOM_TITLE_INVALID.name());
        }

        @Test
        void 수용_인원수_경계값을_검증한다() {
            User host = createAndSaveUser("test-session", "호스트");
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom(TEST_ROOM_TITLE, 1, host.getUserId()),
                    RoomErrorCode.ROOM_CAPACITY_INVALID.name());

            User minCapacityHost = createAndSaveUser("test-session-min-capacity", "호스트2");
            assertThatCode(() -> roomService.createRoom(TEST_ROOM_TITLE, 2, minCapacityHost.getUserId()))
                    .doesNotThrowAnyException();

            User maxCapacityHost = createAndSaveUser("test-session-max-capacity", "호스트3");
            assertThatCode(() -> roomService.createRoom(TEST_ROOM_TITLE, 10, maxCapacityHost.getUserId()))
                    .doesNotThrowAnyException();

            User overCapacityHost = createAndSaveUser("test-session-over-capacity", "호스트4");
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom(TEST_ROOM_TITLE, 11, overCapacityHost.getUserId()),
                    RoomErrorCode.ROOM_CAPACITY_INVALID.name());
        }

        @Test
        void 유효하지_않은_요청이면_방과_플레이어가_생성되지_않는다() {
            // given
            User host = createAndSaveUser("test-session-invalid-request", "호스트");

            // when
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom("   ", TEST_CAPACITY, host.getUserId()),
                    RoomErrorCode.ROOM_TITLE_INVALID.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.createRoom(TEST_ROOM_TITLE, 11, host.getUserId()),
                    RoomErrorCode.ROOM_CAPACITY_INVALID.name());

            // then
            assertThat(roomRepository.findAll()).isEmpty();
            assertThat(playerRepository.findAll()).isEmpty();
        }
    }

    @Nested
    class CheckRoomAvailabilityTests {

        @Test
        void 입장_가능한_방의_정보를_반환한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트");
            User guest = createAndSaveUser("guest-session", "게스트");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            // when
            var response = roomService.checkRoomAvailability(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(response.roomId()).isEqualTo(TEST_ROOM_ID);
            assertThat(response.canJoin()).isTrue();
        }

        @Test
        void 존재하지_않는_유저여도_입장_가능_여부는_조회된다() {
            User host = createAndSaveUser("host-session", "호스트");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            var response = roomService.checkRoomAvailability(TEST_ROOM_ID, NON_EXISTENT_USER_ID);
            assertThat(response.roomId()).isEqualTo(TEST_ROOM_ID);
            assertThat(response.canJoin()).isTrue();
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            User guest = createAndSaveUser("guest-session", "게스트");
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability("9999", guest.getUserId()),
                    RoomErrorCode.ROOM_NOT_FOUND.name());
        }

        @Test
        void 방_아이디가_유효하지_않을경우_예외를_발생한다() {
            User guest = createAndSaveUser("guest-session", "게스트");
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability("12AB", guest.getUserId()),
                    RoomErrorCode.ROOM_NUMBER_ERROR.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability("999", guest.getUserId()),
                    RoomErrorCode.ROOM_NUMBER_ERROR.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability("10000", guest.getUserId()),
                    RoomErrorCode.ROOM_NUMBER_ERROR.name());
        }

        @Test
        void 게임_중인_방일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트");
            User guest = createAndSaveUser("guest-session", "게스트");
            Room room = createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);
            room.startGame();

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_IN_GAME.name());
        }

        @Test
        void 가득_찬_방일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트");
            User guest = createAndSaveUser("guest-session", "게스트");
            Room room = createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            // 나머지 플레이어들 추가하여 방을 가득 채움
            for (int i = 1; i < TEST_CAPACITY; i++) {
                User player = createAndSaveUser("player-" + i, "플레이어" + i);
                playerRepository.create(Player.create(player.getUserId(), room.getRoomId(), player.getNickname()));
            }

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_FULL.name());
        }

        @Test
        void 이미_같은_방에_참가중인_유저는_입장_가능_확인시_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트");
            User guest = createAndSaveUser("guest-session", "게스트");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);
            roomService.joinRoom(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.USER_ALREADY_IN_ROOM.name());
        }

        @Test
        void 이미_다른_방에_참가중인_유저는_입장_가능_확인시_예외를_발생한다() {
            // given
            User host1 = createAndSaveUser("host-session-1", "호스트1");
            User host2 = createAndSaveUser("host-session-2", "호스트2");
            User guest = createAndSaveUser("guest-session", "게스트");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host1);
            createAndSaveRoomWithHost("9999", "다른 방", TEST_CAPACITY, host2);
            roomService.joinRoom(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.checkRoomAvailability("9999", guest.getUserId()),
                    RoomErrorCode.USER_ALREADY_IN_ROOM.name());
        }
    }

    @Nested
    class JoinRoomTests {
        @Test
        void 플레이어가_방에_정상적으로_참가한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            // when
            PlayerJoinEvent result = roomService.joinRoom(TEST_ROOM_ID, guest.getUserId());

            // then: 반환값(공개 API 계약) 검증
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_JOIN);

            // 방 정보 검증
            assertThat(result.room().roomId()).isEqualTo(TEST_ROOM_ID);
            assertThat(result.room().roomTitle()).isEqualTo(TEST_ROOM_TITLE);
            assertThat(result.room().hostId()).isEqualTo(host.getUserId());

            // 플레이어 목록 검증
            assertThat(result.players()).hasSize(2);
            assertThat(result.players())
                    .extracting("userId")
                    .containsExactlyInAnyOrder(host.getUserId(), guest.getUserId());
            assertThat(result.players().stream()
                    .filter(playerInfo -> playerInfo.userId().equals(host.getUserId()))
                    .findFirst()
                    .orElseThrow()
                    .role()).isEqualTo(PlayerRole.HOST);
            assertThat(result.players().stream()
                    .filter(playerInfo -> playerInfo.userId().equals(guest.getUserId()))
                    .findFirst()
                    .orElseThrow()
                    .role()).isEqualTo(PlayerRole.GUEST);
        }

        @Test
        void 존재하지_않는_유저일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom(TEST_ROOM_ID, NON_EXISTENT_USER_ID),
                    UserErrorCode.USER_NOT_FOUND.name());
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_NOT_FOUND.name());
        }

        @Test
        void 방_아이디가_유효하지_않을경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session-invalid-room-id", "게스트유저");

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom("12AB", guest.getUserId()),
                    RoomErrorCode.ROOM_NUMBER_ERROR.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom("999", guest.getUserId()),
                    RoomErrorCode.ROOM_NUMBER_ERROR.name());
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom("10000", guest.getUserId()),
                    RoomErrorCode.ROOM_NUMBER_ERROR.name());
        }

        @Test
        void 이미_현재_방에_참가한_플레이어일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);
            roomService.joinRoom(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.USER_ALREADY_IN_ROOM.name());
        }

        @Test
        void 다른_방에_참가한_유저일_경우_예외를_발생한다() {
            // given
            User host1 = createAndSaveUser("host-session-1", "호스트1");
            User host2 = createAndSaveUser("host-session-2", "호스트2");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host1);
            createAndSaveRoomWithHost("9999", "다른 방", TEST_CAPACITY, host2);
            roomService.joinRoom(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom("9999", guest.getUserId()),
                    RoomErrorCode.USER_ALREADY_IN_ROOM.name());
        }

        @Test
        void 방이_가득_찬_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            // 호스트 포함 방을 가득 채움
            for (int i = 1; i < TEST_CAPACITY; i++) {
                User player = createAndSaveUser("player-" + i, "플레이어" + i);
                playerRepository.create(Player.create(player.getUserId(), room.getRoomId(), player.getNickname()));
            }

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_FULL.name());
        }

        @Test
        void 게임_중인_방일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);
            room.startGame();

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_IN_GAME.name());
        }
    }

    @Nested
    class LeaveRoomTests {
        @Test
        void 플레이어가_방에서_정상적으로_퇴장한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when
            var result = roomService.leaveRoom(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.playerLeftEvent().type()).isEqualTo(RoomResponseType.PLAYER_LEFT);
            assertThat(result.playerLeftEvent().player().userId()).isEqualTo(guest.getUserId());
            assertThat(result.playerLeftEvent().player().role()).isEqualTo(PlayerRole.GUEST);
            assertThat(result.hostChangedEvent()).isNull();
        }

        @Test
        void 플레이어_정보가_없으면_퇴장할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            assertThrowsCustomExceptionWithCode(
                    () -> roomService.leaveRoom(TEST_ROOM_ID, NON_EXISTENT_USER_ID),
                    PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM.name());
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.leaveRoom(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_NOT_FOUND.name());
        }

        @Test
        void 유저는_존재하지만_어떤_방에도_참가하지_않으면_퇴장할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest1 = createAndSaveUser("guest-session-1", "게스트유저1");
            User guest2 = createAndSaveUser("guest-session-2", "게스트유저2");

            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);
            playerRepository.create(Player.create(guest1.getUserId(), TEST_ROOM_ID, guest1.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.leaveRoom(TEST_ROOM_ID, guest2.getUserId()),
                    PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM.name());
        }

        @Test
        void 다른_방에_참가한_플레이어가_퇴장요청하면_예외를_발생한다() {
            // given
            User host1 = createAndSaveUser("host-session-1", "호스트1");
            User host2 = createAndSaveUser("host-session-2", "호스트2");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host1);
            createAndSaveRoomWithHost("9999", "다른 방", TEST_CAPACITY, host2);
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.leaveRoom("9999", guest.getUserId()),
                    PlayerErrorCode.PLAYER_NOT_IN_ROOM.name());
        }

        @Test
        void 호스트가_퇴장할_경우_HostChangedEvent를_반환하고_다음_플레이어가_호스트가_된다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest1 = createAndSaveUser("guest-session-1", "게스트유저1");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest1.getUserId(), TEST_ROOM_ID, guest1.getNickname()));

            // when
            var result = roomService.leaveRoom(TEST_ROOM_ID, host.getUserId());

            // then
            assertThat(result.playerLeftEvent().type()).isEqualTo(RoomResponseType.PLAYER_LEFT);
            assertThat(result.playerLeftEvent().player().userId()).isEqualTo(host.getUserId());
            assertThat(result.playerLeftEvent().player().role()).isEqualTo(PlayerRole.HOST);
            assertThat(result.hostChangedEvent()).isNotNull();
            assertThat(result.hostChangedEvent().type()).isEqualTo(RoomResponseType.HOST_CHANGED);
            assertThat(result.hostChangedEvent().oldHost().userId()).isEqualTo(host.getUserId());
            assertThat(result.hostChangedEvent().oldHost().role()).isEqualTo(PlayerRole.GUEST);
            assertThat(result.hostChangedEvent().newHost().userId()).isEqualTo(guest1.getUserId());
            assertThat(result.hostChangedEvent().newHost().role()).isEqualTo(PlayerRole.HOST);
            assertThat(result.hostChangedEvent().reason()).isEqualTo("HOST_LEFT");

            Room updatedRoom = roomRepository.findById(TEST_ROOM_ID).orElseThrow();
            assertThat(updatedRoom.getHostId()).isEqualTo(guest1.getUserId());
        }

        @Test
        void 준비완료_상태의_게스트가_방장이_되면_준비상태가_NOT_READY로_초기화된다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));
            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            Player readyGuest = playerRepository.findById(guest.getUserId()).orElseThrow();
            assertThat(readyGuest.getReadyStatus()).isEqualTo(ReadyStatus.READY);

            // when
            var result = roomService.leaveRoom(TEST_ROOM_ID, host.getUserId());

            // then
            Player newHost = playerRepository.findById(guest.getUserId()).orElseThrow();
            assertThat(newHost.getReadyStatus()).isEqualTo(ReadyStatus.NOT_READY);
            assertThat(result.hostChangedEvent()).isNotNull();
            assertThat(result.hostChangedEvent().newHost().status()).isEqualTo(ReadyStatus.NOT_READY);
        }

        @Test
        void 마지막_플레이어가_퇴장할_경우_방이_삭제된다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));

            // when
            var result = roomService.leaveRoom(TEST_ROOM_ID, host.getUserId());

            // then
            assertThat(result.playerLeftEvent().type()).isEqualTo(RoomResponseType.PLAYER_LEFT);
            assertThat(result.playerLeftEvent().player().userId()).isEqualTo(host.getUserId());
            assertThat(result.playerLeftEvent().player().role()).isEqualTo(PlayerRole.HOST);
            assertThat(result.hostChangedEvent()).isNull();
            assertThat(roomRepository.findById(TEST_ROOM_ID)).isEmpty();
            assertThat(playerRepository.countByRoomId(TEST_ROOM_ID)).isZero();
        }
    }

    @Nested
    class ReadyPlayerTests {
        @Test
        void 플레이어가_정상적으로_준비_완료_상태가_된다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when
            var result = roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_READY);
            assertThat(result.player().userId()).isEqualTo(guest.getUserId());
            assertThat(result.player().nickname()).isEqualTo("게스트유저");
            assertThat(result.player().role()).isEqualTo(PlayerRole.GUEST);
            assertThat(result.allReady()).isTrue(); // 호스트는 준비 상태가 아니여도 모든 게스트가 준비 상태이므로 true
        }

        @Test
        void 플레이어_정보가_없으면_준비_완료할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            assertThrowsCustomExceptionWithCode(
                    () -> roomService.readyPlayer(TEST_ROOM_ID, NON_EXISTENT_USER_ID),
                    PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM.name());
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_NOT_FOUND.name());
        }

        @Test
        void 유저는_존재하지만_어떤_방에도_참가하지_않으면_준비_완료할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            User otherPlayer = createAndSaveUser("other-session", "다른플레이어");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.readyPlayer(TEST_ROOM_ID, otherPlayer.getUserId()),
                    PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM.name());
        }

        @Test
        void 다른_방에_참가중인_플레이어가_준비_요청하면_예외를_발생한다() {
            // given
            User host1 = createAndSaveUser("ready-host-session-1", "호스트1");
            User host2 = createAndSaveUser("ready-host-session-2", "호스트2");
            User guest = createAndSaveUser("ready-guest-session", "게스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host1);
            createAndSaveRoomWithHost("9999", "다른 방", TEST_CAPACITY, host2);
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.readyPlayer("9999", guest.getUserId()),
                    PlayerErrorCode.PLAYER_NOT_IN_ROOM.name());
        }

        @Test
        void 호스트는_준비_완료_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.readyPlayer(TEST_ROOM_ID, host.getUserId()),
                    RoomErrorCode.HOST_CANNOT_READY.name());
        }

        @Test
        void 이미_준비_완료_상태일_경우에도_예외를_발생하지_않는다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThatCode(() -> roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 게임_중에는_준비_완료_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.startGame();
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_IN_GAME.name());
        }
    }

    @Nested
    class UnreadyPlayerTests {
        @Test
        void 플레이어가_정상적으로_준비_취소_상태가_된다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // when
            var result = roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_UNREADY);
            assertThat(result.player().userId()).isEqualTo(guest.getUserId());
            assertThat(result.player().nickname()).isEqualTo("게스트유저");
            assertThat(result.player().role()).isEqualTo(PlayerRole.GUEST);
        }

        @Test
        void 플레이어_정보가_없으면_준비_취소할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host);

            assertThrowsCustomExceptionWithCode(
                    () -> roomService.unreadyPlayer(TEST_ROOM_ID, NON_EXISTENT_USER_ID),
                    PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM.name());
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_NOT_FOUND.name());
        }

        @Test
        void 유저는_존재하지만_어떤_방에도_참가하지_않으면_준비_취소할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            User otherPlayer = createAndSaveUser("other-session", "다른플레이어");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.unreadyPlayer(TEST_ROOM_ID, otherPlayer.getUserId()),
                    PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM.name());
        }

        @Test
        void 다른_방에_참가중인_플레이어가_준비_취소_요청하면_예외를_발생한다() {
            // given
            User host1 = createAndSaveUser("unready-host-session-1", "호스트1");
            User host2 = createAndSaveUser("unready-host-session-2", "호스트2");
            User guest = createAndSaveUser("unready-guest-session", "게스트유저");
            createAndSaveRoomWithHost(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_CAPACITY, host1);
            createAndSaveRoomWithHost("9999", "다른 방", TEST_CAPACITY, host2);
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.unreadyPlayer("9999", guest.getUserId()),
                    PlayerErrorCode.PLAYER_NOT_IN_ROOM.name());
        }

        @Test
        void 호스트는_준비_취소_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.unreadyPlayer(TEST_ROOM_ID, host.getUserId()),
                    RoomErrorCode.HOST_CANNOT_READY.name());
        }

        @Test
        void 준비_완료_상태가_아닐_경우에도_예외를_발생하지_않는다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            // when then (준비하지 않은 상태에서 준비 취소 시도)
            assertThatCode(() -> roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .doesNotThrowAnyException();
        }

        @Test
        void 게임_중에는_준비_취소_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            playerRepository.create(Player.create(host.getUserId(), TEST_ROOM_ID, host.getNickname()));
            playerRepository.create(Player.create(guest.getUserId(), TEST_ROOM_ID, guest.getNickname()));

            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());
            room.startGame();

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId()),
                    RoomErrorCode.ROOM_IN_GAME.name());
        }
    }
}
