package com.gulab.sigkillserver.domain.game.controller;

import com.gulab.sigkillserver.domain.game.dto.stomp.command.ChoiceSubmitCommand;
import com.gulab.sigkillserver.domain.game.dto.stomp.command.GameIdCommand;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.ChoiceSubmitEvent;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameLoadEvent;
import com.gulab.sigkillserver.domain.game.service.GameFlowOrchestrator;
import com.gulab.sigkillserver.domain.game.service.GameService;
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
public class GameWebSocketController {

    private final GameService gameService;
    private final GameFlowOrchestrator gameFlowOrchestrator;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/load")
    public void loadGame(@Payload @Valid GameIdCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        GameLoadEvent gameLoadEvent = gameService.loadGame(userId, request.gameId());
        messagingTemplate.convertAndSend("/topic/game/" + request.gameId(), gameLoadEvent);

        if (gameLoadEvent.payload().allLoaded()) {
            gameFlowOrchestrator.onAllPlayersLoaded(gameLoadEvent.roomId(), gameLoadEvent.gameId());
        }

        log.debug("game.load success - gameId={}, userId={}", request.gameId(), userId);
    }

    @MessageMapping("/game/submit")
    public void choiceSubmit(@Payload @Valid ChoiceSubmitCommand request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        ChoiceSubmitEvent choiceSubmitEvent = gameService.submitChoice(
                userId,
                request.gameId(),
                request.quizId(),
                request.choiceNumber()
        );
        messagingTemplate.convertAndSend("/topic/game/" + request.gameId(), choiceSubmitEvent);

        log.debug("game.submit success - gameId={}, quizId={}, userId={}",
                request.gameId(), request.quizId(), userId);
    }
}
