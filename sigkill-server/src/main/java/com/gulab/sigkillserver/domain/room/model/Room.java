package com.gulab.sigkillserver.domain.room.model;

import com.gulab.sigkillserver.common.BaseEntity;
import lombok.Getter;

/**
 * 방 정보
 */
@Getter
public class Room extends BaseEntity {

    private final String roomId;
    private final String roomTitle;
    private final Integer capacity;
    private Long hostId;
    private RoomStatus status;
    private boolean closing;

    /**
     * private 생성자
     */
    private Room(String roomId, String roomTitle, Long hostId, Integer capacity, RoomStatus status, boolean closing) {
        this.roomId = roomId;
        this.roomTitle = roomTitle;
        this.hostId = hostId;
        this.capacity = capacity;
        this.status = status;
        this.closing = closing;
    }

    /**
     * 새 방 생성
     *
     * @param id       방 ID (4자리 랜덤 숫자)
     * @param title    방 제목
     * @param hostId   방장 User ID
     * @param capacity 최대 수용 인원
     * @return 생성된 Room 객체
     */
    public static Room create(String id, String title, Long hostId, Integer capacity) {
        return new Room(id, title, hostId, capacity, RoomStatus.WAITING, false);
    }

    public boolean isInGame() {
        return status == RoomStatus.INGAME;
    }

    /**
     * 게임 시작 (상태를 INGAME으로 변경)
     */
    public void startGame() {
        this.status = RoomStatus.INGAME;
        this.closing = false;
    }

    /**
     * 게임 종료 (상태를 WAITING으로 변경)
     */
    public void endGame() {
        this.status = RoomStatus.WAITING;
        this.closing = false;
    }

    /**
     * 호스트 변경
     */
    public void changeHost(Long newHostId) {
        this.hostId = newHostId;
    }

    public void markClosing() {
        this.closing = true;
    }

    public void clearClosing() {
        this.closing = false;
    }
}
