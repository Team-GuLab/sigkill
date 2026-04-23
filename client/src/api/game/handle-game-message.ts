import { useGameStore } from "@/store/game-store";
import type { GameWebSocketMessage } from "./types";

/**
 * 게임 관련 stomp 메시지 핸들러
 * @param message - 게임 stomp 메시지
 */
export const handleGameMessage = (message: GameWebSocketMessage) => {
  const { transition } = useGameStore.getState();

  switch (message.type) {
    case "GAME_LOADED": {
      transition({ type: "GAME_LOADED", allLoaded: message.payload.allLoaded });
      break;
    }

    case "QUIZ_START": {
      transition({ type: "QUIZ_START", quiz: message.payload.quiz });
      break;
    }

    case "CHOICE_SUBMIT": {
      transition({
        type: "CHOICE_SUBMIT",
        userId: message.payload.actor.userId,
        choiceNumber: message.payload.choiceNumber,
      });
      break;
    }

    case "QUIZ_END": {
      const { answer, players } = message.payload;
      transition({ type: "QUIZ_END", answer, playerResults: players });
      // QUIZ_RESULT 단계에서 3초 후 플레이어 생존 상태 반영
      setTimeout(() => {
        useGameStore
          .getState()
          .transition({ type: "PLAYER_STATUS_UPDATED", players });
      }, 3000);
      break;
    }

    case "GAME_END": {
      setTimeout(() => {
        transition({ type: "GAME_END", gameEnd: message.payload });
      }, 3000);
      break;
    }

    default:
      // @ts-ignore
      console.warn("Unknown game message type:", message);
  }
};
