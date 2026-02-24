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

    public boolean isQuizIndexOutOfBounds() {
        return currentQuizIndex < 0 || currentQuizIndex >= quizIds.size();
    }

    public long startNextQuiz(long quizStartTime) {
        this.currentQuizIndex++;
        this.quizStartTime = quizStartTime;
        return quizIds.get(currentQuizIndex);
    }

    public long getCurrentQuizId() {
        if (isQuizIndexOutOfBounds()) {
            throw new IllegalStateException("현재 퀴즈 인덱스가 범위를 벗어났습니다.");
        }
        return quizIds.get(currentQuizIndex);
    }

    public int getTotalQuizCount() {
        return quizIds.size();
    }
}
