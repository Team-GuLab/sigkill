import type { PlayerSlot } from "@/routes/pages/waiting-room";
import { Avatar, AvatarBadge, AvatarFallback } from "@/ui/avatar";
import { Badge } from "@/ui/badge";
import { Crown, User } from "lucide-react";

/**
 * 대기방의 플레이어 목록
 * @param slots 플레이어 슬롯 배열
 * @param capacity 방 정원
 * @returns
 */
export default function PlayerList({
  slots,
  capacity,
}: {
  slots: PlayerSlot[];
  capacity: number;
}) {
  return (
    <div
      className="space-y-2"
      style={{
        // capacity 기준으로 높이 고정 (각 슬롯 높이 약 56px + gap 8px)
        minHeight: `${capacity * 64}px`,
      }}
    >
      {slots.map(slot => {
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
