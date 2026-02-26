package com.gulab.sigkillserver.domain.game.constant;

public class GameConstants {
    /**
     * 기본 퀴즈 카테고리 ID
     */
    public static final String DEFAULT_CATEGORY_ID = "CS";

    /**
     * 게임 로딩 완료 후 첫 퀴즈 시작 대기 시간 (밀리초)
     */
    public static final long INITIAL_QUIZ_START_DELAY_MILLIS = 3_000L;

    /**
     * 퀴즈 정답 제출 제한 시간 (밀리초)
     */
    public static final long QUIZ_COUNTDOWN_MILLIS = 10_000L;

    /**
     * 퀴즈 정답 제출 허용 시간 (밀리초)
     */
    public static final long QUIZ_ANSWER_ALLOWANCE_MILLIS = 500L;

    /**
     * 퀴즈 종료 후 다음 퀴즈 시작 대기 시간 (밀리초)
     */
    public static final long NEXT_QUIZ_START_DELAY_MILLIS = 10_000L;

    /**
     * 최대 퀴즈 개수
     */
    public static final int QUIZ_COUNT = 10;

    private GameConstants() {
    }
}
