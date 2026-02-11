import { ws } from "msw";

const WS_URL = import.meta.env.VITE_WS_URL || "ws://localhost:15674/ws";

const room = ws.link(WS_URL);

export const wsHandlers = [
  room.addEventListener("connection", ({ client }) => {
    console.log(`[MSW] WebSocket connection intercepted: ${WS_URL}`);

    const SHOULD_FAIL = false;

    if (SHOULD_FAIL) {
      console.log("[MSW] Simulating connection failure...");
      throw new Error("Connection failed");
    }

    client.addEventListener("message", event => {
      const message = event.data;

      if (typeof message === "string") {
        // 1. CONNECT 프레임 처리
        if (message.startsWith("CONNECT") || message.startsWith("STOMP")) {
          console.log("[MSW] Received STOMP CONNECT frame. Sending CONNECTED.");
          client.send("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\u0000");
        }

        // 2. SEND 프레임 처리
        if (message.startsWith("SEND")) {
          // destination 헤더 추출
          const destMatch = message.match(/destination:(.*)\n/);
          const destination = destMatch ? destMatch[1].trim() : "";

          if (destination === "/app/room/join") {
            console.log(
              "[MSW] Received JOIN request for /app/room/join. Sending response to /queue/room/init...",
            );

            const responseData = {
              type: "PLAYER_JOIN",
              room: {
                roomId: "1234",
                roomTitle: "재미있는 퀴즈방 [MSW]",
                hostId: "session-def-456",
                capacity: 10,
                status: "WAITING",
                playerCount: 3,
              },
              players: [
                { playerId: "1", nickname: "귀여운사자", status: "READY" },
                { playerId: "2", nickname: "슬픈코끼리", status: "NOT_READY" },
                { playerId: "3", nickname: "멋진하마", status: "NOT_READY" },
              ],
            };

            // STOMP MESSAGE 프레임 생성
            // 주의: stompjs 클라이언트는 subscription 헤더가 일치해야 메시지를 처리할 수 있습니다.
            // 보통 첫 번째 구독은 id:sub-0 입니다.
            // content-length 헤더는 생략 가능합니다.
            const responseFrame = `MESSAGE
destination:/queue/room/init
subscription:sub-0
message-id:msg-${Date.now()}
content-type:application/json

${JSON.stringify(responseData)}\0`;

            // 0.5초 후 응답 전송
            setTimeout(() => {
              client.send(responseFrame);
            }, 500);
          }
        }
      }
    });

    client.addEventListener("close", event => {
      console.log("Client is closing the connection");
    });
  }),
];
