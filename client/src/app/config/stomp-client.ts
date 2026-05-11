import { Client } from "@stomp/stompjs";

let client: Client | null = null;

const getWsUrl = (): string => {
  if (import.meta.env.DEV) {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${protocol}//${window.location.host}/ws`;
  }
  return import.meta.env.VITE_WS_URL;
};

export const getClient = (): Client => {
  if (!client) {
    client = new Client({
      brokerURL: getWsUrl(),
      reconnectDelay: 5000,
    });
  }
  return client;
};
