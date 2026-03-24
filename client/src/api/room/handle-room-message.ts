import { toast } from "sonner";
import type { RoomWebSocketMessage } from "./types";
import { useRoomStore } from "@/store/room-store";
import { useUserStore } from "@/store/user-store";
import { useGameStore } from "@/store/game-store";

/**
 * 대기방 관련 stomp 메시지 핸들러
 * @param message - 방 stomp 메시지
 * @param myUserId - 내 유저 ID
 */
export const handleRoomMessage = (
  message: RoomWebSocketMessage,
  myUserId: number,
) => {
  // 게임 진행 중에는 대기방 토스트를 표시하지 않음
  // (게임 시작 후 방 구독이 남아있어 메시지가 계속 수신될 수 있음)
  const isInGame = useGameStore.getState().phase.status !== "IDLE";

  switch (message.type) {
    case "ROOM_SNAPSHOT": {
      const { setRoomInfo, setPlayers, reset } = useRoomStore.getState();
      reset();
      setRoomInfo(message.room);
      setPlayers(message.players);
      break;
    }

    case "PLAYER_JOIN": {
      const { user } = useUserStore.getState();
      const { roomInfo, setRoomInfo } = useRoomStore.getState();

      if (!roomInfo) {
        if (!isInGame)
          toast.success(`방 "${message.room.roomTitle}"에 입장했습니다!`);
        setRoomInfo(message.room);
      }

      if (!isInGame && message.player.userId !== user?.userId) {
        toast.info(`${message.player.nickname}님이 입장했습니다.`);
      }
      break;
    }

    case "PLAYER_LEFT": {
      const { players, setPlayers } = useRoomStore.getState();
      const leftPlayer = message.player;
      if (!isInGame) toast.info(`${leftPlayer.nickname}님이 퇴장했습니다.`);
      setPlayers(players.filter(p => p.userId !== leftPlayer.userId));
      break;
    }

    case "PLAYER_READY": {
      const { players, setPlayers } = useRoomStore.getState();
      const readyPlayer = message.player;
      setPlayers(
        players.map(p =>
          p.userId === readyPlayer?.userId ? { ...p, status: "READY" } : p,
        ),
      );
      if (!isInGame && myUserId !== readyPlayer?.userId) {
        toast.info(`${readyPlayer?.nickname}님이 준비했습니다.`);
      }
      break;
    }

    case "PLAYER_UNREADY": {
      const { players, setPlayers } = useRoomStore.getState();
      const unreadyPlayer = message.player;
      setPlayers(
        players.map(p =>
          p.userId === unreadyPlayer?.userId
            ? { ...p, status: "NOT_READY" }
            : p,
        ),
      );
      if (!isInGame && myUserId !== unreadyPlayer?.userId) {
        toast.info(`${unreadyPlayer?.nickname}님이 준비 취소했습니다.`);
      }
      break;
    }

    case "HOST_CHANGED": {
      const { players, setPlayers } = useRoomStore.getState();
      if (!isInGame)
        toast.info(`방장이 ${message.newHost.nickname}님으로 변경되었습니다.`);
      setPlayers(
        players.map(p => {
          if (p.userId === message.newHost.userId)
            return { ...p, role: "HOST" };
          if (p.userId === message.oldHost.userId)
            return { ...p, role: "GUEST" };
          return p;
        }),
      );
      break;
    }

    default:
      // @ts-ignore
      console.warn("Unknown message type:", message);
  }
};
