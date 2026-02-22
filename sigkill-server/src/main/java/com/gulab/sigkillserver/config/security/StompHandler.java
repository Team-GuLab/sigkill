package com.gulab.sigkillserver.config.security;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.Principal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
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
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private static final String ROOM_TOPIC_PREFIX = "/topic/room/";
    private static final String MDC_CHANNEL = "channel";
    private static final String MDC_STOMP_COMMAND = "stompCommand";
    private static final String MDC_DESTINATION = "destination";
    private static final String MDC_SESSION_ID = "sessionId";
    private static final Set<String> ALLOWED_USER_QUEUE_DESTINATIONS = Set.of(
            "/user/queue/errors",
            "/user/queue/pong"
    );

    private final PlayerRepository playerRepository;
    private final MeterRegistry meterRegistry;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.error("STOMP 메시지 처리 실패: StompHeaderAccessor를 가져올 수 없음");
            throw new IllegalStateException("STOMP 메시지 처리 중 오류가 발생했습니다.");
        }

        populateWsMdc(accessor);
        recordStompCommandMetric(accessor.getCommand());
        try {
            // 메시지에서 STOMP 커맨드 추출
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // Spring Security가 핸드셰이크 시점에 주입한 Principal 가져오기
                Principal user = accessor.getUser();

                if (user == null) {
                    log.warn("[CONNECT] 실패: 인증 정보 없음 (세션 만료 또는 쿠키 누락)");
                    throw new AccessDeniedException("로그인이 필요합니다.");
                }

                log.debug("[CONNECT] 성공: User={}, SessionId={}", user.getName(), accessor.getSessionId());
            }

            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                Principal user = accessor.getUser();
                if (user == null) {
                    log.warn("[SUBSCRIBE] 실패: 인증 정보 없음 (세션 만료 또는 쿠키 누락)");
                    throw new AccessDeniedException("로그인이 필요합니다.");
                }

                authorizeSubscribe(user, accessor.getDestination());

                log.debug("[SUBSCRIBE] 성공: User={}, SessionId={}, Destination={}", user.getName(),
                        accessor.getSessionId(), accessor.getDestination());
            }

            if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                Principal user = accessor.getUser();
                if (user != null) {
                    log.debug("[DISCONNECT]: User={}, SessionId={}", user.getName(), accessor.getSessionId());
                } else {
                    log.debug("[DISCONNECT]: SessionId={}", accessor.getSessionId());
                }
            }

            if (StompCommand.SEND.equals(accessor.getCommand())) {
                Principal user = accessor.getUser();
                if (user == null) {
                    log.warn("[SEND] 실패: 인증 정보 없음 (세션 만료 또는 쿠키 누락)");
                    throw new AccessDeniedException("로그인이 필요합니다.");
                }

                log.debug("[SEND]: User={}, SessionId={}, Destination={}", user.getName(), accessor.getSessionId(),
                        accessor.getDestination());
            }

            return message;
        } catch (RuntimeException e) {
            clearWsMdc();
            throw e;
        }
    }

    @Override
    public void afterSendCompletion(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent,
                                    Exception ex) {
        clearWsMdc();
    }

    private void authorizeSubscribe(Principal user, String destination) {
        if (destination == null || destination.isBlank()) {
            throw new AccessDeniedException("구독 대상이 올바르지 않습니다.");
        }

        if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
            authorizeRoomTopicSubscription(user, destination);
            return;
        }

        if (!ALLOWED_USER_QUEUE_DESTINATIONS.contains(destination)) {
            throw new AccessDeniedException("구독 권한이 없습니다.");
        }
    }

    private void authorizeRoomTopicSubscription(Principal user, String destination) {
        String roomId = destination.substring(ROOM_TOPIC_PREFIX.length());
        if (roomId.isBlank()) {
            throw new AccessDeniedException("구독 대상이 올바르지 않습니다.");
        }

        Long userId = parseUserId(user.getName());
        var player = playerRepository.findById(userId);

        if (player.isPresent()) {
            validateCurrentRoomSubscription(player.get(), roomId, destination, userId);
            return;
        }

        log.debug("[SUBSCRIBE] join 구독 - userId={}, roomId={}", userId, roomId);
    }

    private void validateCurrentRoomSubscription(Player player, String roomId, String destination, Long userId) {
        if (player.getRoomId().equals(roomId)) {
            return;
        }

        log.warn("[SUBSCRIBE] 실패: 다른 방 멤버 - userId={}, currentRoomId={}, destination={}",
                userId, player.getRoomId(), destination);
        throw new AccessDeniedException("방 구독 권한이 없습니다.");
    }

    private Long parseUserId(String principalName) {
        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("유효하지 않은 사용자 정보입니다.");
        }
    }

    private void populateWsMdc(StompHeaderAccessor accessor) {
        MDC.put(MDC_CHANNEL, "WS");
        if (accessor.getCommand() != null) {
            MDC.put(MDC_STOMP_COMMAND, accessor.getCommand().name());
        } else {
            MDC.remove(MDC_STOMP_COMMAND);
        }

        if (accessor.getDestination() != null) {
            MDC.put(MDC_DESTINATION, accessor.getDestination());
        } else {
            MDC.remove(MDC_DESTINATION);
        }

        if (accessor.getSessionId() != null) {
            MDC.put(MDC_SESSION_ID, accessor.getSessionId());
        } else {
            MDC.remove(MDC_SESSION_ID);
        }
    }

    private void clearWsMdc() {
        MDC.remove(MDC_SESSION_ID);
        MDC.remove(MDC_DESTINATION);
        MDC.remove(MDC_STOMP_COMMAND);
        MDC.remove(MDC_CHANNEL);
    }

    private void recordStompCommandMetric(StompCommand command) {
        if (command == null) {
            return;
        }

        meterRegistry.counter(
                "sigkill.stomp.frames.total",
                "command",
                command.name()
        ).increment();
    }
}
