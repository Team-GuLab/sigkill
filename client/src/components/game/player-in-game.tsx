import type { GamePlayer, QuizResult } from "@/api/game/types";
import { Dog, Ghost } from "lucide-react";

interface PlayerInGameProps {
  player: GamePlayer;
  quizResult?: QuizResult;
}
/**
 * 인게임 플레이어
 * @param player - 플레이어 상태
 * @param quizResult - 퀴즈 단건 결과
 * @returns
 */
export default function PlayerInGame({
  player,
  quizResult,
}: PlayerInGameProps) {
  const quizResultAnimation =
    quizResult === "CORRECT"
      ? "animate-bounce"
      : quizResult === "WRONG" || quizResult === "NO_SUBMISSION"
        ? "animate-blink"
        : "";

  return (
    <div className={quizResultAnimation}>
      {player.status === "DEAD" ? (
        <Ghost className="h-12 w-12 text-gray-500/50" />
      ) : (
        <Dog className="text-primary h-12 w-12" />
      )}
    </div>
  );
}
