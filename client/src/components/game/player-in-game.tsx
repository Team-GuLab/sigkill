import type { GamePlayer, QuizResult } from "@/api/game/types";
import { Dog, Ghost } from "lucide-react";

interface PlayerInGameProps {
  player: GamePlayer;
  quizResult?: QuizResult;
}

export default function PlayerInGame({ player, quizResult }: PlayerInGameProps) {
  const animationClass =
    quizResult === "CORRECT"
      ? "animate-bounce"
      : quizResult === "WRONG"
        ? "animate-blink"
        : "";

  return (
    <div className={animationClass}>
      {player.status === "DEAD" ? (
        <Ghost className="h-12 w-12 text-gray-500/50" />
      ) : (
        <Dog className="text-primary h-12 w-12" />
      )}
    </div>
  );
}
