package com.gulab.sigkillserver.domain.game.model;

import java.time.Instant;
import lombok.Getter;

@Getter
public class SelectedChoice {
    private final long quizId;
    private final long userId;
    private final int choiceId;
    private final long timestamp;

    private SelectedChoice(long quizId, long userId, int choiceId, long timestamp) {
        this.quizId = quizId;
        this.userId = userId;
        this.choiceId = choiceId;
        this.timestamp = timestamp;
    }

    public static SelectedChoice create(long quizId, long userId, int choiceIndex) {
        return new SelectedChoice(quizId, userId, choiceIndex, Instant.now().toEpochMilli());
    }
}
