package com.gulab.sigkillserver.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 커스텀 예외 클래스
 */
@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {
    private final CustomErrorCodeInterface errorCode;

    public CustomErrorCode getErrorCode() {
        return this.errorCode.getErrorCode();
    }
}
