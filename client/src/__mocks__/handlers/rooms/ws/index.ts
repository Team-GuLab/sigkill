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

        // 1. CONNECT 프레임 처리
        if (message.startsWith("CONNECT")) {
          console.log("[MSW] Received STOMP CONNECT frame. Sending CONNECTED.");
          client.send("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0");
        }

        // 2. SEND 프레임 처리 - destination별로 라우팅
        if (message.startsWith("SEND")) {
          const destination = extractDestination(message);

          console.log(`[MSW] Routing to destination: ${destination}`);

          switch (destination) {
            case "/app/room/confirm-join":
              handleRoomJoin(room, currentSubscriptionId);
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

    client.addEventListener("close", () => {
      console.log("Client is closing the connection");
    });
  }),
];
