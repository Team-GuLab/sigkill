package com.gulab.sigkillserver.domain.room.dto.rest.response;

import com.gulab.sigkillserver.domain.room.dto.rest.WebSocketInfo;

/**
 * 방 참가 가능 여부 응답 DTO
 */
public record RoomAvailabilityResponse(
        WebSocketInfo ws
) {
}
