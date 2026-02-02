package com.gulab.sigkillserver.common;

import java.time.ZonedDateTime;

/**
 * API 응답 기본 형식
 */
public record BaseResponse<T>(ZonedDateTime timeStamp, String code, String message, T result) {
    public static <T> BaseResponse<T> onSuccess(T result) {
        return new BaseResponse<>(
                ZonedDateTime.now(),
                "SUCCESS",
                "요청이 성공적으로 처리되었습니다.",
                result
        );
    }

    public static <T> BaseResponse<T> onFailure(String code, String message, T result) {
        return new BaseResponse<>(
                ZonedDateTime.now(),
                code,
                message,
                result
        );
    }
}
