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
import {
  useSetGameInfo,
  useSetGamePlayers,
  useSetIsInGame,
} from "@/store/game-store";
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
  const setGamePlayers = useSetGamePlayers();
  const setIsInGame = useSetIsInGame();
  const [isPending, setIsPending] = useState(false);
  const [isGameStarting, setIsGameStarting] = useState(false);
  const resetRoom = useResetRoom();

  const isGameStarted = useRef(false);
  const roomUnsubscribe = useRef<(() => void) | undefined>(undefined);
  const errorUnsubscribe = useRef<(() => void) | undefined>(undefined);

  useEffect(() => {
    if (!roomId) return;

    const setupConnection = async () => {
      try {
        setIsPending(true);
        const client = getClient();

        // 이미 연결된 웹소켓이 있는 경우 재사용, 그렇지 않으면 새로 연결
        if (!client.active) {
          await connectWebSocket();
        }

        // 연결 종료 감지 (비정상 종료 포함)
        client.onWebSocketClose = event => {
          console.log("WebSocket closed. Code:", event.code);
          if (event.code !== 1000) {
            toast.error("연결이 종료되었습니다.");
            navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
          }
        };

        // subscribeManager가 동일 destination 중복 구독을 자동으로 방지하므로
        // 재진입 시에도 이전 구독이 먼저 해제되고 새로 등록됨
        roomUnsubscribe.current = subscribeRoom(roomId, message => {
          if (message.type === "GAME_START") {
            isGameStarted.current = true;
            setIsInGame(true);
            setGameInfo(message.roomId, message.gameId);
            setGamePlayers(message.payload.players);
            setIsGameStarting(true);
            navigate(`/game/${message.gameId}`, { replace: true });
            return;
          }

          handleRoomMessage(message, myUserId);
        });

        errorUnsubscribe.current = subscribeError(error => {
          handleErrorMessage(error);
        });

        publishMessage("/app/room/join", { roomId });
        publishMessage("/app/room/snapshot", { roomId });
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
      // 게임이 시작된 경우: 구독을 유지하고 웹소켓도 유지
      // (게임 화면에서 계속 사용)
      if (isGameStarted.current) {
        return;
      }

      // 게임이 시작되지 않은 경우 완전 정리
      roomUnsubscribe.current?.();
      errorUnsubscribe.current?.();
      disconnectWebSocket();
      resetRoom();
    };
  }, [roomId, myUserId, navigate, resetRoom]);

  return { isPending, isGameStarting };
};
