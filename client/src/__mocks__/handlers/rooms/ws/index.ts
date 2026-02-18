import { ws } from "msw";
import { handleRoomJoin } from "./handle-room-join";
import { extractDestination } from "@/__mocks__/utils/extract-destination";

const WS_URL = import.meta.env.VITE_WS_URL || "ws://localhost:15674/ws";

const room = ws.link(WS_URL);

export const wsHandlers = [
  room.addEventListener("connection", ({ client }) => {
    let currentSubscriptionId = "sub-0";

    client.addEventListener("message", event => {
      const message = event.data;

      if (typeof message === "string") {
        // 0. SUBSCRIBE 프레임 처리
        if (message.startsWith("SUBSCRIBE")) {
          const idMatch = message.match(/id:(.*)\n/);
          if (idMatch) {
            currentSubscriptionId = idMatch[1].trim();
            console.log(
              `[MSW] Captured subscription ID: ${currentSubscriptionId}`,
            );
          }
        }

        // Receipt 헤더 확인 및 응답
        const receiptMatch = message.match(/receipt:(.*)\n/);
        if (receiptMatch) {
          const receiptId = receiptMatch[1].trim();
          console.log(`[MSW] Sending RECEIPT for subscription: ${receiptId}`);
          client.send(`RECEIPT\nreceipt-id:${receiptId}\n\n\0`);
        }

        // 1. CONNECT 프레임 처리
        if (message.startsWith("CONNECT")) {
          console.log("[MSW] Received STOMP CONNECT frame. Sending CONNECTED.");
          client.send("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0");
        }

        // 2. SEND 프레임 처리 - destination별로 라우팅
        if (message.startsWith("SEND")) {
          const destination = extractDestination(message);

          // Body 파싱 로직 추가
          let payload: any = {};
          try {
            // 헤더와 바디 사이의 빈 줄(\n\n)을 기준으로 분리
            const parts = message.split("\n\n");
            if (parts.length > 1) {
              // 마지막의 null 문자(\0) 제거
              const bodyStr = parts[1].replace(/\0/g, "");
              if (bodyStr.trim()) {
                payload = JSON.parse(bodyStr);
              }
            }
          } catch (e) {
            console.error("[MSW] Failed to parse SEND body:", e);
          }

          console.log(`[MSW] Routing to destination: ${destination}`);

          switch (destination) {
            case "/app/room/join":
              handleRoomJoin(room, currentSubscriptionId, payload);
              break;

            default:
              console.warn(`[MSW] Unknown destination: ${destination}`);
          }
        }

        // 3. DISCONNECT 프레임 처리
        if (message.startsWith("DISCONNECT")) {
          console.log("[MSW] Received DISCONNECT frame. Closing connection.");
          client.close();
        }
      }
    });

    client.addEventListener("close", event => {
      console.log("Client is closing the connection");
    });
  }),
];
