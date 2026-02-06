package com.gulab.sigkillserver.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class StompHandler implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 메시지에서 STOMP 커맨드 추출
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Spring Security가 핸드셰이크 시점에 주입한 Principal 가져오기
            Authentication user = (Authentication) accessor.getUser();

            if (user == null) {
                log.error("웹소켓 연결 실패: 인증 정보 없음 (세션 만료 또는 쿠키 누락)");
                throw new AccessDeniedException("로그인이 필요합니다.");
            }

            log.info("웹소켓 연결 성공: User={}, SessionId={}", user.getName(), accessor.getSessionId());
        }

        return message;
    }
}