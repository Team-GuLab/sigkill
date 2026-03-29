package com.gulab.sigkillserver.domain.room.exception;

import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlayerErrorCode implements CustomErrorCodeInterface {
    PLAYER_NOT_IN_ANY_ROOM("유저가 어떤 방에도 참여 중이지 않습니다", HttpStatus.NOT_FOUND),
    PLAYER_NOT_IN_ROOM("유저가 해당 방에 참여 중이지 않습니다", HttpStatus.NOT_FOUND),
    PLAYER_NOT_ACTIVE("입장이 아직 확정되지 않았습니다", HttpStatus.CONFLICT),
    ;

    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public CustomErrorCode getErrorCode() {
        return CustomErrorCode.builder()
                .code(this.name())
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}
