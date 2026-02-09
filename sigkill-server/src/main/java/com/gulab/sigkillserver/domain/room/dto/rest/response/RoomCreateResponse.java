package com.gulab.sigkillserver.domain.room.dto.rest.response;

import com.gulab.sigkillserver.domain.room.model.Room;

/**
 * 방 생성 응답 DTO
 */
public record RoomCreateResponse(
        String roomId,
        String roomTitle,
        Integer playerCount,
        Integer capacity,
        String status
) {
    public static RoomCreateResponse of(Room room) {
        return new RoomCreateResponse(
                room.getRoomId(),
                room.getRoomTitle(),
                room.getPlayerCount(),
                room.getCapacity(),
                room.getStatus().name()
        );
    }
}
