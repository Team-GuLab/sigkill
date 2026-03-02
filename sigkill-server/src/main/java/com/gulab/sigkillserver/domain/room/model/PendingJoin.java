package com.gulab.sigkillserver.domain.room.model;

import java.util.Objects;

public record PendingJoin(
        String joinTxId,
        String roomId,
        Long userId,
        long createdAtMillis,
        long expiresAtMillis
) {
    public static PendingJoin create(
            String joinTxId,
            String roomId,
            Long userId,
            long createdAtMillis,
            long expiresAtMillis
    ) {
        Objects.requireNonNull(joinTxId, "joinTxId must not be null");
        Objects.requireNonNull(roomId, "roomId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");

        return new PendingJoin(joinTxId, roomId, userId, createdAtMillis, expiresAtMillis);
    }

    public boolean isExpiredAt(long nowEpochMillis) {
        return expiresAtMillis <= nowEpochMillis;
    }
}
