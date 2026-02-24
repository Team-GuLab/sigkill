package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizStartPayload;

public record QuizStartEvent(
        GameResponseType type,
        String roomId,
        Long gameId,
        long occurredAt,
        QuizStartPayload payload
) {
    public static QuizStartEvent of(String roomId, Long gameId, long occurredAt, QuizStartPayload payload) {
        return new QuizStartEvent(
                GameResponseType.QUIZ_START,
                roomId,
                gameId,
                occurredAt,
                payload
        );
    }
}
