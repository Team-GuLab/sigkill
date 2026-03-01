import { useEffect, useState } from "react";
import { Check, X } from "lucide-react";
import { publishMessage } from "@/app/config/web-socket-client";
import {
  useGameId,
  useGamePlayers,
  useGameQuiz,
  useGameQuizEndAnswer,
} from "@/store/game-store";
import { useUser } from "@/store/user-store";
import { Button } from "@/ui/button";

export default function Choices() {
  const quiz = useGameQuiz();
  const gameId = useGameId();
  const quizEndAnswer = useGameQuizEndAnswer();
  const players = useGamePlayers();
  const user = useUser();
  const [selectedChoiceNumber, setSelectedChoiceNumber] = useState<
    number | null
  >(null);

  const isCurrentPlayerDead =
    players.find(p => p.userId === user?.userId)?.status === "DEAD";

  useEffect(() => {
    setSelectedChoiceNumber(null);
  }, [quiz?.quizId]);

  const handleChoiceClick = (clickedNumber: number) => {
    if (!gameId || !quiz || quizEndAnswer || isCurrentPlayerDead) return;
    setSelectedChoiceNumber(clickedNumber);
    publishMessage("/app/game/submit", {
      gameId,
      quizId: quiz.quizId,
      choiceNumber: clickedNumber,
    });
  };

  const getChoiceClassName = (number: number) => {
    if (!quizEndAnswer) {
      return selectedChoiceNumber === number
        ? "border-primary bg-primary hover:bg-primary h-12 border-2 text-sm font-medium text-white hover:text-white"
        : "h-12 border-2 border-slate-300 bg-slate-50 text-sm font-medium text-slate-700 hover:border-purple-500 hover:bg-purple-50";
    }

    if (number === quizEndAnswer.correctChoiceNumber) {
      return "h-12 border-2 border-green-500 bg-green-50 text-sm font-medium text-green-700";
    }
    if (number === selectedChoiceNumber) {
      return "h-12 border-2 border-red-500 bg-red-50 text-sm font-medium text-red-700";
    }
    return "h-12 border-2 border-slate-200 bg-slate-50 text-sm font-medium text-slate-400";
  };

  return (
    <>
      {quiz?.choices.map(choice => (
        <Button
          key={choice.number}
          variant="outline"
          className={`relative ${getChoiceClassName(choice.number)}`}
          disabled={!!quizEndAnswer || isCurrentPlayerDead}
          onClick={() => handleChoiceClick(choice.number)}
        >
          {choice.number}. {choice.text}
          {quizEndAnswer &&
            choice.number === quizEndAnswer.correctChoiceNumber && (
              <span className="absolute right-3 text-green-600">
                <Check size={18} strokeWidth={2.5} />
              </span>
            )}
          {quizEndAnswer &&
            choice.number === selectedChoiceNumber &&
            choice.number !== quizEndAnswer.correctChoiceNumber && (
              <span className="absolute right-3 text-red-600">
                <X size={18} strokeWidth={2.5} />
              </span>
            )}
        </Button>
      ))}
    </>
  );
}
