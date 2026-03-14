import { getClient } from "./stomp-client";
import type { IMessage, StompSubscription } from "@stomp/stompjs";

// destination별 활성 구독을 추적
// WebSocket 재연결 시 구독 복구에도 사용
const activeSubscriptions = new Map<
  string,
  { subscription: StompSubscription; onMessage: (message: IMessage) => void }
>();

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
  const client = getClient();

  if (!client.connected) {
    console.warn(`Subscribing to ${destination} but client is not connected.`);
  }

  // 동일 destination에 이미 구독이 있으면 먼저 해제
  activeSubscriptions.get(destination)?.subscription.unsubscribe();

  const handler = (message: IMessage) => {
    try {
      const data = JSON.parse(message.body) as T;
      onMessage(data);
    } catch (error) {
      console.error(`Failed to parse message from ${destination}:`, error);
    }
  };

  const subscription = client.subscribe(destination, handler);
  activeSubscriptions.set(destination, { subscription, onMessage: handler });

  return () => {
    subscription.unsubscribe();
    activeSubscriptions.delete(destination);
  };
};

/**
 * WebSocket 재연결 시 활성 구독을 복구
 * connectWebSocket의 onConnected 콜백에서 호출
 */
export const resubscribeAll = () => {
  const client = getClient();
  for (const [destination, { onMessage }] of activeSubscriptions) {
    const subscription = client.subscribe(destination, onMessage);
    activeSubscriptions.set(destination, { subscription, onMessage });
  }
};
