package com.gulab.sigkillserver.domain.room.dto;

public record WebSocketInfo(
        String endpoint,
        String protocol,
        String messageFormat
) {
    public static WebSocketInfo of(String roomId) {
        return new WebSocketInfo(
                "/ws/rooms/" + roomId,
                "websocket",
                "json"
        );
    }
}