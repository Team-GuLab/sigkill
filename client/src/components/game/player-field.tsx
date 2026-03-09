import {
  useGameChoiceSubmits,
  useGamePlayers,
  useGameQuizEndAnswer,
  useGameQuizEndPlayerResults,
} from "@/store/game-store";
import PlayerInGame from "./player-in-game";
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
    <div className="grid grid-cols-3 gap-6 md:grid-cols-4 lg:grid-cols-6">
      {players.map(player => {
        const choiceNumber = choiceSubmits[player.userId];

        const quizResult = quizEndAnswer
          ? quizEndPlayerResults.find(p => p.userId === player.userId)
              ?.quizResult
          : undefined;

        return (
          <div key={player.userId} className="flex flex-col items-center">
            <PlayerInGame
              player={player}
              quizResult={quizResult}
              submittedChoice={choiceNumber}
            />
            <p
              className={`mt-2 max-w-14 text-center text-xs font-medium break-keep whitespace-pre-wrap ${player.userId === user?.userId && "font-semibold underline underline-offset-3"}`}
            >
              {player.nickname}
            </p>
          </div>
        );
      })}
    </div>
  );
}
