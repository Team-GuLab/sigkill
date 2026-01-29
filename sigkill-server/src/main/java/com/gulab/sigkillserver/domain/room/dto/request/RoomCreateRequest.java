package com.gulab.sigkillserver.domain.room.dto.request;

/**
 * 방 생성 요청 DTO
 */
public record RoomCreateRequest(
        String roomTitle,
        Integer playerCount,
        Integer capacity
) {
}
