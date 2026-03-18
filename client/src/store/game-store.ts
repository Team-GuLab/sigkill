import { create } from "zustand";
import { combine, devtools, subscribeWithSelector } from "zustand/middleware";
import type {
  QuizDetail,
  GamePlayer,
  QuizAnswer,
  GameEnd,
} from "@/api/game/types";

// 게임 단계
export type GamePhase =
  | { status: "IDLE" }
  | { status: "LOADING" }
  | { status: "WAITING_QUIZ" }
  | {
      status: "QUIZ_ACTIVE";
      quiz: QuizDetail;
      choiceSubmits: Partial<Record<number, number>>;
    }
  | {
      status: "QUIZ_RESULT";
      quiz: QuizDetail;
      answer: QuizAnswer;
      playerResults: GamePlayer[];
    }
  | { status: "GAME_OVER"; gameEnd: GameEnd };

// 상태 전이 이벤트
export type GameEvent =
  | {
      type: "GAME_START";
      roomId: string;
      gameId: number;
      players: GamePlayer[];
    }
  | { type: "GAME_LOADED"; allLoaded: boolean }
  | { type: "QUIZ_START"; quiz: QuizDetail }
  | { type: "CHOICE_SUBMIT"; userId: number; choiceNumber: number }
  | { type: "QUIZ_END"; answer: QuizAnswer; playerResults: GamePlayer[] }
  | { type: "PLAYER_STATUS_UPDATED"; players: GamePlayer[] }
  | { type: "GAME_END"; gameEnd: GameEnd };

// 순수 전이 함수
type State = typeof initialState;

function reduce(state: State, event: GameEvent): Partial<State> {
  switch (event.type) {
    case "GAME_START":
      return {
        roomId: event.roomId,
        gameId: event.gameId,
        players: event.players,
        phase: { status: "LOADING" },
      };

    case "GAME_LOADED": {
      if (state.phase.status !== "LOADING" || !event.allLoaded) return {};
      return { phase: { status: "WAITING_QUIZ" } };
    }

    case "QUIZ_START": {
      const { status } = state.phase;
      if (status !== "WAITING_QUIZ" && status !== "QUIZ_RESULT") return {};
      return {
        phase: { status: "QUIZ_ACTIVE", quiz: event.quiz, choiceSubmits: {} },
      };
    }

    case "CHOICE_SUBMIT": {
      if (state.phase.status !== "QUIZ_ACTIVE") return {};
      return {
        phase: {
          ...state.phase,
          choiceSubmits: {
            ...state.phase.choiceSubmits,
            [event.userId]: event.choiceNumber,
          },
        },
      };
    }

    case "QUIZ_END": {
      if (state.phase.status !== "QUIZ_ACTIVE") return {};
      return {
        phase: {
          status: "QUIZ_RESULT",
          quiz: state.phase.quiz,
          answer: event.answer,
          playerResults: event.playerResults,
        },
      };
    }

    case "PLAYER_STATUS_UPDATED":
      return { players: event.players };

    case "GAME_END":
      return { phase: { status: "GAME_OVER", gameEnd: event.gameEnd } };

    default:
      return {};
  }
}

// 셀렉터 폴백용 안정 참조 (인라인 {}·[] 반환 시 매 렌더마다 새 참조 → 무한 루프 방지)
const EMPTY_CHOICE_SUBMITS: Partial<Record<number, number>> = {};
const EMPTY_PLAYER_RESULTS: GamePlayer[] = [];

// 스토어
const initialState = {
  roomId: null as string | null,
  gameId: null as number | null,
  players: [] as GamePlayer[],
  phase: { status: "IDLE" } as GamePhase,
};

export const useGameStore = create(
  devtools(
    subscribeWithSelector(
      combine(initialState, set => ({
        transition: (event: GameEvent) => set(state => reduce(state, event)),
        reset: () => set(initialState),
      })),
    ),
    { name: "GameStore" },
  ),
);

// 셀렉터
export const useGamePhase = () => useGameStore(state => state.phase);

export const useGameRoomId = () => useGameStore(state => state.roomId);
export const useGameId = () => useGameStore(state => state.gameId);
export const useGamePlayers = () => useGameStore(state => state.players);

export const useIsInGame = () =>
  useGameStore(state => state.phase.status !== "IDLE");

export const useGameAllLoaded = () =>
  useGameStore(
    state => state.phase.status !== "IDLE" && state.phase.status !== "LOADING",
  );

export const useGameQuiz = () =>
  useGameStore(state => {
    const { phase } = state;
    return phase.status === "QUIZ_ACTIVE" || phase.status === "QUIZ_RESULT"
      ? phase.quiz
      : null;
  });

export const useGameChoiceSubmits = () =>
  useGameStore(state =>
    state.phase.status === "QUIZ_ACTIVE"
      ? state.phase.choiceSubmits
      : EMPTY_CHOICE_SUBMITS,
  );

export const useGameQuizEndAnswer = () =>
  useGameStore(state =>
    state.phase.status === "QUIZ_RESULT" ? state.phase.answer : null,
  );

export const useGameQuizEndPlayerResults = () =>
  useGameStore(state =>
    state.phase.status === "QUIZ_RESULT"
      ? state.phase.playerResults
      : EMPTY_PLAYER_RESULTS,
  );

export const useGameEnd = () =>
  useGameStore(state =>
    state.phase.status === "GAME_OVER" ? state.phase.gameEnd : null,
  );

export const useResetGame = () => useGameStore(state => state.reset);
export const useGameTransition = () => useGameStore(state => state.transition);
