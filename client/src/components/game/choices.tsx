import { useGameQuiz } from "@/store/game-store";
import { Button } from "@/ui/button";

export default function Choices() {
  const choices = useGameQuiz()?.choices ?? [];

  return (
    <>
      {choices.map(choice => (
        <Button
          key={choice.number}
          variant="outline"
          className="h-12 border-2 border-slate-300 bg-slate-50 text-sm font-medium text-slate-700 hover:border-purple-500 hover:bg-purple-50"
        >
          {choice.number}. {choice.text}
        </Button>
      ))}
    </>
  );
}
