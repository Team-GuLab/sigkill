package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.model.Player;

public record PlayerReadyEvent(
        RoomResponseType type,
        PlayerInfo player,
        boolean allReady
) {
    public static PlayerReadyEvent of(Player player, Long hostId, boolean allReady) {
        return new PlayerReadyEvent(
                RoomResponseType.PLAYER_READY,
                PlayerInfo.of(player, hostId),
                allReady
        );
    }
}
