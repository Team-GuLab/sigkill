import { Progress } from "@/ui/progress";
import { Button } from "@/ui/button";
import { Dog, Ghost } from "lucide-react";
import { useGameId, useGameQuiz, useGamePlayers } from "@/store/game-store";
import { useGameSocket } from "@/hooks/game/use-game-socket";
import type { QuizDetail } from "@/api/game/types";

export default function GameRoom() {
  const gameId = useGameId();
  const quiz = useGameQuiz();
  const players = useGamePlayers();

  // question 필드 존재 여부로 QuizDetail narrowing
  const currentQuiz = quiz && "question" in quiz ? (quiz as QuizDetail) : null;

  useGameSocket({ gameId: gameId ?? 0 });

  if (!gameId) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="text-gray-500">게임 데이터를 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col pt-8">
      {/* 퀴즈 출제 배너 */}
      <section className="mx-auto flex w-full flex-col gap-4">
        <div className="shrink-0 rounded-sm bg-linear-to-r from-pink-500 to-purple-600 px-4 py-3 text-white">
          <div className="mb-2 flex flex-col justify-between gap-2 text-sm font-medium">
            <span className="justify-end text-white/90">
              {currentQuiz?.currentQuizIndex ??
                (quiz ? quiz.currentQuizIndex + 1 : 1)}
              /{currentQuiz?.totalQuizCount ?? quiz?.totalQuizCount ?? "?"}
            </span>
            <div className="flex max-h-20 flex-col overflow-auto">
              <span className="flex justify-center">
                {currentQuiz?.question ?? "퀴즈를 기다리는 중..."}
              </span>
            </div>
          </div>
        </div>

        {/* 타이머 게이지 */}
        <Progress className="h-3 bg-white" />
      </section>

      {/* 플레이어 캐릭터 영역 */}
      <section className="flex flex-1 items-center justify-center overflow-hidden py-6">
        <div className="grid grid-cols-3 gap-12">
          {players.length > 0 &&
            players.map(player => (
              <div key={player.userId}>
                {player.status === "DEAD" ? (
                  <Ghost className="h-12 w-12" />
                ) : (
                  <Dog className="h-12 w-12 text-stone-800" />
                )}
              </div>
            ))}
        </div>
      </section>

      {/* 선지 */}
      <section className="my-2 flex shrink-0 flex-col space-y-2">
        {currentQuiz?.choices.map(choice => (
          <Button
            key={choice.number}
            variant="outline"
            className="h-12 border-2 border-slate-300 bg-slate-50 text-sm font-medium text-slate-700 hover:border-purple-500 hover:bg-purple-50"
          >
            {choice.number}. {choice.text}
          </Button>
        ))}
      </section>
    </div>
  );
}
