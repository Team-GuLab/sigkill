package com.gulab.sigkillserver.domain.room.controller;

import com.gulab.sigkillserver.domain.room.dto.stomp.command.RoomJoinCommand;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
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

    /**
     * 방 참가 클라이언트: SEND /app/room/join 브로드캐스트: /topic/room/{roomId}
     */
    @MessageMapping("/room/join")
    public void joinRoom(@Valid @Payload RoomJoinCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        log.info("방 참가 요청 - roomId: {}, userId: {}", request.roomId(), userId);

        PlayerJoinEvent playerJoinEvent = roomService.joinRoom(request.roomId(), userId);

        // 방에 있는 모든 사람에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + request.roomId(), playerJoinEvent);

        log.info("방 참가 브로드캐스트 완료 - roomId: {}, players: {}",
                request.roomId(), playerJoinEvent.players().size());
    }
}
