package com.gulab.sigkillserver.domain.room.model;

import lombok.AccessLevel;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "room", timeToLive = 7200) // 2시간 TTL
public class Room {

    @Id
    private Long roomId;

    private String roomTitle;

    private Integer playerCount;

    private Integer capacity;

    @Indexed
    private RoomStatus status;

    private Long hostSessionId;  // 호스트의 세션 ID

    @Builder
    public Room(Long roomId, String roomTitle, Integer playerCount, Integer capacity,
                RoomStatus status, Long hostSessionId) {
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.playerCount = playerCount != null ? playerCount : 0;
        this.capacity = capacity != null ? capacity : 10;
        this.status = status != null ? status : RoomStatus.WAITING;
        this.hostSessionId = hostSessionId;
    }

    /**
     * 플레이어 추가
     */
    public void addPlayer() {
        if (this.playerCount >= this.capacity) {
            throw new IllegalStateException("방이 가득 찼습니다.");
        }
        this.playerCount++;
    }

    /**
     * 플레이어 제거
     */
    public void removePlayer() {
        if (this.playerCount > 0) {
            this.playerCount--;
        }
    }

    /**
     * 게임 시작
     */
    public void startGame() {
        if (this.status == RoomStatus.INGAME) {
            throw new IllegalStateException("이미 게임이 진행 중입니다.");
        }
        this.status = RoomStatus.INGAME;
    }

    /**
     * 게임 종료
     */
    public void endGame() {
        this.status = RoomStatus.WAITING;
    }

    /**
     * 호스트 변경
     */
    public void changeHost(Long newHostSessionId) {
        this.hostSessionId = newHostSessionId;
    }

    /**
     * 입장 가능 여부 확인
     */
    public boolean canJoin() {
        return this.status == RoomStatus.WAITING && this.playerCount < this.capacity;
    }

    /**
     * 방 상태 enum
     */
    public enum RoomStatus {
        WAITING,  // 대기 중
        INGAME    // 게임 중
    }
}
