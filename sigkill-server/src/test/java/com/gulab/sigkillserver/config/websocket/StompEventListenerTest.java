package com.gulab.sigkillserver.config.websocket;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.service.RoomExitCoordinator;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class StompEventListenerTest {

    private final RoomExitCoordinator roomExitCoordinator = mock(RoomExitCoordinator.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final StompEventListener stompEventListener = new StompEventListener(
            roomExitCoordinator,
            playerRepository
    );

    private Player activePlayer(Long userId, String roomId, String nickname) {
        Player player = Player.create(userId, roomId, nickname);
        player.activate();
        return player;
    }

    @Test
    void disconnect_시_플레이어가_있으면_자동_퇴장_이벤트를_브로드캐스트한다() {
        // given
        Long userId = 1L;
        Player player = activePlayer(userId, "1001", "tester");
        when(playerRepository.findById(userId)).thenReturn(Optional.of(player));

        SessionDisconnectEvent event = createDisconnectEvent("session-1", () -> String.valueOf(userId));

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(roomExitCoordinator).leaveRoomByDisconnect(player);
    }

    @Test
    void disconnect_시_호스트_변경_이벤트가_있으면_추가로_브로드캐스트한다() {
        // given
        Long userId = 1L;
        Player leavingPlayer = activePlayer(userId, "1001", "oldHost");
        when(playerRepository.findById(userId)).thenReturn(Optional.of(leavingPlayer));

        SessionDisconnectEvent event = createDisconnectEvent("session-1", () -> String.valueOf(userId));

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(roomExitCoordinator).leaveRoomByDisconnect(leavingPlayer);
    }

    @Test
    void disconnect_시_pending_플레이어도_자동_퇴장_처리를_위임한다() {
        // given
        Long userId = 1L;
        Player pendingPlayer = Player.create(userId, "1001", "pending");
        when(playerRepository.findById(userId)).thenReturn(Optional.of(pendingPlayer));

        SessionDisconnectEvent event = createDisconnectEvent("session-1", () -> String.valueOf(userId));

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(roomExitCoordinator).leaveRoomByDisconnect(pendingPlayer);
    }

    @Test
    void disconnect_시_pending_호스트도_자동_퇴장_처리를_위임한다() {
        // given
        Long userId = 1L;
        Player pendingHost = Player.create(userId, "1001", "pendingHost");
        when(playerRepository.findById(userId)).thenReturn(Optional.of(pendingHost));

        SessionDisconnectEvent event = createDisconnectEvent("session-1", () -> String.valueOf(userId));

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(roomExitCoordinator).leaveRoomByDisconnect(pendingHost);
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
        verify(roomExitCoordinator, never()).leaveRoomByDisconnect(any());
    }

    @Test
    void disconnect_시_principal이_없으면_아무작업도_하지_않는다() {
        // given
        SessionDisconnectEvent event = createDisconnectEvent("session-1", null);

        // when
        stompEventListener.disconnectHandle(event);

        // then
        verify(playerRepository, never()).findById(any());
        verify(roomExitCoordinator, never()).leaveRoomByDisconnect(any());
    }

    private SessionDisconnectEvent createDisconnectEvent(String sessionId, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId(sessionId);
        accessor.setUser(principal);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionDisconnectEvent(this, message, sessionId, CloseStatus.NORMAL);
    }
}
