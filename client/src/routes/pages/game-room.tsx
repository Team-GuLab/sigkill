import { useGameId, useResetGame } from "@/store/game-store";
import { useResetRoom } from "@/store/room-store";
import { useGameSocket } from "@/hooks/game/use-game-socket";
import QuizBanner from "@/components/game/quiz-banner";
import QuizTimer from "@/components/game/quiz-timer";
import PlayerField from "@/components/game/player-field";
import Choices from "@/components/game/choices";
import QuizResultAlert from "@/components/game/quiz-result-alert";
import GameEndAlert from "@/components/game/game-end-alert";
import GameResultOverlay from "@/components/game/game-result-overlay";
import { useEffect } from "react";

export default function GameRoom() {
  const gameId = useGameId();
  const resetGame = useResetGame();
  const resetRoom = useResetRoom();

  useGameSocket({ gameId: gameId ?? 0 });

  useEffect(() => {
    return () => {
      resetGame();
      resetRoom();
    };
  }, [resetGame, resetRoom]);

  if (!gameId) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="text-gray-500">게임 데이터를 불러올 수 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col pt-2">
      <GameResultOverlay />
      {/* 퀴즈 출제 배너 */}
      <section className="mx-auto flex w-full flex-col gap-2">
        <QuizBanner />

        {/* 타이머 게이지 */}
        <QuizTimer />
      </section>

      {/* 플레이어 캐릭터 영역 */}
      <section className="flex flex-1 items-center justify-center overflow-hidden py-6">
        <PlayerField />
      </section>

      {/* 선지 */}
      <section className="my-2 flex shrink-0 flex-col space-y-2">
        <GameEndAlert />
        <QuizResultAlert />
        <Choices />
      </section>
    </div>
  );
}
