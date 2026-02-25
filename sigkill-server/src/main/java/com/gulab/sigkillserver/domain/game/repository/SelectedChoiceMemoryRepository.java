package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.SelectedChoice;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class SelectedChoiceMemoryRepository implements SelectedChoiceRepository {

    // 키: 게임 ID + 퀴즈 ID, 값: 유저 ID -> 제출한 선지
    private final Map<GameQuizKey, Map<Long, SelectedChoice>> store = new ConcurrentHashMap<>();

    @Override
    public SelectedChoice save(SelectedChoice sc) {
        GameQuizKey key = new GameQuizKey(sc.gameId(), sc.quizId());
        store.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(sc.userId(), sc);
        return sc;
    }

    @Override
    public List<SelectedChoice> findByGameIdAndQuizId(long gameId, long quizId) {
        Map<Long, SelectedChoice> byUser = store.get(new GameQuizKey(gameId, quizId));
        if (byUser == null) {
            return List.of();
        }
        return List.copyOf(byUser.values());
    }

    @Override
    public void deleteByGameIdAndQuizId(long gameId, long quizId) {
        store.remove(new GameQuizKey(gameId, quizId));
    }

    @Override
    public void deleteByGameId(long gameId) {
        store.keySet().removeIf(key -> key.gameId() == gameId);
    }

    private record GameQuizKey(long gameId, long quizId) {}
}
