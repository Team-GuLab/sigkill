package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import com.gulab.sigkillserver.domain.game.dto.stomp.shared.ChoiceSubmitPayload;

public record ChoiceSubmitEvent(
        GameResponseType type,
        String roomId,
        Long gameId,
        long occurredAt,
        ChoiceSubmitPayload payload
) {
    public static ChoiceSubmitEvent of(String roomId, Long gameId, long occurredAt, ChoiceSubmitPayload payload) {
        return new ChoiceSubmitEvent(
                GameResponseType.CHOICE_SUBMIT,
                roomId,
                gameId,
                occurredAt,
                payload
        );
    }
}
