import type { Player } from "@/api/room/types";
import { MAX_CAPACITY } from "@/constants/room";
import { Avatar, AvatarBadge, AvatarFallback } from "@/ui/avatar";
import { Badge } from "@/ui/badge";
import { Crown, User } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

export type PlayerSlot = {
  slotIndex: number;
  player: Player | null;
};

/**
 * 대기방의 플레이어 목록
 * @param players 플레이어 목록
 * @param capacity 방 정원
 * @returns
 */
interface PlayerListProps {
  players: Player[];
  capacity: number;
}
export default function PlayerList({
  players,
  capacity = MAX_CAPACITY,
}: PlayerListProps) {
  // userId -> slotIndex 매핑 (플레이어를 특정 슬롯에 고정)
  const [playerSlotMapping, setPlayerSlotMapping] = useState<
    Map<number, number>
  >(new Map());

  // 슬롯 배열 생성 (capacity 크기, 각 슬롯에 플레이어 또는 null)
  const playerSlots = useMemo<PlayerSlot[]>(() => {
    const playerSlots: PlayerSlot[] = Array.from(
      { length: capacity },
      (_, i) => ({
        slotIndex: i,
        player: null,
      }),
    );

    // 매핑된 슬롯에 플레이어 배치
    players.forEach(player => {
      const slotIndex = playerSlotMapping.get(player.userId);
      if (slotIndex !== undefined && slotIndex < capacity) {
        playerSlots[slotIndex].player = player;
      }
    });

    return playerSlots;
  }, [players, playerSlotMapping, capacity]);

  // 플레이어 변경 시 슬롯 매핑 업데이트
  useEffect(() => {
    setPlayerSlotMapping(prevMapping => {
      const newMapping = new Map(prevMapping);

      // 새로 입장한 플레이어를 빈 슬롯에 할당
      players.forEach(player => {
        if (!newMapping.has(player.userId)) {
          // 빈 슬롯 찾기 (가장 앞쪽부터)
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
  }, [players, capacity]);

  return (
    <div
      className="space-y-2"
      style={{
        // capacity 기준으로 높이 고정 (각 슬롯 높이 약 56px + gap 8px)
        minHeight: `${capacity * 64}px`,
      }}
    >
      {playerSlots.map(slot => {
        const player = slot.player;

        // 빈 슬롯
        if (!player) {
          return (
            <div
              key={`slot-${slot.slotIndex}`}
              className="flex items-center justify-between rounded-lg border border-dashed border-gray-300 bg-gray-50 px-3 py-2"
            >
              <div className="flex items-center gap-3">
                <Avatar>
                  <AvatarFallback className="bg-gray-200 text-gray-400">
                    <User className="h-4 w-4" />
                  </AvatarFallback>
                </Avatar>
                <span className="text-sm text-gray-400">빈 자리</span>
              </div>
            </div>
          );
        }

        // 플레이어가 있는 슬롯
        return (
          <div
            key={`slot-${slot.slotIndex}-${player.userId}`}
            className="flex items-center justify-between rounded-lg border bg-white px-3 py-2 shadow-2xs"
          >
            <div className="flex items-center gap-3">
              <Avatar className="overflow-visible">
                <AvatarFallback className="bg-accent text-primary border font-semibold">
                  {player.nickname.charAt(0).toUpperCase()}
                </AvatarFallback>
                {player.role === "HOST" && (
                  <AvatarBadge className="flex items-center justify-center border-0">
                    <Crown className="fill-yellow-500 text-yellow-500" />
                  </AvatarBadge>
                )}
              </Avatar>
              <span className="flex items-center gap-1 text-sm font-medium">
                {player.nickname}
              </span>
            </div>
            {player.role === "GUEST" && player.status === "READY" && (
              <Badge>READY</Badge>
            )}
          </div>
        );
      })}
    </div>
  );
}
