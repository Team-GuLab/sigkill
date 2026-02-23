package com.gulab.sigkillserver.domain.game.exception;

import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GameErrorCode implements CustomErrorCodeInterface {
    GAME_NOT_FOUND("게임을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    GAME_NOT_IN_PROGRESS("게임이 진행 중이 아닙니다", HttpStatus.BAD_REQUEST),
    GAME_IN_PROGRESS("게임이 이미 진행 중입니다", HttpStatus.CONFLICT),
    INVALID_GAME_STATE("게임 상태가 유효하지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR),
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
