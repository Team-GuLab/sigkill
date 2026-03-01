package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class QuizChoiceNumberMappingMemoryRepository implements QuizChoiceNumberMappingRepository {

    private final Map<MappingKey, QuizChoiceNumberMapping> store = new ConcurrentHashMap<>();

    @Override
    public QuizChoiceNumberMapping save(QuizChoiceNumberMapping quizChoiceNumberMapping) {
        MappingKey key = MappingKey.of(quizChoiceNumberMapping.getGameId(), quizChoiceNumberMapping.getQuizId());
        store.put(key, quizChoiceNumberMapping);
        return quizChoiceNumberMapping;
    }

    @Override
    public Optional<QuizChoiceNumberMapping> findByGameIdAndQuizId(long gameId, long quizId) {
        return Optional.ofNullable(store.get(MappingKey.of(gameId, quizId)));
    }

    @Override
    public void deleteByGameIdAndQuizId(long gameId, long quizId) {
        store.remove(MappingKey.of(gameId, quizId));
    }

    @Override
    public void deleteByGameId(long gameId) {
        store.keySet().removeIf(key -> key.gameId() == gameId);
    }

    private record MappingKey(long gameId, long quizId) {
        private static MappingKey of(long gameId, long quizId) {
            return new MappingKey(gameId, quizId);
        }
    }
}
