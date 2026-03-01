package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.SelectedChoice;
import java.util.List;

public interface SelectedChoiceRepository {
    SelectedChoice save(SelectedChoice selectedChoice);

    List<SelectedChoice> findByGameIdAndQuizId(long gameId, long quizId);

    void deleteByGameIdAndQuizId(long gameId, long quizId);

    void deleteByGameId(long gameId);
}
