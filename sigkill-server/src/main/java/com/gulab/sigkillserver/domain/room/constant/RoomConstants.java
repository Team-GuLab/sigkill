package com.gulab.sigkillserver.domain.room.constant;

/**
 * 방 관련 상수
 */
public class RoomConstants {

    /**
     * 기본 방 정원
     */
    public static final int DEFAULT_CAPACITY = 6;

    /**
     * 최소 방 정원
     */
    public static final int MIN_CAPACITY = 2;

    /**
     * 최대 방 정원
     */
    public static final int MAX_CAPACITY = 10;

    /**
     * 방 제목 최대 길이
     */
    public static final int MAX_TITLE_LENGTH = 20;

    /**
     * 게임 시작 최소 인원
     */
    public static final int MIN_PLAYERS_TO_START = 2;

    private RoomConstants() {
    }
}
