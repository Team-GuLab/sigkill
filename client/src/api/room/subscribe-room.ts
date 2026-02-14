import { subscribeManager } from "@/app/config/stomp-subscribe-manager";
import type { PlayerJoinResponse } from "@/api/room/types";

/**
 * 다른 플레이어 입장 구독
 */
export const subscribeRoom = (
  roomId: string,
  onRoomJoin: (data: PlayerJoinResponse) => void,
) => {
  const unsubscribe = subscribeManager<PlayerJoinResponse>(
    `/topic/room/${roomId}`,
    data => {
      onRoomJoin(data);
    },
  );

  return unsubscribe;
};
