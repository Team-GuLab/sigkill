package com.gulab.sigkillserver.common.exception.handler;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
        log.error("CustomException 발생: {}", e.getErrorCode().getMessage(), e);
        return createResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Validation 실패: {}", e.getMessage());
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("잘못된 요청 파라미터입니다.");

        return ResponseEntity
                .status(400)
                .body(BaseResponse.onFailure(
                        "COMMON400",
                        message,
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<String>> handleException(Exception e) {
        log.error("예상치 못한 예외가 발생했습니다.", e);
        return ResponseEntity
                .status(500)
                .body(BaseResponse.onFailure(
                        "COMMON500",
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
