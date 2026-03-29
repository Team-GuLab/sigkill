import { publishMessage } from "@/app/config/web-socket-client";
import { Button } from "@/ui/button";
import { Bot } from "lucide-react";

interface AddBotButtonProps {
  roomId: string | undefined;
}

export default function AddBotButton({ roomId }: AddBotButtonProps) {
  const handleAddBot = () => {
    if (!roomId) return;
    publishMessage("/app/room/bot", { roomId });
  };

  return (
    <Button
      variant="ghost"
      size="sm"
      className="border-secondary h-6 bg-stone-300 p-2 text-xs text-stone-700"
      onClick={handleAddBot}
    >
      <Bot className="h-3.5 w-3.5" />봇 생성
    </Button>
  );
}
