package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

public record QuizProgressInfo(
        Long quizId,
        int currentQuizIndex,
        int totalQuizCount
) {
}
