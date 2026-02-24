import { create } from "zustand";
import { combine, devtools, subscribeWithSelector } from "zustand/middleware";
import type { Player, RoomItem } from "@/api/room/types";

type RoomInfo = Omit<RoomItem, "playerCount" | "canJoin"> | null;

const initialState = {
  roomInfo: null as RoomInfo,
  players: [] as Player[],
};

export const useRoomStore = create(
  devtools(
    subscribeWithSelector(
      combine(initialState, set => ({
        setRoomInfo: (roomInfo: RoomInfo) => set({ roomInfo }),
        setPlayers: (players: Player[]) => set({ players }),
        reset: () => set(initialState),
      })),
    ),
    { name: "RoomStore" },
  ),
);

export const useRoomInfo = () => useRoomStore(state => state.roomInfo);
export const usePlayers = () => useRoomStore(state => state.players);
export const useSetRoomInfo = () => useRoomStore(state => state.setRoomInfo);
export const useSetPlayers = () => useRoomStore(state => state.setPlayers);
export const useResetRoom = () => useRoomStore(state => state.reset);
