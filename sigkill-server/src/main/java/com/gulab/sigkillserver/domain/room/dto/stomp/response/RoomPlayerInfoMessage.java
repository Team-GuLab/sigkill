package com.gulab.sigkillserver.domain.room.dto.stomp.response;

import com.gulab.sigkillserver.domain.room.model.RoomPlayerStatus;

public record RoomPlayerInfoMessage(
        String id,
        String nickname,
        RoomPlayerStatus status
) {
}