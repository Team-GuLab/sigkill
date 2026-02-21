package com.gulab.sigkillserver.domain.room.dto.stomp.shared;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.ReadyStatus;
import java.util.Objects;

public record PlayerInfo(
        Long userId,
        String nickname,
        ReadyStatus status,
        String role
) {
    public static PlayerInfo of(Player player, Long hostId) {
        return new PlayerInfo(
                player.getUserId(),
                player.getNickname(),
                player.getReadyStatus(),
                Objects.equals(player.getUserId(), hostId) ? "HOST" : "GUEST"
        );
    }
}
