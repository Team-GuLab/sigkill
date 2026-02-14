import { subscribeManager } from "@/app/config/stomp-subscribe-manager";
import type { RoomWebSocketMessage } from "@/api/room/types";

/**
 * 방 관련 웹소켓 메시지 구독
 * - PLAYER_JOIN: 플레이어 입장
 * - PLAYER_LEFT: 플레이어 퇴장
 * - PLAYER_READY: 플레이어 준비 상태 변경
 * - GAME_START: 게임 시작
 */
export const subscribeRoom = (
  roomId: string,
  onMessage: (data: RoomWebSocketMessage) => void,
) => {
  const unsubscribe = subscribeManager<RoomWebSocketMessage>(
    `/topic/room/${roomId}`,
    data => {
      onMessage(data);
    },
  );

  return unsubscribe;
};
