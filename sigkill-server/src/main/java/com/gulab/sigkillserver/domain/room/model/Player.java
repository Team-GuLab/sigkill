package com.gulab.sigkillserver.domain.room.model;

import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ALREADY_HOST;

import com.gulab.sigkillserver.common.exception.CustomException;
import lombok.Getter;

@Getter
public class Player {
    private final String playerId;
    private final String userId;
    private final String roomId;
    private final String nickname;
    private PlayerRole role;
    private RoomPlayerStatus status;

    private Player(String playerId, String userId, String roomId, String nickname, PlayerRole role,
                   RoomPlayerStatus status) {
        this.playerId = playerId;
        this.userId = userId;
        this.roomId = roomId;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    public static Player create(String playerId, String userId, String roomId, String nickname,
                                PlayerRole role) {
        return new Player(playerId, userId, roomId, nickname, role, RoomPlayerStatus.NOT_READY);
    }

    public void ready() {
        this.status = RoomPlayerStatus.READY;
    }

    public void unready() {
        this.status = RoomPlayerStatus.NOT_READY;
    }

    public void changeToHost() {
        if (this.role == PlayerRole.HOST) {
            throw new CustomException(ALREADY_HOST);
        }
        this.role = PlayerRole.HOST;
    }
}
