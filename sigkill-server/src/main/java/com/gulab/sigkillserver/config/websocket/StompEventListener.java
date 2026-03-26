package com.gulab.sigkillserver.config.websocket;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.service.RoomExitCoordinator;
import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// 스프링과 STOMP 는 세션관리를 내부적으로 처리하므로 추적이 어려움. WebSocket 연결, 구독, 연결 해제 이벤트를 기록하고 확인하기 위한 리스너 클래스. 디버깅, 로그 목적
@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {

    private final RoomExitCoordinator roomExitCoordinator;
    private final PlayerRepository playerRepository;
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

            playerRepository.findById(userId).ifPresent(roomExitCoordinator::leaveRoomByDisconnect);
        } finally {
            MDC.remove("channel");
        }
    }
}
