package com.gulab.sigkillserver.domain.room.exception;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;

import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RoomErrorCode implements CustomErrorCodeInterface {
    ROOM_CAPACITY_INVALID("ROOM000", String.format("방 인원 수는 %d명 이상 %d명 이하여야 합니다.", MIN_CAPACITY, MAX_CAPACITY), HttpStatus.BAD_REQUEST),
    ROOM_TITLE_INVALID("ROOM001", String.format("방 제목의 길이는 최대 %d자 입니다.", MAX_TITLE_LENGTH), HttpStatus.BAD_REQUEST),
    ROOM_NOT_FOUND("ROOM002", "방을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ROOM_FULL("ROOM003", "방이 가득 찼습니다", HttpStatus.CONFLICT),
    ROOM_IN_GAME("ROOM004", "이미 게임이 진행 중인 방입니다", HttpStatus.CONFLICT),
    ROOM_CREATE_ERROR("ROOM005", "방 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

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
