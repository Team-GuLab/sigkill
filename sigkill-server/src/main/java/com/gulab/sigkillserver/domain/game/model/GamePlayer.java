package com.gulab.sigkillserver.domain.game.model;

import com.gulab.sigkillserver.common.BaseEntity;
import lombok.Getter;

@Getter
public class GamePlayer extends BaseEntity {
    private final long userId;
    private final long gameId;
    private final String nickname;
    private int score;
    private boolean isLoaded;
    private GamePlayerStatus status;

    private GamePlayer(long userId, long gameId, String nickname, int score, GamePlayerStatus status) {
        this.userId = userId;
        this.gameId = gameId;
        this.nickname = nickname;
        this.score = score;
        this.isLoaded = false;
        this.status = status;
    }

    public static GamePlayer create(long userId, long gameId, String nickname) {
        return new GamePlayer(userId, gameId, nickname, 0, GamePlayerStatus.ALIVE);
    }

    public boolean isAlive() {
        return this.status == GamePlayerStatus.ALIVE;
    }

    public void kill() {
        this.status = GamePlayerStatus.DEAD;
    }

    public void addScore(int scoreToAdd) {
        this.score += scoreToAdd;
    }

    public void markAsLoaded() {
        this.isLoaded = true;
    }
}
