package com.gulab.sigkillserver.domain.game.model;

import com.gulab.sigkillserver.common.BaseEntity;
import java.util.List;
import lombok.Getter;

@Getter
public class Game extends BaseEntity {
    private static final long QUIZ_COUNTDOWN_MILLIS = 5_000L;
    private final long gameId;
    private final String roomId;
    private final GameStatus status;

    private final List<Long> quizIds;
    private Integer currentQuizIndex;
    private Long quizStartTime;

    private Game(long gameId, String roomId, GameStatus status, List<Long> quizIds, int currentQuizIndex,
                 Long quizStartTime) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.status = status;
        this.quizIds = List.copyOf(quizIds);
        this.currentQuizIndex = currentQuizIndex;
        this.quizStartTime = quizStartTime;
    }

    public static Game create(long gameId, String roomId, List<Long> quizIds) {
        return new Game(gameId, roomId, GameStatus.INGAME, quizIds, -1, null);
    }
}
