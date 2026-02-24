package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

public record ChoiceSubmitPayload(
        QuizProgressInfo quiz,
        ActorInfo actor,
        int choiceNumber
) {
}
