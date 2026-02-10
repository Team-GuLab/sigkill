package com.gulab.sigkillserver.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomResponse;
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
    private static final String TEST_HOST_ID = "test-host-session-playerId";
    private static final String TEST_GUEST_ID = "test-player-session-playerId";
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

    @Nested
    class FetchRoomsTests {

        @Test
        void 방_목록을_정상적으로_조회한다() {
            // given
            Room room1 = Room.create("1111", "방1", "test-host-playerId-1", 6);
            Room room2 = Room.create("2222", "방2", "test-host-playerId-2", 6);
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
                Room room = Room.create(String.format("%04d", i), "방" + i, "test-host-playerId-" + i, 6);
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
            Room room1 = Room.create("1111", "방1", "test-host-playerId-1", 6); // 입장 가능
            Room room2 = Room.create("2222", "방2", "test-host-playerId-2", 6); // 입장 불가 (풀)
            for (int i = 0; i < 6; i++) {
                room2.addPlayer("player-" + i);
            }
            Room room3 = Room.create("3333", "방3", "test-host-playerId-3", 6); // 입장 가능
            Room room4 = Room.create("4444", "방4", "test-host-playerId-4", 6); // 입장 불가 (게임 중)

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
            // given when
            roomService.createRoom("방1", 6, TEST_HOST_ID);

            // then
            roomRepository.findById(TEST_ROOM_ID).ifPresent(room -> {
                assertThat(room.getRoomId()).matches("\\d{4}");
                assertThat(room.getRoomTitle()).isEqualTo("방1");
                assertThat(room.getCapacity()).isEqualTo(6);
                assertThat(room.getHostId()).isEqualTo(TEST_HOST_ID);
                assertThat(room.getPlayerCount()).isEqualTo(1);
                assertThat(room.getPlayerIds()).contains(TEST_HOST_ID);
                assertThat(room.getStatus()).isEqualTo(RoomStatus.WAITING);
            });
        }

        @Test
        void 제목이_유효하지_않을경우_예외가_발생한다() {
            assertThatThrownBy(() -> roomService.createRoom("   ", TEST_CAPACITY, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class);
            String longTitle = "A".repeat(101);
            assertThatThrownBy(() -> roomService.createRoom(longTitle, TEST_CAPACITY, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 수용_인원수가_유효하지_않을경우_예외가_발생한다() {
            assertThatThrownBy(() -> roomService.createRoom(TEST_ROOM_TITLE, 1, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class);
            assertThatThrownBy(() -> roomService.createRoom(TEST_ROOM_TITLE, 11, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class CheckRoomAvailabilityTests {

        @Test
        void 입장_가능한_방의_정보를_반환한다() {
            // given
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room);

            // when
            var response = roomService.checkRoomAvailability(TEST_ROOM_ID, TEST_GUEST_ID);

            // then
            assertThat(response.roomId()).isEqualTo(TEST_ROOM_ID);
            assertThat(response.canJoin()).isTrue();
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            assertThatThrownBy(() -> roomService.checkRoomAvailability("9999", TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방_아이디가_유효하지_않을경우_예외를_발생한다() {
            assertThatThrownBy(() -> roomService.checkRoomAvailability("12AB", TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
            assertThatThrownBy(() -> roomService.checkRoomAvailability("10000", TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중인_방일_경우_예외를_발생한다() {
            // given
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.startGame();
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 가득_찬_방일_경우_예외를_발생한다() {
            // given
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            for (int i = 1; i < TEST_CAPACITY; i++) {
                room.addPlayer("player-" + i);
            }
            roomRepository.save(room);
            // when then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class JoinRoomTests {
        @Test
        void 플레이어가_방에_정상적으로_참가한다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room);

            // when
            var result = roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID);

            // then: 반환값(공개 API 계약) 검증
            assertThat(result).isNotNull();
            assertThat(result.selfEvent()).isNotNull();
            assertThat(result.othersEvent()).isNotNull();

            // 본인에게 전달되는 이벤트 검증
            assertThat(result.selfEvent().room().roomId()).isEqualTo(TEST_ROOM_ID);
            assertThat(result.selfEvent().room().roomTitle()).isEqualTo(TEST_ROOM_TITLE);
            assertThat(result.selfEvent().room().hostId()).isEqualTo(TEST_HOST_ID);
            assertThat(result.selfEvent().players()).hasSize(2);
            assertThat(result.selfEvent().players())
                    .extracting("playerId")
                    .containsExactlyInAnyOrder(TEST_HOST_ID, TEST_GUEST_ID);

            // 다른 플레이어들에게 전달되는 이벤트 검증
            assertThat(result.othersEvent().player().playerId()).isEqualTo(TEST_GUEST_ID);
            assertThat(result.othersEvent().player().nickname()).isEqualTo("게스트유저");
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(user);

            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 이미_현재_방에_참가한_플레이어일_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(user);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room);
            roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID);

            // when then
            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 다른_방에_참가한_유저일_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(user);
            Room room1 = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room1);
            Room room2 = Room.create("9999", "다른 방", "other-host-playerId", TEST_CAPACITY);
            roomRepository.save(room2);
            roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID);

            // when then
            assertThatThrownBy(() -> roomService.joinRoom("9999", TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방이_가득_찬_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(user);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            for (int i = 1; i < TEST_CAPACITY; i++) {
                room.addPlayer("player-" + i);
            }
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중인_방일_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(user);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.startGame();
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class LeaveRoomTests {
        @Test
        void 플레이어가_방에서_정상적으로_퇴장한다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);

            // when
            var result = roomService.leaveRoom(TEST_ROOM_ID, TEST_GUEST_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_LEFT);
            assertThat(result.player().playerId()).isEqualTo(TEST_GUEST_ID);
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);

            // when then
            assertThatThrownBy(() -> roomService.leaveRoom(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방에_참가하지_않은_플레이어일_경우_예외를_발생한다() {
            // given
            String user2Id = "other-player-session-playerId";
            User user1 = User.create(TEST_GUEST_ID, "게스트유저1", UserRole.GUEST);
            userRepository.save(user1);
            User user2 = User.create(user2Id, "게스트유저2", UserRole.GUEST);
            userRepository.save(user2);

            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room);

            roomService.joinRoom(TEST_ROOM_ID, TEST_GUEST_ID);

            // when then
            assertThatThrownBy(() -> roomService.leaveRoom(TEST_ROOM_ID, user2Id))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 호스트가_퇴장할_경우_다음_플레이어가_호스트가_된다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest1 = User.create("guest1-session-playerId", "게스트유저1", UserRole.GUEST);
            userRepository.save(guest1);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(guest1.getUserId());
            roomRepository.save(room);

            // when
            roomService.leaveRoom(TEST_ROOM_ID, TEST_HOST_ID);

            // then
            assertThat(room.getHostId()).isEqualTo(guest1.getUserId());
        }

        @Test
        void 마지막_플레이어가_퇴장할_경우_방이_삭제된다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room);

            // when
            roomService.leaveRoom(TEST_ROOM_ID, TEST_HOST_ID);

            // then
            assertThat(roomRepository.findById(TEST_ROOM_ID)).isEmpty();
        }
    }

    @Nested
    class ReadyPlayerTests {
        @Test
        void 플레이어가_정상적으로_준비_완료_상태가_된다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);

            // when
            var result = roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_READY);
            assertThat(result.player().playerId()).isEqualTo(TEST_GUEST_ID);
            assertThat(result.player().nickname()).isEqualTo("게스트유저");
            assertThat(result.allReady()).isFalse(); // 호스트는 준비 상태가 아님
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(user);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방에_참가하지_않은_플레이어일_경우_예외를_발생한다() {
            // given
            String otherPlayerId = "other-player-session-playerId";
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            User otherPlayer = User.create(otherPlayerId, "다른플레이어", UserRole.GUEST);
            userRepository.save(otherPlayer);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, otherPlayerId))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 호스트는_준비_완료_할_수_없다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 이미_준비_완료_상태일_경우_예외를_발생한다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중에는_준비_완료_할_수_없다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            room.startGame();
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }
    }

    @Nested
    class UnreadyPlayerTests {
        @Test
        void 플레이어가_정상적으로_준비_취소_상태가_된다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID);

            // when
            var result = roomService.unreadyPlayer(TEST_ROOM_ID, TEST_GUEST_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(RoomResponseType.PLAYER_UNREADY);
            assertThat(result.player().playerId()).isEqualTo(TEST_GUEST_ID);
            assertThat(result.player().nickname()).isEqualTo("게스트유저");
        }

        @Test
        void 존재하지_않는_방일_경우_예외를_발생한다() {
            // given
            User user = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(user);

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 방에_참가하지_않은_플레이어일_경우_예외를_발생한다() {
            // given
            String otherPlayerId = "other-player-session-playerId";
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            User otherPlayer = User.create(otherPlayerId, "다른플레이어", UserRole.GUEST);
            userRepository.save(otherPlayer);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID);

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, otherPlayerId))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 호스트는_준비_취소_할_수_없다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            roomRepository.save(room);

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 준비_완료_상태가_아닐_경우_예외를_발생한다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);

            // when then (준비하지 않은 상태에서 준비 취소 시도)
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        void 게임_중에는_준비_취소_할_수_없다() {
            // given
            User host = User.create(TEST_HOST_ID, "호스트유저", UserRole.GUEST);
            userRepository.save(host);
            User guest = User.create(TEST_GUEST_ID, "게스트유저", UserRole.GUEST);
            userRepository.save(guest);
            Room room = Room.create(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.addPlayer(TEST_GUEST_ID);
            roomRepository.save(room);
            roomService.readyPlayer(TEST_ROOM_ID, TEST_GUEST_ID);
            room.startGame();

            // when then
            assertThatThrownBy(() -> roomService.unreadyPlayer(TEST_ROOM_ID, TEST_GUEST_ID))
                    .isInstanceOf(CustomException.class);
        }
    }
}
