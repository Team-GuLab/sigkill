package com.gulab.sigkillserver.config.security;

import java.security.Principal;
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
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class StompHandler implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.error("STOMP 메시지 처리 실패: StompHeaderAccessor를 가져올 수 없음");
            throw new IllegalStateException("STOMP 메시지 처리 중 오류가 발생했습니다.");
        }

        // 메시지에서 STOMP 커맨드 추출
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Spring Security가 핸드셰이크 시점에 주입한 Principal 가져오기
            Principal user = accessor.getUser();

            if (user == null) {
                log.error("[CONNECT] 실패: 인증 정보 없음 (세션 만료 또는 쿠키 누락)");
                throw new AccessDeniedException("로그인이 필요합니다.");
            }

            log.info("[CONNECT] 성공: User={}, SessionId={}", user.getName(), accessor.getSessionId());
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user == null) {
                log.error("[SUBSCRIBE] 실패: 인증 정보 없음 (세션 만료 또는 쿠키 누락)");
                throw new AccessDeniedException("로그인이 필요합니다.");
            }

            log.info("[SUBSCRIBE] 성공: User={}, SessionId={}, Destination={}", user.getName(), accessor.getSessionId(),
                    accessor.getDestination());
        }

        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user != null) {
                log.info("[DISCONNECT]: User={}, SessionId={}", user.getName(), accessor.getSessionId());
            } else {
                log.info("[DISCONNECT]: SessionId={}", accessor.getSessionId());
            }
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user == null) {
                log.error("[SEND] 실패: 인증 정보 없음 (세션 만료 또는 쿠키 누락)");
                throw new AccessDeniedException("로그인이 필요합니다.");
            }

            log.info("[SEND]: User={}, SessionId={}, Destination={}", user.getName(), accessor.getSessionId(),
                    accessor.getDestination());
        }

        return message;
    }
}