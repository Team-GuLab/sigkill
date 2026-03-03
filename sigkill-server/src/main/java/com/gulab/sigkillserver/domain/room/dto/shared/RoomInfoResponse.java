package com.gulab.sigkillserver.domain.room.dto.shared;

import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.model.RoomStatus;

public record RoomInfoResponse(
        String roomId,
        String roomTitle,
        Long hostId,
        int capacity,
        RoomStatus status
) {
    public static RoomInfoResponse of(Room room) {
        return new RoomInfoResponse(
                room.getRoomId(),
                room.getRoomTitle(),
                room.getHostId(),
                room.getCapacity(),
                room.getStatus()
        );
    }
}