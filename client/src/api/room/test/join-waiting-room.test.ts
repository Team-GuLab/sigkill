import { joinWaitingRoom } from "../join-waiting-room";
import * as WebSocketClient from "@/app/config/web-socket-client";
import * as SubscribeManager from "@/app/config/stomp-subscribe-manager";
import type { RoomJoinResponse } from "../types";

describe("대기방에 입장하는 테스트", () => {
  const roomId = "1234";
  const mockOnJoinSuccess = vi.fn();

  it("정상적으로 연결, 구독, 메시지 발행이 이루어져야 한다", async () => {
    // Arrange
    const mockUnsubscribe = vi.fn();
    vi.mocked(SubscribeManager.subscribeManager).mockReturnValue(
      mockUnsubscribe,
    );

    // Act
    await joinWaitingRoom(roomId, mockOnJoinSuccess);

    // Assert
    expect(WebSocketClient.connectWebSocket).toHaveBeenCalledTimes(1);
    expect(SubscribeManager.subscribeManager).toHaveBeenCalledWith(
      "/queue/room/init",
      expect.any(Function),
    );
    expect(WebSocketClient.publishMessage).toHaveBeenCalledWith(
      "/app/room/join",
      { roomId: "1234" },
    );
  });

  it("구독 콜백이 호출되면 onJoinSuccess가 실행되고 구독이 해제되어야 한다", async () => {
    // Setup
    const mockUnsubscribe = vi.fn();
    vi.mocked(SubscribeManager.subscribeManager).mockReturnValue(
      mockUnsubscribe,
    );

    // Action 1: 함수 호출
    await joinWaitingRoom(roomId, mockOnJoinSuccess);

    // Action 2: 응답 시뮬레이션 (PLAYER_JOIN)
    const mockResponse: RoomJoinResponse = {
      type: "PLAYER_JOIN",
      room: {
        roomId: "1234",
        roomTitle: "테스트 방",
        hostId: "host1",
        capacity: 10,
        status: "WAITING",
      },
      players: [],
    };

    const firstCall = vi.mocked(SubscribeManager.subscribeManager).mock
      .calls[0];
    const onMessage = firstCall?.[1];

    onMessage(mockResponse);

    // Assert
    expect(mockOnJoinSuccess).toHaveBeenCalledWith(mockResponse);
    expect(mockUnsubscribe).toHaveBeenCalledTimes(1);
  });
});
