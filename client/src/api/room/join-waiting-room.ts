import { publishMessage } from "@/app/config/web-socket-client";
import { subscribeManager } from "@/app/config/stomp-subscribe-manager";
import type { RoomJoinResponse } from "@/api/room/types";

/**
 * 대기방 입장 프로세스
 * 1. 웹소켓 연결
 * 2. /queue/room/init 구독
 * 3. /app/room/join 메시지 발행
 * 4. 응답 수신 시 콜백 실행
 */
export const joinWaitingRoom = async (
  roomId: string,
  onJoinSuccess: (data: RoomJoinResponse) => void,
) => {
  const unsubscribe = subscribeManager<RoomJoinResponse>(
    "/queue/room/init",
    data => {
      console.log("Received RoomJoinResponse:", data);
      if (data.type === "PLAYER_JOIN") {
        onJoinSuccess(data);
        unsubscribe();
      }
    },
  );

  publishMessage("/app/room/join", { roomId });
};
