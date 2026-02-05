package com.gulab.sigkillserver.domain.room.dto.response;

import com.gulab.sigkillserver.domain.room.dto.WebSocketInfo;
import com.gulab.sigkillserver.domain.room.model.Room;

/**
 * 방 생성 응답 DTO
 */
public record RoomCreateResponse(
        String roomId,
        String roomTitle,
        Integer currentCapacity,
        Integer capacity,
        String status,
        WebSocketInfo ws
) {
    public static RoomCreateResponse of(Room room) {
        return new RoomCreateResponse(
                room.getRoomId(),
                room.getRoomTitle(),
                room.getCurrentCapacity(),
                room.getCapacity(),
                room.getStatus().name(),
                WebSocketInfo.of(room.getRoomId())
        );
    }
}
