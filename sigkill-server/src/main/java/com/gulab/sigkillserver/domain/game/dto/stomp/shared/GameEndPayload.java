package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

import java.util.List;

public record GameEndPayload(
        GameEndReason reason,
        List<GameRankingInfo> rankings
) {
    public GameEndPayload {
        rankings = List.copyOf(rankings);
    }
}
