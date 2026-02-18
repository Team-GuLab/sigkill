import { Client } from "@stomp/stompjs";

let client: Client | null = null;

const getClient = (): Client => {
  if (!client) {
    client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL,
      reconnectDelay: 0, // TODO: 이후 상황에 따라 변경 필요
    });
  }
  return client;
};

export const connectWebSocket = async (): Promise<void> => {
  const wsClient = getClient();

  // 안전한 재연결을 위한 이미 활성화된 상태라면 연결 해제 선행
  if (wsClient.active) {
    await wsClient.deactivate();
  }

  return new Promise((resolve, reject) => {
    wsClient.onConnect = () => {
      console.log("Connected to WebSocket");
      resolve();
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
      reject(new Error("WebSocket connection failed"));
    };

    wsClient.onDisconnect = frame => {
      console.log("Disconnected successfully");
    };

    wsClient.onWebSocketClose = event => {
      console.log("WebSocket closed. Code:", event.code);
    };

    wsClient.activate();
  });
};

export const disconnectWebSocket = async () => {
  // client가 아직 생성되지 않았다면 아무것도 하지 않음
  if (!client) return;

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
