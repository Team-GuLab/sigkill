package com.gulab.sigkillserver.domain.room.exception;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;

import com.gulab.sigkillserver.common.exception.CustomErrorCode;
import com.gulab.sigkillserver.common.exception.CustomErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RoomErrorCode implements CustomErrorCodeInterface {
    ROOM_CAPACITY_INVALID(String.format("방 인원 수는 %d명 이상 %d명 이하여야 합니다", MIN_CAPACITY, MAX_CAPACITY),
            HttpStatus.BAD_REQUEST),
    ROOM_TITLE_INVALID(String.format("방 제목의 길이는 최대 %d자 입니다", MAX_TITLE_LENGTH), HttpStatus.BAD_REQUEST),
    ROOM_NOT_FOUND("방을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    ROOM_FULL("방이 가득 찼습니다", HttpStatus.CONFLICT),
    ROOM_IN_GAME("이미 게임이 진행 중인 방입니다", HttpStatus.CONFLICT),
    ROOM_CREATE_ERROR("방 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    ROOM_NUMBER_ERROR("방 번호는 4자리 정수여야 합니다", HttpStatus.BAD_REQUEST),
    ROOM_PAGING_PARAMETER_INVALID("페이지 번호와 페이지 크기는 0 이상의 정수여야 합니다", HttpStatus.BAD_REQUEST),
    ROOM_ID_ALREADY_EXISTS("이미 존재하는 방 ID입니다", HttpStatus.CONFLICT),
    PLAYER_ALREADY_HOST("이미 호스트인 플레이어는 호스트로 변경할 수 없습니다", HttpStatus.BAD_REQUEST),
    PLAYER_ID_ALREADY_EXISTS("이미 존재하는 플레이어 ID입니다", HttpStatus.CONFLICT),
    PLAYER_NOT_FOUND("플레이어를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    USER_ALREADY_IN_ROOM("사용자가 이미 방에 참여 중입니다", HttpStatus.CONFLICT);

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
