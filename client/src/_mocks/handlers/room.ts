import { http, HttpResponse } from "msw";

export const roomHandlers = [
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
