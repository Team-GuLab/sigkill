package com.gulab.sigkillserver.domain.room.dto.stomp.shared;

import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.model.RoomStatus;

public record RoomInfo(
        String roomId,
        String roomTitle,
        String hostId,
        int capacity,
        RoomStatus status
) {
    public static RoomInfo of(Room room) {
        return new RoomInfo(
                room.getRoomId(),
                room.getRoomTitle(),
                room.getHostId(),
                room.getCapacity(),
                room.getStatus()
        );
    }
}