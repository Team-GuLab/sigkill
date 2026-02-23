package com.gulab.sigkillserver.domain.game.constant;

public class GameConstants {
    /**
     * 기본 퀴즈 카테고리 ID
     */
    public static final String DEFAULT_CATEGORY_ID = "CS";

    /**
     * 퀴즈 정답 제출 제한 시간 (밀리초)
     */
    public static final long QUIZ_COUNTDOWN_MILLIS = 5_000L;

    /**
     * 최대 퀴즈 개수
     */
    public static final int QUIZ_COUNT = 10;

    private GameConstants() {
    }
}
