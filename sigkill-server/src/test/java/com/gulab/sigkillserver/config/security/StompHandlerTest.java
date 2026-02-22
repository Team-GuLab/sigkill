package com.gulab.sigkillserver.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class StompHandlerTest {

    private PlayerRepository playerRepository;
    private StompHandler stompHandler;
    private MessageChannel messageChannel;

    @BeforeEach
    void setup() {
        playerRepository = mock(PlayerRepository.class);
        stompHandler = new StompHandler(playerRepository, new SimpleMeterRegistry());
        messageChannel = mock(MessageChannel.class);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void room_topic_현재_방_멤버_구독은_허용한다() {
        // given
        when(playerRepository.findById(1L)).thenReturn(Optional.of(Player.create(1L, "1001", "user1")));
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, () -> "1", "/topic/room/1001");

        // when
        Message<?> result = stompHandler.preSend(message, messageChannel);

        // then
        assertThat(result).isSameAs(message);
        verify(playerRepository).findById(1L);
    }

    @Test
    void room_topic_pre_join_구독은_허용한다() {
        // given
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, () -> "1", "/topic/room/1001");

        // when
        Message<?> result = stompHandler.preSend(message, messageChannel);

        // then
        assertThat(result).isSameAs(message);
    }

    @Test
    void room_topic_다른_방_멤버_구독은_거부한다() {
        // given
        when(playerRepository.findById(1L)).thenReturn(Optional.of(Player.create(1L, "2002", "user1")));
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, () -> "1", "/topic/room/1001");

        // when // then
        assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("구독 권한");
    }

    @Test
    void 사용자_큐_허용_목적지는_구독할_수_있다() {
        // given
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, () -> "1", "/user/queue/pong");

        // when
        Message<?> result = stompHandler.preSend(message, messageChannel);

        // then
        assertThat(result).isSameAs(message);
    }

    @Test
    void 허용되지_않은_큐는_구독할_수_없다() {
        // given
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, () -> "1", "/topic/global");

        // when // then
        assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("구독 권한");
    }

    @Test
    void room_topic_구독시_principal_형식이_잘못되면_거부한다() {
        // given
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, () -> "guest-user", "/topic/room/1001");

        // when // then
        assertThatThrownBy(() -> stompHandler.preSend(message, messageChannel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("유효하지 않은 사용자");
    }

    @Test
    void afterSendCompletion_호출시_MDC_값을_정리한다() {
        // given
        Message<byte[]> message = createMessage(StompCommand.SEND, () -> "1", "/app/room/ready");

        // when
        stompHandler.preSend(message, messageChannel);
        stompHandler.afterSendCompletion(message, messageChannel, true, null);

        // then
        assertThat(MDC.get("channel")).isNull();
        assertThat(MDC.get("stompCommand")).isNull();
        assertThat(MDC.get("destination")).isNull();
        assertThat(MDC.get("sessionId")).isNull();
    }

    private Message<byte[]> createMessage(StompCommand command, Principal principal, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        accessor.setUser(principal);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
