package com.gulab.sigkillserver.domain.room.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomEnvelopeResponse;
import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;
import com.gulab.sigkillserver.domain.room.model.RoomStatus;
import com.gulab.sigkillserver.domain.room.service.PendingRoomJoinOrchestrator;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomControllerTest {

    private static final Long USER_ID = 1L;
    private static final String ROOM_ID = "1234";

    private RoomService roomService;
    private PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator;
    private RoomController roomController;

    @BeforeEach
    void setup() {
        roomService = mock(RoomService.class);
        pendingRoomJoinOrchestrator = mock(PendingRoomJoinOrchestrator.class);
        roomController = new RoomController(roomService, pendingRoomJoinOrchestrator);
    }

    @Test
    void fresh_join이면_pending_timeout을_스케줄한다() {
        // given
        RoomInfoResponse roomInfoResponse = new RoomInfoResponse(ROOM_ID, "테스트 방", USER_ID, 6, RoomStatus.WAITING);
        when(roomService.joinRoom(ROOM_ID, USER_ID)).thenReturn(
                RoomService.JoinRoomResult.createdPending(roomInfoResponse)
        );

        // when
        BaseResponse<RoomEnvelopeResponse> response = roomController.join(USER_ID, ROOM_ID);

        // then
        assertThat(response.result().room()).isEqualTo(roomInfoResponse);
        verify(pendingRoomJoinOrchestrator).schedulePendingJoinTimeout(ROOM_ID, USER_ID);
    }

    @Test
    void replay_join이면_pending_timeout을_다시_스케줄하지_않는다() {
        // given
        RoomInfoResponse roomInfoResponse = new RoomInfoResponse(ROOM_ID, "테스트 방", USER_ID, 6, RoomStatus.WAITING);
        when(roomService.joinRoom(ROOM_ID, USER_ID)).thenReturn(
                RoomService.JoinRoomResult.replayPending(roomInfoResponse)
        );

        // when
        BaseResponse<RoomEnvelopeResponse> response = roomController.join(USER_ID, ROOM_ID);

        // then
        assertThat(response.result().room()).isEqualTo(roomInfoResponse);
        verifyNoInteractions(pendingRoomJoinOrchestrator);
    }
}
