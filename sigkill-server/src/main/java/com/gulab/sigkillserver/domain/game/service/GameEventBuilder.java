package com.gulab.sigkillserver.domain.game.service;

import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.ActorInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.ChoiceSubmitPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndReason;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameRankingInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameStartQuizInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizAnswerInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizChoiceInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPlayerInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizProgressInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizResult;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizStartInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizStartPayload;
import com.gulab.sigkillserver.domain.game.model.Game;
import com.gulab.sigkillserver.domain.game.model.GamePlayer;
import com.gulab.sigkillserver.domain.game.model.GamePlayerStatus;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameEventBuilder {

    public GameStartEvent toGameStartEvent(Room room, Game game, List<Player> players) {
        List<QuizEndPlayerInfo> playerInfos = players.stream()
                .map(player -> new QuizEndPlayerInfo(
                        player.getUserId(),
                        player.getNickname(),
                        GamePlayerStatus.ALIVE,
                        QuizResult.NONE,
                        0
                ))
                .toList();
        return GameStartEvent.of(
                room.getRoomId(),
                game.getGameId(),
                new GameStartPayload(
                        new GameStartQuizInfo(0, game.getTotalQuizCount()),
                        playerInfos
                )
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
            List<QuizEndPlayerInfo> quizAnswerInfoList,
            int answerNumber,
            long occurredAt
    ) {
        QuizProgressInfo quizProgressInfo = new QuizProgressInfo(
                quiz.quizId(),
                game.getCurrentQuizIndex() + 1,
                game.getTotalQuizCount()
        );
        QuizAnswerInfo quizAnswerInfo = new QuizAnswerInfo(answerNumber, quiz.explanation());
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
    public QuizEndPlayerInfo toQuizEndPlayerInfo(
            GamePlayer gamePlayer,
            QuizResult quizResult) {
        return new QuizEndPlayerInfo(
                gamePlayer.getUserId(),
                gamePlayer.getNickname(),
                gamePlayer.isAlive() ? GamePlayerStatus.ALIVE : GamePlayerStatus.DEAD,
                quizResult,
                gamePlayer.getScore()
        );
    }

    public GameEndEvent toGameEndEvent(
            Room room,
            Game game,
            GameEndReason reason,
            List<GameRankingInfo> rankings,
            long occurredAt
    ) {
        return GameEndEvent.of(
                room.getRoomId(),
                game.getGameId(),
                occurredAt,
                new GameEndPayload(reason, rankings)
        );
    }

    public List<GameRankingInfo> buildRankings(List<GamePlayer> gamePlayers) {
        List<GamePlayer> sortedPlayers = gamePlayers.stream()
                .sorted(
                        Comparator.comparingInt(GamePlayer::getScore)
                                .reversed()
                                .thenComparingLong(GamePlayer::getUserId)
                )
                .toList();

        List<GameRankingInfo> rankings = new ArrayList<>();
        Integer previousScore = null;
        int currentRank = 0;
        for (int i = 0; i < sortedPlayers.size(); i++) {
            GamePlayer gamePlayer = sortedPlayers.get(i);

            if (previousScore == null || gamePlayer.getScore() != previousScore) {
                currentRank = i + 1;
                previousScore = gamePlayer.getScore();
            }

            rankings.add(new GameRankingInfo(
                    currentRank,
                    gamePlayer.getUserId(),
                    gamePlayer.getNickname(),
                    gamePlayer.getScore()
            ));
        }

        return rankings;
    }
}
