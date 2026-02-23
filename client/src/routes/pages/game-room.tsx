import { Progress } from "@/ui/progress";
import { Button } from "@/ui/button";
import { Skull } from "lucide-react";
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
    <div className="flex h-screen flex-col">
      {/* 퀴즈 출제 배너 */}
      <section className="bg-linear-to-r from-pink-500 to-purple-600 px-4 py-3 text-white shadow-md">
        <div className="mx-auto max-w-3xl">
          <div className="mb-2 flex items-center justify-between text-sm font-medium">
            <span className="line-clamp-1">
              {currentQuiz?.question ?? "퀴즈를 기다리는 중..."}
            </span>
            <span className="text-white/90">
              {currentQuiz?.currentQuizIndex ??
                (quiz ? quiz.currentQuizIndex + 1 : 1)}
              /{currentQuiz?.totalQuizCount ?? quiz?.totalQuizCount ?? "?"}
            </span>
          </div>
          {/* 타이머 게이지 */}
          <Progress className="h-2 bg-white" />
        </div>
      </section>

      {/* 플레이어 캐릭터 영역 */}
      <div className="flex flex-1 items-center justify-center px-4">
        {currentQuiz ? (
          <div className="w-full max-w-2xl text-center">
            <h2 className="mb-8 text-2xl font-bold text-gray-800">
              {currentQuiz.question}
            </h2>
          </div>
        ) : (
          <div className="grid grid-cols-5 gap-4">
            {players.length > 0 &&
              players.map(player => (
                <div
                  key={player.userId}
                  className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-200 shadow-md"
                >
                  {player.status === "DEAD" ? (
                    <Skull className="h-8 w-8 text-gray-600" />
                  ) : (
                    <div className="h-8 w-8 rounded-full bg-gray-300" />
                  )}
                </div>
              ))}
          </div>
        )}
      </div>

      {/* 선지 */}
      <div className="space-y-3 bg-white pt-4 pb-6 shadow-inner">
        {currentQuiz?.choices.map(choice => (
          <Button
            key={choice.number}
            variant="outline"
            className="h-14 w-full border-2 border-slate-300 bg-slate-50 text-left text-lg font-medium text-slate-700 hover:border-purple-500 hover:bg-purple-50"
          >
            {choice.number}. {choice.text}
          </Button>
        ))}
      </div>
    </div>
  );
}
