package com.gulab.sigkillserver.config.websocket;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.room.dto.service.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.HostChangedEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerLeftEvent;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class StompEventListenerTest {

    private final RoomService roomService = mock(RoomService.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final StompEventListener stompEventListener = new StompEventListener(roomService, playerRepository, messagingTemplate);

    @Test
    void disconnect_시_플레이어가_있으면_자동_퇴장_이벤트를_브로드캐스트한다() {
        // given
        Long userId = 1L;
        Player player = Player.create(userId, "1001", "tester");
        PlayerLeftEvent playerLeftEvent = PlayerLeftEvent.of(player);
        when(playerRepository.findById(userId)).thenReturn(Optional.of(player));
        when(roomService.leaveRoom("1001", userId)).thenReturn(LeaveRoomResult.of(playerLeftEvent));

        SessionDisconnectEvent event = createDisconnectEvent("session-1", () -> String.valueOf(userId));

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(roomService).leaveRoom("1001", userId);
        verify(messagingTemplate).convertAndSend("/topic/room/1001", playerLeftEvent);
    }

    @Test
    void disconnect_시_호스트_변경_이벤트가_있으면_추가로_브로드캐스트한다() {
        // given
        Long userId = 1L;
        Player leavingPlayer = Player.create(userId, "1001", "oldHost");
        Player newHost = Player.create(2L, "1001", "newHost");
        PlayerLeftEvent playerLeftEvent = PlayerLeftEvent.of(leavingPlayer);
        HostChangedEvent hostChangedEvent = HostChangedEvent.of(newHost, leavingPlayer, "HOST_LEFT");
        when(playerRepository.findById(userId)).thenReturn(Optional.of(leavingPlayer));
        when(roomService.leaveRoom("1001", userId))
                .thenReturn(LeaveRoomResult.of(playerLeftEvent, hostChangedEvent));

        SessionDisconnectEvent event = createDisconnectEvent("session-1", () -> String.valueOf(userId));

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(messagingTemplate).convertAndSend("/topic/room/1001", playerLeftEvent);
        verify(messagingTemplate).convertAndSend("/topic/room/1001", hostChangedEvent);
    }

    @Test
    void disconnect_시_플레이어가_없으면_자동_퇴장을_실행하지_않는다() {
        // given
        Long userId = 1L;
        when(playerRepository.findById(userId)).thenReturn(Optional.empty());

        SessionDisconnectEvent event = createDisconnectEvent("session-1", () -> String.valueOf(userId));

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(roomService, never()).leaveRoom(anyString(), anyLong());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void disconnect_시_principal이_없으면_아무작업도_하지_않는다() {
        // given
        SessionDisconnectEvent event = createDisconnectEvent("session-1", null);

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(playerRepository, never()).findById(any());
        verify(roomService, never()).leaveRoom(anyString(), anyLong());
        verifyNoInteractions(messagingTemplate);
    }

    private SessionDisconnectEvent createDisconnectEvent(String sessionId, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId(sessionId);
        accessor.setUser(principal);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }
}
