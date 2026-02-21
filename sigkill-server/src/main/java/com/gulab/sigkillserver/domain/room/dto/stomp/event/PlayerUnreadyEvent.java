package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.model.Player;

public record PlayerUnreadyEvent(
        RoomResponseType type,
        PlayerInfo player
) {
    public static PlayerUnreadyEvent of(Player player, Long hostId) {
        return new PlayerUnreadyEvent(
                RoomResponseType.PLAYER_UNREADY,
                PlayerInfo.of(player, hostId)
        );
    }
}
