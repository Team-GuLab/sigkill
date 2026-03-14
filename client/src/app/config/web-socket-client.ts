import { getClient } from "./stomp-client";
import { resubscribeAll } from "./stomp-subscribe-manager";

/**
 * WebSocket 연결
 * @param onConnected - 초기 연결 및 재연결 시마다 호출되는 콜백
 */
export const connectWebSocket = async (): Promise<void> => {
  const wsClient = getClient();

  return new Promise((resolve, reject) => {
    wsClient.onConnect = () => {
      console.log("Connected to WebSocket");
      resolve();
      // 재연결 시 기존 STOMP 구독 복구
      resubscribeAll();
    };

    wsClient.onStompError = frame => {
      console.error("Broker reported error: " + frame.headers["message"]);
      console.error("Additional details: " + frame.body);
      reject(
        new Error("WebSocket connection error: " + frame.headers["message"]),
      );
    };

    wsClient.onWebSocketError = event => {
      console.error("WebSocket error", event);
      reject(new Error("WebSocket connection error"));
    };

    wsClient.onDisconnect = () => {
      console.log("Disconnected successfully");
    };

    wsClient.onWebSocketClose = event => {
      console.log("WebSocket closed. Code:", event.code);
    };

    wsClient.activate();
  });
};

export const disconnectWebSocket = async () => {
  const client = getClient();

  if (client.active) {
    await client.deactivate();
  }
};

export const publishMessage = (destination: string, body: any) => {
  const wsClient = getClient();

  if (wsClient.active) {
    wsClient.publish({ destination, body: JSON.stringify(body) });
  } else {
    console.error("Cannot publish message. WebSocket is not active.");
  }
};

export { getClient };
