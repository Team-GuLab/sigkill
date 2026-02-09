package com.gulab.sigkillserver.domain.room.service;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.DEFAULT_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_CAPACITY_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_CREATE_ERROR;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_FULL;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_IN_GAME;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NOT_FOUND;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NUMBER_ERROR;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_PAGING_PARAMETER_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_TITLE_INVALID;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.dto.rest.WebSocketInfo;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomAvailabilityResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomResponse;
import com.gulab.sigkillserver.domain.room.dto.service.JoinRoomResult;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerLeftEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerUnreadyEvent;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Room Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    /**
     * 방 목록 조회
     */
    public RoomListResponse fetchRooms(int page, int size) {
        log.info("방 목록 조회 - page: {}, size: {}", page, size);
        validatePaginationParameters(page, size);

        Comparator<Room> comparator = Comparator.comparing(Room::canJoin).reversed()
                .thenComparing(Room::getCreatedAt, Comparator.reverseOrder());
        // TODO: updatedAt 기준 정렬로 변경

        int totalCount = (int) roomRepository.count();
        int totalPages = (totalCount + size - 1) / size;
        int offset = page * size;

        // 페이지 범위 초과 시 빈 리스트 반환
        if (offset >= totalCount) {
            return new RoomListResponse(
                    Collections.emptyList(),
                    page,
                    size,
                    totalCount,
                    totalPages,
                    false
            );
        }

        // Repository에서 정렬 및 페이징 처리
        List<RoomResponse> roomResponses = roomRepository.findAll(comparator, offset, size)
                .stream()
                .map(RoomResponse::of)
                .toList();

        boolean hasNext = (offset + size) < totalCount;

        return new RoomListResponse(
                roomResponses,
                page,
                size,
                totalCount,
                totalPages,
                hasNext
        );
    }

    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw new CustomException(ROOM_PAGING_PARAMETER_INVALID);
        }
        if (size <= 0 || size > 100) {
            throw new CustomException(ROOM_PAGING_PARAMETER_INVALID);
        }
    }

    /**
     * 방 생성
     */
    public RoomCreateResponse createRoom(String roomTitle, Integer capacity, String sessionId) {
        roomTitle = roomTitle.strip();
        int resolvedCapacity = capacity != null ? capacity : DEFAULT_CAPACITY;
        log.info("방 생성 - title: {}, capacity: {}", roomTitle, resolvedCapacity);
        validateRoomCreateRequest(roomTitle, resolvedCapacity);

        int maxAttempts = 100;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                String roomId = generateRoomId();
                Room room = Room.create(roomId, roomTitle, sessionId, resolvedCapacity);
                room = roomRepository.save(room);
                log.info("방 생성 완료 - roomId: {}", roomId);
                return RoomCreateResponse.of(room);
            } catch (IllegalStateException e) {
                log.debug("Room ID 중복 발생, 재시도 중 (attempt: {}): {}", i + 1, e.getMessage());
            }
        }

        throw new CustomException(ROOM_CREATE_ERROR);
    }

    /**
     * 4자리 랜덤 정수 생성 (1000 ~ 9999)
     */
    private String generateRoomId() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
    }

    private void validateRoomCreateRequest(String roomTitle, int capacity) {
        if (roomTitle.isBlank() || roomTitle.length() > MAX_TITLE_LENGTH) {
            throw new CustomException(ROOM_TITLE_INVALID);
        }

        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new CustomException(ROOM_CAPACITY_INVALID);
        }
    }

    /**
     * 방 참가 가능 여부 확인
     */
    public RoomAvailabilityResponse checkRoomAvailability(String roomId, String sessionId) {
        log.info("방 참가 가능 여부 확인 - roomId: {}", roomId);
        validateRoomId(roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOM_NOT_FOUND));

        // TODO: 동시성 문제 해결
        if (room.isInGame()) {
            throw new CustomException(ROOM_IN_GAME);
        }

        if (room.isFull()) {
            throw new CustomException(ROOM_FULL);
        }

        // TODO: 플레이어가 이미 다른 방에 접속했을 경우 처리
        return new RoomAvailabilityResponse(WebSocketInfo.of(room.getRoomId()));
    }

    private void validateRoomId(String roomId) {
        int roomIdInt;
        try {
            roomIdInt = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
        if (roomIdInt < 1000 || roomIdInt > 9999) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
    }

    /**
     * 플레이어 방 참가
     */
    public JoinRoomResult joinRoom(String roomId, String sessionId) {
        return null;
    }

    /**
     * 플레이어 방 퇴장
     */
    public PlayerLeftEvent leaveRoom(String roomId, String sessionId) {
        return null;
    }

    /**
     * 호스트 변경
     */
    public PlayerLeftEvent changeHost(String roomId, String newHostId, String previousHostId) {
        return null;
    }

    /**
     * 플레이어 준비 완료
     */
    public PlayerReadyEvent readyPlayer(String roomId, String sessionId) {
        return null;
    }

    /**
     * 플레이어 준비 취소
     */
    public PlayerUnreadyEvent unreadyPlayer(String roomId, String sessionId) {
        return null;
    }
}
