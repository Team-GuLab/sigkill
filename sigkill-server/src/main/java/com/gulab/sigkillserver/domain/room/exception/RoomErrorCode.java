package com.gulab.sigkillserver.domain.room.exception;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_PLAYERS_TO_START;

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
    HOST_CANNOT_READY("방장은 준비 상태를 변경할 수 없습니다", HttpStatus.BAD_REQUEST),
    ROOM_CREATE_ERROR("방 생성에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    ROOM_NUMBER_ERROR("방 번호는 4자리 정수여야 합니다", HttpStatus.BAD_REQUEST),
    ROOM_PAGING_PARAMETER_INVALID("페이지 번호와 페이지 크기는 0 이상의 정수여야 합니다", HttpStatus.BAD_REQUEST),
    ROOM_ID_ALREADY_EXISTS("이미 존재하는 방 ID입니다", HttpStatus.CONFLICT),
    PLAYER_ID_ALREADY_EXISTS("이미 존재하는 플레이어 ID입니다", HttpStatus.CONFLICT),
    ONLY_HOST_CAN_START_GAME("게임 시작은 방장만 할 수 있습니다", HttpStatus.FORBIDDEN),
    NOT_ENOUGH_PLAYERS_TO_START(String.format("게임 시작 최소 인원은 %d명입니다.", MIN_PLAYERS_TO_START), HttpStatus.CONFLICT),
    PLAYERS_NOT_READY("모든 플레이어가 준비 상태여야 합니다.", HttpStatus.CONFLICT),
    USER_ALREADY_IN_ROOM("사용자가 이미 방에 참여 중입니다", HttpStatus.CONFLICT),
    ROOM_JOIN_RESERVATION_NOT_FOUND("방 입장 예약 정보를 찾을 수 없습니다", HttpStatus.CONFLICT),
    ROOM_JOIN_RESERVATION_INVALID("방 입장 예약 정보가 유효하지 않습니다", HttpStatus.CONFLICT),
    ROOM_ALREADY_STARTED("이미 게임이 시작된 방입니다", HttpStatus.CONFLICT),
    ROOM_NOT_STARTED("게임이 시작되지 않은 방입니다", HttpStatus.CONFLICT);

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
