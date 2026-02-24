package com.gulab.sigkillserver.domain.game.model;

import com.gulab.sigkillserver.common.BaseEntity;
import lombok.Getter;

@Getter
public class GamePlayer extends BaseEntity {
    private final long userId;
    private final long gameId;
    private int score;
    private GamePlayerStatus status;

    private GamePlayer(long userId, long gameId, int score, GamePlayerStatus status) {
        this.userId = userId;
        this.gameId = gameId;
        this.score = score;
        this.status = status;
    }

    public static GamePlayer create(long userId, long gameId) {
        return new GamePlayer(userId, gameId, 0, GamePlayerStatus.ALIVE);
    }

    public void kill() {
        this.status = GamePlayerStatus.DEAD;
    }
}
