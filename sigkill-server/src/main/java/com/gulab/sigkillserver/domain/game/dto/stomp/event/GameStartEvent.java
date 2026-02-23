package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartPayload;
import java.time.Instant;

public record GameStartEvent(
        GameResponseType type,
        String roomId,
        Long gameId,
        long occurredAt,
        GameStartPayload payload
) {
    public static GameStartEvent of(String roomId, Long gameId, GameStartPayload payload) {
        return new GameStartEvent(
                GameResponseType.GAME_START,
                roomId,
                gameId,
                Instant.now().toEpochMilli(),
                payload
        );
    }
}
