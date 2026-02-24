package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import java.util.List;
import java.util.Optional;

public interface QuizRepository {
    List<Quiz> findByCategoryId(String categoryId, int count);

    Optional<Quiz> findById(long quizId);
}
