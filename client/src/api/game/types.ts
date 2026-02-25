// 게임 초기 정보
export interface QuizInfo {
  currentQuizIndex: number;
  totalQuizCount: number;
}

// 게임 플레이어 상태
export type PlayerStatus = "ALIVE" | "DEAD";
export type QuizResult = "CORRECT" | "WRONG" | "NO_SUBMISSION" | "SKIPPED_DEAD";

export interface GamePlayer {
  userId: number;
  nickname: string;
  status: PlayerStatus;
  quizResult: QuizResult;
  score: number;
}

// 게임 종료 이유
export type GameEndReason = "ONE_SURVIVOR" | "ALL_DEAD" | "QUIZ_END";

// 게임 랭킹
export interface GameRanking {
  rank: number;
  userId: number;
  nickname: string;
  score: number;
}

// 게임 상태
export interface GameState {
  roomId: string;
  gameId: number;
  quiz: QuizInfo;
}

// 퀴즈에 대한 선지
export interface QuizChoice {
  number: number;
  text: string;
}

// 개별 퀴즈 상세 정보
export interface QuizDetail {
  quizId: number;
  currentQuizIndex: number;
  totalQuizCount: number;
  startTime: number;
  endTime: number;
  question: string;
  choices: QuizChoice[];
}

// STOMP 응답 메시지
export interface QuizStartMessage {
  type: "QUIZ_START";
  roomId: string;
  gameId: number;
  occurredAt: number;
  payload: {
    quiz: QuizDetail;
  };
}

export interface ChoiceSubmitMessage {
  type: "CHOICE_SUBMIT";
  roomId: string;
  gameId: number;
  occurredAt: number;
  payload: {
    quiz: QuizInfo & { quizId: number };
    actor: {
      userId: number;
      nickname: string;
    };
    choiceNumber: number;
  };
}

export interface QuizEndMessage {
  type: "QUIZ_END";
  roomId: string;
  gameId: number;
  occurredAt: number;
  payload: {
    quiz: QuizInfo & { quizId: number };
    answer: {
      correctChoiceNumber: number;
      explanation: string;
    };
    players: GamePlayer[];
  };
}

export interface GameEndMessage {
  type: "GAME_END";
  roomId: string;
  gameId: number;
  occurredAt: number;
  payload: {
    reason: GameEndReason;
    rankings: GameRanking[];
  };
}

export type GameWebSocketMessage =
  | QuizStartMessage
  | ChoiceSubmitMessage
  | QuizEndMessage
  | GameEndMessage;
