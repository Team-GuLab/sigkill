import { subscribeManager } from "@/app/config/stomp-subscribe-manager";
import type { GameWebSocketMessage } from "./types";

/**
 * 게임 관련 웹소켓 메시지 구독
 * @param gameId - 게임 ID
 * @param onMessage - 메시지 수신 콜백
 */
export const subscribeGame = (
  gameId: number,
  onMessage: (data: GameWebSocketMessage) => void,
) => {
  return subscribeManager<GameWebSocketMessage>(
    `/topic/game/${gameId}`,
    data => {
      onMessage(data);
    },
  );
};
