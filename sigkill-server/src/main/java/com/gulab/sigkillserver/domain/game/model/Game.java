package com.gulab.sigkillserver.domain.game.model;

import static com.gulab.sigkillserver.domain.game.exception.QuizErrorCode.QUIZ_INDEX_OUT_OF_BOUNDS;

import com.gulab.sigkillserver.common.BaseEntity;
import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.constant.GameConstants;
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
        int nextQuizIndex = this.currentQuizIndex + 1;
        if (nextQuizIndex >= quizIds.size()) {
            throw new CustomException(QUIZ_INDEX_OUT_OF_BOUNDS);
        }

        this.currentQuizIndex = nextQuizIndex;
        this.quizStartTime = quizStartTime;
        return quizIds.get(currentQuizIndex);
    }

    public long getCurrentQuizId() {
        if (isQuizIndexOutOfBounds()) {
            throw new CustomException(QUIZ_INDEX_OUT_OF_BOUNDS);
        }
        return quizIds.get(currentQuizIndex);
    }

    public int getTotalQuizCount() {
        return quizIds.size();
    }

    public boolean hasExceededDeadline(long submitTime) {
        long deadline = quizStartTime
                + GameConstants.QUIZ_COUNTDOWN_MILLIS
                + GameConstants.QUIZ_ANSWER_ALLOWANCE_MILLIS;
        return submitTime > deadline;
    }
}
