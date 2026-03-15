package com.gulab.sigkillserver.domain.room.model;

import com.gulab.sigkillserver.common.BaseEntity;
import lombok.Getter;

@Getter
public class Player extends BaseEntity {
    private final Long userId;
    private final String roomId;
    private final String nickname;
    private ReadyStatus readyStatus;
    private JoinStatus joinStatus;

    private Player(Long userId, String roomId, String nickname, ReadyStatus readyStatus, JoinStatus joinStatus) {
        this.userId = userId;
        this.roomId = roomId;
        this.nickname = nickname;
        this.readyStatus = readyStatus;
        this.joinStatus = joinStatus;
    }

    public static Player create(Long userId, String roomId, String nickname) {
        return new Player(userId, roomId, nickname, ReadyStatus.NOT_READY, JoinStatus.PENDING);
    }

    public void ready() {
        this.readyStatus = ReadyStatus.READY;
    }

    public void unready() {
        this.readyStatus = ReadyStatus.NOT_READY;
    }

    public boolean isReady() {
        return this.readyStatus == ReadyStatus.READY;
    }

    public void activate() {
        this.joinStatus = JoinStatus.ACTIVE;
    }

    public boolean isPending() {
        return this.joinStatus == JoinStatus.PENDING;
    }

    public boolean isActive() {
        return this.joinStatus == JoinStatus.ACTIVE;
    }
}
