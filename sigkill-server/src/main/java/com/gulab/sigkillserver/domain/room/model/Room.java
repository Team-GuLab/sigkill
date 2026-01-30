package com.gulab.sigkillserver.domain.room.model;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.DEFAULT_CAPACITY;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * 방 정보
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "room", timeToLive = 7200) // 2시간 TTL
public class Room {

    @Id
    private Long roomId;

    private String roomTitle;

    @Builder.Default
    private Integer playerCount = 0;

    @Builder.Default
    private Integer capacity = DEFAULT_CAPACITY;

    @Indexed
    @Builder.Default
    private RoomStatus status = RoomStatus.WAITING;

    private String hostSessionId;

    /**
     * 방 상태 enum
     */
    public enum RoomStatus {
        WAITING,  // 대기 중
        INGAME    // 게임 중
    }
}
