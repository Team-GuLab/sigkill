// tanstack 쿼리키 팩토리
export const QUERY_KEYS = {
  // 방
  room: {
    all: ["rooms"],
    list: (params?: { page?: number; size?: number }) => [
      ...QUERY_KEYS.room.all,
      "list",
      params,
    ],
  } as const,
};
