package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.PendingJoin;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PendingJoinMemoryRepository implements PendingJoinRepository {

    private final Map<String, PendingJoin> store = new ConcurrentHashMap<>();

    @Override
    public PendingJoin save(PendingJoin pendingJoin) {
        store.put(pendingJoin.joinTxId(), pendingJoin);
        return pendingJoin;
    }

    @Override
    public Optional<PendingJoin> findByJoinTxId(String joinTxId) {
        return Optional.ofNullable(store.get(joinTxId));
    }

    @Override
    public Optional<PendingJoin> findByUserId(Long userId) {
        return store.values().stream()
                .filter(pendingJoin -> pendingJoin.userId().equals(userId))
                .findFirst();
    }

    @Override
    public Optional<PendingJoin> findByRoomIdAndUserId(String roomId, Long userId) {
        return store.values().stream()
                .filter(pendingJoin -> pendingJoin.roomId().equals(roomId))
                .filter(pendingJoin -> pendingJoin.userId().equals(userId))
                .findFirst();
    }

    @Override
    public List<PendingJoin> findAllByRoomId(String roomId) {
        return store.values().stream()
                .filter(pendingJoin -> pendingJoin.roomId().equals(roomId))
                .toList();
    }

    @Override
    public int countUnexpiredByRoomId(String roomId, long nowEpochMillis) {
        return (int) store.values().stream()
                .filter(pendingJoin -> pendingJoin.roomId().equals(roomId))
                .filter(pendingJoin -> !pendingJoin.isExpiredAt(nowEpochMillis))
                .count();
    }

    @Override
    public void deleteByJoinTxId(String joinTxId) {
        store.remove(joinTxId);
    }

    @Override
    public void deleteByRoomId(String roomId) {
        store.values().removeIf(pendingJoin -> pendingJoin.roomId().equals(roomId));
    }

    @Override
    public void deleteByUserId(Long userId) {
        store.values().removeIf(pendingJoin -> pendingJoin.userId().equals(userId));
    }

    @Override
    public void deleteExpired(long nowEpochMillis) {
        store.values().removeIf(pendingJoin -> pendingJoin.isExpiredAt(nowEpochMillis));
    }
}
