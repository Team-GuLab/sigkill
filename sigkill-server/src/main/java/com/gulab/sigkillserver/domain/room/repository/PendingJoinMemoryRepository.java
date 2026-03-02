package com.gulab.sigkillserver.domain.room.repository;

import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.USER_ALREADY_HAS_PENDING_JOIN;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.model.PendingJoin;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PendingJoinMemoryRepository implements PendingJoinRepository {

    private final Map<String, PendingJoin> store = new ConcurrentHashMap<>();
    private final Map<Long, String> userPendingIndex = new ConcurrentHashMap<>();

    @Override
    public PendingJoin save(PendingJoin pendingJoin) {
        String existingJoinTxId = userPendingIndex.putIfAbsent(pendingJoin.userId(), pendingJoin.joinTxId());
        if (existingJoinTxId != null && !existingJoinTxId.equals(pendingJoin.joinTxId())) {
            throw new CustomException(USER_ALREADY_HAS_PENDING_JOIN);
        }
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
    public List<PendingJoin> findAllByUserId(Long userId) {
        return store.values().stream()
                .filter(pendingJoin -> pendingJoin.userId().equals(userId))
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
        PendingJoin removed = store.remove(joinTxId);
        if (removed != null) {
            userPendingIndex.remove(removed.userId(), removed.joinTxId());
        }
    }

    @Override
    public void deleteByRoomId(String roomId) {
        store.values().stream()
                .filter(pendingJoin -> pendingJoin.roomId().equals(roomId))
                .map(PendingJoin::joinTxId)
                .toList()
                .forEach(this::deleteByJoinTxId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        findAllByUserId(userId).stream()
                .map(PendingJoin::joinTxId)
                .toList()
                .forEach(this::deleteByJoinTxId);
    }

    @Override
    public void deleteExpired(long nowEpochMillis) {
        store.values().stream()
                .filter(pendingJoin -> pendingJoin.isExpiredAt(nowEpochMillis))
                .map(PendingJoin::joinTxId)
                .toList()
                .forEach(this::deleteByJoinTxId);
    }
}
