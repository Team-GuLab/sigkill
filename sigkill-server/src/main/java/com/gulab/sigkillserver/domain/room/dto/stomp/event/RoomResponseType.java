package com.gulab.sigkillserver.domain.room.dto.stomp.event;

public enum RoomResponseType {
    ROOM_SNAPSHOT,
    PLAYER_JOIN,
    OTHER_PLAYER_JOIN,
    PLAYER_LEFT,
    HOST_CHANGED,
    PLAYER_READY,
    PLAYER_UNREADY,
}
