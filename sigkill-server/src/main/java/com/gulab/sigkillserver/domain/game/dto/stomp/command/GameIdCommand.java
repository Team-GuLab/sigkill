package com.gulab.sigkillserver.domain.game.dto.stomp.command;

import jakarta.validation.constraints.NotBlank;

public record GameIdCommand(
        @NotBlank long gameId
) {
}