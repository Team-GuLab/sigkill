package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

public record QuizAnswerInfo(
        int correctChoiceNumber,
        String explanation
) {
}
