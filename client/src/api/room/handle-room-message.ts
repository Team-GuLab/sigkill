import { toast } from "sonner";
import type { Dispatch, SetStateAction } from "react";
import type { RoomWebSocketMessage, RoomItem, Player } from "./types";

type RoomInfoState = Omit<RoomItem, "playerCount" | "canJoin"> | null;

/**
 * 대기방 관련 stomp 메시지 핸들러
 * @param message - 방 stomp 메시지
 * @param setRoomInfo - 방 정보 상태 설정 함수
 * @param setPlayers - 플레이어 상태 설정 함수
 * @param myUserId - 내 유저 ID
 */
export const handleRoomMessage = (
  message: RoomWebSocketMessage,
  setRoomInfo: Dispatch<SetStateAction<RoomInfoState>>,
  setPlayers: Dispatch<SetStateAction<Player[]>>,
  myUserId: number,
) => {
  switch (message.type) {
    case "PLAYER_JOIN":
      // 플레이어 입장 시
      setRoomInfo(prev => {
        if (!prev) {
          toast.success(`방 "${message.room.roomTitle}"에 입장했습니다!`);
          return message.room;
        }
        return prev;
      });

      setPlayers(prevPlayers => {
        // 새로 입장한 플레이어 찾기
        const newPlayer = message.players.find(
          p => !prevPlayers.some(existing => existing.userId === p.userId),
        );
        if (newPlayer && prevPlayers.length > 0) {
          toast.info(`${newPlayer.nickname}님이 입장했습니다.`);
        }
        return message.players;
      });
      break;

    case "PLAYER_LEFT":
      // 플레이어 퇴장 시
      setPlayers(prevPlayers => {
        const leftPlayer = message.player;
        toast.info(`${leftPlayer.nickname}님이 퇴장했습니다.`);
        return prevPlayers.filter(p => p.userId !== leftPlayer.userId);
      });
      break;

    case "PLAYER_READY":
      // 플레이어 준비 상태 변경 시
      const readyPlayer = message.player;
      setPlayers(prevPlayers => {
        return prevPlayers.map(p => {
          return p.userId === readyPlayer?.userId
            ? { ...p, status: "READY" }
            : p;
        });
      });

      if (myUserId !== readyPlayer?.userId) {
        toast.info(`${readyPlayer?.nickname}님이 준비했습니다.`);
      }
      break;

    case "PLAYER_UNREADY":
      // 플레이어 준비 취소 시
      const unreadyPlayer = message.player;
      setPlayers(prevPlayers => {
        return prevPlayers.map(p =>
          p.userId === unreadyPlayer?.userId
            ? { ...p, status: "NOT_READY" }
            : p,
        );
      });

      if (myUserId !== unreadyPlayer?.userId) {
        toast.info(`${unreadyPlayer?.nickname}님이 준비 취소했습니다.`);
      }
      break;

    case "HOST_CHANGED":
      toast.info(`방장이 ${message.newHost.nickname}님으로 변경되었습니다.`);
      setPlayers(prevPlayers =>
        prevPlayers.map(p => {
          if (p.userId === message.newHost.userId) {
            return { ...p, role: "HOST" };
          }
          if (p.userId === message.oldHost.userId) {
            return { ...p, role: "GUEST" };
          }
          return p;
        }),
      );
      break;

    default:
      // @ts-ignore
      console.warn("Unknown message type:", message);
  }
};
