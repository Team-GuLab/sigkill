package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import java.util.Optional;

public interface QuizChoiceNumberMappingRepository {

    QuizChoiceNumberMapping save(QuizChoiceNumberMapping quizChoiceNumberMapping);

    Optional<QuizChoiceNumberMapping> findByGameIdAndQuizId(long gameId, long quizId);

    void deleteByGameIdAndQuizId(long gameId, long quizId);

    void deleteByGameId(long gameId);
}
