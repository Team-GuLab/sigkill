package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;
import com.gulab.sigkillserver.domain.room.model.Room;

/**
 * 플레이어 참가 메시지
 */
public record PlayerJoinEvent(
        RoomResponseType type,
        RoomInfoResponse room,
        PlayerInfo player
) {
    public static PlayerJoinEvent of(Room room, PlayerInfo player) {
        return new PlayerJoinEvent(
                RoomResponseType.PLAYER_JOIN,
                RoomInfoResponse.of(room),
                player
        );
    }
}
