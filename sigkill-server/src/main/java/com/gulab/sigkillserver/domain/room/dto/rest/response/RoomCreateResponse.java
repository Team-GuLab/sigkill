package com.gulab.sigkillserver.domain.room.dto.rest.response;

import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.dto.stomp.shared.RoomInfo;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.ReadyStatus;
import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.List;

/**
 * 방 생성 응답 DTO
 */
public record RoomCreateResponse(
        RoomInfo room,
        List<PlayerInfo> players
) {
    public static RoomCreateResponse of(Room room) {
        return new RoomCreateResponse(
                RoomInfo.of(room),
                List.of(new PlayerInfo(room.getHostId(), "", ReadyStatus.NOT_READY, "HOST"))
        );
    }

    public static RoomCreateResponse of(Room room, Player hostPlayer) {
        return new RoomCreateResponse(
                RoomInfo.of(room),
                List.of(PlayerInfo.of(hostPlayer, room.getHostId()))
        );
    }

    // 하위 호환용 접근 메서드
    public String roomId() {
        return room.roomId();
    }

    public String roomTitle() {
        return room.roomTitle();
    }

    public Integer playerCount() {
        return players.size();
    }

    public Integer capacity() {
        return room.capacity();
    }

    public String status() {
        return room.status().name();
    }
}
