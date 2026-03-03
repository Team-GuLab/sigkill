package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.model.Player;

public record PlayerLeftEvent(
        RoomResponseType type,
        PlayerInfo player
) {
    public static PlayerLeftEvent of(Player player, Long hostId) {
        return new PlayerLeftEvent(
                RoomResponseType.PLAYER_LEFT,
                PlayerInfo.of(player, hostId)
        );
    }
}
