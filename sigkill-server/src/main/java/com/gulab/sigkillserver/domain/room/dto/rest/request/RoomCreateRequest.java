package com.gulab.sigkillserver.domain.room.dto.rest.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 방 생성 요청 DTO
 */
public record RoomCreateRequest(
        @NotBlank String roomTitle,
        Integer capacity
) {
}
