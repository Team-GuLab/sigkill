package com.gulab.sigkillserver.domain.game.model;

import lombok.Getter;

@Getter
public class SelectedChoice {
    private final long gameId;
    private final long quizId;
    private final long userId;
    private final long choiceId;
    private final long timestamp;

    private SelectedChoice(long gameId, long quizId, long userId, long choiceId, long timestamp) {
        this.gameId = gameId;
        this.quizId = quizId;
        this.userId = userId;
        this.choiceId = choiceId;
        this.timestamp = timestamp;
    }

    public static SelectedChoice create(long gameId, long quizId, long userId, long choiceId, long timestamp) {
        return new SelectedChoice(gameId, quizId, userId, choiceId, timestamp);
    }
}
