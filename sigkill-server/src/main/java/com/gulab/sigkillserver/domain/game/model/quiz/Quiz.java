package com.gulab.sigkillserver.domain.game.model.quiz;

import java.util.List;

public record Quiz(
        long quizId,
        String categoryId,
        String question,
        String explanation,
        long correctChoiceId,
        int difficulty,
        List<QuizChoice> choices
) {
    public Quiz {
        choices = List.copyOf(choices);
    }

    public boolean isCorrectChoice(long choiceId) {
        return correctChoiceId == choiceId;
    }
}
