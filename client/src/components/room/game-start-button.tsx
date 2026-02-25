import { publishMessage } from "@/app/config/web-socket-client";
import { Button } from "@/ui/button";
import { Play } from "lucide-react";
import { toast } from "sonner";
import type { Player } from "@/api/room/types";

interface GameStartButtonProps {
  roomId: string | undefined;
  players: Player[];
  canStart: boolean;
}

export default function GameStartButton({
  roomId,
  players,
  canStart,
}: GameStartButtonProps) {
  const handleGameStart = () => {
    if (!roomId) return;

    // 게스트가 없는 경우
    const guests = players.filter(p => p.role === "GUEST");
    if (guests.length === 0) {
      toast.error("게임을 시작하려면 최소 1명의 참가자가 필요합니다.");
      return;
    }

    // 모든 게스트가 READY 상태가 아닌 경우
    if (!canStart) {
      toast.error(`모든 참가자가 준비되어야 합니다.`);
      return;
    }

    publishMessage("/app/room/start", { roomId });
  };

  return (
    <Button
      className="text-md h-full flex-1"
      onClick={handleGameStart}
      disabled={!canStart}
    >
      <Play className="mr-2 h-4 w-4" />
      게임 시작
    </Button>
  );
}
