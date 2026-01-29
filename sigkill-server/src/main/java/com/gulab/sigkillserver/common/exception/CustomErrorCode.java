package com.gulab.sigkillserver.common.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * 에러코드 데이터 클래스
 */
@Getter
@Builder
public class CustomErrorCode implements Serializable {
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
