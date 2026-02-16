package com.gulab.sigkillserver.domain.room.dto.stomp.event;

import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.dto.stomp.shared.RoomInfo;
import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.List;

/**
 * 플레이어 본인 참가 퍼스널 메시지
 */
public record PlayerJoinEvent(
        RoomResponseType type,
        RoomInfo room,
        List<PlayerInfo> players
) {
    public static PlayerJoinEvent of(Room room, List<PlayerInfo> players) {
        return new PlayerJoinEvent(
                RoomResponseType.PLAYER_JOIN,
                RoomInfo.of(room),
                players
        );
    }
}
