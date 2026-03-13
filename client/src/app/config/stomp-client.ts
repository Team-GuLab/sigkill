import { Client } from "@stomp/stompjs";

let client: Client | null = null;

export const getClient = (): Client => {
  if (!client) {
    client = new Client({
      brokerURL: import.meta.env.VITE_WS_URL,
      reconnectDelay: 5000,
    });
  }
  return client;
};
