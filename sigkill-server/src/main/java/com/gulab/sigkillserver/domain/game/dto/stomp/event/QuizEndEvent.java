package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPayload;

public record QuizEndEvent(
        GameResponseType type,
        String roomId,
        Long gameId,
        long occurredAt,
        QuizEndPayload payload
) {
    public static QuizEndEvent of(String roomId, Long gameId, long occurredAt, QuizEndPayload payload) {
        return new QuizEndEvent(
                GameResponseType.QUIZ_END,
                roomId,
                gameId,
                occurredAt,
                payload
        );
    }
}
