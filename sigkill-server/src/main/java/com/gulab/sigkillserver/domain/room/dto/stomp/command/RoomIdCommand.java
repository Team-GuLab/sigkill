package com.gulab.sigkillserver.domain.room.dto.stomp.command;

import jakarta.validation.constraints.NotBlank;

public record RoomIdCommand(
        @NotBlank String roomId
) {
}
