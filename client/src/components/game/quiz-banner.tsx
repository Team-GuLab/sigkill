import {
  useGameAllLoaded,
  useGameEnd,
  useGameQuiz,
  useGameQuizEndAnswer,
} from "@/store/game-store";

export default function QuizBanner() {
  const quiz = useGameQuiz();
  const allLoaded = useGameAllLoaded();
  const quizEndAnswer = useGameQuizEndAnswer();
  const gameEnd = useGameEnd();

  return (
    <div className="shrink-0 rounded-sm bg-linear-to-r from-pink-500 to-purple-600 px-4 py-3 text-white">
      <div className="mb-2 flex flex-col justify-between gap-2 text-sm font-medium">
        {gameEnd ? (
          <span className="flex justify-center text-white/90">
            게임이 종료되었어요. 곧 게임 결과를 확인할 수 있어요.
          </span>
        ) : quiz ? (
          <>
            {/* 현재 퀴즈 인덱스/전체 퀴즈 개수 */}
            <span className="justify-end text-white/90">
              {quiz.currentQuizIndex}/{quiz.totalQuizCount}
            </span>
            {/* 현재 퀴즈 질문 */}
            <div className="flex max-h-20 flex-col overflow-auto">
              <span className="flex justify-center">
                {quizEndAnswer ? quizEndAnswer.explanation : quiz.question}
              </span>
            </div>
          </>
        ) : (
          <span className="justify-end text-white/90">
            {allLoaded
              ? "곧 게임이 시작됩니다."
              : "다른 플레이어를 기다리는 중입니다."}
          </span>
        )}
      </div>
    </div>
  );
}
