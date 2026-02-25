package com.gulab.sigkillserver.domain.game.model.quiz;

import java.util.Map;
import java.util.Optional;
import lombok.Getter;

@Getter
public class QuizChoiceNumberMapping {

    private final long gameId;
    private final long quizId;
    private final Map<Integer, Long> numberToChoiceId;

    private QuizChoiceNumberMapping(long gameId, long quizId, Map<Integer, Long> numberToChoiceId) {
        this.gameId = gameId;
        this.quizId = quizId;
        this.numberToChoiceId = Map.copyOf(numberToChoiceId);
    }

    public static QuizChoiceNumberMapping create(long gameId, long quizId, Map<Integer, Long> numberToChoiceId) {
        if (numberToChoiceId == null || numberToChoiceId.isEmpty()) {
            throw new IllegalArgumentException("numberToChoiceId must not be null or empty");
        }
        return new QuizChoiceNumberMapping(gameId, quizId, numberToChoiceId);
    }

    public Optional<Long> findChoiceIdByNumber(int choiceNumber) {
        return Optional.ofNullable(numberToChoiceId.get(choiceNumber));
    }
}
