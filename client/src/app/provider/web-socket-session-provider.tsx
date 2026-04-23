import { createContext, useCallback, useContext, useRef } from "react";
import {
  connectWebSocket,
  disconnectWebSocket,
  getClient,
} from "@/app/config/web-socket-client";
import { useResetRoom } from "@/store/room-store";

interface WebSocketSessionContextValue {
  /** 세션 연결 수립 */
  connectSession: (roomId: string) => Promise<void>;
  /** 세션 연결 해제 */
  disconnectSession: () => void;
}

const WebSocketSessionContext =
  createContext<WebSocketSessionContextValue | null>(null);

export function WebSocketSessionProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const currentRoomIdRef = useRef<string | null>(null);
  const resetRoom = useResetRoom();

  const connectSession = useCallback(async (roomId: string) => {
    currentRoomIdRef.current = roomId;
    const client = getClient();
    if (!client.active) {
      await connectWebSocket();
    }
  }, []);

  const disconnectSession = useCallback(async () => {
    currentRoomIdRef.current = null;
    await disconnectWebSocket();
    resetRoom();
  }, [resetRoom]);

  return (
    <WebSocketSessionContext
      value={{
        connectSession,
        disconnectSession,
      }}
    >
      {children}
    </WebSocketSessionContext>
  );
}

export const useWebSocketSession = () => {
  const ctx = useContext(WebSocketSessionContext);
  if (!ctx) {
    throw new Error(
      "useWebSocketSession은 WebSocketSessionProvider 내부에서만 사용할 수 있습니다.",
    );
  }
  return ctx;
};
