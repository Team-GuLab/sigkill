package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import java.util.List;

public interface QuizCatalogRepository {
    List<Quiz> findByCategoryId(String categoryId);

    Quiz findById(long quizId);
}
