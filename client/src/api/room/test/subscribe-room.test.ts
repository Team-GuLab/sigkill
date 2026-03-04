import { subscribeRoom } from "../subscribe-room";
import * as SubscribeManager from "@/app/config/stomp-subscribe-manager";
import type { RoomWebSocketMessage } from "../types";

describe("대기방 웹소켓 구독 테스트", () => {
  const roomId = "1234";
  const mockOnMessage = vi.fn();

  it("올바른 토픽으로 subscribeManager가 호출되어야 한다", () => {
    // Arrange
    const mockUnsubscribe = vi.fn();
    vi.mocked(SubscribeManager.subscribeManager).mockReturnValue(
      mockUnsubscribe,
    );

    // Act
    subscribeRoom(roomId, mockOnMessage);

    // Assert
    expect(SubscribeManager.subscribeManager).toHaveBeenCalledWith(
      `/topic/room/${roomId}`,
      expect.any(Function),
    );
  });

  it("subscribeManager의 unsubscribe 함수를 반환해야 한다", () => {
    // Arrange
    const mockUnsubscribe = vi.fn();
    vi.mocked(SubscribeManager.subscribeManager).mockReturnValue(
      mockUnsubscribe,
    );

    // Act
    const unsubscribe = subscribeRoom(roomId, mockOnMessage);

    // Assert
    expect(unsubscribe).toBe(mockUnsubscribe);
  });

  it("구독 콜백이 호출되면 onMessage가 수신한 데이터와 함께 실행되어야 한다", () => {
    // Arrange
    const mockUnsubscribe = vi.fn();
    vi.mocked(SubscribeManager.subscribeManager).mockReturnValue(
      mockUnsubscribe,
    );

    const mockMessage: RoomWebSocketMessage = {
      type: "PLAYER_JOIN",
      room: {
        roomId: "1234",
        roomTitle: "테스트 방",
        capacity: 10,
        status: "WAITING",
      },
      player: {
        userId: 1,
        nickname: "테스트 유저",
        status: "READY",
        role: "HOST",
      },
    };

    // Act
    subscribeRoom(roomId, mockOnMessage);

    const firstCall = vi.mocked(SubscribeManager.subscribeManager).mock
      .calls[0];
    const onMessageCallback = firstCall?.[1];
    onMessageCallback(mockMessage);

    // Assert
    expect(mockOnMessage).toHaveBeenCalledWith(mockMessage);
    expect(mockOnMessage).toHaveBeenCalledTimes(1);
  });
});
