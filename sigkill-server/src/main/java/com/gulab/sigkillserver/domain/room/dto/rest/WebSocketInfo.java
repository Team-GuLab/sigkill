package com.gulab.sigkillserver.domain.room.dto.rest;

public record WebSocketInfo(
        String endpoint,
        String protocol,
        String messageFormat,
        String roomId
) {
    public static WebSocketInfo of(String roomId) {
        return new WebSocketInfo(
                "/ws",
                "websocket",
                "json",
                roomId
        );
    }
}