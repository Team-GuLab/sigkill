package com.gulab.sigkillserver.domain.room.service;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_CAPACITY_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_CREATE_ERROR;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_FULL;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_IN_GAME;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NOT_FOUND;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_TITLE_INVALID;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.dto.WebSocketInfo;
import com.gulab.sigkillserver.domain.room.dto.request.RoomCreateRequest;
import com.gulab.sigkillserver.domain.room.dto.response.RoomAvailabilityResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomResponse;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.model.Room.RoomStatus;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Room Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;

    /**
     * 방 목록 조회
     */
    public RoomListResponse fetchRooms(int page, int size) {
        log.info("방 목록 조회 - page: {}, size: {}", page, size);

        // TODO: 정렬 로직 개선
        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomRepository.findAllByStatus(pageable, RoomStatus.WAITING);
        new RoomListResponse(
                rooms.stream().map(room -> new RoomResponse(
                        room.getRoomId(),
                        room.getRoomTitle(),
                        room.getPlayerCount(),
                        room.getCapacity(),
                        room.getStatus().name()
                )).toList(),
                rooms.getNumber(),
                rooms.getSize(),
                rooms.getTotalElements(),
                rooms.getTotalPages(),
                rooms.hasNext()
        );
        return null;
    }

    /**
     * 방 생성
     */
    public RoomCreateResponse createRoom(RoomCreateRequest request, String sessionId) {
        log.info("방 생성 - title: {}, capacity: {}", request.roomTitle(), request.capacity());
        validateRoomCreateRequest(request);

        Long roomId = generateRoomId();

        Room room = Room.builder()
                .roomId(roomId)
                .roomTitle(request.roomTitle())
                .capacity(request.capacity())
                .hostSessionId(sessionId)
                .build();

        room = roomRepository.save(room);
        log.info("방 생성 완료 - roomId: {}", roomId);
        return RoomCreateResponse.of(room);
    }

    /**
     * 4자리 랜덤 정수 생성 (1000 ~ 9999)
     * 중복 체크하여 유니크한 ID 반환
     */
    private Long generateRoomId() {
        int maxAttempts = 100;

        for (int i = 0; i < maxAttempts; i++) {
            Long roomId = ThreadLocalRandom.current().nextLong(1000, 10000);

            // 중복 체크
            if (!roomRepository.existsById(roomId)) {
                return roomId;
            }
        }

        throw new CustomException(ROOM_CREATE_ERROR);
    }

    private void validateRoomCreateRequest(RoomCreateRequest request) {
        String roomTitle = request.roomTitle().strip();
        if (roomTitle.isBlank() || roomTitle.length() > MAX_TITLE_LENGTH) {
            throw new CustomException(ROOM_TITLE_INVALID);
        }

        if (request.capacity() != null &&
            (request.capacity() < MIN_CAPACITY || request.capacity() > MAX_CAPACITY)) {
            throw new CustomException(ROOM_CAPACITY_INVALID);
        }
    }

    /**
     * 방 참가 가능 여부 확인
     */
    public RoomAvailabilityResponse checkRoomAvailability(Long roomId, String sessionId) {
        log.info("방 참가 가능 여부 확인 - roomId: {}", roomId);
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new CustomException(ROOM_NOT_FOUND));
        if (room.getStatus() == RoomStatus.INGAME) {
            throw new CustomException(ROOM_IN_GAME);
        }

        if (room.getPlayerCount().equals(room.getCapacity())) {
            throw new CustomException(ROOM_FULL);
        }

        // TODO: 플레이어가 이미 다른 방에 접속했을 경우 처리
        return new RoomAvailabilityResponse(WebSocketInfo.of(room.getRoomId().toString()));
    }
}
