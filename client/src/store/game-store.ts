import { create } from "zustand";
import { combine, devtools, subscribeWithSelector } from "zustand/middleware";
import type { QuizDetail, GamePlayer } from "@/api/game/types";

const initialState = {
  roomId: null as string | null,
  gameId: null as number | null,
  quiz: null as QuizDetail | null,
  players: [] as GamePlayer[],
  allLoaded: false,
  // 현재 라운드에서 각 플레이어가 제출한 선지 번호
  choiceSubmits: {} as Partial<Record<number, number>>,
};

export const useGameStore = create(
  devtools(
    subscribeWithSelector(
      combine(initialState, set => ({
        setGameInfo: (roomId: string, gameId: number) =>
          set({ roomId, gameId }),
        setQuiz: (quiz: QuizDetail) => set({ quiz }),
        setPlayers: (players: GamePlayer[]) => set({ players }),
        setAllLoaded: (allLoaded: boolean) => set({ allLoaded }),
        setChoiceSubmit: (userId: number, choiceNumber: number) =>
          set(state => ({
            choiceSubmits: { ...state.choiceSubmits, [userId]: choiceNumber },
          })),
        resetChoiceSubmits: () => set({ choiceSubmits: {} }),
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
export const useGameAllLoaded = () => useGameStore(state => state.allLoaded);
export const useGameChoiceSubmits = () =>
  useGameStore(state => state.choiceSubmits);
export const useSetGameInfo = () => useGameStore(state => state.setGameInfo);
export const useSetGameQuiz = () => useGameStore(state => state.setQuiz);
export const useSetGamePlayers = () => useGameStore(state => state.setPlayers);
export const useResetGame = () => useGameStore(state => state.reset);
