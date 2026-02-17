package com.gulab.sigkillserver.common.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.exception.RoomErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

class StompExceptionHandlerTest {

    private final StompExceptionHandler stompExceptionHandler = new StompExceptionHandler();

    @Test
    void custom_exception_응답에_공통_type을_포함한다() {
        // given
        CustomException exception = new CustomException(RoomErrorCode.ROOM_NOT_FOUND);

        // when
        StompExceptionHandler.ErrorMessage response = stompExceptionHandler.handleCustomException(exception);

        // then
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.code()).isEqualTo("ROOM_NOT_FOUND");
    }

    @Test
    void access_denied_exception_응답에_공통_type을_포함한다() {
        // given
        AccessDeniedException exception = new AccessDeniedException("forbidden");

        // when
        StompExceptionHandler.ErrorMessage response = stompExceptionHandler.handleAccessDeniedException(exception);

        // then
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.code()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void illegal_argument_exception_응답에_공통_type을_포함한다() {
        // given
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 요청입니다.");

        // when
        StompExceptionHandler.ErrorMessage response = stompExceptionHandler.handleIllegalArgumentException(exception);

        // then
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.code()).isEqualTo("INVALID_REQUEST");
    }

    @Test
    void method_argument_not_valid_exception_응답에_공통_type을_포함한다() {
        // given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("roomIdCommand", "roomId", "roomId는 필수입니다.");
        when(exception.getMessage()).thenReturn("validation failed");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        StompExceptionHandler.ErrorMessage response =
                stompExceptionHandler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.message()).isEqualTo("roomId는 필수입니다.");
    }

    @Test
    void constraint_violation_exception_응답에_공통_type을_포함한다() {
        // given
        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(exception.getMessage()).thenReturn("constraint failed");
        when(exception.getConstraintViolations()).thenReturn(Set.of(violation));
        when(violation.getMessage()).thenReturn("roomId는 필수입니다.");

        // when
        StompExceptionHandler.ErrorMessage response = stompExceptionHandler.handleConstraintViolationException(exception);

        // then
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.message()).isEqualTo("roomId는 필수입니다.");
    }

    @Test
    void illegal_state_exception_응답에_공통_type을_포함한다() {
        // given
        IllegalStateException exception = new IllegalStateException("잘못된 상태입니다.");

        // when
        StompExceptionHandler.ErrorMessage response = stompExceptionHandler.handleIllegalStateException(exception);

        // then
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.code()).isEqualTo("INVALID_STATE");
    }

    @Test
    void fallback_exception_응답에_공통_type을_포함한다() {
        // given
        Exception exception = new Exception("unknown");

        // when
        StompExceptionHandler.ErrorMessage response = stompExceptionHandler.handleException(exception);

        // then
        assertThat(response.type()).isEqualTo("ERROR");
        assertThat(response.code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
