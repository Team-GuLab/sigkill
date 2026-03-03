package com.gulab.sigkillserver.domain.room.controller;

import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.dto.stomp.command.RoomIdCommand;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerUnreadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.RoomSnapshotEvent;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketController {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/snapshot")
    public void roomSnapshot(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        RoomSnapshotEvent roomSnapshotEvent = roomService.snapshot(request.roomId(), userId);

        messagingTemplate.convertAndSend("/topic/room", roomSnapshotEvent);
        
        log.debug("방 스냅샷 브로드캐스트 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }

    @MessageMapping("/room/join")
    public void joinRoom(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        PlayerJoinEvent playerJoinEvent = roomService.joinEvent(request.roomId(), userId);

        messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), playerJoinEvent);

        log.debug("방 참가 알림 브로드캐스트 완료 - roomId: {}", request.roomId());
    }

    @MessageMapping("/room/leave")
    public void leaveRoom(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        LeaveRoomResult leaveRoomResult = roomService.leaveRoom(request.roomId(), userId);

        messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), leaveRoomResult.playerLeftEvent());
        if (leaveRoomResult.hasHostChangedEvent()) {
            messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), leaveRoomResult.hostChangedEvent());
        }
        log.debug("방 퇴장 브로드캐스트 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }

    @MessageMapping("/room/ready")
    public void playerReady(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        PlayerReadyEvent playerReadyEvent = roomService.readyPlayer(request.roomId(), userId);

        messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), playerReadyEvent);

        log.debug("준비 상태 변경 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }

    @MessageMapping("/room/unready")
    public void playerUnready(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        PlayerUnreadyEvent playerUnreadyEvent = roomService.unreadyPlayer(request.roomId(), userId);

        messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), playerUnreadyEvent);

        log.debug("준비 취소 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }

    @MessageMapping("/room/start")
    public void startGame(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        GameStartEvent gameStartEvent = roomService.startGame(request.roomId(), userId);

        messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), gameStartEvent);

        log.debug("게임 시작 브로드캐스트 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }
}
