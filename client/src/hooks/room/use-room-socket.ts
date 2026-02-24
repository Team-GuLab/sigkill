import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import {
  connectWebSocket,
  disconnectWebSocket,
  getClient,
  publishMessage,
} from "@/app/config/web-socket-client";
import { subscribeRoom } from "@/api/room/subscribe-room";
import { handleRoomMessage } from "@/api/room/handle-room-message";
import { subscribeError } from "@/api/error/subscribe-error";
import { handleErrorMessage } from "@/api/error/handle-error-message";
import { ROUTE_PATHS } from "@/routes/paths";
import { useSetGameInfo } from "@/store/game-store";
import { useResetRoom } from "@/store/room-store";

/**
 * 대기방 웹소켓 연결 및 상태 관리
 * @param roomId - 방 ID
 * @param myUserId - 내 유저 ID
 */
interface UseRoomSocketProps {
  roomId: string | undefined;
  myUserId: number;
}

export const useRoomSocket = ({ roomId, myUserId }: UseRoomSocketProps) => {
  const navigate = useNavigate();
  const setGameInfo = useSetGameInfo();
  const [isPending, setIsPending] = useState(false);
  const resetRoom = useResetRoom();

  const roomUnsubscribe = useRef<(() => void) | undefined>(undefined);
  const errorUnsubscribe = useRef<(() => void) | undefined>(undefined);

  const isGameStarted = useRef(false);

  useEffect(() => {
    if (!roomId) return;

    const setupConnection = async () => {
      try {
        setIsPending(true);
        await connectWebSocket();
        const client = getClient();

        // 연결 종료 감지 (비정상 종료 포함)
        client.onWebSocketClose = event => {
          console.log("WebSocket closed. Code:", event.code);
          if (event.code !== 1000) {
            toast.error("연결이 종료되었습니다.");
            navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
          }
        };

        // 웹소켓 메시지 핸들러
        roomUnsubscribe.current = subscribeRoom(roomId, message => {
          if (message.type === "GAME_START") {
            isGameStarted.current = true;
            setGameInfo(message.roomId, message.gameId);
            toast.success("게임이 시작됩니다!");
            navigate(`/game/${message.gameId}`, { replace: true });
            return;
          }

          handleRoomMessage(message, myUserId);
        });

        // 에러 메시지 구독
        errorUnsubscribe.current = subscribeError(error => {
          handleErrorMessage(error);
        });

        publishMessage("/app/room/join", { roomId });
      } catch (error) {
        console.error("Connection failed:", error);
        navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
        toast.error("연결 중 오류로 인해 방 목록으로 이동합니다.");
      } finally {
        setIsPending(false);
      }
    };

    setupConnection();

    return () => {
      // TODO: 더 나은 방법으로 개선 필요
      if (isGameStarted.current) {
        return;
      }

      // 게임이 시작되지 않은 경우 정상적으로 cleanup
      const client = getClient();
      if (client && client.active) {
        if (roomUnsubscribe.current) roomUnsubscribe.current();
        if (errorUnsubscribe.current) errorUnsubscribe.current();
      }
      disconnectWebSocket();
      resetRoom();
    };
  }, [roomId, myUserId, navigate, resetRoom]);

  return { isPending };
};
