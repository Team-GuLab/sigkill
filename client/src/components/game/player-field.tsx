import {
  useGameChoiceSubmits,
  useGamePlayers,
  useGameQuizEndAnswer,
  useGameQuizEndPlayerResults,
} from "@/store/game-store";
import PlayerInGame from "./player-in-game";
import SpeechBubble from "./speech-bubble";
import { useUser } from "@/store/user-store";

export default function PlayerField() {
  const user = useUser();
  const players = useGamePlayers();
  const choiceSubmits = useGameChoiceSubmits();
  const quizEndAnswer = useGameQuizEndAnswer();
  const quizEndPlayerResults = useGameQuizEndPlayerResults();

  if (players.length === 0) {
    return (
      <div className="flex items-center justify-center">
        <p className="text-gray-500">플레이어 입장을 기다리는 중...</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-3 gap-12">
      {players.map(player => {
        const choiceNumber = choiceSubmits[player.userId];

        const quizResult = quizEndAnswer
          ? quizEndPlayerResults.find(p => p.userId === player.userId)
              ?.quizResult
          : undefined;

        return (
          <div key={player.userId} className="flex flex-col items-center">
            <div
              className={`mb-2 ${choiceNumber === undefined && "invisible"}`}
            >
              <SpeechBubble choiceNumber={choiceNumber ?? 0} />
            </div>
            <PlayerInGame player={player} quizResult={quizResult} />
            <p
              className={`mt-2 text-xs font-medium ${player.userId === user?.userId && "font-semibold underline underline-offset-4"}`}
            >
              {player.nickname}
            </p>
          </div>
        );
      })}
    </div>
  );
}
