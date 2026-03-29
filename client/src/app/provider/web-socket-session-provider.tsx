import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
} from "react";
import {
  connectWebSocket,
  disconnectWebSocket,
  getClient,
  publishMessage,
} from "@/app/config/web-socket-client";
import { useResetRoom } from "@/store/room-store";

interface WebSocketSessionContextValue {
  /** 세션 연결: WaitingRoom 진입 시 호출 */
  connectSession: (roomId: string) => Promise<void>;
  /** disconnect 예약: 방 목록 등으로 이동할 때 WaitingRoom 언마운트 시 호출 */
  scheduleDisconnect: () => void;
  /** 스냅샷 재요청: 재연결 후 서버 상태 동기화 */
  requestSnapshot: () => void;
}

const WebSocketSessionContext =
  createContext<WebSocketSessionContextValue | null>(null);

export function WebSocketSessionProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const currentRoomIdRef = useRef<string | null>(null);
  const disconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const resetRoom = useResetRoom();

  const requestSnapshot = useCallback(() => {
    const roomId = currentRoomIdRef.current;
    if (roomId) {
      publishMessage("/app/room/snapshot", { roomId });
    }
  }, []);

  const connectSession = useCallback(async (roomId: string) => {
    currentRoomIdRef.current = roomId;
    const client = getClient();
    if (!client.active) {
      await connectWebSocket();
    }
  }, []);

  const scheduleDisconnect = useCallback(() => {
    disconnectTimerRef.current = setTimeout(async () => {
      currentRoomIdRef.current = null;
      await disconnectWebSocket();
      console.log("WebSocket 연결이 종료되었습니다.");
      resetRoom();
    }, 100);
  }, [resetRoom]);

  // // 백그라운드 복귀 시 연결 상태 확인 및 재연결
  // useEffect(() => {
  //   const handleVisibilityChange = async () => {
  //     if (document.visibilityState !== "visible") return;
  //     if (!currentRoomIdRef.current) return;

  //     const client = getClient();
  //     if (client.active && client.connected) return;

  //     try {
  //       await connectWebSocket();
  //       console.log("백그라운드 복귀 후 WebSocket 재연결 성공");
  //       // connectWebSocket의 onConnect에서 resubscribeAll()이 호출되므로
  //       // 구독 복구 후 스냅샷을 재요청해 서버 상태를 동기화
  //       requestSnapshot();
  //     } catch (error) {
  //       console.error("백그라운드 복귀 후 재연결 실패:", error);
  //     }
  //   };

  //   document.addEventListener("visibilitychange", handleVisibilityChange);
  //   return () =>
  //     document.removeEventListener("visibilitychange", handleVisibilityChange);
  // }, [requestSnapshot]);

  return (
    <WebSocketSessionContext
      value={{
        connectSession,
        scheduleDisconnect,
        requestSnapshot,
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
