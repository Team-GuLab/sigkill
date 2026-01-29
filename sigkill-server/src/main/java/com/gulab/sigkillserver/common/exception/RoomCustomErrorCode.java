package com.gulab.sigkillserver.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Room 도메인 에러코드
 */
@Getter
@AllArgsConstructor
public enum RoomCustomErrorCode implements CustomErrorCodeInterface {
    ROOM_NOT_FOUND("ROOM404", "방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ROOM_FULL("ROOM409", "방이 가득차 참가 불가능합니다.", HttpStatus.CONFLICT),
    ROOM_ALREADY_STARTED("ROOM409_2", "이미 게임이 시작된 방입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public CustomErrorCode getErrorCode() {
        return CustomErrorCode.builder()
                .code(code)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}
