package com.gulab.sigkillserver.domain.room.model;

import com.gulab.sigkillserver.common.BaseEntity;
import lombok.Getter;

@Getter
public class Player extends BaseEntity {
    private final Long userId;
    private final String roomId;
    private final String nickname;
    private ReadyStatus readyStatus;

    private Player(Long userId, String roomId, String nickname, ReadyStatus readyStatus) {
        this.userId = userId;
        this.roomId = roomId;
        this.nickname = nickname;
        this.readyStatus = readyStatus;
    }

    public static Player create(Long userId, String roomId, String nickname) {
        return new Player(userId, roomId, nickname, ReadyStatus.UNREADY);
    }

    public void ready() {
        this.readyStatus = ReadyStatus.READY;
    }

    public void unready() {
        this.readyStatus = ReadyStatus.UNREADY;
    }

    public boolean isReady() {
        return this.readyStatus == ReadyStatus.READY;
    }
}
