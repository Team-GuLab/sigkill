import { useGameStore } from "@/store/game-store";
import type { GameWebSocketMessage } from "./types";

/**
 * 게임 관련 stomp 메시지 핸들러
 * @param message - 게임 stomp 메시지
 */
export const handleGameMessage = (message: GameWebSocketMessage) => {
  switch (message.type) {
    case "GAME_LOADED": {
      const { setAllLoaded } = useGameStore.getState();
      setAllLoaded(message.payload.allLoaded);
      break;
    }

    case "QUIZ_START": {
      const { setQuiz } = useGameStore.getState();
      setQuiz(message.payload.quiz);
      break;
    }

    case "CHOICE_SUBMIT":
      break;

    case "QUIZ_END": {
      const { setPlayers } = useGameStore.getState();
      setPlayers(message.payload.players);
      break;
    }

    case "GAME_END":
      break;

    default:
      // @ts-ignore
      console.warn("Unknown game message type:", message);
  }
};
