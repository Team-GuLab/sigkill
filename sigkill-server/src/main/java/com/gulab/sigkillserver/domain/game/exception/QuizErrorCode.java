package com.gulab.sigkillserver.domain.game.exception;

import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QuizErrorCode implements CustomErrorCodeInterface {
    QUIZ_NOT_FOUND("퀴즈를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    QUIZ_CATALOG_LOAD_FAILED("퀴즈 데이터 로드에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_CATALOG_EMPTY("퀴즈 데이터가 비어 있습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_DATA_NULL("퀴즈 데이터가 null 입니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_CATEGORY_INVALID("퀴즈 카테고리가 올바르지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_CHOICES_INVALID("퀴즈 선택지가 올바르지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_CHOICE_ID_DUPLICATED("퀴즈 선택지 ID가 중복되었습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_CORRECT_CHOICE_INVALID("정답 선택지가 존재하지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_ID_DUPLICATED("퀴즈 ID가 중복되었습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    QUIZ_INDEX_OUT_OF_BOUNDS("퀴즈 인덱스가 범위를 벗어났습니다", HttpStatus.INTERNAL_SERVER_ERROR);

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
