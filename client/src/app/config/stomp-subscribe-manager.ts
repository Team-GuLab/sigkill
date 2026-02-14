import client from "./web-socket-client";
import type { IMessage } from "@stomp/stompjs";

/**
 * STOMP 구독 관리자
 * @param destination - 구독할 경로
 * @param onMessage - 메시지 수신 시 처리할 콜백
 * @returns - 구독 해제 함수
 */
export const subscribeManager = <T>(
  destination: string,
  onMessage: (data: T) => void,
) => {
  if (!client.connected) {
    console.warn(`Subscribing to ${destination} but client is not connected.`);
  }

  const subscription = client.subscribe(destination, (message: IMessage) => {
    try {
      const data = JSON.parse(message.body) as T;
      onMessage(data);
    } catch (error) {
      console.error(`Failed to parse message from ${destination}:`, error);
    }
  });

  return () => {
    subscription.unsubscribe();
  };
};
