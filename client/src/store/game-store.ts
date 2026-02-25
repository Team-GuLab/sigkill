import { create } from "zustand";
import { combine, devtools, subscribeWithSelector } from "zustand/middleware";
import type { QuizDetail, GamePlayer } from "@/api/game/types";

const initialState = {
  roomId: null as string | null,
  gameId: null as number | null,
  quiz: null as QuizDetail | null,
  players: [] as GamePlayer[],
};

export const useGameStore = create(
  devtools(
    subscribeWithSelector(
      combine(initialState, set => ({
        setGameInfo: (roomId: string, gameId: number) =>
          set({ roomId, gameId }),
        setQuiz: (quiz: QuizDetail) => set({ quiz }),
        setPlayers: (players: GamePlayer[]) => set({ players }),
        reset: () => set(initialState),
      })),
    ),
    {
      name: "GameStore",
    },
  ),
);

export const useGameRoomId = () => useGameStore(state => state.roomId);
export const useGameId = () => useGameStore(state => state.gameId);
export const useGameQuiz = () => useGameStore(state => state.quiz);
export const useGamePlayers = () => useGameStore(state => state.players);
export const useSetGameInfo = () => useGameStore(state => state.setGameInfo);
export const useSetGameQuiz = () => useGameStore(state => state.setQuiz);
export const useSetGamePlayers = () => useGameStore(state => state.setPlayers);
export const useResetGame = () => useGameStore(state => state.reset);
