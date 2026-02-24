package com.gulab.sigkillserver.domain.game.model;

public record SelectedChoice(
        long gameId,
        long quizId,
        long userId,
        long choiceId,
        long timestamp
) {
    public static SelectedChoice create(long gameId, long quizId, long userId, long choiceId, long timestamp) {
        return new SelectedChoice(gameId, quizId, userId, choiceId, timestamp);
    }
}
