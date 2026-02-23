import { useEffect, useRef } from "react";
import { subscribeGame } from "@/api/game/subscribe-game";
import { handleGameMessage } from "@/api/game/handle-game-message";
import { getClient } from "@/app/config/web-socket-client";

interface UseGameSocketParams {
  gameId: number;
}

/**
 * 게임룸 소켓 연결 및 상태 관리
 * @param gameId - 게임 ID
 */
export const useGameSocket = ({ gameId }: UseGameSocketParams) => {
  const unsubscribe = useRef<(() => void) | undefined>(undefined);

  useEffect(() => {
    if (!gameId) return;

    const client = getClient();

    if (!client || !client.connected) {
      console.error("WebSocket is not connected");
      return;
    }

    unsubscribe.current = subscribeGame(gameId, message => {
      handleGameMessage(message);
    });

    return () => {
      if (unsubscribe.current) {
        unsubscribe.current();
      }
    };
  }, [gameId]);
};
