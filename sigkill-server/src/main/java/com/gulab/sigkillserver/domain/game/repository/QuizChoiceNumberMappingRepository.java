package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import java.util.Optional;

public interface QuizChoiceNumberMappingRepository {

    QuizChoiceNumberMapping save(QuizChoiceNumberMapping quizChoiceNumberMapping);

    Optional<QuizChoiceNumberMapping> findByGameIdAndQuizId(long gameId, long quizId);

    void deleteByGameIdAndQuizId(long gameId, long quizId);

    void deleteByGameId(long gameId);

    /**
     * 모든 선택지 번호 매핑 데이터 정리
     *
     * @return 삭제된 매핑 수
     */
    int clear();
}
