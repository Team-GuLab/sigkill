package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.model.Player;

public record OtherPlayerJoinEvent(
        RoomResponseType type,
        PlayerInfo player
) {
    public static OtherPlayerJoinEvent of(Player player) {
        return new OtherPlayerJoinEvent(
                RoomResponseType.OTHER_PLAYER_JOIN,
                PlayerInfo.of(player)
        );
    }
}
