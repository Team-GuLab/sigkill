package com.gulab.sigkillserver.domain.room.controller;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.room.dto.request.RoomCreateRequest;
import com.gulab.sigkillserver.domain.room.dto.response.RoomAvailabilityResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.dto.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Room Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;

    /**
     * 방 목록 조회
     */
    @GetMapping("/v1/rooms")
    public BaseResponse<RoomListResponse> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        log.info("GET /api/v1/rooms - page: {}, size: {}", page, size);
        RoomListResponse response = roomService.fetchRooms(page, size);
        return BaseResponse.onSuccess(response);
    }

    /**
     * 방 생성
     */
    @PostMapping("/v1/rooms")
    public BaseResponse<RoomCreateResponse> createRoom(@RequestBody RoomCreateRequest request) {
        log.info("POST /api/v1/rooms - title: {}", request.roomTitle());
        RoomCreateResponse response = roomService.createRoom(request);
        return BaseResponse.onSuccess(response);
    }

    /**
     * 방 참가 가능 여부 확인
     */
    @GetMapping("/v1/rooms/{roomId}/availability")
    public BaseResponse<RoomAvailabilityResponse> checkRoomAvailability(@PathVariable Long roomId) {
        log.info("GET /api/v1/rooms/{}/availability", roomId);
        RoomAvailabilityResponse response = roomService.checkRoomAvailability(roomId);
        return BaseResponse.onSuccess(response);
    }
}
