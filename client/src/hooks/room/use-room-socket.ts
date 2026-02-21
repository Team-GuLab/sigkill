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
import type { Player, RoomItem } from "@/api/room/types";

/**
 * 대기방 웹소켓 연결 및 상태 관리
 * @param roomId - 방 ID
 * @param myUserId - 내 유저 ID
 * @param initialPlayers - 초기 플레이어 목록
 * @returns - 방 정보와 플레이어 목록
 */
interface UseRoomSocketProps {
  roomId: string | undefined;
  myUserId: number;
  initialPlayers: Player[];
}
export const useRoomSocket = ({
  roomId,
  myUserId,
  initialPlayers,
}: UseRoomSocketProps) => {
  const navigate = useNavigate();
  const [isPending, setIsPending] = useState(false);
  const [roomInfo, setRoomInfo] = useState<Omit<
    RoomItem,
    "playerCount" | "canJoin"
  > | null>(null);
  const [players, setPlayers] = useState<Player[]>(initialPlayers);

  const unsubscribe = useRef<(() => void) | undefined>(undefined);
  const errorUnsubscribe = useRef<(() => void) | undefined>(undefined);

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
        unsubscribe.current = subscribeRoom(roomId, message => {
          handleRoomMessage(message, setRoomInfo, setPlayers, myUserId);
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
      const client = getClient();
      if (client && client.active) {
        if (unsubscribe.current) unsubscribe.current();
        if (errorUnsubscribe.current) errorUnsubscribe.current();
      }
      disconnectWebSocket();
    };
  }, [roomId, myUserId, navigate]);

  return { roomInfo, players, isPending };
};
