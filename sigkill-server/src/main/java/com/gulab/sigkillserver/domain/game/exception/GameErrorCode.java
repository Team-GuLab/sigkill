package com.gulab.sigkillserver.domain.game.exception;

import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameErrorCode implements CustomErrorCodeInterface {
    GAME_ALREADY_EXISTS("해당 방에 이미 진행 중인 게임이 존재합니다", HttpStatus.CONFLICT),
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
