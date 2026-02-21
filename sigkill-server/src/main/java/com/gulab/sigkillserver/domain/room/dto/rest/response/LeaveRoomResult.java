package com.gulab.sigkillserver.domain.room.dto.rest.response;

import com.gulab.sigkillserver.domain.room.dto.stomp.event.HostChangedEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerLeftEvent;
import java.util.Objects;

public record LeaveRoomResult(
        PlayerLeftEvent playerLeftEvent,
        HostChangedEvent hostChangedEvent
) {
    public LeaveRoomResult {
        Objects.requireNonNull(playerLeftEvent, "playerLeftEvent 는 null 이면 안됩니다.");
    }

    public static LeaveRoomResult of(PlayerLeftEvent playerLeftEvent) {
        return new LeaveRoomResult(playerLeftEvent, null);
    }

    public static LeaveRoomResult of(PlayerLeftEvent playerLeftEvent, HostChangedEvent hostChangedEvent) {
        return new LeaveRoomResult(playerLeftEvent, hostChangedEvent);
    }

    public boolean hasHostChangedEvent() {
        return hostChangedEvent != null;
    }
}
