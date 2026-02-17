package com.gulab.sigkillserver.config.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StompHealthControllerTest {

    private final StompHealthController controller = new StompHealthController();

    @Test
    void ping_요청시_pong_응답을_반환한다() {
        // given
        String userId = "123";

        long before = Instant.now().toEpochMilli();

        // when
        StompHealthController.PongMessage pongMessage = controller.ping(() -> userId);
        long after = Instant.now().toEpochMilli();

        // then
        assertThat(pongMessage.type()).isEqualTo("PONG");
        assertThat(pongMessage.userId()).isEqualTo(userId);
        assertThat(pongMessage.serverTime()).isBetween(before, after);
    }

    @Test
    void principal_없으면_unknown_user로_응답한다() {
        // when
        StompHealthController.PongMessage pongMessage = controller.ping(null);

        // then
        assertThat(pongMessage.type()).isEqualTo("PONG");
        assertThat(pongMessage.userId()).isEqualTo("UNKNOWN");
    }
}
