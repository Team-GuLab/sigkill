package com.gulab.sigkillserver.domain.room.dto.response;

import com.gulab.sigkillserver.domain.room.dto.WebSocketInfo;
import com.gulab.sigkillserver.domain.room.model.Room;

/**
 * 방 생성 응답 DTO
 */
public record RoomCreateResponse(
        Long roomId,
        String roomTitle,
        Integer playerCount,
        Integer capacity,
        String status,
        WebSocketInfo ws
) {
    public static RoomCreateResponse of(Room room) {
        WebSocketInfo wsInfo = new WebSocketInfo(
                "/ws/rooms/" + room.getRoomId(),
                "websocket",
                "json"
        );

        return new RoomCreateResponse(
                room.getRoomId(),
                room.getRoomTitle(),
                room.getPlayerCount(),
                room.getCapacity(),
                room.getStatus().name(),
                wsInfo
        );
    }


}
