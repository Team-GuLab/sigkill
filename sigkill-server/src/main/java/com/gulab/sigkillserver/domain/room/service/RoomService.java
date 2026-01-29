package com.gulab.sigkillserver.domain.room.service;

import com.gulab.sigkillserver.domain.room.dto.request.RoomCreateRequest;
import com.gulab.sigkillserver.domain.room.dto.response.RoomAvailabilityResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
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

    /**
     * 방 목록 조회
     */
    public RoomListResponse fetchRooms(int page, int size) {
        log.info("방 목록 조회 - page: {}, size: {}", page, size);
        // TODO: 구현 필요
        // 1. 대기중 우선
        // 2. 참여 가능 여부 우선
        // 3. 현재 인원 많은 순
        return null;
    }

    /**
     * 방 생성
     */
    public RoomCreateResponse createRoom(RoomCreateRequest request) {
        log.info("방 생성 - title: {}", request.roomTitle());
        // TODO: 구현 필요
        return null;
    }

    /**
     * 방 참가 가능 여부 확인
     */
    public RoomAvailabilityResponse checkRoomAvailability(Long roomId) {
        log.info("방 참가 가능 여부 확인 - roomId: {}", roomId);
        // TODO: 구현 필요
        return null;
    }
}
