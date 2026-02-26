import { useEffect, useState } from "react";
import { publishMessage } from "@/app/config/web-socket-client";
import { useGameId, useGameQuiz } from "@/store/game-store";
import { Button } from "@/ui/button";

export default function Choices() {
  const quiz = useGameQuiz();
  const gameId = useGameId();
  const [selectedNumber, setSelectedNumber] = useState<number | null>(null);

  useEffect(() => {
    setSelectedNumber(null);
  }, [quiz?.quizId]);

  const handleChoiceClick = (number: number) => {
    if (!gameId || !quiz) return;
    setSelectedNumber(number);
    publishMessage("/app/game/submit", {
      gameId,
      quizId: quiz.quizId,
      choiceNumber: number,
    });
  };

  return (
    <>
      {quiz?.choices.map(choice => (
        <Button
          key={choice.number}
          variant="outline"
          className={
            selectedNumber === choice.number
              ? "border-primary bg-primary hover:bg-primary h-12 border-2 text-sm font-medium text-white hover:text-white"
              : "h-12 border-2 border-slate-300 bg-slate-50 text-sm font-medium text-slate-700 hover:border-purple-500 hover:bg-purple-50"
          }
          onClick={() => handleChoiceClick(choice.number)}
        >
          {choice.number}. {choice.text}
        </Button>
      ))}
    </>
  );
}
