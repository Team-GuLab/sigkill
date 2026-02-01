import { http, HttpResponse } from "msw";

// 더미 방 데이터 생성
const generateMockRooms = (page: number, size: number) => {
  const totalElements = 23;
  const startIndex = page * size;
  const rooms = [];

  for (let i = 0; i < size && startIndex + i < totalElements; i++) {
    const roomId = startIndex + i + 1;
    const isPlaying = Math.random() > 0.7;
    const playerCount = Math.floor(Math.random() * 10) + 1;

    rooms.push({
      roomId,
      title: `${["JavaScript", "TypeScript", "React", "Node.js", "Python"][Math.floor(Math.random() * 5)]} 퀴즈 ${roomId}`,
      playerCount: playerCount,
      capacity: 10,
      status: isPlaying ? "PLAYING" : "WAITING",
    });
  }

  return rooms;
};

export const roomHandlers = [
  // 방 목록 조회 API
  http.get("/api/v1/rooms", ({ request }) => {
    const url = new URL(request.url);
    const mode = url.searchParams.get("mode");

    if (mode === "network-error") {
      return HttpResponse.error();
    }

    const page = parseInt(url.searchParams.get("page") || "0", 10);
    const size = parseInt(url.searchParams.get("size") || "6", 10);

    const totalElements = 23;
    const totalPages = Math.ceil(totalElements / size);
    const rooms = generateMockRooms(page, size);

    return HttpResponse.json({
      timeStamp: new Date().toISOString(),
      code: "COMMON001",
      message: "요청이 성공적으로 처리되었습니다.",
      result: {
        rooms,
        page,
        size,
        totalElements,
        totalPages,
        hasNext: page < totalPages - 1,
      },
    });
  }),

  http.post("/api/v1/rooms", () => {
    return HttpResponse.json(
      {
        timeStamp: "2026-01-29T14:23:45.123+09:00",
        code: "COMMON001",
        message: "요청이 성공적으로 처리되었습니다.",
        result: {
          room_id: 1234,
          room_title: "JavaScript 퀴즈",
          player_count: 0,
          capacity: 10,
          status: "WAITING",
          ws: {
            endpoint: "/ws/rooms/1234",
            protocol: "websocket",
            message_format: "json",
          },
        },
      },
      { status: 201 },
    );
  }),
];
