import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: import.meta.env.VITE_WS_URL,
  reconnectDelay: 0, // TODO: 이후 상황에 따라 변경 필요
});

export const connectWebSocket = async (): Promise<void> => {
  // 안전한 재연결을 위한 이미 활성화된 상태라면 연결 해제 선행
  if (client.active) {
    await client.deactivate();
  }

  return new Promise((resolve, reject) => {
    client.onConnect = () => {
      console.log("Connected to WebSocket");
      resolve();
    };

    client.onStompError = frame => {
      console.error("Broker reported error: " + frame.headers["message"]);
      console.error("Additional details: " + frame.body);
      reject(
        new Error("WebSocket connection error: " + frame.headers["message"]),
      );
    };

    client.onWebSocketError = event => {
      console.error("WebSocket error", event);
      reject(new Error("WebSocket connection failed"));
    };

    client.onDisconnect = frame => {
      console.log("Disconnected successfully");
    };

    client.onWebSocketClose = event => {
      console.log("WebSocket closed. Code:", event.code);
    };

    client.activate();
  });
};

export const disconnectWebSocket = () => {
  if (client.active) {
    client.deactivate();
  }
};

export const publishMessage = (destination: string, body: any) => {
  if (client.active) {
    client.publish({ destination, body: JSON.stringify(body) });
  } else {
    console.error("Cannot publish message. WebSocket is not active.");
  }
};

export default client;
