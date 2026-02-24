package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndPayload;

public record GameEndEvent(
        GameResponseType type,
        String roomId,
        Long gameId,
        long occurredAt,
        GameEndPayload payload
) {
    public static GameEndEvent of(String roomId, Long gameId, long occurredAt, GameEndPayload payload) {
        return new GameEndEvent(
                GameResponseType.GAME_END,
                roomId,
                gameId,
                occurredAt,
                payload
        );
    }
}
