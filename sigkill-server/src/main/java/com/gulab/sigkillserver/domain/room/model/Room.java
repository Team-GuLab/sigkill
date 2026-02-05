package com.gulab.sigkillserver.domain.room.model;

import com.gulab.sigkillserver.common.BaseEntity;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 * 방 정보
 */
@Getter
public class Room extends BaseEntity {

    private final String roomId;
    private final String roomTitle;
    private final String hostId;
    private final Set<String> playerIds;
    private final Integer capacity;
    private RoomStatus status;

    /**
     * private 생성자
     */
    private Room(String roomId, String roomTitle, String hostId, Integer capacity, RoomStatus status) {
        super(ZonedDateTime.now(), ZonedDateTime.now());
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.hostId = hostId;
        this.capacity = capacity;
        this.status = status;
        this.playerIds = ConcurrentHashMap.newKeySet();
    }

    public int getCurrentCapacity() {
        return playerIds.size();
    }

    public void addPlayer(String playerId) {
        playerIds.add(playerId);
    }

    public void removePlayer(String playerId) {
        playerIds.remove(playerId);
    }

    public boolean isFull() {
        return playerIds.size() >= capacity;
    }

    public boolean isInGame() {
        return status == RoomStatus.INGAME;
    }

    public boolean canJoin() {
        return !isFull() && !isInGame();
    }

    /**
     * 새 방 생성
     *
     * @param id 방 ID (4자리 랜덤 숫자)
     * @param title 방 제목
     * @param hostId 방장 세션 ID
     * @param capacity 최대 수용 인원
     * @return 생성된 Room 객체
     */
    public static Room create(String id, String title, String hostId, Integer capacity) {
        Room room = new Room(id, title, hostId, capacity, RoomStatus.WAITING);
        room.addPlayer(hostId);
        return room;
    }

    /**
     * 플레이어 입장
     *
     * @param playerId 입장할 플레이어 ID
     * @return this
     */
    public Room join(String playerId) {
        if (canJoin()) {
            this.addPlayer(playerId);
        }
        return this;
    }

    /**
     * 게임 시작 (상태를 INGAME으로 변경)
     */
    public void startGame() {
        this.status = RoomStatus.INGAME;
    }

    /**
     * 게임 종료 (상태를 WAITING으로 변경)
     */
    public void endGame() {
        this.status = RoomStatus.WAITING;
    }
}
