import { useSetGameQuiz, useSetGamePlayers } from "@/store/game-store";
import type { GameWebSocketMessage } from "./types";

/**
 * 게임 관련 stomp 메시지 핸들러
 * @param message - 게임 stomp 메시지
 */
export const handleGameMessage = (message: GameWebSocketMessage) => {
  const setQuiz = useSetGameQuiz();
  const setPlayers = useSetGamePlayers();

  switch (message.type) {
    case "QUIZ_START":
      setQuiz(message.payload.quiz);
      break;

    case "CHOICE_SUBMIT":
      break;

    case "QUIZ_END":
      setPlayers(message.payload.players);
      break;

    case "GAME_END":
      break;

    default:
      // @ts-ignore
      console.warn("Unknown game message type:", message);
  }
};
