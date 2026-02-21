import { publishMessage } from "@/app/config/web-socket-client";
import { Button } from "@/ui/button";
import { Play } from "lucide-react";

export default function GameStartButton({
  roomId,
}: {
  roomId: string | undefined;
}) {
  return (
    <Button
      className="text-md h-full flex-1"
      onClick={() => {
        if (!roomId) return;
        publishMessage("/app/game/start", { roomId });
      }}
    >
      <Play className="mr-2 h-4 w-4" />
      게임 시작
    </Button>
  );
}
