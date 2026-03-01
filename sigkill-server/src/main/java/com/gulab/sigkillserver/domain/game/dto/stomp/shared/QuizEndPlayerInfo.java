package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

import com.gulab.sigkillserver.domain.game.model.GamePlayerStatus;

public record QuizEndPlayerInfo(
        Long userId,
        String nickname,
        GamePlayerStatus status,
        QuizResult quizResult,
        int score
) {
}
