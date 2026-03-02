import type { ws } from "msw";

/**
 * STOMP MESSAGE 프레임 생성 및 브로드캐스트
 */
function sendMessage(
  room: ReturnType<typeof ws.link>,
  subscriptionId: string,
  destination: string,
  data: any,
) {
  const responseFrame = `MESSAGE
destination:${destination}
subscription:${subscriptionId}
message-id:msg-${Date.now()}
content-type:application/json

${JSON.stringify(data)}\0`;

  room.broadcast(responseFrame);
}

/**
 * 플레이어가 대기방에 입장할 때 호출됨
 */
export function handleRoomJoin(
  room: ReturnType<typeof ws.link>,
  subscriptionId: string,
) {
  console.log("[MSW] Handling /app/room/confirm-join request");

  const responseData = {
    type: "PLAYER_JOIN",
    room: {
      roomId: "1234", // 요청받은 roomId로 응답
      roomTitle: "재미있는 퀴즈방 [MSW]",
      capacity: 6,
      status: "WAITING",
    },
    players: [
      {
        userId: 1,
        nickname: "귀여운사자",
        status: "READY",
        role: "HOST",
      },
      {
        userId: 2,
        nickname: "슬픈코끼리",
        status: "NOT_READY",
        role: "GUEST",
      },
      {
        userId: 3,
        nickname: "멋진하마",
        status: "NOT_READY",
        role: "GUEST",
      },
    ],
  };

  setTimeout(() => {
    sendMessage(room, subscriptionId, `/topic/room/1234`, responseData);
  }, 200);
}
