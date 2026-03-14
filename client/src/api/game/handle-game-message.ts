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
      const {
        setQuiz,
        resetChoiceSubmits,
        setQuizEndAnswer,
        setQuizEndPlayerResults,
      } = useGameStore.getState();
      // 새 라운드 시작 시 이전 라운드의 게임 정보 초기화
      resetChoiceSubmits();
      setQuizEndAnswer(null);
      setQuizEndPlayerResults([]);
      setQuiz(message.payload.quiz);
      break;
    }

    case "CHOICE_SUBMIT": {
      const { setChoiceSubmit } = useGameStore.getState();
      setChoiceSubmit(
        message.payload.actor.userId,
        message.payload.choiceNumber,
      );
      break;
    }

    case "QUIZ_END": {
      const { setQuizEndAnswer, setQuizEndPlayerResults } =
        useGameStore.getState();
      setQuizEndAnswer(message.payload.answer);
      setQuizEndPlayerResults(message.payload.players);
      // 플레이어 상태 반영
      const players = message.payload.players;
      setTimeout(() => {
        useGameStore.getState().setPlayers(players);
      }, 3000);
      break;
    }

    case "GAME_END": {
      const { setGameEnd } = useGameStore.getState();
      const { setIsInGame } = useGameStore.getState();
      setGameEnd(message.payload);
      setIsInGame(false);

      break;
    }

    default:
      // @ts-ignore
      console.warn("Unknown game message type:", message);
  }
};
