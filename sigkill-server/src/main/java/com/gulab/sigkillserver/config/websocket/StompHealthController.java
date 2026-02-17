package com.gulab.sigkillserver.config.websocket;

import java.security.Principal;
import java.time.Instant;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class StompHealthController {

    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public PongMessage ping(Principal principal) {
        String userId = principal != null ? principal.getName() : "UNKNOWN";
        return PongMessage.of(userId);
    }

    public record PongMessage(
            String type,
            String userId,
            String serverTime
    ) {
        private static final String TYPE = "PONG";

        public static PongMessage of(String userId) {
            return new PongMessage(TYPE, userId, Instant.now().toString());
        }
    }
}
