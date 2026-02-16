package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.model.Player;

public record PlayerLeftEvent(
        RoomResponseType type,
        PlayerInfo player
) {
    public static PlayerLeftEvent of(Player player) {
        return new PlayerLeftEvent(
                RoomResponseType.PLAYER_LEFT,
                PlayerInfo.of(player)
        );
    }
}
