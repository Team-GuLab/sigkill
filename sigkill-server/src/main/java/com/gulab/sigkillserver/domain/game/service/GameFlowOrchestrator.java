package com.gulab.sigkillserver.domain.game.service;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.EndQuizOrGameEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GameFlowOrchestrator {

    private final GameService gameService;
    private final RoomRepository roomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler gameTaskScheduler;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Set<Long> initialQuizStartTriggeredGames = ConcurrentHashMap.newKeySet();

    public GameFlowOrchestrator(
            GameService gameService,
            RoomRepository roomRepository,
            SimpMessagingTemplate messagingTemplate,
            @Qualifier("gameTaskScheduler") TaskScheduler gameTaskScheduler
    ) {
        this.gameService = gameService;
        this.roomRepository = roomRepository;
        this.messagingTemplate = messagingTemplate;
        this.gameTaskScheduler = gameTaskScheduler;
    }

    public void onAllPlayersLoaded(String roomId, Long gameId) {
        if (!initialQuizStartTriggeredGames.add(gameId)) {
            log.debug("game.flow initial quiz-start already scheduled - roomId={}, gameId={}", roomId, gameId);
            return;
        }
        scheduleQuizStart(roomId, gameId, GameConstants.INITIAL_QUIZ_START_DELAY_MILLIS);
    }

    public void cancelFlow(Long gameId) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(gameId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            log.debug("game.flow canceled - gameId={}", gameId);
        }
        initialQuizStartTriggeredGames.remove(gameId);
    }

    public FlowCleanupResult clearAllFlows() {
        int scheduledTaskCount = scheduledTasks.size();
        scheduledTasks.values().forEach(scheduledFuture -> scheduledFuture.cancel(false));
        scheduledTasks.clear();

        int initialQuizStartFlagCount = initialQuizStartTriggeredGames.size();
        initialQuizStartTriggeredGames.clear();

        log.info("game.flow cleanup - canceledScheduledTasks={}, clearedInitialFlags={}",
                scheduledTaskCount, initialQuizStartFlagCount);
        return new FlowCleanupResult(scheduledTaskCount, initialQuizStartFlagCount);
    }

    private void scheduleQuizStart(String roomId, Long gameId, long delayMillis) {
        ScheduledFuture<?> future = gameTaskScheduler.schedule(
                () -> handleQuizStart(roomId, gameId),
                Instant.now().plusMillis(delayMillis)
        );
        replaceScheduledTask(gameId, future);
        log.debug("game.flow action=quiz-start-scheduled, roomId={}, gameId={}, delayMillis={}",
                roomId, gameId, delayMillis);
    }

    private void handleQuizStart(String roomId, Long gameId) {
        scheduledTasks.remove(gameId);
        try {
            Room room = findRoom(roomId);
            if (room == null) {
                log.warn("game.flow action=quiz-start-skipped, reason=room-not-found, roomId={}, gameId={}",
                        roomId, gameId);
                initialQuizStartTriggeredGames.remove(gameId);
                return;
            }

            QuizStartEvent quizStartEvent = gameService.startQuiz(room.getHostId(), roomId, gameId);
            messagingTemplate.convertAndSend("/topic/game/" + gameId, quizStartEvent);
            scheduleQuizEnd(roomId, gameId, quizStartEvent.payload().quiz().quizId(),
                    quizStartEvent.payload().quiz().endTime());

            log.info("방 퀴즈 시작됨 - action=quiz-started, roomId={}, gameId={}, quizId={}, quizOrder={}/{}",
                    roomId,
                    gameId,
                    quizStartEvent.payload().quiz().quizId(),
                    quizStartEvent.payload().quiz().currentQuizIndex() + 1,
                    quizStartEvent.payload().quiz().totalQuizCount());
        } catch (CustomException e) {
            log.warn("방 퀴즈 시작 실패 - action=quiz-start-failed, roomId={}, gameId={}, code={}, message={}",
                    roomId, gameId, e.getErrorCode().getCode(), e.getErrorCode().getMessage());
            initialQuizStartTriggeredGames.remove(gameId);
        } catch (RuntimeException e) {
            log.error("game.flow quiz-start unexpected failure - gameId={}", gameId, e);
            initialQuizStartTriggeredGames.remove(gameId);
        }
    }

    private void scheduleQuizEnd(String roomId, Long gameId, Long quizId, long quizEndAtMillis) {
        ScheduledFuture<?> future = gameTaskScheduler.schedule(
                () -> handleQuizEnd(roomId, gameId, quizId),
                Instant.ofEpochMilli(quizEndAtMillis)
        );
        replaceScheduledTask(gameId, future);
        log.debug("game.flow action=quiz-end-scheduled, roomId={}, gameId={}, quizId={}, endAt={}",
                roomId, gameId, quizId, quizEndAtMillis);
    }

    private void handleQuizEnd(String roomId, Long gameId, Long quizId) {
        scheduledTasks.remove(gameId);
        try {
            Room room = findRoom(roomId);
            if (room == null) {
                log.warn("game.flow action=quiz-end-skipped, reason=room-not-found, roomId={}, gameId={}, quizId={}",
                        roomId, gameId, quizId);
                return;
            }

            EndQuizOrGameEvent endQuizOrGameEvent = gameService.endQuiz(room.getHostId(), roomId, gameId, quizId);
            messagingTemplate.convertAndSend("/topic/game/" + gameId, endQuizOrGameEvent.quizEndEvent());
            log.info("방 퀴즈 종료됨 - action=quiz-ended, roomId={}, gameId={}, quizId={}",
                    roomId, gameId, quizId);

            if (endQuizOrGameEvent.hasGameEnd()) {
                messagingTemplate.convertAndSend("/topic/game/" + gameId, endQuizOrGameEvent.gameEndEvent());
                initialQuizStartTriggeredGames.remove(gameId);
                log.info("방 게임 종료됨 - action=game-ended, roomId={}, gameId={}, quizId={}, reason={}",
                        roomId,
                        gameId,
                        quizId,
                        endQuizOrGameEvent.gameEndEvent().payload().reason());
                return;
            }

            scheduleQuizStart(roomId, gameId, GameConstants.NEXT_QUIZ_START_DELAY_MILLIS);
            log.debug("game.flow action=next-quiz-scheduled, roomId={}, gameId={}, quizId={}",
                    roomId, gameId, quizId);
        } catch (CustomException e) {
            log.warn("방 퀴즈 종료 실패 - action=quiz-end-failed, roomId={}, gameId={}, quizId={}, code={}, message={}",
                    roomId,
                    gameId,
                    quizId,
                    e.getErrorCode().getCode(),
                    e.getErrorCode().getMessage());
        } catch (RuntimeException e) {
            log.error("game.flow quiz-end unexpected failure - gameId={}, quizId={}", gameId, quizId, e);
        }
    }

    private void replaceScheduledTask(Long gameId, ScheduledFuture<?> newFuture) {
        if (newFuture == null) {
            return;
        }

        ScheduledFuture<?> previous = scheduledTasks.put(gameId, newFuture);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    private Room findRoom(String roomId) {
        return roomRepository.findById(roomId)
                .orElse(null);
    }
    public record FlowCleanupResult(int canceledScheduledTaskCount, int clearedInitialQuizStartFlagCount) {
    }
}
