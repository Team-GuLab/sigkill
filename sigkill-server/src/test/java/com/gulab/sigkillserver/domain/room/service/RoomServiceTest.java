package com.gulab.sigkillserver.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomResponse;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.RoomResponseType;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.model.RoomStatus;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RoomServiceTest {

    // 테스트용 고정 데이터
    private static final String TEST_ROOM_ID = "1234";
    private static final String TEST_ROOM_TITLE = "테스트 방";
    private static final Integer TEST_CAPACITY = 10;
    private RoomService roomService;
    private RoomRepository roomRepository;
    private UserMemoryRepository userRepository;

    @BeforeEach
    void setup() {
        roomRepository = new RoomMemoryRepository();
        userRepository = new UserMemoryRepository();
        roomService = new RoomService(roomRepository, userRepository);
    }

    /**
     * User 생성 및 저장 헬퍼 메서드
     */
    private User createAndSaveUser(String sessionId, String nickname) {
        User user = User.create(sessionId, nickname, UserRole.GUEST);
        return userRepository.save(user);
    }

    @Nested
    class FetchRoomsTests {

        @Test
        void 방_목록을_정상적으로_조회한다() {
            // given
            User host1 = createAndSaveUser("session-1", "호스트1");
            User host2 = createAndSaveUser("session-2", "호스트2");
            Room room1 = Room.create("1111", "방1", host1.getUserId(), 6);
            Room room2 = Room.create("2222", "방2", host2.getUserId(), 6);
            roomRepository.save(room1);
            roomRepository.save(room2);

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
                Room room = Room.create(String.format("%04d", i), "방" + i, host.getUserId(), 6);
                roomRepository.save(room);
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

            Room room1 = Room.create("1111", "방1", host1.getUserId(), 6); // 입장 가능
            Room room2 = Room.create("2222", "방2", host2.getUserId(), 6); // 입장 불가 (풀)
            for (int i = 0; i < 5; i++) {
                User player = createAndSaveUser("player-2-" + i, "플레이어" + i);
                room2.addPlayer(player.getUserId());
            }
            Room room3 = Room.create("3333", "방3", host3.getUserId(), 6); // 입장 가능
            Room room4 = Room.create("4444", "방4", host4.getUserId(), 6); // 입장 불가 (게임 중)

            room4.startGame();

            roomRepository.save(room1);
            roomRepository.save(room2);
            roomRepository.save(room3);
            roomRepository.save(room4);

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
            assertThatThrownBy(() -> roomService.fetchRooms(-1, 0))
                    .isInstanceOf(CustomException.class);
            assertThatThrownBy(() -> roomService.fetchRooms(0, -1))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class CreateRoomTests {

        @Test
        void 방을_정상적으로_생성한다() {
            // given
            User host = createAndSaveUser("test-session", "호스트");

            // when
            roomService.createRoom("방1", 6, host.getUserId());

            // then
            assertThat(roomRepository.findAll()).hasSize(1);
            Room room = roomRepository.findAll().get(0);
            assertThat(room.getRoomId()).matches("\\d{4}");
            assertThat(room.getRoomTitle()).isEqualTo("방1");
            assertThat(room.getCapacity()).isEqualTo(6);
            assertThat(room.getHostId()).isEqualTo(host.getUserId());
            assertThat(room.getPlayerCount()).isEqualTo(1);
            assertThat(room.getPlayerIds()).contains(host.getUserId());
            assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
        }

        @Test
        void 제목이_유효하지_않을경우_예외가_발생한다() {
            User host = createAndSaveUser("test-session", "호스트");
            assertThatThrownBy(() -> roomService.createRoom("   ", TEST_CAPACITY, host.getUserId()))
                    .isInstanceOf(CustomException.class);
            String longTitle = "A".repeat(101);
            assertThatThrownBy(() -> roomService.createRoom(longTitle, TEST_CAPACITY, host.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 수용_인원수가_유효하지_않을경우_예외가_발생한다() {
            User host = createAndSaveUser("test-session", "호스트");
            assertThatThrownBy(() -> roomService.createRoom(TEST_ROOM_TITLE, 1, host.getUserId()))
                    .isInstanceOf(CustomException.class);
            assertThatThrownBy(() -> roomService.createRoom(TEST_ROOM_TITLE, 11, host.getUserId()))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class CheckRoomAvailabilityTests {

        @Test
        void 입장_가능한_방의_정보를_반환한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트");
            User guest = createAndSaveUser("guest-session", "게스트");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            // when
            var response = roomService.checkRoomAvailability(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(response.roomId()).isEqualTo(TEST_ROOM_ID);
            assertThat(response.canJoin()).isTrue();
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            User guest = createAndSaveUser("guest-session", "게스트");
            assertThatThrownBy(() -> roomService.checkRoomAvailability("9999", guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방_아이디가_유효하지_않을경우_예외를_발생한다() {
            User guest = createAndSaveUser("guest-session", "게스트");
            assertThatThrownBy(() -> roomService.checkRoomAvailability("12AB", guest.getUserId()))
                    .isInstanceOf(CustomException.class);
            assertThatThrownBy(() -> roomService.checkRoomAvailability("10000", guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중인_방일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트");
            User guest = createAndSaveUser("guest-session", "게스트");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.startGame();
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 가득_찬_방일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트");
            User guest = createAndSaveUser("guest-session", "게스트");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            for (int i = 1; i < TEST_CAPACITY; i++) {
                User player = createAndSaveUser("player-" + i, "플레이어" + i);
                room.addPlayer(player.getUserId());
            }
            roomRepository.save(room);
            // when then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class JoinRoomTests {
        @Test
        void 플레이어가_방에_정상적으로_참가한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

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
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 이미_현재_방에_참가한_플레이어일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);
            roomService.joinRoom(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 다른_방에_참가한_유저일_경우_예외를_발생한다() {
            // given
            User host1 = createAndSaveUser("host-session-1", "호스트1");
            User host2 = createAndSaveUser("host-session-2", "호스트2");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room1 = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host1.getUserId(), TEST_CAPACITY);
            roomRepository.save(room1);
            Room room2 = Room.create("9999", "다른 방", host2.getUserId(), TEST_CAPACITY);
            roomRepository.save(room2);
            roomService.joinRoom(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThatThrownBy(() -> roomService.joinRoom("9999", guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방이_가득_찬_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            for (int i = 1; i < TEST_CAPACITY; i++) {
                User player = createAndSaveUser("player-" + i, "플레이어" + i);
                room.addPlayer(player.getUserId());
            }
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중인_방일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.startGame();
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
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
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);

            // when
            var result = roomService.leaveRoom(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_LEFT);
            assertThat(result.player().userId()).isEqualTo(guest.getUserId());
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            // when then
            assertThatThrownBy(() -> roomService.leaveRoom(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방에_참가하지_않은_플레이어일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest1 = createAndSaveUser("guest-session-1", "게스트유저1");
            User guest2 = createAndSaveUser("guest-session-2", "게스트유저2");

            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            roomService.joinRoom(TEST_ROOM_ID, guest1.getUserId());

            // when then
            assertThatThrownBy(() -> roomService.leaveRoom(TEST_ROOM_ID, guest2.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 호스트가_퇴장할_경우_다음_플레이어가_호스트가_된다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest1 = createAndSaveUser("guest-session-1", "게스트유저1");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.addPlayer(guest1.getUserId());
            roomRepository.save(room);

            // when
            roomService.leaveRoom(TEST_ROOM_ID, host.getUserId());

            // then
            assertThat(room.getHostId()).isEqualTo(guest1.getUserId());
        }

        @Test
        void 마지막_플레이어가_퇴장할_경우_방이_삭제된다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            // when
            roomService.leaveRoom(TEST_ROOM_ID, host.getUserId());

            // then
            assertThat(roomRepository.findById(TEST_ROOM_ID)).isEmpty();
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
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);

            // when
            var result = roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_READY);
            assertThat(result.player().userId()).isEqualTo(guest.getUserId());
            assertThat(result.player().nickname()).isEqualTo("게스트유저");
            assertThat(result.allReady()).isFalse(); // 호스트는 준비 상태가 아님
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방에_참가하지_않은_플레이어일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            User otherPlayer = createAndSaveUser("other-session", "다른플레이어");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, otherPlayer.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 호스트는_준비_완료_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, host.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 이미_준비_완료_상태일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중에는_준비_완료_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.addPlayer(guest.getUserId());
            room.startGame();
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
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
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // when
            var result = roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId());

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_UNREADY);
            assertThat(result.player().userId()).isEqualTo(guest.getUserId());
            assertThat(result.player().nickname()).isEqualTo("게스트유저");
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User guest = createAndSaveUser("guest-session", "게스트유저");

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방에_참가하지_않은_플레이어일_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            User otherPlayer = createAndSaveUser("other-session", "다른플레이어");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, otherPlayer.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 호스트는_준비_취소_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, host.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 준비_완료_상태가_아닐_경우_예외를_발생한다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);

            // when then (준비하지 않은 상태에서 준비 취소 시도)
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중에는_준비_취소_할_수_없다() {
            // given
            User host = createAndSaveUser("host-session", "호스트유저");
            User guest = createAndSaveUser("guest-session", "게스트유저");
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, host.getUserId(), TEST_CAPACITY);
            room.addPlayer(guest.getUserId());
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, guest.getUserId());
            room.startGame();

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, guest.getUserId()))
                    .isInstanceOf(CustomException.class);
        }
    }
}
