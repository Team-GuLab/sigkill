import { create } from "zustand";
import {
  combine,
  devtools,
  persist,
  subscribeWithSelector,
} from "zustand/middleware";
import type { User } from "@/api/user/types";

const initialState = {
  user: null as User | null,
  isAuthenticated: false,
};

export const useUserStore = create(
  devtools(
    persist(
      subscribeWithSelector(
        combine(initialState, set => ({
          login: (user: User) =>
            set({
              user,
              isAuthenticated: true,
            }),
          logout: () =>
            set({
              user: null,
              isAuthenticated: false,
            }),
          updateNickname: (nickname: string) =>
            set(state => ({
              user: state.user ? { ...state.user, nickname } : null,
            })),
        })),
      ),
      {
        name: "user-storage",
      },
    ),
    {
      name: "UserStore",
    },
  ),
);

export const useUser = () => useUserStore(state => state.user);
export const useIsAuthenticated = () =>
  useUserStore(state => state.isAuthenticated);
export const useLogin = () => useUserStore(state => state.login);
export const useLogout = () => useUserStore(state => state.logout);
export const useUpdateNickname = () =>
  useUserStore(state => state.updateNickname);
