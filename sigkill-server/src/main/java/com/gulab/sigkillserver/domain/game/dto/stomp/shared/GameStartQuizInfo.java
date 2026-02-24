package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

public record GameStartQuizInfo(
        int currentQuizIndex,
        int totalQuizCount
) {
}
