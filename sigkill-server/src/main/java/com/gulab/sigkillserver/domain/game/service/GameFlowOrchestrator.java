package com.gulab.sigkillserver.domain.game.service;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.EndQuizOrGameEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
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

    private static final long QUIZ_START_DELAY_MILLIS = 3_000L;

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
        scheduleQuizStart(roomId, gameId, QUIZ_START_DELAY_MILLIS);
    }

    public void cancelFlow(Long gameId) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(gameId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            log.debug("game.flow canceled - gameId={}", gameId);
        }
        initialQuizStartTriggeredGames.remove(gameId);
    }

    private void scheduleQuizStart(String roomId, Long gameId, long delayMillis) {
        ScheduledFuture<?> future = gameTaskScheduler.schedule(
                () -> handleQuizStart(roomId, gameId),
                Instant.now().plusMillis(delayMillis)
        );
        replaceScheduledTask(gameId, future);
        log.info("game.flow quiz-start scheduled - roomId={}, gameId={}, delayMillis={}",
                roomId, gameId, delayMillis);
    }

    private void handleQuizStart(String roomId, Long gameId) {
        scheduledTasks.remove(gameId);
        try {
            Long hostId = resolveHostId(roomId);
            if (hostId == null) {
                log.warn("game.flow quiz-start skipped - room not found, roomId={}, gameId={}", roomId, gameId);
                initialQuizStartTriggeredGames.remove(gameId);
                return;
            }

            QuizStartEvent quizStartEvent = gameService.startQuiz(hostId, roomId, gameId);
            messagingTemplate.convertAndSend("/topic/game/" + gameId, quizStartEvent);
            scheduleQuizEnd(roomId, gameId, quizStartEvent.payload().quiz().quizId(), quizStartEvent.payload().quiz().endTime());

            log.info("game.flow quiz-start executed - roomId={}, gameId={}, quizId={}",
                    roomId, gameId, quizStartEvent.payload().quiz().quizId());
        } catch (CustomException e) {
            log.warn("game.flow quiz-start failed - gameId={}, code={}, message={}",
                    gameId, e.getErrorCode().getCode(), e.getErrorCode().getMessage());
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
        log.info("game.flow quiz-end scheduled - roomId={}, gameId={}, quizId={}, endAt={}",
                roomId, gameId, quizId, quizEndAtMillis);
    }

    private void handleQuizEnd(String roomId, Long gameId, Long quizId) {
        scheduledTasks.remove(gameId);
        try {
            Long hostId = resolveHostId(roomId);
            if (hostId == null) {
                log.warn("game.flow quiz-end skipped - room not found, roomId={}, gameId={}, quizId={}",
                        roomId, gameId, quizId);
                return;
            }

            EndQuizOrGameEvent endQuizOrGameEvent = gameService.endQuiz(hostId, roomId, gameId, quizId);
            messagingTemplate.convertAndSend("/topic/game/" + gameId, endQuizOrGameEvent.quizEndEvent());
            if (endQuizOrGameEvent.hasGameEnd()) {
                messagingTemplate.convertAndSend("/topic/game/" + gameId, endQuizOrGameEvent.gameEndEvent());
                initialQuizStartTriggeredGames.remove(gameId);
                log.info("game.flow game-end reached - roomId={}, gameId={}, quizId={}", roomId, gameId, quizId);
                return;
            }

            scheduleQuizStart(roomId, gameId, QUIZ_START_DELAY_MILLIS);
            log.info("game.flow next-quiz scheduled - roomId={}, gameId={}, quizId={}", roomId, gameId, quizId);
        } catch (CustomException e) {
            log.warn("game.flow quiz-end failed - gameId={}, quizId={}, code={}, message={}",
                    gameId, quizId, e.getErrorCode().getCode(), e.getErrorCode().getMessage());
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

    private Long resolveHostId(String roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getHostId())
                .orElse(null);
    }
}
