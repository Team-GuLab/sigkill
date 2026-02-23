package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

import java.util.List;

public record QuizEndPayload(
        QuizProgressInfo quiz,
        QuizAnswerInfo answer,
        List<QuizEndPlayerInfo> players
) {
    public QuizEndPayload {
        players = List.copyOf(players);
    }
}
