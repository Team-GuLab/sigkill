package com.gulab.sigkillserver.domain.room.controller;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.request.RoomCreateRequest;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Room Controller
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class RoomController {

    private final RoomService roomService;

    /**
     * 방 목록 조회
     */
    @GetMapping("/rooms")
    public BaseResponse<RoomListResponse> getRooms(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") int page,
            @RequestParam(defaultValue = "6") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다") int size) {
        RoomListResponse response = roomService.fetchRooms(page, size);
        return BaseResponse.onSuccess(response);
    }

    /**
     * 방 생성
     */
    @PostMapping("/rooms")
    public BaseResponse<RoomInfoResponse> createRoom(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        RoomInfoResponse response = roomService.createRoom(request.roomTitle(), request.capacity(), userId);
        return BaseResponse.onSuccess(response);
    }

    /**
     * 방 참가
     */
    @PostMapping("/rooms/{roomId}/join")
    public BaseResponse<RoomInfoResponse> join(
            @AuthenticationPrincipal Long userId,
            @PathVariable String roomId
    ) {
        RoomInfoResponse response = roomService.joinRoom(roomId, userId);
        return BaseResponse.onSuccess(response);
    }
}
