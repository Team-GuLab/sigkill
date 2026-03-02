import {
  useGameEnd,
  useGameQuizEndAnswer,
  useGameQuizEndPlayerResults,
} from "@/store/game-store";
import { useUser } from "@/store/user-store";
import GameAlert from "./game-alert";
import type { QuizResult } from "@/api/game/types";

const QUIZ_RESULT_ALERT_MAP = {
  CORRECT: {
    title: "정답!",
    description: "잘하셨어요:)",
    variant: "default" as const,
  },
  WRONG: {
    title: "오답",
    description: "아쉽게도 오답이에요. 지금부터 관전은 가능해요.",
    variant: "destructive" as const,
  },
  NO_SUBMISSION: {
    title: "미제출",
    description: "시간 내에 답변을 제출하지 않았어요.",
    variant: "warning" as const,
  },
} satisfies Record<
  Exclude<QuizResult, "NONE" | "SKIPPED_DEAD">,
  {
    title: string;
    description: string;
    variant: "default" | "destructive" | "warning";
  }
>;

/**
 * 정답/오답/미제출 등의 퀴즈 결과를 알림
 * @returns 퀴즈 결과 알림 얼럿
 */
export default function QuizResultAlert() {
  const quizEndAnswer = useGameQuizEndAnswer();
  const quizEndPlayerResults = useGameQuizEndPlayerResults();
  const gameEnd = useGameEnd();
  const user = useUser();

  if (!quizEndAnswer || !user || gameEnd) return null;

  const myResult = quizEndPlayerResults.find(
    p => p.userId === user.userId,
  )?.quizResult;

  if (!myResult || !(myResult in QUIZ_RESULT_ALERT_MAP)) return null;

  const alert =
    QUIZ_RESULT_ALERT_MAP[myResult as keyof typeof QUIZ_RESULT_ALERT_MAP];

  return <GameAlert {...alert} />;
}
