package com.gulab.sigkillserver.domain.room.dto.stomp.command;

import jakarta.validation.constraints.NotBlank;

public record RoomJoinCommand(
        @NotBlank String roomId,
        @NotBlank String joinTxId
) {
}
