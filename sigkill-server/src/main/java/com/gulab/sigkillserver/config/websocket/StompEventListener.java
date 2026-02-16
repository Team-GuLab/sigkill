package com.gulab.sigkillserver.config.websocket;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// 스프링과 STOMP 는 세션관리를 내부적으로 처리하므로 추적이 어려움. WebSocket 연결, 구독, 연결 해제 이벤트를 기록하고 확인하기 위한 리스너 클래스. 디버깅, 로그 목적
@Slf4j
@Component
public class StompEventListener {
    private final Set<String> sessions = ConcurrentHashMap.newKeySet();

    @EventListener
    public void connectHandle(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.add(accessor.getSessionId());

        log.info("WebSocket 연결됨: SessionId={}, TotalSessions={}", accessor.getSessionId(), sessions.size());
    }

    @EventListener
    public void disconnectHandle(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.remove(accessor.getSessionId());

        log.info("WebSocket 연결 해제됨: SessionId={}, TotalSessions={}", accessor.getSessionId(), sessions.size());
    }
}
