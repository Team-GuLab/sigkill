package com.gulab.sigkillserver.domain.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 방 생성 요청 DTO
 */
public record RoomCreateRequest(
        @NotBlank String roomTitle,
        @NotNull Integer capacity
) {
}
