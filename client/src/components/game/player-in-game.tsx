import type { GamePlayer, QuizResult } from "@/api/game/types";
import SlimeIcon from "./slime-icon";
import GhostIcon from "./ghost-icon";
import SpeechBubble from "./speech-bubble";

interface PlayerInGameProps {
  player: GamePlayer;
  quizResult?: QuizResult;
  submittedChoice?: number;
}
/**
 * 인게임 플레이어
 * @param player - 플레이어 상태
 * @param quizResult - 퀴즈 단건 결과
 * @param submittedChoice - 제출한 선지 번호
 * @returns
 */
export default function PlayerInGame({
  player,
  quizResult,
  submittedChoice,
}: PlayerInGameProps) {
  const quizResultAnimation =
    quizResult === "CORRECT"
      ? "animate-bounce"
      : quizResult === "WRONG" || quizResult === "NO_SUBMISSION"
        ? "animate-blink"
        : "";

  return (
    <div className={`relative ${quizResultAnimation}`}>
      {player.status === "DEAD" ? (
        <GhostIcon className="h-14 w-14" />
      ) : (
        <SlimeIcon className="h-14 w-14" />
      )}
      {submittedChoice !== undefined && (
        <div className="absolute -right-3 bottom-0">
          <SpeechBubble choiceNumber={submittedChoice} />
        </div>
      )}
    </div>
  );
}
