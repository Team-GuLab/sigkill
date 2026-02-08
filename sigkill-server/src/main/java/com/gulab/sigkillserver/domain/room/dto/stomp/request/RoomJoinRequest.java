package com.gulab.sigkillserver.domain.room.dto.stomp.request;

import jakarta.validation.constraints.NotBlank;

public record RoomJoinRequest(
        @NotBlank String roomId
) {
}
