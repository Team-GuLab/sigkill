package com.gulab.sigkillserver.domain.room.dto.response;

import com.gulab.sigkillserver.domain.room.model.Room;

/**
 * 방 정보 응답 DTO
 */
public record RoomResponse(
        Long roomId,
        String roomTitle,
        Integer playerCount,
        Integer capacity,
        String status
) {
    public static RoomResponse of(Room room) {
        return new RoomResponse(
                room.getRoomId(),
                room.getRoomTitle(),
                room.getPlayerCount(),
                room.getCapacity(),
                room.getStatus().name()
        );
    }
}
