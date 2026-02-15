package com.gulab.sigkillserver.domain.room.dto.stomp.shared;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.ReadyStatus;

public record PlayerInfo(
        Long userId,
        String nickname,
        ReadyStatus status
) {
    public static PlayerInfo of(Player player) {
        return new PlayerInfo(player.getUserId(), player.getNickname(), player.getReadyStatus());
    }
}