package com.gulab.sigkillserver.domain.bot.service;

import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.service.PendingRoomJoinOrchestrator;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final BotUserService botUserService;
    private final RoomService roomService;
    private final PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator;
    private final SimpMessagingTemplate messagingTemplate;
    @Qualifier("botTaskScheduler")
    private final TaskScheduler botTaskScheduler;

    private final Set<String> drainingRoomIds = ConcurrentHashMap.newKeySet();

    public void scheduleDrainIfWaitingBotOnly(String roomId) {
        if (!isWaitingBotOnlyRoom(roomId)) {
            return;
        }
        if (!drainingRoomIds.add(roomId)) {
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
            Room room = roomRepository.findById(roomId).orElse(null);
            if (room == null || room.isInGame()) {
                drainingRoomIds.remove(roomId);
                return;
            }

            List<Player> botPlayers = findBotPlayers(roomId);
            if (botPlayers.isEmpty() || hasHumanPlayers(roomId)) {
                drainingRoomIds.remove(roomId);
                return;
            }

            Player leavingBot = selectBotToLeave(room, botPlayers);
            boolean wasActive = leavingBot.isActive();

            LeaveRoomResult leaveRoomResult = roomService.leaveRoom(roomId, leavingBot.getUserId());
            pendingRoomJoinOrchestrator.cancelPendingJoinTimeout(leavingBot.getUserId());
            botUserService.deleteBotUser(leavingBot.getUserId());

            if (wasActive || leaveRoomResult.hasHostChangedEvent()) {
                messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveRoomResult.playerLeftEvent());
                if (leaveRoomResult.hasHostChangedEvent()) {
                    messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveRoomResult.hostChangedEvent());
                }
            }

            if (isWaitingBotOnlyRoom(roomId)) {
                scheduleDrain(roomId, randomDelay(MIN_BOT_LEAVE_DELAY_MILLIS, MAX_BOT_LEAVE_DELAY_MILLIS));
                return;
            }

            drainingRoomIds.remove(roomId);
        } catch (RuntimeException e) {
            drainingRoomIds.remove(roomId);
            log.warn("bot.cleanup waiting-room-drain failed - roomId={}, message={}", roomId, e.getMessage());
        }
    }

    private Player selectBotToLeave(Room room, List<Player> botPlayers) {
        return botPlayers.stream()
                .filter(player -> player.getUserId().equals(room.getHostId()))
                .findFirst()
                .orElseGet(() -> botPlayers.stream()
                        .min(Comparator.comparing(Player::getCreatedAt))
                        .orElseThrow());
    }

    private boolean isWaitingBotOnlyRoom(String roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.isInGame()) {
            return false;
        }
        List<Player> players = playerRepository.findAllByRoomId(roomId);
        if (players.isEmpty()) {
            return false;
        }
        return players.stream().allMatch(player -> botUserService.isBotUser(player.getUserId()));
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

    private long randomDelay(long minDelayMillis, long maxDelayMillis) {
        return ThreadLocalRandom.current().nextLong(minDelayMillis, maxDelayMillis + 1);
    }
}
