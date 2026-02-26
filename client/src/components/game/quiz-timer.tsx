import { useEffect, useRef, useState } from "react";
import { useGameQuiz } from "@/store/game-store";

export default function QuizTimer() {
  const quiz = useGameQuiz();
  const [remainingMs, setRemainingMs] = useState(0);
  // 클라이언트 수신 시각을 기준으로 경과 시간을 계산
  const receivedAtRef = useRef<number>(0);

  useEffect(() => {
    if (!quiz) return;

    receivedAtRef.current = Date.now();

    const duration = quiz.endTime - quiz.startTime;

    const update = () => {
      const elapsed = Date.now() - receivedAtRef.current;
      setRemainingMs(Math.max(0, duration - elapsed));
    };

    update();
    const id = setInterval(update, 100);
    return () => clearInterval(id);
  }, [quiz?.startTime]);

  if (!quiz) return null;

  const totalDuration = quiz.endTime - quiz.startTime;
  const remainingPercent = Math.min(100, (remainingMs / totalDuration) * 100);
  const displayTime = (remainingMs / 1000).toFixed(1);

  return (
    <div className="relative h-6 w-full">
      <div className="absolute top-1/2 h-3 w-full -translate-y-1/2 rounded-full bg-white" />
      {/* 게이지 */}
      <div
        className="bg-primary absolute top-1/2 left-0 h-3 -translate-y-1/2 rounded-full"
        style={{ width: `${remainingPercent}%` }}
      />
      {/* 남은 시간 레이블 */}
      <span
        className="text-secondary absolute top-1/2 -translate-y-1/2 pl-1 text-xs font-bold whitespace-nowrap"
        style={{ left: `${remainingPercent}%` }}
      >
        {displayTime}s
      </span>
    </div>
  );
}
