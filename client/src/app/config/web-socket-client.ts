import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: import.meta.env.VITE_WS_URL,
  reconnectDelay: 0, // TODO: 이후 상황에 따라 변경 필요
});

export const connectWebSocket = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    // 이미 연결된 경우 바로 성공 처리
    if (client.connected) {
      resolve();
      return;
    }

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
