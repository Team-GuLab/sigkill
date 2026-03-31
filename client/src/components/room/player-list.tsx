import type { Player } from "@/api/room/types";
import { MAX_CAPACITY } from "@/constants/room";
import { useUser } from "@/store/user-store";
import { Avatar, AvatarBadge, AvatarFallback } from "@/ui/avatar";
import { Badge } from "@/ui/badge";
import { Bot, Crown, User } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

export type PlayerSlotData = {
  slotIndex: number;
  player: Player | null;
};

const isBot = (player: Player) => player.nickname.startsWith("[봇]");

interface PlayerSlotProps {
  slot: PlayerSlotData;
  isMe: boolean;
}

function PlayerSlot({ slot, isMe }: PlayerSlotProps) {
  const { player } = slot;

  if (!player) {
    return (
      <div
        key={`slot-${slot.slotIndex}`}
        className="flex items-center justify-between rounded-lg border border-gray-300 bg-gray-50 px-3 py-2"
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

  const bot = isBot(player);

  return (
    <div
      className={`flex items-center justify-between rounded-lg border px-3 py-2 ${isMe ? "bg-primary/10" : "bg-white"}`}
    >
      <div className="flex items-center gap-3">
        <Avatar className="overflow-visible">
          <AvatarFallback
            className={`bg-accent text-primary border ${isMe ? "font-bold" : "font-semibold"}`}
          >
            {bot ? "봇" : player.nickname.charAt(0).toUpperCase()}
          </AvatarFallback>
          {player.role === "HOST" && (
            <AvatarBadge className="flex items-center justify-center border-0">
              <Crown className="fill-yellow-500 text-yellow-500" />
            </AvatarBadge>
          )}
          {bot && (
            <AvatarBadge className="top-0 right-0 bottom-auto flex items-center justify-center border-0">
              <Bot />
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
}

/**
 * 대기방의 플레이어 목록
 * @param players 플레이어 목록
 * @param capacity 방 정원
 */
interface PlayerListProps {
  players: Player[];
  capacity: number;
}

export default function PlayerList({
  players,
  capacity = MAX_CAPACITY,
}: PlayerListProps) {
  const user = useUser();
  // userId -> slotIndex 매핑 (플레이어를 특정 슬롯에 고정)
  const [playerSlotMapping, setPlayerSlotMapping] = useState<
    Map<number, number>
  >(new Map());

  // 슬롯 배열 생성 (capacity 크기, 각 슬롯에 플레이어 또는 null)
  const playerSlots = useMemo<PlayerSlotData[]>(() => {
    const slots: PlayerSlotData[] = Array.from(
      { length: capacity },
      (_, i) => ({
        slotIndex: i,
        player: null,
      }),
    );

    players.forEach(player => {
      const slotIndex = playerSlotMapping.get(player.userId);
      if (slotIndex !== undefined && slotIndex < capacity) {
        slots[slotIndex].player = player;
      }
    });

    return slots;
  }, [players, playerSlotMapping, capacity]);

  // 플레이어 변경 시 슬롯 매핑 업데이트
  useEffect(() => {
    setPlayerSlotMapping(prevMapping => {
      const newMapping = new Map(prevMapping);

      players.forEach(player => {
        if (!newMapping.has(player.userId)) {
          const occupiedSlots = new Set(newMapping.values());
          for (let i = 0; i < capacity; i++) {
            if (!occupiedSlots.has(i)) {
              newMapping.set(player.userId, i);
              break;
            }
          }
        }
      });

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
    <div className="space-y-2" style={{ minHeight: `${capacity * 64}px` }}>
      {playerSlots.map(slot => (
        <PlayerSlot
          key={
            slot.player
              ? `slot-${slot.slotIndex}-${slot.player.userId}`
              : `slot-${slot.slotIndex}`
          }
          slot={slot}
          isMe={user?.userId === slot.player?.userId}
        />
      ))}
    </div>
  );
}
