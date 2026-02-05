package com.gulab.sigkillserver.domain.room.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 방 생성 요청 DTO
 */
public record RoomCreateRequest(
        @NotBlank String roomTitle,
        Integer capacity
) {
    public RoomCreateRequest {
        if (capacity == null) {
            capacity = 6;  // 기본값
        }
    }
}
