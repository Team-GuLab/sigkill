package com.gulab.sigkillserver.common.exception.handler;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<String>> handleCustomException(CustomException e) {
        log.warn("CustomException 발생: code={}, message={}", e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        return createResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Validation 실패: {}", e.getMessage());

        return ResponseEntity
                .status(400)
                .body(BaseResponse.onFailure(
                        "INVALID_REQUEST",
                        "요청 형식이 잘못됐습니다.",
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation 실패: {}", e.getMessage());

        return ResponseEntity
                .status(400)
                .body(BaseResponse.onFailure(
                        "INVALID_REQUEST",
                        "요청 형식이 잘못됐습니다.",
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<String>> handleException(Exception e) {
        log.error("예상치 못한 예외가 발생했습니다.", e);
        return ResponseEntity
                .status(500)
                .body(BaseResponse.onFailure(
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다.",
                        null
                ));
    }

    private ResponseEntity<BaseResponse<String>> createResponseEntity(CustomErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus().value())
                .body(BaseResponse.onFailure(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        null
                ));
    }
}
