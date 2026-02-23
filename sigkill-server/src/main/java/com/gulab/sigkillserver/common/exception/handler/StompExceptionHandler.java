package com.gulab.sigkillserver.common.exception.handler;

import com.gulab.sigkillserver.common.exception.CustomException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * WebSocket STOMP 메시지 예외 핸들러
 */
@Slf4j
@ControllerAdvice
public class StompExceptionHandler {

    private static final String ERROR_TYPE = "ERROR";

    /**
     * CustomException 처리
     */
    @MessageExceptionHandler(CustomException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleCustomException(CustomException e) {
        log.warn("WebSocket CustomException 발생: code={}, message={}",
                e.getErrorCode().getCode(), e.getErrorCode().getMessage());
        return ErrorMessage.of(
                e.getErrorCode().getCode(),
                e.getErrorCode().getMessage()
        );
    }

    /**
     * AccessDeniedException 처리 (인증 실패)
     */
    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleAccessDeniedException(AccessDeniedException e) {
        log.warn("WebSocket 접근 거부: {}", e.getMessage());
        return ErrorMessage.of(
                "ACCESS_DENIED",
                "접근 권한이 없습니다."
        );
    }

    /**
     * IllegalArgumentException 처리 (잘못된 요청)
     */
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("WebSocket 잘못된 요청: {}", e.getMessage());
        return ErrorMessage.of(
                "INVALID_REQUEST",
                e.getMessage() != null ? e.getMessage() : "잘못된 요청입니다."
        );
    }

    /**
     * MethodArgumentNotValidException 처리 (Payload validation 실패)
     */
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("WebSocket Payload validation 실패: {}", e.getMessage());

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "요청 형식이 잘못됐습니다.")
                .orElse("요청 형식이 잘못됐습니다.");

        return ErrorMessage.of(
                "INVALID_REQUEST",
                message
        );
    }

    /**
     * ConstraintViolationException 처리 (메서드 파라미터 validation 실패)
     */
    @MessageExceptionHandler(ConstraintViolationException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("WebSocket ConstraintViolation 발생: {}", e.getMessage());

        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage() != null ? violation.getMessage() : "요청 형식이 잘못됐습니다.")
                .orElse("요청 형식이 잘못됐습니다.");

        return ErrorMessage.of(
                "INVALID_REQUEST",
                message
        );
    }

    /**
     * IllegalStateException 처리 (잘못된 상태)
     */
    @MessageExceptionHandler(IllegalStateException.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleIllegalStateException(IllegalStateException e) {
        log.warn("WebSocket 잘못된 상태: {}", e.getMessage());
        return ErrorMessage.of(
                "INVALID_STATE",
                e.getMessage() != null ? e.getMessage() : "잘못된 상태입니다."
        );
    }

    /**
     * 모든 예외 처리 (fallback)
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorMessage handleException(Exception e) {
        log.error("WebSocket 예상치 못한 예외 발생", e);
        return ErrorMessage.of(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다."
        );
    }

    /**
     * WebSocket 에러 메시지 DTO
     */
    public record ErrorMessage(
            String type,
            String code,
            String message
    ) {
        public static ErrorMessage of(String code, String message) {
            return new ErrorMessage(ERROR_TYPE, code, message);
        }
    }
}
