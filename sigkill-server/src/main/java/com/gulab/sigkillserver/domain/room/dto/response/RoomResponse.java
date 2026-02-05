package com.gulab.sigkillserver.domain.room.dto.response;

import com.gulab.sigkillserver.domain.room.model.Room;

/**
 * 방 정보 응답 DTO
 */
public record RoomResponse(
        String roomId,
        String roomTitle,
        Integer playerCount,
        Integer capacity,
        String status
) {
    public static RoomResponse of(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getTitle(),
                room.getCurrentCapacity(),
                room.getCapacity(),
                room.getStatus().name()
        );
    }
}
