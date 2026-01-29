package com.gulab.sigkillserver.domain.room.dto.response;

import com.gulab.sigkillserver.domain.room.dto.WebSocketInfo;

/**
 * 방 참가 가능 여부 응답 DTO
 */
public record RoomAvailabilityResponse(
        WebSocketInfo ws
) {
}
