package com.gulab.sigkillserver.domain.game.dto.stomp.event;

import java.util.List;

public record GameLoadPayload(
        List<PlayerLoadInfo> players,
        boolean allLoaded
) {
    public GameLoadPayload {
        players = List.copyOf(players);
    }
}
