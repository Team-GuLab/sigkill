package com.gulab.sigkillserver.domain.room.dto.rest.response;

import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;

/**
 * 방 단건 REST 응답 DTO
 */
public record RoomEnvelopeResponse(
        RoomInfoResponse room
) {
    public static RoomEnvelopeResponse of(RoomInfoResponse roomInfoResponse) {
        return new RoomEnvelopeResponse(roomInfoResponse);
    }
}
