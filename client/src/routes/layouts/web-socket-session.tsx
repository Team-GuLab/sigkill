import { Outlet } from "react-router";
import { WebSocketSessionProvider } from "@/app/provider/web-socket-session-provider";

/**
 * WebSocket 세션이 필요한 라우트(WaitingRoom, GameRoom)의 공통 부모
 */
export default function WebSocketSessionLayout() {
  return (
    <WebSocketSessionProvider>
      <Outlet />
    </WebSocketSessionProvider>
  );
}
