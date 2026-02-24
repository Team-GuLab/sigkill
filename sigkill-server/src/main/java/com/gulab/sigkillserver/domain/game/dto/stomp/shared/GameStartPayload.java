package com.gulab.sigkillserver.domain.game.dto.stomp.shared;

import java.util.List;

public record GameStartPayload(
        GameStartQuizInfo quiz,
        List<ActorInfo> players
) {
}
