package com.gulab.sigkillserver.domain.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.game.constant.GameConstants;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.EndQuizOrGameEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.GameEndReason;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizAnswerInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizEndPayload;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizProgressInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizStartInfo;
import com.gulab.sigkillserver.domain.game.dto.stomp.shared.QuizStartPayload;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;

class GameFlowOrchestratorTest {

    private final GameService gameService = mock(GameService.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final TaskScheduler gameTaskScheduler = mock(TaskScheduler.class);
    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

    private final GameFlowOrchestrator gameFlowOrchestrator = new GameFlowOrchestrator(
            gameService,
            roomRepository,
            messagingTemplate,
            gameTaskScheduler,
            applicationEventPublisher
    );

    @SuppressWarnings("unchecked")
    private ScheduledFuture<Object> mockScheduledFuture() {
        return (ScheduledFuture<Object>) mock(ScheduledFuture.class);
    }

    @Nested
    class OnAllPlayersLoadedTests {

        @Test
        void 모든_플레이어_로드완료시_3초뒤_퀴즈시작을_예약한다() {
            // given
            ScheduledFuture<Object> firstFuture = mockScheduledFuture();
            when(gameTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> firstFuture);
            Instant before = Instant.now();

            // when
            gameFlowOrchestrator.onAllPlayersLoaded("1001", 77L);

            // then
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(gameTaskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
            long delayMillis = Duration.between(before, instantCaptor.getValue()).toMillis();
            assertThat(delayMillis)
                    .isBetween(
                            GameConstants.INITIAL_QUIZ_START_DELAY_MILLIS - 500L,
                            GameConstants.INITIAL_QUIZ_START_DELAY_MILLIS + 500L
                    );
        }

        @Test
        void 모든_플레이어_로드완료_이벤트가_중복되어도_첫_퀴즈시작은_한번만_예약한다() {
            // given
            ScheduledFuture<Object> firstFuture = mockScheduledFuture();
            when(gameTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> firstFuture);

            // when
            gameFlowOrchestrator.onAllPlayersLoaded("1001", 77L);
            gameFlowOrchestrator.onAllPlayersLoaded("1001", 77L);

            // then
            verify(gameTaskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
        }

        @Test
        void 퀴즈_종료후_게임이_계속되면_10초뒤_다음_퀴즈를_예약한다() {
            // given
            String roomId = "1001";
            Long gameId = 77L;
            Long quizId = 9001L;
            Long hostId = 1L;

            ScheduledFuture<Object> firstFuture = mockScheduledFuture();
            ScheduledFuture<Object> secondFuture = mockScheduledFuture();
            ScheduledFuture<Object> thirdFuture = mockScheduledFuture();
            when(gameTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> firstFuture)
                    .thenAnswer(invocation -> secondFuture)
                    .thenAnswer(invocation -> thirdFuture);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(Room.create(roomId, "테스트", hostId, 6)));

            long quizStartTime = Instant.now().toEpochMilli();
            long quizEndTime = quizStartTime + 5_000L;
            QuizStartEvent quizStartEvent = QuizStartEvent.of(
                    roomId,
                    gameId,
                    quizStartTime,
                    new QuizStartPayload(
                            new QuizStartInfo(quizId, 1, 10, quizStartTime, quizEndTime, "질문", List.of())
                    )
            );
            when(gameService.startQuiz(hostId, roomId, gameId)).thenReturn(quizStartEvent);

            QuizEndEvent quizEndEvent = QuizEndEvent.of(
                    roomId,
                    gameId,
                    quizEndTime,
                    new QuizEndPayload(
                            new QuizProgressInfo(quizId, 1, 10),
                            new QuizAnswerInfo(1, "설명"),
                            List.of()
                    )
            );
            when(gameService.endQuiz(hostId, roomId, gameId, quizId))
                    .thenReturn(new EndQuizOrGameEvent(quizEndEvent, null));

            // when
            gameFlowOrchestrator.onAllPlayersLoaded(roomId, gameId);

            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(gameTaskScheduler).schedule(runnableCaptor.capture(), instantCaptor.capture());
            runnableCaptor.getValue().run(); // quiz start

            verify(gameTaskScheduler, times(2)).schedule(runnableCaptor.capture(), instantCaptor.capture());
            List<Runnable> secondCaptureRunnables = runnableCaptor.getAllValues();
            secondCaptureRunnables.get(secondCaptureRunnables.size() - 1).run(); // quiz end

            // then
            verify(messagingTemplate).convertAndSend("/topic/game/77", quizStartEvent);
            verify(messagingTemplate).convertAndSend("/topic/game/77", quizEndEvent);
            verify(gameTaskScheduler, times(3)).schedule(runnableCaptor.capture(), instantCaptor.capture());

            List<Instant> thirdCaptureInstants = instantCaptor.getAllValues();
            long nextDelayMillis = Duration.between(Instant.now(), thirdCaptureInstants.get(thirdCaptureInstants.size() - 1)).toMillis();
            assertThat(nextDelayMillis)
                    .isBetween(
                            GameConstants.NEXT_QUIZ_START_DELAY_MILLIS - 500L,
                            GameConstants.NEXT_QUIZ_START_DELAY_MILLIS + 500L
                    );
        }

        @Test
        void 퀴즈_종료후_게임이_끝나면_GAME_END를_브로드캐스트하고_다음퀴즈를_예약하지_않는다() {
            // given
            String roomId = "1001";
            Long gameId = 77L;
            Long quizId = 9001L;
            Long hostId = 1L;

            ScheduledFuture<Object> firstFuture = mockScheduledFuture();
            ScheduledFuture<Object> secondFuture = mockScheduledFuture();
            when(gameTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> firstFuture)
                    .thenAnswer(invocation -> secondFuture);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(Room.create(roomId, "테스트", hostId, 6)));

            long quizStartTime = Instant.now().toEpochMilli();
            long quizEndTime = quizStartTime + 5_000L;
            QuizStartEvent quizStartEvent = QuizStartEvent.of(
                    roomId,
                    gameId,
                    quizStartTime,
                    new QuizStartPayload(
                            new QuizStartInfo(quizId, 1, 10, quizStartTime, quizEndTime, "질문", List.of())
                    )
            );
            when(gameService.startQuiz(hostId, roomId, gameId)).thenReturn(quizStartEvent);

            QuizEndEvent quizEndEvent = QuizEndEvent.of(
                    roomId,
                    gameId,
                    quizEndTime,
                    new QuizEndPayload(
                            new QuizProgressInfo(quizId, 1, 10),
                            new QuizAnswerInfo(1, "설명"),
                            List.of()
                    )
            );
            GameEndEvent gameEndEvent = GameEndEvent.of(
                    roomId,
                    gameId,
                    quizEndTime + 1,
                    new GameEndPayload(GameEndReason.ONE_SURVIVOR, List.of())
            );
            when(gameService.endQuiz(hostId, roomId, gameId, quizId))
                    .thenReturn(new EndQuizOrGameEvent(quizEndEvent, gameEndEvent));

            // when
            gameFlowOrchestrator.onAllPlayersLoaded(roomId, gameId);

            ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(gameTaskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
            runnableCaptor.getValue().run(); // quiz start

            verify(gameTaskScheduler, times(2)).schedule(runnableCaptor.capture(), any(Instant.class));
            List<Runnable> secondCaptureRunnables = runnableCaptor.getAllValues();
            secondCaptureRunnables.get(secondCaptureRunnables.size() - 1).run(); // quiz end

            // then
            verify(messagingTemplate).convertAndSend("/topic/game/77", quizEndEvent);
            verify(messagingTemplate).convertAndSend("/topic/game/77", gameEndEvent);
            verify(gameTaskScheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
            verify(gameService, times(1)).startQuiz(hostId, roomId, gameId);
        }
    }

    @Nested
    class ClearAllFlowsTests {

        @Test
        void 전체_게임_플로우_정리시_예약된_태스크와_초기_시작_플래그를_모두_초기화한다() {
            // given
            ScheduledFuture<Object> scheduledFuture = mockScheduledFuture();
            when(gameTaskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                    .thenAnswer(invocation -> scheduledFuture);
            gameFlowOrchestrator.onAllPlayersLoaded("1001", 77L);

            // when
            GameFlowOrchestrator.FlowCleanupResult cleanupResult = gameFlowOrchestrator.clearAllFlows();

            // then
            verify(scheduledFuture, times(1)).cancel(false);
            assertThat(cleanupResult).isEqualTo(new GameFlowOrchestrator.FlowCleanupResult(1, 1));
        }

        @Test
        void 정리할_플로우가_없으면_정리_카운트는_모두_0이다() {
            // when
            GameFlowOrchestrator.FlowCleanupResult cleanupResult = gameFlowOrchestrator.clearAllFlows();

            // then
            assertThat(cleanupResult).isEqualTo(new GameFlowOrchestrator.FlowCleanupResult(0, 0));
        }
    }
}
