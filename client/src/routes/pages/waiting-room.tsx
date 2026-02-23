import { useEffect, useState, useMemo } from "react";
import { useLocation, useNavigate, useParams } from "react-router";
import type { Player } from "@/api/room/types";
import PlayerList from "@/components/room/player-list";
import { Button } from "@/ui/button";
import { ROUTE_PATHS } from "@/routes/paths";
import { MAX_CAPACITY } from "@/constants/room";
import { useUser } from "@/store/user-store";
import GameStartButton from "@/components/room/game-start-button";
import ReadyButton from "@/components/room/ready-button";
import { useRoomSocket } from "@/hooks/room/use-room-socket";
import { WaitingRoomSkeleton } from "@/components/room/waiting-room-skeleton";

export type PlayerSlot = {
  slotIndex: number;
  player: Player | null;
};

export default function WaitingRoom() {
  const { roomId } = useParams<{ roomId: string }>();

  const location = useLocation();
  const state = location.state as { players: Player[] } | null;
  const initialPlayers = state?.players ?? [];

  const navigate = useNavigate();

  const user = useUser();
  const myUserId = user!.userId;

  const { roomInfo, players, isPending } = useRoomSocket({
    roomId,
    myUserId,
    initialPlayers,
  });

  // userId -> slotIndex 매핑 (플레이어를 특정 슬롯에 고정)
  const [playerSlotMapping, setPlayerSlotMapping] = useState<
    Map<number, number>
  >(new Map());

  // 슬롯 배열 생성 (capacity 크기, 각 슬롯에 플레이어 또는 null)
  const playerSlots = useMemo<PlayerSlot[]>(() => {
    const capacity = roomInfo?.capacity || MAX_CAPACITY;
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
          const capacity = roomInfo?.capacity || MAX_CAPACITY;
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

  const isHost = myUserId
    ? players.find(p => p.userId === myUserId)?.role === "HOST"
    : false;

  const myReadyStatus = myUserId
    ? players.find(p => p.userId === myUserId)?.status === "READY"
    : false;

  // 모든 게스트가 READY 상태인지 확인
  const isAllGuestsReady = useMemo(() => {
    const guests = players.filter(p => p.role === "GUEST");
    if (guests.length === 0) return false;
    return guests.every(p => p.status === "READY");
  }, [players]);

  if (isPending) {
    return <WaitingRoomSkeleton />;
  }

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
        <PlayerList
          slots={playerSlots}
          capacity={roomInfo?.capacity || MAX_CAPACITY}
        />
      </section>

      {/* 버튼 */}
      <div className="bg-background sticky bottom-0 flex-none">
        <div className="flex h-10 items-center gap-2">
          <Button
            disabled={myReadyStatus === true}
            variant="gray"
            className="text-md h-full w-28"
            onClick={() => {
              navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
            }}
          >
            나가기
          </Button>
          {isHost ? (
            <GameStartButton
              roomId={roomId}
              players={players}
              canStart={isAllGuestsReady}
            />
          ) : (
            <ReadyButton roomId={roomId} myReadyStatus={myReadyStatus} />
          )}
        </div>
      </div>
    </div>
  );
}
