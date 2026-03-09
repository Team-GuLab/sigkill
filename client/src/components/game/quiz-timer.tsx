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
  }, [quiz?.quizId]);

  if (!quiz) return null;

  const totalDuration = quiz.endTime - quiz.startTime;
  const remainingPercent = Math.min(100, (remainingMs / totalDuration) * 100);
  const remainingSec = remainingMs / 1000;
  const displayTime = remainingSec.toFixed(1);

  // 남은 시간에 따른 색상 전환
  const gaugeGradient =
    remainingSec <= 3
      ? "from-red-600 via-red-400 to-red-300"
      : remainingSec <= 5
        ? "from-purple-700 via-purple-500 to-purple-300"
        : "from-blue-700 via-blue-500 to-blue-300";

  const labelColor =
    remainingSec <= 3
      ? "text-red-500"
      : remainingSec <= 5
        ? "text-purple-500"
        : "text-blue-500";

  return (
    <div className="relative h-7 w-full">
      {/* 배경 트랙 */}
      <div className="absolute top-1/2 h-4 w-full -translate-y-1/2 overflow-hidden rounded-full bg-gray-200 shadow-inner" />

      {/* 액체 게이지 */}
      <div
        className={`absolute top-1/2 left-0 h-4 -translate-y-1/2 overflow-hidden rounded-full bg-gradient-to-r transition-colors duration-300 ${gaugeGradient}`}
        style={{ width: `${remainingPercent}%` }}
      >
        {/* 상단 광택 - 액체 느낌 */}
        <div className="absolute inset-x-0 top-0 h-2 rounded-full bg-gradient-to-b from-white/40 to-transparent" />
        {/* 하단 반사 */}
        <div className="absolute inset-x-1 bottom-0.5 h-1 rounded-full bg-white/10" />
      </div>

      {/* 남은 시간 레이블 */}
      <span
        className={`absolute top-1/2 -translate-y-1/2 pl-1 text-xs font-bold whitespace-nowrap transition-colors duration-300 ${labelColor}`}
        style={{ left: `${remainingPercent}%` }}
      >
        {displayTime}s
      </span>
    </div>
  );
}
