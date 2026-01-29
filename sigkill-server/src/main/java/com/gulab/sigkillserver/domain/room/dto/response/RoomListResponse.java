package com.gulab.sigkillserver.domain.room.dto.response;

import java.util.List;

/**
 * 방 목록 응답 DTO
 */
public record RoomListResponse(
        List<RoomResponse> rooms,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages,
        Boolean hasNext
) {
}
