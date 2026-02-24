package com.gulab.sigkillserver.domain.game.service;

import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.ActorInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.ChoiceSubmitPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartQuizInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizAnswerInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizChoiceInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPlayerInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizProgressInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizStartInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizStartPayload;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameEventBuilder {

    public GameStartEvent toGameStartEvent(Room room, Game game) {
        return GameStartEvent.of(
                room.getRoomId(),
                game.getGameId(),
                new GameStartPayload(new GameStartQuizInfo(0, game.getTotalQuizCount()))
        );
    }

    public QuizStartEvent toQuizStartEvent(
            Room room,
            Game game,
            Quiz quiz,
            long quizStartTime,
            List<QuizChoiceInfo> quizChoiceInfos
    ) {
        QuizStartInfo quizStartInfo = new QuizStartInfo(
                quiz.quizId(),
                game.getCurrentQuizIndex() + 1,
                game.getTotalQuizCount(),
                quizStartTime,
                quizStartTime + GameConstants.QUIZ_COUNTDOWN_MILLIS,
                quiz.question(),
                quizChoiceInfos
        );

        return QuizStartEvent.of(
                room.getRoomId(),
                game.getGameId(),
                quizStartTime,
                new QuizStartPayload(quizStartInfo)
        );
    }

    public ChoiceSubmitEvent toChoiceSubmitEvent(
            Room room,
            Game game,
            Quiz quiz,
            Player player,
            int choiceNumber,
            long occurredAt
    ) {
        QuizProgressInfo quizProgressInfo = new QuizProgressInfo(
                quiz.quizId(),
                game.getCurrentQuizIndex() + 1,
                game.getTotalQuizCount()
        );
        ActorInfo actorInfo = new ActorInfo(player.getUserId(), player.getNickname());
        ChoiceSubmitPayload choiceSubmitPayload = new ChoiceSubmitPayload(quizProgressInfo, actorInfo, choiceNumber);
        return ChoiceSubmitEvent.of(
                room.getRoomId(),
                game.getGameId(),
                occurredAt,
                choiceSubmitPayload
        );
    }

    public QuizEndEvent toQuizEndEvent(
            Room room,
            Game game,
            Quiz quiz,
            int answerNumber,
            long occurredAt
    ) {
        QuizProgressInfo quizProgressInfo = new QuizProgressInfo(
                quiz.quizId(),
                game.getCurrentQuizIndex() + 1,
                game.getTotalQuizCount()
        );
        QuizAnswerInfo quizAnswerInfo = new QuizAnswerInfo(answerNumber, quiz.explanation());
        List<QuizEndPlayerInfo> quizAnswerInfoList = new ArrayList<>(); // TODO
        QuizEndPayload quizEndPayload = new QuizEndPayload(
                quizProgressInfo,
                quizAnswerInfo,
                quizAnswerInfoList
        );
        return QuizEndEvent.of(
                room.getRoomId(),
                game.getGameId(),
                occurredAt,
                quizEndPayload
        );
    }

//    private QuizEndPlayerInfo toQuizEndPlayerInfo(Player player, boolean isCorrect) {
//        new QuizEndPlayerInfo(player.getUserId(), player.getNickname(), )
//        return new QuizEndPlayerInfo(player.getUserId(), player.getNickname(), isCorrect);
//    }
}
