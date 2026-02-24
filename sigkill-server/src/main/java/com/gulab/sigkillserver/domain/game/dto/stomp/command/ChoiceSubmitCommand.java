package com.gulab.sigkillserver.domain.game.dto.stomp.command;

import jakarta.validation.constraints.NotNull;

public record ChoiceSubmitCommand(
        @NotNull Long gameId,
        @NotNull Long quizId,
        @NotNull Integer choiceNumber
) {
}
