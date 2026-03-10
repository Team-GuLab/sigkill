package com.gulab.sigkillserver.config.websocket;

import com.gulab.sigkillserver.config.AppProfileProperties;
import com.gulab.sigkillserver.config.security.StompHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.session.web.socket.config.annotation.AbstractSessionWebSocketMessageBrokerConfigurer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class StompConfig extends AbstractSessionWebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_INTERVAL_MILLIS = 10_000L;
    private final StompHandler stompHandler;
    private final AppProfileProperties appProfileProperties;

    @Override
    protected void configureStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(appProfileProperties.getWebSocket().getAllowedOriginPatterns().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(stompHeartbeatTaskScheduler())
                .setHeartbeatValue(new long[]{HEARTBEAT_INTERVAL_MILLIS, HEARTBEAT_INTERVAL_MILLIS});
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    public TaskScheduler stompHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    // WebSocket connect, subscribe, disconnect 시에는 http 헤더 및 메시지를 넣을 수 있으므로 StompHandler에서 인증 처리
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        super.configureClientInboundChannel(registration);
        registration.interceptors(stompHandler);
    }
}
