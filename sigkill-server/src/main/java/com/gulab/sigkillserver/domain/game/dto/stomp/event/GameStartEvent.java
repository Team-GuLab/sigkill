package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartPayload;

public record GameStartEvent(
        GameResponseType type,
        String roomId,
        Long gameId,
        long occurredAt,
        GameStartPayload payload
) {
    public static GameStartEvent of(String roomId, Long gameId, long occurredAt, GameStartPayload payload) {
        return new GameStartEvent(
                GameResponseType.GAME_START,
                roomId,
                gameId,
                occurredAt,
                payload
        );
    }
}
