package com.gulab.sigkillserver.config.websocket;

import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// 스프링과 STOMP 는 세션관리를 내부적으로 처리하므로 추적이 어려움. WebSocket 연결, 구독, 연결 해제 이벤트를 기록하고 확인하기 위한 리스너 클래스. 디버깅, 로그 목적
@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final RoomService roomService;
    private final PlayerRepository playerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> sessions = ConcurrentHashMap.newKeySet();

    @EventListener
    public void connectHandle(SessionConnectedEvent event) {
        MDC.put("channel", "WS");
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            sessions.add(accessor.getSessionId());

            log.debug("WebSocket 연결됨: SessionId={}, TotalSessions={}", accessor.getSessionId(), sessions.size());
        } finally {
            MDC.remove("channel");
        }
    }

    @EventListener
    public void disconnectHandle(SessionDisconnectEvent event) {
        MDC.put("channel", "WS");
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
            sessions.remove(accessor.getSessionId());

            log.debug("WebSocket 연결 해제됨: SessionId={}, TotalSessions={}", accessor.getSessionId(), sessions.size());

            Principal user = accessor.getUser();
            if (user == null) {
                return;
            }

            Long userId;
            try {
                userId = Long.parseLong(user.getName());
            } catch (NumberFormatException e) {
                log.warn("DISCONNECT userId 파싱 실패 - principalName={}", user.getName());
                return;
            }

            playerRepository.findById(userId).ifPresentOrElse(this::leaveRoomByDisconnect,
                    () -> log.debug("DISCONNECT 후 정리 대상 플레이어 없음 - userId={}", userId));
        } finally {
            MDC.remove("channel");
        }
    }

    private void leaveRoomByDisconnect(Player player) {
        String roomId = player.getRoomId();
        Long userId = player.getUserId();

        LeaveRoomResult leaveRoomResult;
        try {
            leaveRoomResult = roomService.leaveRoom(roomId, userId);
        } catch (RuntimeException e) {
            log.warn("DISCONNECT 자동 퇴장 처리 실패 - roomId={}, userId={}, message={}", roomId, userId, e.getMessage());
            return;
        }

        messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveRoomResult.playerLeftEvent());
        if (leaveRoomResult.hasHostChangedEvent()) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveRoomResult.hostChangedEvent());
        }

        log.debug("DISCONNECT 자동 퇴장 처리 완료 - roomId={}, userId={}", roomId, userId);
    }
}
