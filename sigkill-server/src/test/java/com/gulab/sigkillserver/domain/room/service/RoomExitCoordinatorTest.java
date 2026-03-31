package com.gulab.sigkillserver.domain.room.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.bot.service.WaitingBotRoomCleanupService;
import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.HostChangedEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerLeftEvent;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RoomExitCoordinatorTest {

    private final RoomService roomService = mock(RoomService.class);
    private final PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator = mock(PendingRoomJoinOrchestrator.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final WaitingBotRoomCleanupService waitingBotRoomCleanupService = mock(WaitingBotRoomCleanupService.class);
    private final RoomExitCoordinator roomExitCoordinator = new RoomExitCoordinator(
            roomService,
            pendingRoomJoinOrchestrator,
            playerRepository,
            messagingTemplate,
            waitingBotRoomCleanupService
    );

    @Test
    void pending_호스트_퇴장은_host_changed가_있어도_브로드캐스트하지_않는다() {
        // given
        String roomId = "1001";
        Long pendingHostId = 1L;
        Long newHostId = 2L;
        Player pendingHost = Player.create(pendingHostId, roomId, "pendingHost");
        Player newHost = Player.create(newHostId, roomId, "newHost");
        newHost.activate();
        LeaveRoomResult leaveRoomResult = LeaveRoomResult.of(
                PlayerLeftEvent.of(pendingHost, pendingHostId),
                HostChangedEvent.of(newHost, pendingHost, newHostId, "HOST_LEFT")
        );
        when(playerRepository.findById(pendingHostId)).thenReturn(java.util.Optional.of(pendingHost));
        when(roomService.leaveRoom(roomId, pendingHostId)).thenReturn(leaveRoomResult);

        // when
        roomExitCoordinator.leaveRoom(roomId, pendingHostId);

        // then
        verify(messagingTemplate, never()).convertAndSend("/topic/room/" + roomId, leaveRoomResult.playerLeftEvent());
        verify(messagingTemplate, never()).convertAndSend("/topic/room/" + roomId, leaveRoomResult.hostChangedEvent());
    }

    @Test
    void disconnect된_pending_호스트도_브로드캐스트하지_않는다() {
        // given
        String roomId = "1001";
        Long pendingHostId = 1L;
        Long newHostId = 2L;
        Player pendingHost = Player.create(pendingHostId, roomId, "pendingHost");
        Player newHost = Player.create(newHostId, roomId, "newHost");
        newHost.activate();
        LeaveRoomResult leaveRoomResult = LeaveRoomResult.of(
                PlayerLeftEvent.of(pendingHost, pendingHostId),
                HostChangedEvent.of(newHost, pendingHost, newHostId, "HOST_LEFT")
        );
        when(roomService.leaveRoom(roomId, pendingHostId)).thenReturn(leaveRoomResult);

        // when
        roomExitCoordinator.leaveRoomByDisconnect(pendingHost);

        // then
        verify(messagingTemplate, never()).convertAndSend("/topic/room/" + roomId, leaveRoomResult.playerLeftEvent());
        verify(messagingTemplate, never()).convertAndSend("/topic/room/" + roomId, leaveRoomResult.hostChangedEvent());
    }

    @Test
    void active_플레이어_퇴장은_기존대로_브로드캐스트한다() {
        // given
        String roomId = "1001";
        Long userId = 1L;
        Player activePlayer = Player.create(userId, roomId, "active");
        activePlayer.activate();
        LeaveRoomResult leaveRoomResult = LeaveRoomResult.of(PlayerLeftEvent.of(activePlayer, 99L));
        when(playerRepository.findById(userId)).thenReturn(java.util.Optional.of(activePlayer));
        when(roomService.leaveRoom(roomId, userId)).thenReturn(leaveRoomResult);

        // when
        roomExitCoordinator.leaveRoom(roomId, userId);

        // then
        verify(messagingTemplate).convertAndSend("/topic/room/" + roomId, leaveRoomResult.playerLeftEvent());
    }
}
