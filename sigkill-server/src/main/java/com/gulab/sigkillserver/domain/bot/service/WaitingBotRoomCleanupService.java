package com.gulab.sigkillserver.domain.bot.service;

import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.lock.RoomLockManager;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.service.PendingRoomJoinOrchestrator;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingBotRoomCleanupService {

    private static final long MIN_BOT_LEAVE_DELAY_MILLIS = 500L;
    private static final long MAX_BOT_LEAVE_DELAY_MILLIS = 1_500L;
    private static final long DRAIN_RETRY_BASE_DELAY_MILLIS = 1_000L;
    private static final long DRAIN_RETRY_MAX_DELAY_MILLIS = 8_000L;

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final BotUserService botUserService;
    private final RoomService roomService;
    private final RoomLockManager roomLockManager;
    private final PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator;
    private final SimpMessagingTemplate messagingTemplate;
    @Qualifier("botTaskScheduler")
    private final TaskScheduler botTaskScheduler;

    private final Set<String> drainingRoomIds = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> drainFailureCounts = new ConcurrentHashMap<>();

    public void scheduleDrainIfWaitingBotOnly(String roomId) {
        boolean shouldSchedule = roomLockManager.executeWithLock(roomId, () -> {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (!isWaitingBotOnlyRoom(room)) {
                return false;
            }
            room.markClosing();
            return drainingRoomIds.add(roomId);
        });
        if (!shouldSchedule) {
            return;
        }
        scheduleDrain(roomId, randomDelay(MIN_BOT_LEAVE_DELAY_MILLIS, MAX_BOT_LEAVE_DELAY_MILLIS));
    }

    private void scheduleDrain(String roomId, long delayMillis) {
        botTaskScheduler.schedule(
                () -> drainRoom(roomId),
                Instant.now().plusMillis(delayMillis)
        );
    }

    private void drainRoom(String roomId) {
        try {
            DrainResult drainResult = roomLockManager.executeWithLock(roomId, () -> drainOneBot(roomId));
            if (drainResult == null) {
                completeDrain(roomId);
                return;
            }

            pendingRoomJoinOrchestrator.cancelPendingJoinTimeout(drainResult.botUserId());
            botUserService.deleteBotUser(drainResult.botUserId());
            drainFailureCounts.remove(roomId);
            broadcastDrainEvents(roomId, drainResult);

            if (drainResult.shouldContinue()) {
                scheduleDrain(roomId, randomDelay(MIN_BOT_LEAVE_DELAY_MILLIS, MAX_BOT_LEAVE_DELAY_MILLIS));
                return;
            }

            completeDrain(roomId);
        } catch (RuntimeException e) {
            handleDrainFailure(roomId, e);
        }
    }

    private DrainResult drainOneBot(String roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (!isWaitingBotOnlyRoom(room)) {
            clearClosing(room);
            return null;
        }

        List<Player> botPlayers = findBotPlayers(roomId);
        if (botPlayers.isEmpty()) {
            clearClosing(room);
            return null;
        }

        Player leavingBot = selectBotToLeave(room, botPlayers);
        boolean wasActive = leavingBot.isActive();
        LeaveRoomResult leaveRoomResult = roomService.leaveRoom(roomId, leavingBot.getUserId());

        Room remainingRoom = roomRepository.findById(roomId).orElse(null);
        boolean shouldContinue = isWaitingBotOnlyRoom(remainingRoom);
        if (!shouldContinue) {
            clearClosing(remainingRoom);
        }

        return new DrainResult(leavingBot.getUserId(), wasActive, leaveRoomResult, shouldContinue);
    }

    private Player selectBotToLeave(Room room, List<Player> botPlayers) {
        return botPlayers.stream()
                .filter(player -> player.getUserId().equals(room.getHostId()))
                .findFirst()
                .orElseGet(() -> botPlayers.stream()
                        .min(Comparator.comparing(Player::getCreatedAt))
                        .orElseThrow());
    }

    private boolean isWaitingBotOnlyRoom(Room room) {
        if (room == null || room.isInGame()) {
            return false;
        }
        List<Player> players = playerRepository.findAllByRoomId(room.getRoomId());
        if (players.isEmpty()) {
            return false;
        }
        return players.stream().allMatch(player -> botUserService.isBotUser(player.getUserId()));
    }

    private List<Player> findBotPlayers(String roomId) {
        return playerRepository.findAllByRoomId(roomId).stream()
                .filter(player -> botUserService.isBotUser(player.getUserId()))
                .toList();
    }

    private void clearClosing(Room room) {
        if (room != null) {
            room.clearClosing();
        }
    }

    private void broadcastDrainEvents(String roomId, DrainResult drainResult) {
        if (!drainResult.wasActive() && !drainResult.leaveRoomResult().hasHostChangedEvent()) {
            return;
        }

        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, drainResult.leaveRoomResult().playerLeftEvent());
            if (drainResult.leaveRoomResult().hasHostChangedEvent()) {
                messagingTemplate.convertAndSend("/topic/room/" + roomId, drainResult.leaveRoomResult().hostChangedEvent());
            }
        } catch (RuntimeException e) {
            log.warn("bot.cleanup waiting-room-drain broadcast failed - roomId={}", roomId, e);
        }
    }

    private void handleDrainFailure(String roomId, RuntimeException e) {
        int failureCount = drainFailureCounts.merge(roomId, 1, Integer::sum);
        if (!shouldRetryDrain(roomId)) {
            completeDrain(roomId);
            log.warn("bot.cleanup waiting-room-drain failed without retry - roomId={}, failureCount={}",
                    roomId, failureCount, e);
            return;
        }

        long retryDelayMillis = retryDelayMillis(failureCount);
        log.warn("bot.cleanup waiting-room-drain failed - roomId={}, failureCount={}, retryDelayMillis={}",
                roomId, failureCount, retryDelayMillis, e);
        scheduleDrain(roomId, retryDelayMillis);
    }

    private boolean shouldRetryDrain(String roomId) {
        return roomLockManager.executeWithLock(roomId, () -> {
            Room room = roomRepository.findById(roomId).orElse(null);
            if (!isWaitingBotOnlyRoom(room)) {
                clearClosing(room);
                return false;
            }
            room.markClosing();
            return true;
        });
    }

    private long retryDelayMillis(int failureCount) {
        long multiplier = 1L << Math.min(Math.max(failureCount - 1, 0), 3);
        return Math.min(DRAIN_RETRY_BASE_DELAY_MILLIS * multiplier, DRAIN_RETRY_MAX_DELAY_MILLIS);
    }

    private void completeDrain(String roomId) {
        drainingRoomIds.remove(roomId);
        drainFailureCounts.remove(roomId);
    }

    private long randomDelay(long minDelayMillis, long maxDelayMillis) {
        return ThreadLocalRandom.current().nextLong(minDelayMillis, maxDelayMillis + 1);
    }

    private record DrainResult(
            Long botUserId,
            boolean wasActive,
            LeaveRoomResult leaveRoomResult,
            boolean shouldContinue
    ) {
    }
}
