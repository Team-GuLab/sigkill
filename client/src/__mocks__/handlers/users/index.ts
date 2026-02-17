import { http, HttpResponse } from "msw";

// 닉네임 생성을 위한 형용사와 명사 배열
const adjectives = [
  "뽀족한",
  "귀여운",
  "용감한",
  "똑똑한",
  "빠른",
  "느긋한",
  "화난",
  "즐거운",
  "수줍은",
  "명랑한",
];

const nouns = [
  "달맞이",
  "토끼",
  "고양이",
  "강아지",
  "펭귄",
  "코알라",
  "여우",
  "사슴",
  "다람쥐",
  "햄스터",
];

// 랜덤 닉네임 생성 함수
const generateRandomNickname = (): string => {
  const adjective = adjectives[Math.floor(Math.random() * adjectives.length)];
  const noun = nouns[Math.floor(Math.random() * nouns.length)];
  return `${adjective} ${noun}`;
};

// 랜덤 사용자 ID 생성 함수 (1~10000 사이)
const generateRandomUserId = (): number => {
  return Math.floor(Math.random() * 10000) + 1;
};

export const userHandlers = [
  // 비회원 로그인 API
  http.post("/api/v1/users/guest-login", () => {
    const sessionId = Math.random().toString(36).substring(2, 15).toUpperCase();

    return HttpResponse.json(
      {
        timeStamp: new Date().toISOString(),
        code: "SUCCESS",
        message: "요청이 성공적으로 처리되었습니다.",
        result: {
          userId: generateRandomUserId(),
          nickname: generateRandomNickname(),
        },
      },
      {
        status: 200,
        headers: {
          "set-cookie": `JSESSIONID=${sessionId}; path=/; httpOnly; sameSite=Lax`,
        },
      },
    );
  }),
];
