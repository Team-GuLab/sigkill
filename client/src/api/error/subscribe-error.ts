import { subscribeManager } from "@/app/config/stomp-subscribe-manager";
import type { ErrorWebSocketMessage } from "./types";

/**
 * 에러 메시지 구독
 * @param onMessage - 에러 메시지 처리 콜백
 * @returns - 구독 해제 함수
 */
export const subscribeError = (
  onMessage: (data: ErrorWebSocketMessage) => void,
) => {
  return subscribeManager<ErrorWebSocketMessage>("/user/queue/errors", data => {
    onMessage(data);
  });
};
