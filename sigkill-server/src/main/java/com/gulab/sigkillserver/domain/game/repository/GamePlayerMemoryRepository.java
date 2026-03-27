package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.GamePlayer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class GamePlayerMemoryRepository implements GamePlayerRepository {
    private final Map<Long, GamePlayer> store = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> gameIdIndex = new ConcurrentHashMap<>();

    @Override
    public synchronized GamePlayer save(GamePlayer gamePlayer) {
        GamePlayer previous = store.put(gamePlayer.getUserId(), gamePlayer);
        if (previous != null && previous.getGameId() != gamePlayer.getGameId()) {
            removeUserFromGameIndex(previous.getGameId(), previous.getUserId());
        }
        gameIdIndex.computeIfAbsent(gamePlayer.getGameId(), ignored -> ConcurrentHashMap.newKeySet())
                .add(gamePlayer.getUserId());
        return gamePlayer;
    }

    @Override
    public List<GamePlayer> getByGameId(long gameId) {
        Set<Long> userIds = gameIdIndex.get(gameId);
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream()
                .map(store::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public synchronized void deleteByGameIdAndUserId(long gameId, long userId) {
        GamePlayer storedGamePlayer = store.get(userId);
        if (storedGamePlayer == null || storedGamePlayer.getGameId() != gameId) {
            return;
        }

        store.remove(userId);
        removeUserFromGameIndex(gameId, userId);
    }

    @Override
    public synchronized void deleteByGameId(long gameId) {
        Set<Long> userIds = gameIdIndex.remove(gameId);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userIds.forEach(store::remove);
    }

    private void removeUserFromGameIndex(long gameId, long userId) {
        Set<Long> userIds = gameIdIndex.get(gameId);
        if (userIds == null) {
            return;
        }
        userIds.remove(userId);
        if (userIds.isEmpty()) {
            gameIdIndex.remove(gameId);
        }
    }

    @Override
    public synchronized int clear() {
        int removedCount = store.size();
        store.clear();
        gameIdIndex.clear();
        return removedCount;
    }
}
