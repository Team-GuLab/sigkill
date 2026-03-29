import { useEffect, useRef } from "react";
import { subscribeGame } from "@/api/game/subscribe-game";
import { handleGameMessage } from "@/api/game/handle-game-message";
import { getClient, publishMessage } from "@/app/config/web-socket-client";

interface UseSubscribeGameParams {
  gameId: number;
}

/**
 * 게임룸 구독 관리
 * - 연결 자체는 WebSocketSessionProvider가 담당
 * - 대기방->게임방 전환 시 WebSocket 연결과 room/error 구독은 유지된 상태로 진입
 * - 이 훅은 game 구독과 메시지 처리만 담당
 */
export const useSubscribeGame = ({ gameId }: UseSubscribeGameParams) => {
  const unsubscribeGame = useRef<(() => void) | undefined>(undefined);

  useEffect(() => {
    if (!gameId) return;

    const client = getClient();

    if (!client || !client.connected) {
      console.error("WebSocket is not connected");
      return;
    }

    unsubscribeGame.current = subscribeGame(gameId, message => {
      handleGameMessage(message);
    });

    // 각 플레이어는 게임 로딩 완료를 서버에 알림
    publishMessage("/app/game/load", { gameId });

    return () => {
      unsubscribeGame.current?.();
    };
  }, [gameId]);
};
