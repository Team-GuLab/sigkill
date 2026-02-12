package com.gulab.sigkillserver.domain.room.model;

import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.PLAYER_ALREADY_HOST;

import com.gulab.sigkillserver.common.BaseEntity;
import com.gulab.sigkillserver.common.exception.CustomException;
import lombok.Getter;

@Getter
public class Player extends BaseEntity {
    private final Long userId;
    private final String roomId;
    private final String nickname;
    private PlayerRole role;
    private RoomPlayerStatus status;

    private Player(Long userId, String roomId, String nickname, PlayerRole role,
                   RoomPlayerStatus status) {
        this.userId = userId;
        this.roomId = roomId;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    public static Player create(Long userId, String roomId, String nickname,
                                PlayerRole role) {
        return new Player(userId, roomId, nickname, role, RoomPlayerStatus.NOT_READY);
    }

    public void ready() {
        this.status = RoomPlayerStatus.READY;
    }

    public void unready() {
        this.status = RoomPlayerStatus.NOT_READY;
    }

    public void changeToHost() {
        if (this.role == PlayerRole.HOST) {
            throw new CustomException(PLAYER_ALREADY_HOST);
        }
        this.role = PlayerRole.HOST;
    }
}
