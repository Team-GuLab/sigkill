package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;
import java.util.List;

public record RoomSnapshotEvent(
        RoomResponseType type,
        RoomInfoResponse room,
        List<PlayerInfo> players
) {
    public static RoomSnapshotEvent of(RoomInfoResponse room, List<PlayerInfo> players) {
        return new RoomSnapshotEvent(
                RoomResponseType.ROOM_SNAPSHOT,
                room,
                players
        );
    }
}
