package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

import java.util.List;

public record QuizStartInfo(
        Long quizId,
        int currentQuizIndex,
        int totalQuizCount,
        long startTime,
        long endTime,
        String question,
        List<QuizChoiceInfo> choices
) {
    public QuizStartInfo {
        choices = List.copyOf(choices);
    }
}
