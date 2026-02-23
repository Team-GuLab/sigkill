package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

public record GameRankingInfo(
        int rank,
        Long userId,
        String nickname,
        int score
) {
}
