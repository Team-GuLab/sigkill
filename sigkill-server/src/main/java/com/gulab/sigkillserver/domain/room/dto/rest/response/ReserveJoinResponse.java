package com.gulab.sigkillserver.domain.room.dto.rest.response;

public record ReserveJoinResponse(
        String joinTxId,
        long expiresAt,
        long ttlMillis
) {
}
