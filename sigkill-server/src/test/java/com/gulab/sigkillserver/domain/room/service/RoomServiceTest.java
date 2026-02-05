package com.gulab.sigkillserver.domain.room.service;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_CAPACITY_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_FULL;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_IN_GAME;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NOT_FOUND;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NUMBER_ERROR;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_TITLE_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.dto.request.RoomCreateRequest;
import com.gulab.sigkillserver.domain.room.dto.response.RoomAvailabilityResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomResponse;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RoomService 테스트")
class RoomServiceTest {

    private RoomService roomService;
    private RoomRepository roomRepository;

    // 테스트용 고정 데이터
    private static final String TEST_ROOM_ID = "1234";
    private static final String TEST_ROOM_TITLE = "테스트 방";
    private static final String TEST_HOST_ID = "test-host-session-id";
    private static final String TEST_PLAYER_ID = "test-player-session-id";
    private static final Integer TEST_CAPACITY = 5;

    @BeforeEach
    void setup() {
        roomRepository = new RoomMemoryRepository();
        roomService = new RoomService(roomRepository);
    }

    @Nested
    @DisplayName("방 목록 조회 기능")
    class FetchRoomsTests {

        @Test
        @DisplayName("방 목록을 정상적으로 조회한다")
        void fetchRooms_Success() {
            // Given
            Room room1 = createAndSaveRoom("1111", "방1", TEST_HOST_ID, 5);
            Room room2 = createAndSaveRoom("2222", "방2", TEST_HOST_ID, 5);
            int page = 0;
            int size = 10;

            // When
            RoomListResponse response = roomService.fetchRooms(page, size);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.rooms()).hasSize(2);
            assertThat(response.totalElements()).isEqualTo(2);
            assertThat(response.page()).isEqualTo(page);
            assertThat(response.size()).isEqualTo(size);
        }

        @Test
        @DisplayName("방이 없을 때 빈 리스트를 반환한다")
        void fetchRooms_ReturnsEmpty() {
            // Given
            int page = 0;
            int size = 10;

            // When
            RoomListResponse response = roomService.fetchRooms(page, size);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.rooms()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("페이징이 정상적으로 작동한다")
        void fetchRooms_PaginationWorks() {
            // Given - 15개 방 생성
            for (int i = 1; i <= 15; i++) {
                createAndSaveRoom(String.format("%04d", i), "방" + i, TEST_HOST_ID, 5);
            }
            int page = 0;
            int size = 10;

            // When
            RoomListResponse response = roomService.fetchRooms(page, size);

            // Then
            assertThat(response.rooms()).hasSize(10);
            assertThat(response.totalElements()).isEqualTo(15);
            assertThat(response.totalPages()).isEqualTo(2);
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("입장 가능한 방이 먼저 정렬되고 최신순으로 조회된다")
        void fetchRooms_SortsByCanJoinAndCreatedAt() {
            // Given
            Room fullRoom = createAndSaveRoom("1111", "가득찬방", TEST_HOST_ID, 2);
            fillRoom(fullRoom); // 방을 가득 채움

            Room inGameRoom = createAndSaveRoom("2222", "게임중방", TEST_HOST_ID, 5);
            inGameRoom.startGame();
            roomRepository.save(inGameRoom);

            Room availableRoom1 = createAndSaveRoom("3333", "입장가능방1", TEST_HOST_ID, 5);
            Room availableRoom2 = createAndSaveRoom("4444", "입장가능방2", TEST_HOST_ID, 5);

            // When
            RoomListResponse response = roomService.fetchRooms(0, 10);

            // Then
            List<RoomResponse> rooms = response.rooms();
            assertThat(rooms).hasSize(4);

            // 앞의 2개는 입장 가능한 방이어야 함
            assertThat(rooms.get(0).canJoin()).isTrue();
            assertThat(rooms.get(1).canJoin()).isTrue();

            // 뒤의 2개는 입장 불가한 방이어야 함
            assertThat(rooms.get(2).canJoin()).isFalse();
            assertThat(rooms.get(3).canJoin()).isFalse();

            // 입장 가능한 방들은 최신순으로 정렬 (4444가 3333보다 나중에 생성됨)
            assertThat(rooms.get(0).roomId()).isEqualTo("4444");
            assertThat(rooms.get(1).roomId()).isEqualTo("3333");
        }

        @Test
        @DisplayName("2페이지를 정상적으로 조회한다")
        void fetchRooms_SecondPageWorks() {
            // Given - 15개 방 생성
            for (int i = 1; i <= 15; i++) {
                createAndSaveRoom(String.format("%04d", i), "방" + i, TEST_HOST_ID, 5);
            }
            int page = 1; // 2페이지
            int size = 10;

            // When
            RoomListResponse response = roomService.fetchRooms(page, size);

            // Then
            assertThat(response.rooms()).hasSize(5); // 15개 중 10개는 1페이지, 나머지 5개가 2페이지
            assertThat(response.totalElements()).isEqualTo(15);
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.hasNext()).isFalse(); // 마지막 페이지
        }

        @Test
        @DisplayName("마지막 페이지는 남은 개수만 반환한다")
        void fetchRooms_LastPageReturnsRemaining() {
            // Given - 23개 방 생성
            for (int i = 1; i <= 23; i++) {
                createAndSaveRoom(String.format("%04d", i), "방" + i, TEST_HOST_ID, 5);
            }
            int page = 2; // 3페이지 (0, 1, 2)
            int size = 10;

            // When
            RoomListResponse response = roomService.fetchRooms(page, size);

            // Then
            assertThat(response.rooms()).hasSize(3); // 23개 = 10 + 10 + 3
            assertThat(response.totalElements()).isEqualTo(23);
            assertThat(response.totalPages()).isEqualTo(3);
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("첫 페이지에서 hasNext가 true이다")
        void fetchRooms_FirstPageHasNext() {
            // Given - 15개 방 생성
            for (int i = 1; i <= 15; i++) {
                createAndSaveRoom(String.format("%04d", i), "방" + i, TEST_HOST_ID, 5);
            }

            // When
            RoomListResponse response = roomService.fetchRooms(0, 10);

            // Then
            assertThat(response.hasNext()).isTrue();
        }

        @Test
        @DisplayName("단일 페이지에 모두 포함되면 hasNext가 false이다")
        void fetchRooms_SinglePageHasNoNext() {
            // Given - 5개 방 생성
            for (int i = 1; i <= 5; i++) {
                createAndSaveRoom(String.format("%04d", i), "방" + i, TEST_HOST_ID, 5);
            }

            // When
            RoomListResponse response = roomService.fetchRooms(0, 10);

            // Then
            assertThat(response.rooms()).hasSize(5);
            assertThat(response.hasNext()).isFalse();
        }

        @Test
        @DisplayName("페이지 범위를 초과하면 빈 리스트를 반환한다")
        void fetchRooms_OutOfRangeReturnsEmpty() {
            // Given
            createAndSaveRoom("1111", "방1", TEST_HOST_ID, 5);
            int page = 5; // 존재하지 않는 페이지
            int size = 10;

            // When
            RoomListResponse response = roomService.fetchRooms(page, size);

            // Then
            assertThat(response.rooms()).isEmpty();
            assertThat(response.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("방 생성 기능")
    class CreateRoomTests {

        @Test
        @DisplayName("방을 정상적으로 생성한다")
        void createRoom_Success() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest(TEST_ROOM_TITLE, TEST_CAPACITY);

            // When
            RoomCreateResponse response = roomService.createRoom(request, TEST_HOST_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.roomId()).matches("\\d{4}"); // 4자리 숫자
            assertThat(response.roomTitle()).isEqualTo(TEST_ROOM_TITLE);
            assertThat(response.capacity()).isEqualTo(TEST_CAPACITY);
            assertThat(response.currentCapacity()).isEqualTo(1); // 호스트가 자동 입장

            // DB에서 실제로 저장되었는지 확인
            Room savedRoom = roomRepository.findById(response.roomId()).orElse(null);
            assertThat(savedRoom).isNotNull();
            assertThat(savedRoom.getHostId()).isEqualTo(TEST_HOST_ID);
        }

        @Test
        @DisplayName("빈 제목으로 생성 시 예외가 발생한다")
        void createRoom_EmptyTitle_ThrowsException() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest("", TEST_CAPACITY);

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(request, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_TITLE_INVALID.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("제목이 공백만 있을 때 예외가 발생한다")
        void createRoom_BlankTitle_ThrowsException() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest("   ", TEST_CAPACITY);

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(request, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_TITLE_INVALID.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("제목이 20자를 초과하면 예외가 발생한다")
        void createRoom_TitleTooLong_ThrowsException() {
            // Given
            String longTitle = "a".repeat(MAX_TITLE_LENGTH + 1);
            RoomCreateRequest request = new RoomCreateRequest(longTitle, TEST_CAPACITY);

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(request, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_TITLE_INVALID.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("제목이 최대 길이일 때 정상적으로 생성된다")
        void createRoom_MaxTitleLength_Success() {
            // Given
            String maxLengthTitle = "a".repeat(MAX_TITLE_LENGTH);
            RoomCreateRequest request = new RoomCreateRequest(maxLengthTitle, TEST_CAPACITY);

            // When
            RoomCreateResponse response = roomService.createRoom(request, TEST_HOST_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.roomTitle()).isEqualTo(maxLengthTitle);
        }

        @Test
        @DisplayName("인원이 최소값 미만일 때 예외가 발생한다")
        void createRoom_CapacityBelowMin_ThrowsException() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest(TEST_ROOM_TITLE, MIN_CAPACITY - 1);

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(request, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_CAPACITY_INVALID.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("인원이 최대값 초과일 때 예외가 발생한다")
        void createRoom_CapacityAboveMax_ThrowsException() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest(TEST_ROOM_TITLE, MAX_CAPACITY + 1);

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(request, TEST_HOST_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_CAPACITY_INVALID.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("인원이 최소값일 때 정상적으로 생성된다")
        void createRoom_MinCapacity_Success() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest(TEST_ROOM_TITLE, MIN_CAPACITY);

            // When
            RoomCreateResponse response = roomService.createRoom(request, TEST_HOST_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.capacity()).isEqualTo(MIN_CAPACITY);
        }

        @Test
        @DisplayName("인원이 최대값일 때 정상적으로 생성된다")
        void createRoom_MaxCapacity_Success() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest(TEST_ROOM_TITLE, MAX_CAPACITY);

            // When
            RoomCreateResponse response = roomService.createRoom(request, TEST_HOST_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.capacity()).isEqualTo(MAX_CAPACITY);
        }

        @Test
        @DisplayName("capacity가 null일 때 정상적으로 생성된다")
        void createRoom_NullCapacity_Success() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest(TEST_ROOM_TITLE, null);

            // When
            RoomCreateResponse response = roomService.createRoom(request, TEST_HOST_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.roomId()).isNotBlank();
        }

        @Test
        @DisplayName("생성된 방 ID는 4자리 숫자이다")
        void createRoom_RoomIdIsFourDigits() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest(TEST_ROOM_TITLE, TEST_CAPACITY);

            // When
            RoomCreateResponse response = roomService.createRoom(request, TEST_HOST_ID);

            // Then
            assertThat(response.roomId()).matches("\\d{4}");
            int roomIdInt = Integer.parseInt(response.roomId());
            assertThat(roomIdInt).isBetween(1000, 9999);
        }
    }

    @Nested
    @DisplayName("방 입장 가능 여부 확인 기능")
    class CheckRoomAvailabilityTests {

        @Test
        @DisplayName("입장 가능한 방의 WebSocket 정보를 반환한다")
        void checkRoomAvailability_Success() {
            // Given
            Room room = createAndSaveRoom(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);

            // When
            RoomAvailabilityResponse response = roomService.checkRoomAvailability(TEST_ROOM_ID, TEST_PLAYER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.ws()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 방 ID로 조회 시 예외가 발생한다")
        void checkRoomAvailability_RoomNotFound() {
            // Given
            String nonExistentRoomId = "9999";

            // When & Then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(nonExistentRoomId, TEST_PLAYER_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_NOT_FOUND.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("게임 중인 방에 입장하려고 하면 예외가 발생한다")
        void checkRoomAvailability_RoomInGame_ThrowsException() {
            // Given
            Room room = createAndSaveRoom(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, TEST_CAPACITY);
            room.startGame();
            roomRepository.save(room);

            // When & Then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(TEST_ROOM_ID, TEST_PLAYER_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_IN_GAME.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("정원이 가득 찬 방에 입장하려고 하면 예외가 발생한다")
        void checkRoomAvailability_RoomFull_ThrowsException() {
            // Given
            Room room = createAndSaveRoom(TEST_ROOM_ID, TEST_ROOM_TITLE, TEST_HOST_ID, 2);
            fillRoom(room);

            // When & Then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(TEST_ROOM_ID, TEST_PLAYER_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_FULL.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("방 번호가 숫자가 아니면 예외가 발생한다")
        void checkRoomAvailability_InvalidRoomIdFormat_ThrowsException() {
            // Given
            String invalidRoomId = "abcd";

            // When & Then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(invalidRoomId, TEST_PLAYER_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_NUMBER_ERROR.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("방 번호가 4자리 미만이면 예외가 발생한다")
        void checkRoomAvailability_RoomIdTooShort_ThrowsException() {
            // Given
            String shortRoomId = "999";

            // When & Then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(shortRoomId, TEST_PLAYER_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_NUMBER_ERROR.getErrorCode().getCode()));
        }

        @Test
        @DisplayName("방 번호가 4자리 초과이면 예외가 발생한다")
        void checkRoomAvailability_RoomIdTooLong_ThrowsException() {
            // Given
            String longRoomId = "10000";

            // When & Then
            assertThatThrownBy(() -> roomService.checkRoomAvailability(longRoomId, TEST_PLAYER_ID))
                    .isInstanceOf(CustomException.class)
                    .matches(e -> ((CustomException) e).getErrorCode().getCode()
                            .equals(ROOM_NUMBER_ERROR.getErrorCode().getCode()));
        }
    }

    // 헬퍼 메소드

    /**
     * Room 객체를 생성하고 저장
     */
    private Room createAndSaveRoom(String roomId, String title, String hostId, Integer capacity) {
        Room room = Room.create(roomId, title, hostId, capacity);
        return roomRepository.save(room);
    }

    /**
     * 방을 가득 채움 (capacity까지 플레이어 추가)
     */
    private void fillRoom(Room room) {
        int currentSize = room.getCurrentCapacity();
        for (int i = currentSize; i < room.getCapacity(); i++) {
            room.addPlayer("player-" + i);
        }
        roomRepository.save(room);
    }
}
