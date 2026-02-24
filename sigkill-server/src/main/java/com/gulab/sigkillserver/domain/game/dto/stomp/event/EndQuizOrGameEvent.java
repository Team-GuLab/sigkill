package com.gulab.sigkillserver.domain.game.dto.stomp.event;

public record EndQuizOrGameEvent(
        QuizEndEvent quizEndEvent,
        GameEndEvent gameEndEvent
) {
    public boolean hasGameEnd() {
        return gameEndEvent != null;
    }
}
