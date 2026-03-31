package com.gulab.sigkillserver.domain.room.service;

import com.gulab.sigkillserver.domain.bot.service.WaitingBotRoomCleanupService;
import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomExitCoordinator {

    private final RoomService roomService;
    private final PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WaitingBotRoomCleanupService waitingBotRoomCleanupService;

    public void leaveRoom(String roomId, Long userId) {
        Player player = playerRepository.findById(userId).orElse(null);
        LeaveRoomResult leaveRoomResult = processLeave(roomId, userId, false);
        broadcastRoomExit(roomId, player, leaveRoomResult);
        waitingBotRoomCleanupService.scheduleDrainIfWaitingBotOnly(roomId);
    }

    public void leaveRoomByDisconnect(Player player) {
        LeaveRoomResult leaveRoomResult = processLeave(player.getRoomId(), player.getUserId(), true);
        if (leaveRoomResult == null) {
            return;
        }
        broadcastRoomExit(player.getRoomId(), player, leaveRoomResult);
        waitingBotRoomCleanupService.scheduleDrainIfWaitingBotOnly(player.getRoomId());
    }

    private LeaveRoomResult processLeave(String roomId, Long userId, boolean swallowFailure) {
        try {
            LeaveRoomResult leaveRoomResult = roomService.leaveRoom(roomId, userId);
            pendingRoomJoinOrchestrator.cancelPendingJoinTimeout(userId);
            return leaveRoomResult;
        } catch (RuntimeException e) {
            if (swallowFailure) {
                log.warn("room.exit failed - roomId={}, userId={}, message={}", roomId, userId, e.getMessage(), e);
                return null;
            }
            throw e;
        }
    }

    private void broadcastRoomExit(String roomId, Player player, LeaveRoomResult leaveRoomResult) {
        boolean wasActive = player != null && player.isActive();
        if (!wasActive) {
            return;
        }

        messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveRoomResult.playerLeftEvent());
        if (leaveRoomResult.hasHostChangedEvent()) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveRoomResult.hostChangedEvent());
        }
    }
}
