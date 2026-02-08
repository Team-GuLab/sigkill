package com.gulab.sigkillserver.domain.room.dto.stomp.response;

import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.List;

/**
 * 플레이어 본인 참가 퍼스널 메시지
 */
public record PlayerJoinMessage(
        String type,
        RoomInfo room,
        List<RoomPlayerInfoMessage> players
) {
    public static PlayerJoinMessage of(Room room, List<RoomPlayerInfoMessage> players) {
        return new PlayerJoinMessage(
                "PLAYER_JOIN",
                RoomInfo.of(room),
                players
        );
    }
}
