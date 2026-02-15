package com.gulab.sigkillserver.domain.room.model;

import com.gulab.sigkillserver.common.BaseEntity;
import lombok.Getter;

@Getter
public class Player extends BaseEntity {
    private final Long userId;
    private final String roomId;
    private final String nickname;
    private RoomPlayerStatus status;

    private Player(Long userId, String roomId, String nickname, RoomPlayerStatus status) {
        this.userId = userId;
        this.roomId = roomId;
        this.nickname = nickname;
        this.status = status;
    }

    public static Player create(Long userId, String roomId, String nickname) {
        return new Player(userId, roomId, nickname, RoomPlayerStatus.NOT_READY);
    }

    public void ready() {
        this.status = RoomPlayerStatus.READY;
    }

    public void unready() {
        this.status = RoomPlayerStatus.NOT_READY;
    }
}
