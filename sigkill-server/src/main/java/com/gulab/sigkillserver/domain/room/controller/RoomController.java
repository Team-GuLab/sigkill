package com.gulab.sigkillserver.domain.room.controller;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.request.RoomCreateRequest;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomEnvelopeResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;
import com.gulab.sigkillserver.domain.room.service.PendingRoomJoinOrchestrator;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Room API", description = "방 조회/생성/입장 API")
public class RoomController {

    private final RoomService roomService;
    private final PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator;

    /**
     * 방 목록 조회
     */
    @Operation(
            summary = "방 목록 조회",
            description = "대기방 목록을 페이지 단위로 조회합니다. page는 0 이상, size는 1~100 범위입니다."
    )
    @GetMapping("/rooms")
    public BaseResponse<RoomListResponse> getRooms(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다") int page,
            @Parameter(description = "페이지 크기(1~100)", example = "6")
            @RequestParam(defaultValue = "6") @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다") @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다") int size) {
        RoomListResponse response = roomService.fetchRooms(page, size);
        return BaseResponse.onSuccess(response);
    }

    /**
     * 방 생성
     */
    @Operation(
            summary = "방 생성",
            description = "새 대기방을 생성합니다. 제목은 공백 제외 최대 20자이며, 정원은 2~10명 범위입니다."
    )
    @PostMapping("/rooms")
    public BaseResponse<RoomEnvelopeResponse> createRoom(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        RoomInfoResponse roomInfoResponse = roomService.createRoom(request.roomTitle(), request.capacity(), userId);
        pendingRoomJoinOrchestrator.schedulePendingJoinTimeout(roomInfoResponse.roomId(), userId);
        RoomEnvelopeResponse response = RoomEnvelopeResponse.of(roomInfoResponse);
        return BaseResponse.onSuccess(response);
    }

    /**
     * 방 참가
     */
    @Operation(
            summary = "방 입장",
            description = "방 번호로 대기방에 입장합니다. 같은 방으로의 재요청은 성공으로 재응답하며 pending timeout을 연장하지 않습니다."
    )
    @PostMapping("/rooms/{roomId}/join")
    public BaseResponse<RoomEnvelopeResponse> join(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "4자리 방 번호", example = "1234")
            @PathVariable String roomId
    ) {
        RoomService.JoinRoomResult joinRoomResult = roomService.joinRoom(roomId, userId);
        RoomInfoResponse roomInfoResponse = joinRoomResult.roomInfoResponse();
        if (joinRoomResult.isCreatedPending()) {
            pendingRoomJoinOrchestrator.schedulePendingJoinTimeout(roomId, userId);
        }
        RoomEnvelopeResponse response = RoomEnvelopeResponse.of(roomInfoResponse);
        return BaseResponse.onSuccess(response);
    }
}
