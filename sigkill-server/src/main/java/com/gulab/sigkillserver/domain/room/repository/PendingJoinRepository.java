package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.PendingJoin;
import java.util.List;
import java.util.Optional;

public interface PendingJoinRepository {

    PendingJoin save(PendingJoin pendingJoin);

    Optional<PendingJoin> findByJoinTxId(String joinTxId);

    Optional<PendingJoin> findByRoomIdAndUserId(String roomId, Long userId);

    List<PendingJoin> findAllByRoomId(String roomId);

    int countUnexpiredByRoomId(String roomId, long nowEpochMillis);

    void deleteByJoinTxId(String joinTxId);

    void deleteByRoomId(String roomId);

    void deleteByUserId(Long userId);

    void deleteExpired(long nowEpochMillis);
}
