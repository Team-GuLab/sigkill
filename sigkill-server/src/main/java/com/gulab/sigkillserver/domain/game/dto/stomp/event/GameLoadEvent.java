package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import java.time.Instant;

public record GameLoadEvent(
        GameResponseType type,
        String roomId,
        Long gameId,
        long occurredAt,
        GameLoadPayload payload
) {
    public static GameLoadEvent of(String roomId, Long gameId, GameLoadPayload payload) {
        return new GameLoadEvent(
                GameResponseType.GAME_LOADED,
                roomId,
                gameId,
                Instant.now().toEpochMilli(),
                payload
        );
    }
}
