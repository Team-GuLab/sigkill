package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

public record QuizEndPlayerInfo(
        Long userId,
        String nickname,
        PlayerStatus status,
        QuizResult quizResult,
        int score
) {
}
