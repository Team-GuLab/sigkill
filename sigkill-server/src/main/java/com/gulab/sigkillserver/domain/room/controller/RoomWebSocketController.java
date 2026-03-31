package com.gulab.sigkillserver.domain.room.controller;

import com.gulab.sigkillserver.domain.bot.service.BotOrchestrator;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.dto.stomp.command.RoomIdCommand;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerUnreadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.RoomSnapshotEvent;
import com.gulab.sigkillserver.domain.room.service.PendingRoomJoinOrchestrator;
import com.gulab.sigkillserver.domain.room.service.RoomExitCoordinator;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketController {

    private final RoomService roomService;
    private final PendingRoomJoinOrchestrator pendingRoomJoinOrchestrator;
    private final RoomExitCoordinator roomExitCoordinator;
    private final BotOrchestrator botOrchestrator;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @MessageMapping("/room/snapshot")
    public void roomSnapshot(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        RoomSnapshotEvent roomSnapshotEvent = roomService.snapshot(request.roomId(), userId);

        messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), roomSnapshotEvent);

        log.debug("방 스냅샷 브로드캐스트 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }

    @MessageMapping("/room/join")
    public void joinRoom(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        Optional<PlayerJoinEvent> playerJoinEvent = roomService.confirmJoin(request.roomId(), userId);
        pendingRoomJoinOrchestrator.cancelPendingJoinTimeout(userId);

        playerJoinEvent.ifPresent(event -> messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), event));

        log.debug("방 참가 확정 처리 완료 - roomId: {}, userId: {}, broadcasted={}",
                request.roomId(), userId, playerJoinEvent.isPresent());
    }

    @MessageMapping("/room/leave")
    public void leaveRoom(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        roomExitCoordinator.leaveRoom(request.roomId(), userId);
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
        applicationEventPublisher.publishEvent(gameStartEvent);

        log.debug("게임 시작 브로드캐스트 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }

    @MessageMapping("/room/bot")
    public void addBot(@Valid @Payload RoomIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        botOrchestrator.addBot(request.roomId(), userId);

        log.debug("봇 추가 처리 완료 - roomId: {}, userId: {}", request.roomId(), userId);
    }
}
