import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { subscribeRoom } from "@/api/room/subscribe-room";
import { handleRoomMessage } from "@/api/room/handle-room-message";
import { subscribeError } from "@/api/error/subscribe-error";
import { handleErrorMessage } from "@/api/error/handle-error-message";
import { ROUTE_PATHS } from "@/routes/paths";
import { useGameTransition } from "@/store/game-store";
import { useWebSocketSession } from "@/app/provider/web-socket-session-provider";
import { getClient, publishMessage } from "@/app/config/web-socket-client";

interface UseSubscribeRoomProps {
  roomId: string | undefined;
  myUserId: number;
}

/**
 * 대기방 구독 관리
 */
export const useSubscribeRoom = ({
  roomId,
  myUserId,
}: UseSubscribeRoomProps) => {
  const navigate = useNavigate();
  const transition = useGameTransition();
  const { connectSession, disconnectSession } = useWebSocketSession();

  const [isPending, setIsPending] = useState(false);
  const [isGameStarting, setIsGameStarting] = useState(false);

  const roomUnsubscribe = useRef<(() => void) | undefined>(undefined);
  const errorUnsubscribe = useRef<(() => void) | undefined>(undefined);
  // 게임방 전환 여부 추적: true이면 cleanup에서 구독 유지 + disconnect 생략
  const isTransitioningToGame = useRef(false);

  useEffect(() => {
    if (!roomId) return;

    isTransitioningToGame.current = false;

    const setup = async () => {
      try {
        setIsPending(true);
        await connectSession(roomId);

        // 비정상 종료 감지
        const client = getClient();
        client.onWebSocketClose = event => {
          if (event.code !== 1000) {
            toast.error("연결이 종료되었습니다.");
            navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
          }
        };

        roomUnsubscribe.current = subscribeRoom(roomId, message => {
          if (message.type === "GAME_START") {
            isTransitioningToGame.current = true;
            transition({
              type: "GAME_START",
              roomId: message.roomId,
              gameId: message.gameId,
              players: message.payload.players,
            });
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
      } catch {
        navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
        toast.error("연결 중 오류로 인해 방 목록으로 이동합니다.");
      } finally {
        setIsPending(false);
      }
    };

    setup();

    return () => {
      if (isTransitioningToGame.current) {
        // 게임방 전환: 마운트 시 수행한 기존 구독 유지
        return;
      }
      // 방 목록 등 다른 곳으로 이동: 구독 해제 후 연결 종료
      roomUnsubscribe.current?.();
      errorUnsubscribe.current?.();
      disconnectSession();
    };
  }, [
    roomId,
    myUserId,
    navigate,
    connectSession,
    disconnectSession,
    transition,
  ]);

  return { isPending, isGameStarting };
};
