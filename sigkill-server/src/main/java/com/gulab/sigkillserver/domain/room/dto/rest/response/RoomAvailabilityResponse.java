package com.gulab.sigkillserver.domain.room.dto.rest.response;

/**
 * 방 참가 가능 여부 응답 DTO
 */
public record RoomAvailabilityResponse(
        String roomId,
        boolean canJoin
) {
}
