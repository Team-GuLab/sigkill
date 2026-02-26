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
      const { setQuiz, resetChoiceSubmits } = useGameStore.getState();
      // 새 라운드 시작 시 이전 라운드의 제출 기록 초기화
      resetChoiceSubmits();
      setQuiz(message.payload.quiz);
      break;
    }

    case "CHOICE_SUBMIT": {
      const { setChoiceSubmit } = useGameStore.getState();
      setChoiceSubmit(message.payload.actor.userId, message.payload.choiceNumber);
      break;
    }

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
