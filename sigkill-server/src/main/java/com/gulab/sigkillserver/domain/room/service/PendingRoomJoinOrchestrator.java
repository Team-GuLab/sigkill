package com.gulab.sigkillserver.domain.room.service;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.PENDING_JOIN_TIMEOUT_MILLIS;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PendingRoomJoinOrchestrator {

    private final TaskScheduler taskScheduler;
    private final PlayerRepository playerRepository;
    private final RoomService roomService;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public PendingRoomJoinOrchestrator(
            @Qualifier("gameTaskScheduler") TaskScheduler taskScheduler,
            PlayerRepository playerRepository,
            RoomService roomService
    ) {
        this.taskScheduler = taskScheduler;
        this.playerRepository = playerRepository;
        this.roomService = roomService;
    }

    public void schedulePendingJoinTimeout(String roomId, Long userId) {
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> expirePendingJoin(roomId, userId),
                Instant.now().plusMillis(PENDING_JOIN_TIMEOUT_MILLIS)
        );
        replaceScheduledTask(userId, future);
        log.debug("room.pendingJoin scheduled - roomId={}, userId={}, timeoutMillis={}",
                roomId, userId, PENDING_JOIN_TIMEOUT_MILLIS);
    }

    public void cancelPendingJoinTimeout(Long userId) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(userId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            log.debug("room.pendingJoin canceled - userId={}", userId);
        }
    }

    public PendingJoinCleanupResult clearAllPendingJoinTimeouts() {
        int canceledTaskCount = scheduledTasks.size();
        scheduledTasks.values().forEach(scheduledFuture -> scheduledFuture.cancel(false));
        scheduledTasks.clear();
        log.info("room.pendingJoin cleanup - canceledScheduledTasks={}", canceledTaskCount);
        return new PendingJoinCleanupResult(canceledTaskCount);
    }

    private void expirePendingJoin(String roomId, Long userId) {
        scheduledTasks.remove(userId);
        try {
            Player player = playerRepository.findById(userId).orElse(null);
            if (player == null || !roomId.equals(player.getRoomId()) || player.isActive()) {
                return;
            }
            roomService.leaveRoom(roomId, userId);
            log.info("room.pendingJoin expired - roomId={}, userId={}", roomId, userId);
        } catch (RuntimeException e) {
            log.warn("room.pendingJoin expiration failed - roomId={}, userId={}, message={}",
                    roomId, userId, e.getMessage());
        }
    }

    private void replaceScheduledTask(Long userId, ScheduledFuture<?> newFuture) {
        if (newFuture == null) {
            return;
        }
        ScheduledFuture<?> previous = scheduledTasks.put(userId, newFuture);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    public record PendingJoinCleanupResult(int canceledScheduledTaskCount) {
    }
}
