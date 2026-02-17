import {
  createContext,
  useContext,
  useReducer,
  type Dispatch,
  type ReactNode,
} from "react";
import type { User } from "@/api/user/types";

interface UserState {
  user: User | null;
  isAuthenticated: boolean;
}

type UserAction =
  | { type: "LOGIN"; payload: User }
  | { type: "LOGOUT" }
  | { type: "UPDATE_NICKNAME"; payload: string };

const initialState: UserState = {
  user: null,
  isAuthenticated: false,
};

function userReducer(state: UserState, action: UserAction): UserState {
  switch (action.type) {
    case "LOGIN":
      return {
        ...state,
        user: action.payload,
        isAuthenticated: true,
      };
    case "LOGOUT":
      return {
        ...state,
        user: null,
        isAuthenticated: false,
      };
    default:
      return state;
  }
}

const UserContext = createContext<{
  state: UserState;
  dispatch: Dispatch<UserAction>;
} | null>(null);

export function UserProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(userReducer, initialState);

  return <UserContext value={{ state, dispatch }}>{children}</UserContext>;
}

export function useUser() {
  const context = useContext(UserContext);
  if (!context) {
    throw new Error("useUser must be used within a UserProvider");
  }
  return context;
}
