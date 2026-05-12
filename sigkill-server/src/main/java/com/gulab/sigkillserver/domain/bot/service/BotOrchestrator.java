package com.gulab.sigkillserver.domain.bot.service;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_ROOM_NUMBER;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_ROOM_NUMBER;
import static com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM;
import static com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode.PLAYER_NOT_IN_ROOM;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.CANNOT_CREATE_BOT;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NOT_FOUND;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NUMBER_ERROR;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameEndEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameLoadEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.QuizStartEvent;
import com.gulab.sigkillserver.domain.game.model.GamePlayer;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.game.service.GameFlowOrchestrator;
import com.gulab.sigkillserver.domain.game.service.GameService;
import com.gulab.sigkillserver.domain.lock.RoomLockManager;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import com.gulab.sigkillserver.domain.user.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotOrchestrator {

    private static final long MIN_READY_DELAY_MILLIS = 300L;
    private static final long MAX_READY_DELAY_MILLIS = 1_200L;
    private static final long MIN_LOAD_DELAY_MILLIS = 500L;
    private static final long MAX_LOAD_DELAY_MILLIS = 1_500L;
    private static final long MIN_POST_GAME_DELAY_MILLIS = 500L;
    private static final long MAX_POST_GAME_DELAY_MILLIS = 1_500L;
    private static final long MIN_SUBMIT_OFFSET_MILLIS = 2_000L;
    private static final long SUBMIT_SAFETY_MARGIN_MILLIS = 200L;

    private final BotUserService botUserService;
    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomLockManager roomLockManager;
    private final GameService gameService;
    private final GameFlowOrchestrator gameFlowOrchestrator;
    private final QuizRepository quizRepository;
    private final QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final WaitingBotRoomCleanupService waitingBotRoomCleanupService;
    private final SimpMessagingTemplate messagingTemplate;

    @Qualifier("botTaskScheduler")
    private final TaskScheduler botTaskScheduler;

    public void addBot(String roomId, Long requesterId) {
        validateRoomId(roomId);
        AtomicReference<Long> botUserIdRef = new AtomicReference<>();
        AtomicReference<PlayerJoinEvent> playerJoinEventRef = new AtomicReference<>();

        roomLockManager.executeWithLock(roomId, () -> {
            Room room = getRoomOrThrow(roomId);
            Player requester = getPlayerInRoomOrThrow(requesterId, roomId);
            validateBotAddRequest(room, requester);

            User botUser = botUserService.createBotUser();
            botUserIdRef.set(botUser.getUserId());

            try {
                roomService.joinRoom(roomId, botUser.getUserId());
                PlayerJoinEvent playerJoinEvent = roomService.confirmJoin(roomId, botUser.getUserId())
                        .orElseThrow(() -> new IllegalStateException("bot join confirmation must emit PLAYER_JOIN"));
                playerJoinEventRef.set(playerJoinEvent);
                return null;
            } catch (RuntimeException e) {
                cleanupBotArtifacts(roomId, botUser.getUserId());
                throw e;
            }
        });

        PlayerJoinEvent playerJoinEvent = playerJoinEventRef.get();
        Long botUserId = botUserIdRef.get();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, playerJoinEvent);
        scheduleAfterDelay(
                () -> processBotReady(roomId, botUserId),
                randomDelay(MIN_READY_DELAY_MILLIS, MAX_READY_DELAY_MILLIS)
        );
    }

    @EventListener
    public void onGameStarted(GameStartEvent event) {
        try {
            event.payload().players().stream()
                    .map(player -> player.userId())
                    .filter(botUserService::isBotUser)
                    .forEach(botUserId -> scheduleAfterDelay(
                            () -> processBotLoad(event, botUserId),
                            randomDelay(MIN_LOAD_DELAY_MILLIS, MAX_LOAD_DELAY_MILLIS)
                    ));
        } catch (RuntimeException e) {
            log.warn("bot.game-start hook failed - roomId={}, gameId={}, message={}",
                    event.roomId(), event.gameId(), e.getMessage());
        }
    }

    @EventListener
    public void onQuizStarted(QuizStartEvent event) {
        try {
            List<Long> aliveBotUserIds = gamePlayerRepository.getByGameId(event.gameId()).stream()
                    .filter(GamePlayer::isAlive)
                    .map(GamePlayer::getUserId)
                    .filter(botUserService::isBotUser)
                    .toList();

            long now = Instant.now().toEpochMilli();
            long earliestSubmitAt = Math.max(now, event.payload().quiz().startTime() + MIN_SUBMIT_OFFSET_MILLIS);
            long latestSubmitAt = event.payload().quiz().endTime() - SUBMIT_SAFETY_MARGIN_MILLIS;
            if (latestSubmitAt < earliestSubmitAt) {
                log.debug("bot.quiz-submit skipped - roomId={}, gameId={}, quizId={}, reason=no-submit-window",
                        event.roomId(), event.gameId(), event.payload().quiz().quizId());
                return;
            }

            aliveBotUserIds.forEach(botUserId -> scheduleAt(
                    () -> processBotSubmit(event, botUserId),
                    randomEpochMillis(earliestSubmitAt, latestSubmitAt)
            ));
        } catch (RuntimeException e) {
            log.warn("bot.quiz-start hook failed - roomId={}, gameId={}, quizId={}, message={}",
                    event.roomId(), event.gameId(), event.payload().quiz().quizId(), e.getMessage());
        }
    }

    @EventListener
    public void onGameEnded(GameEndEvent event) {
        try {
            List<Player> botPlayers = findBotPlayers(event.roomId());
            if (botPlayers.isEmpty()) {
                return;
            }

            if (hasHumanPlayers(event.roomId())) {
                botPlayers.forEach(botPlayer -> scheduleAfterDelay(
                        () -> processBotReady(event.roomId(), botPlayer.getUserId()),
                        randomDelay(MIN_POST_GAME_DELAY_MILLIS, MAX_POST_GAME_DELAY_MILLIS)
                ));
                return;
            }

            waitingBotRoomCleanupService.scheduleDrainIfWaitingBotOnly(event.roomId());
        } catch (RuntimeException e) {
            log.warn("bot.game-end hook failed - roomId={}, gameId={}, message={}",
                    event.roomId(), event.gameId(), e.getMessage());
        }
    }

    private void processBotReady(String roomId, Long botUserId) {
        try {
            PlayerReadyEvent playerReadyEvent = roomService.readyPlayer(roomId, botUserId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, playerReadyEvent);
        } catch (RuntimeException e) {
            log.debug("bot.ready skipped - roomId={}, userId={}, message={}", roomId, botUserId, e.getMessage());
        }
    }

    private void processBotLoad(GameStartEvent event, Long botUserId) {
        try {
            GameLoadEvent gameLoadEvent = gameService.loadGame(botUserId, event.gameId());
            messagingTemplate.convertAndSend("/topic/game/" + event.gameId(), gameLoadEvent);
            if (gameLoadEvent.payload().allLoaded()) {
                gameFlowOrchestrator.onAllPlayersLoaded(gameLoadEvent.roomId(), gameLoadEvent.gameId());
            }
        } catch (RuntimeException e) {
            log.debug("bot.load skipped - roomId={}, gameId={}, userId={}, message={}",
                    event.roomId(), event.gameId(), botUserId, e.getMessage());
        }
    }

    private void processBotSubmit(QuizStartEvent event, Long botUserId) {
        try {
            long quizId = event.payload().quiz().quizId();
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new IllegalStateException("quiz must exist for bot submission"));
            QuizChoiceNumberMapping numberMapping = quizChoiceNumberMappingRepository
                    .findByGameIdAndQuizId(event.gameId(), quizId)
                    .orElseThrow(() -> new IllegalStateException("choice mapping must exist for bot submission"));
            int selectedChoiceNumber = selectChoiceNumber(quiz, numberMapping);

            ChoiceSubmitEvent choiceSubmitEvent = gameService.submitChoice(
                    botUserId,
                    event.gameId(),
                    quizId,
                    selectedChoiceNumber
            );
            messagingTemplate.convertAndSend("/topic/game/" + event.gameId(), choiceSubmitEvent);
        } catch (RuntimeException e) {
            log.debug("bot.submit skipped - roomId={}, gameId={}, userId={}, message={}",
                    event.roomId(), event.gameId(), botUserId, e.getMessage());
        }
    }

    private int selectChoiceNumber(Quiz quiz, QuizChoiceNumberMapping numberMapping) {
        int correctChoiceNumber = numberMapping.getNumberToChoiceId().entrySet().stream()
                .filter(entry -> entry.getValue().equals(quiz.correctChoiceId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("correct choice number must exist"));

        if (ThreadLocalRandom.current().nextInt(100) < accuracyPercentage(quiz.difficulty())) {
            return correctChoiceNumber;
        }

        List<Integer> wrongChoiceNumbers = numberMapping.getNumberToChoiceId().entrySet().stream()
                .filter(entry -> !entry.getValue().equals(quiz.correctChoiceId()))
                .map(Map.Entry::getKey)
                .toList();
        return wrongChoiceNumbers.get(ThreadLocalRandom.current().nextInt(wrongChoiceNumbers.size()));
    }

    private int accuracyPercentage(int difficulty) {
        return switch (difficulty) {
            case 1 -> 90;
            case 2 -> 80;
            case 3 -> 70;
            case 4 -> 60;
            case 5 -> 50;
            default -> 50;
        };
    }

    private void validateBotAddRequest(Room room, Player requester) {
        if (!room.getHostId().equals(requester.getUserId())) {
            throw new CustomException(CANNOT_CREATE_BOT);
        }
        if (room.isClosing()) {
            throw new CustomException(CANNOT_CREATE_BOT);
        }
        if (room.isInGame()) {
            throw new CustomException(CANNOT_CREATE_BOT);
        }
        if (playerRepository.countByRoomId(room.getRoomId()) >= room.getCapacity()) {
            throw new CustomException(CANNOT_CREATE_BOT);
        }
    }

    private void cleanupBotArtifacts(String roomId, Long botUserId) {
        try {
            if (playerRepository.findById(botUserId).isPresent()) {
                roomService.leaveRoom(roomId, botUserId);
            }
        } catch (RuntimeException e) {
            log.debug("bot.add cleanup leave skipped - roomId={}, userId={}, message={}",
                    roomId, botUserId, e.getMessage());
        } finally {
            botUserService.deleteBotUser(botUserId);
        }
    }

    private Room getRoomOrThrow(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOM_NOT_FOUND));
    }

    private void validateRoomId(String roomId) {
        int roomIdInt;
        try {
            roomIdInt = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
        if (roomIdInt < MIN_ROOM_NUMBER || roomIdInt > MAX_ROOM_NUMBER) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
    }

    private Player getPlayerInRoomOrThrow(Long userId, String roomId) {
        Player player = playerRepository.findById(userId)
                .orElseThrow(() -> new CustomException(PLAYER_NOT_IN_ANY_ROOM));
        if (!player.getRoomId().equals(roomId)) {
            throw new CustomException(PLAYER_NOT_IN_ROOM);
        }
        return player;
    }

    private boolean hasHumanPlayers(String roomId) {
        return playerRepository.findAllByRoomId(roomId).stream()
                .anyMatch(player -> !botUserService.isBotUser(player.getUserId()));
    }

    private List<Player> findBotPlayers(String roomId) {
        return playerRepository.findAllByRoomId(roomId).stream()
                .filter(player -> botUserService.isBotUser(player.getUserId()))
                .toList();
    }

    private void scheduleAfterDelay(Runnable task, long delayMillis) {
        botTaskScheduler.schedule(task, Instant.now().plusMillis(delayMillis));
    }

    private void scheduleAt(Runnable task, long targetEpochMillis) {
        botTaskScheduler.schedule(task, Instant.ofEpochMilli(targetEpochMillis));
    }

    private long randomDelay(long minDelayMillis, long maxDelayMillis) {
        return ThreadLocalRandom.current().nextLong(minDelayMillis, maxDelayMillis + 1);
    }

    private long randomEpochMillis(long minEpochMillis, long maxEpochMillis) {
        if (minEpochMillis == maxEpochMillis) {
            return minEpochMillis;
        }
        return ThreadLocalRandom.current().nextLong(minEpochMillis, maxEpochMillis + 1);
    }
}
