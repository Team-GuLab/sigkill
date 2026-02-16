package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.model.Player;

public record HostChangedEvent(
        RoomResponseType type,
        PlayerInfo newHost,
        PlayerInfo oldHost,
        String reason
) {
    public static HostChangedEvent of(Player newHost, Player previousHost, String reason) {
        return new HostChangedEvent(
                RoomResponseType.HOST_CHANGED,
                PlayerInfo.of(newHost),
                PlayerInfo.of(previousHost),
                reason
        );
    }
}
