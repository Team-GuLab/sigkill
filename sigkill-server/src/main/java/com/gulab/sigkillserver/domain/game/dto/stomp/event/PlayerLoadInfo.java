package com.gulab.sigkillserver.domain.game.dto.stomp.event;

public record PlayerLoadInfo(
        long userId,
        String nickname,
        boolean isLoaded
) {
}
