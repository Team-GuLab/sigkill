import { Client } from "@stomp/stompjs";

let client: Client | null = null;
let onConnectedCallback: (() => void) | null = null;

const getClient = (): Client => {
  if (!client) {
    client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL,
      reconnectDelay: 5000,
    });
  }
  return client;
};

/**
 * WebSocket 연결
 * @param onConnected - 초기 연결 및 재연결 시마다 호출되는 콜백
 */
export const connectWebSocket = async (
  onConnected?: () => void,
): Promise<void> => {
  const wsClient = getClient();

  onConnectedCallback = onConnected ?? null;

  return new Promise((resolve, reject) => {
    wsClient.onConnect = () => {
      console.log("Connected to WebSocket");
      resolve();
      // 구독 재설정 등 복구 작업
      onConnectedCallback?.();
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
  console.log("디스커넥트");
  if (!client) return;

  onConnectedCallback = null;

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
