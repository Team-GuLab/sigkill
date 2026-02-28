// 공통 STOMP 브로드캐스트 메시지 기본 타입
interface GameBroadcastMessage<T extends string, P> {
  type: T;
  roomId: string;
  gameId: number;
  occurredAt: number;
  payload: P;
}

// 게임 초기 정보
export interface QuizInfo {
  currentQuizIndex: number;
  totalQuizCount: number;
}

// 게임 플레이어 상태
export type PlayerStatus = "ALIVE" | "DEAD";
export type QuizResult =
  | "CORRECT"
  | "WRONG"
  | "NO_SUBMISSION"
  | "SKIPPED_DEAD"
  | "NONE";

export interface GamePlayer {
  userId: number;
  nickname: string;
  status: PlayerStatus;
  quizResult: QuizResult;
  score: number;
}

// 게임 종료 이유
export type GameEndReason = "ONE_SURVIVOR" | "ALL_DEAD" | "QUIZ_EXHAUSTED";

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

// 플레이어별 게임 초기화 완료 상태
export interface GameLoadedPlayer {
  userId: number;
  nickname: string;
  isLoaded: boolean;
}

// STOMP 응답 메시지
export type QuizStartMessage = GameBroadcastMessage<
  "QUIZ_START",
  { quiz: QuizDetail }
>;

export type ChoiceSubmitMessage = GameBroadcastMessage<
  "CHOICE_SUBMIT",
  {
    quiz: QuizInfo & { quizId: number };
    actor: {
      userId: number;
      nickname: string;
    };
    choiceNumber: number;
  }
>;

export interface QuizAnswer {
  correctChoiceNumber: number;
  explanation: string;
}

export type QuizEndMessage = GameBroadcastMessage<
  "QUIZ_END",
  {
    quiz: QuizInfo & { quizId: number };
    answer: QuizAnswer;
    players: GamePlayer[];
  }
>;

export type GameEndMessage = GameBroadcastMessage<
  "GAME_END",
  {
    reason: GameEndReason;
    rankings: GameRanking[];
  }
>;

export type GameLoadedMessage = GameBroadcastMessage<
  "GAME_LOADED",
  {
    players: GameLoadedPlayer[];
    allLoaded: boolean;
  }
>;

export type GameWebSocketMessage =
  | QuizStartMessage
  | ChoiceSubmitMessage
  | QuizEndMessage
  | GameEndMessage
  | GameLoadedMessage;
