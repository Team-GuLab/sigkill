package com.gulab.sigkillserver.domain.game.model;

import com.gulab.sigkillserver.common.BaseEntity;
import java.util.List;
import lombok.Getter;
import lombok.With;

@Getter
public class Game extends BaseEntity {
    @With
    private final Long gameId;
    private final String roomId;

    private final List<Long> quizIds;
    private Integer currentQuizIndex;
    private Long quizStartTime;

    private Game(Long gameId, String roomId, List<Long> quizIds, int currentQuizIndex,
                 Long quizStartTime) {
        this.gameId = gameId;
        this.roomId = roomId;
        this.quizIds = List.copyOf(quizIds);
        this.currentQuizIndex = currentQuizIndex;
        this.quizStartTime = quizStartTime;
    }

    public static Game create(String roomId, List<Long> quizIds) {
        return new Game(null, roomId, quizIds, -1, null);
    }
}
