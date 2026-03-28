package com.gulab.sigkillserver.domain.room.dto.rest.response;

import com.gulab.sigkillserver.domain.room.model.Room;

/**
 * 방 정보 응답 DTO
 */
public record RoomResponse(
        String roomId,
        String roomTitle,
        Integer playerCount,
        Integer capacity,
        String status,
        boolean canJoin
) {
    public static RoomResponse of(Room room, int playerCount) {
        boolean canJoin = !room.isClosing() && !room.isInGame() && playerCount < room.getCapacity();
        return new RoomResponse(
                room.getRoomId(),
                room.getRoomTitle(),
                playerCount,
                room.getCapacity(),
                room.getStatus().name(),
                canJoin
        );
    }
}
