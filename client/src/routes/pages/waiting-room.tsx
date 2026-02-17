import { handleRoomMessage } from "@/api/room/handle-room-message";
import { subscribeRoom } from "@/api/room/subscribe-room";
import { useEffect, useState, useMemo, useRef } from "react";
import { useNavigate, useParams } from "react-router";
import { toast } from "sonner";
import type { Player, RoomItem } from "@/api/room/types";
import client, {
  connectWebSocket,
  disconnectWebSocket,
  publishMessage,
} from "@/app/config/web-socket-client";
import PlayerList from "@/components/room/player-list";
import { Button } from "@/ui/button";
import { Play } from "lucide-react";
import { ROUTE_PATHS } from "@/routes/paths";

export type PlayerSlot = {
  slotIndex: number;
  player: Player | null;
};

export default function WaitingRoom() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const [roomInfo, setRoomInfo] = useState<Omit<
    RoomItem,
    "playerCount" | "canJoin"
  > | null>(null);
  const [players, setPlayers] = useState<Player[]>([]);
  const unsubscribe = useRef<(() => void) | undefined>(undefined);

  // userId -> slotIndex 매핑 (플레이어를 특정 슬롯에 고정)
  const [playerSlotMapping, setPlayerSlotMapping] = useState<
    Map<number, number>
  >(new Map());

  // TODO: 로그인 구현 시 반영
  const currentUserId = 1;

  const isHost = currentUserId
    ? players.find(p => p.userId === currentUserId)?.role === "HOST"
    : false;

  // 슬롯 배열 생성 (capacity 크기, 각 슬롯에 플레이어 또는 null)
  const playerSlots = useMemo<PlayerSlot[]>(() => {
    const capacity = roomInfo?.capacity || 6;
    const slots: PlayerSlot[] = Array.from({ length: capacity }, (_, i) => ({
      slotIndex: i,
      player: null,
    }));

    // 매핑된 슬롯에 플레이어 배치
    players.forEach(player => {
      const slotIndex = playerSlotMapping.get(player.userId);
      if (slotIndex !== undefined && slotIndex < capacity) {
        slots[slotIndex].player = player;
      }
    });

    return slots;
  }, [players, playerSlotMapping, roomInfo?.capacity]);

  // 플레이어 변경 시 슬롯 매핑 업데이트
  useEffect(() => {
    setPlayerSlotMapping(prevMapping => {
      const newMapping = new Map(prevMapping);

      // 새로 입장한 플레이어를 빈 슬롯에 할당
      players.forEach(player => {
        if (!newMapping.has(player.userId)) {
          // 빈 슬롯 찾기 (가장 앞쪽부터)
          const capacity = roomInfo?.capacity || 10;
          const occupiedSlots = new Set(newMapping.values());

          for (let i = 0; i < capacity; i++) {
            if (!occupiedSlots.has(i)) {
              newMapping.set(player.userId, i);
              break;
            }
          }
        }
      });

      // 퇴장한 플레이어의 매핑 제거
      const currentUserIds = new Set(players.map(p => p.userId));
      Array.from(newMapping.keys()).forEach(userId => {
        if (!currentUserIds.has(userId)) {
          newMapping.delete(userId);
        }
      });

      return newMapping;
    });
  }, [players, roomInfo?.capacity]);

  useEffect(() => {
    if (!roomId) return;

    const setupConnection = async () => {
      try {
        await connectWebSocket();

        // Receipt ID 생성
        const receiptId = `receipt-sub-${roomId}-${Date.now()}`;

        // Receipt를 기다리는 Promise 생성
        const receiptPromise = new Promise<void>(resolve => {
          client.watchForReceipt(receiptId, () => {
            console.log(`Subscription confirmed via receipt ${receiptId}`);
            resolve();
          });
        });

        // 웹소켓 메시지 핸들러 - 메시지 타입별로 상태 업데이트
        unsubscribe.current = subscribeRoom(
          roomId,
          message => {
            handleRoomMessage(message, setRoomInfo, setPlayers);
          },
          { receipt: receiptId },
        );

        await receiptPromise;

        publishMessage("/app/room/join", { roomId });
      } catch (error) {
        console.error("Connection failed:", error);
        toast.error("대기방 연결에 실패했습니다.");
      }
    };

    setupConnection();

    return () => {
      if (unsubscribe.current) {
        console.log("unsubscribe");
        unsubscribe.current();
      }
      disconnectWebSocket();
    };
  }, [roomId]);

  return (
    <div className="scrollbar-hide flex h-[calc(100vh-8rem)] flex-col p-2 pt-6">
      <div className="mb-4 flex-none">
        <div className="flex items-center gap-2">
          <div className="flex-1">
            <h1 className="text-lg font-semibold">
              {roomInfo?.roomTitle || "대기방"}
            </h1>
            <p className="text-sm text-gray-500">Room ID: {roomId}</p>
          </div>
        </div>
      </div>

      {/* 플레이어 목록 */}
      <section className="flex-1">
        <h2 className="bg-background sticky top-0 z-10 mb-3 py-2 text-sm font-semibold">
          참가자 ({players.length}/{roomInfo?.capacity || 0}명)
        </h2>
        <PlayerList slots={playerSlots} capacity={roomInfo?.capacity || 10} />
      </section>

      {/* 버튼 */}
      <div className="bg-background sticky bottom-0 flex-none">
        <div className="flex h-10 items-center gap-2">
          <Button
            variant="gray"
            className="text-md h-full w-28"
            onClick={() => {
              navigate(ROUTE_PATHS.ROOM_LIST);
            }}
          >
            나가기
          </Button>
          {isHost ? (
            <Button
              className="text-md h-full flex-1"
              onClick={() => {
                // 게임 시작 메시지 전송
                publishMessage("/app/game/start", { roomId });
              }}
            >
              <Play className="mr-2 h-4 w-4" />
              게임 시작
            </Button>
          ) : (
            <Button
              className="text-md h-full flex-1"
              onClick={() => {
                // 준비 상태 토글 메시지 전송
                publishMessage("/app/room/ready", { roomId });
              }}
            >
              준비
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
