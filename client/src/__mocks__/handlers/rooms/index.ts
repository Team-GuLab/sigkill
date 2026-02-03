import { http, HttpResponse } from "msw";
import type { RoomItem } from "@/api/room/types";

// 모듈 레벨 캐시: 생성된 방 데이터를 저장하여 핸들러 간 공유
let mockRoomsCache: RoomItem[] | null = null;
const TOTAL_ELEMENTS = 23;

// 전체 방 데이터를 한 번만 생성하고 캐싱
const getMockRooms = (): RoomItem[] => {
  if (mockRoomsCache) {
    return mockRoomsCache;
  }

  const rooms: RoomItem[] = [];
  const topics = ["JavaScript", "TypeScript", "React", "Node.js", "Python"];

  for (let i = 0; i < TOTAL_ELEMENTS; i++) {
    const roomId = i + 1;
    const isPlaying = Math.random() > 0.7;
    // const playerCount = Math.floor(Math.random() * 10) + 1;
    const playerCount = 10;

    rooms.push({
      roomId,
      title: `${topics[Math.floor(Math.random() * topics.length)]} 퀴즈 ${roomId}`,
      playerCount,
      capacity: 10,
      status: isPlaying ? "PLAYING" : "WAITING",
    });
  }

  mockRoomsCache = rooms;
  return rooms;
};

// 페이지네이션된 방 목록 반환
const getPaginatedRooms = (page: number, size: number): RoomItem[] => {
  const allRooms = getMockRooms();
  const startIndex = page * size;
  return allRooms.slice(startIndex, startIndex + size);
};

// roomId로 방 찾기
const findRoomById = (roomId: number): RoomItem | undefined => {
  const allRooms = getMockRooms();
  return allRooms.find((room) => room.roomId === roomId);
};

export const roomHandlers = [
  // 방 목록 조회 API
  http.get("/api/v1/rooms", ({ request }) => {
    const url = new URL(request.url);

    const page = parseInt(url.searchParams.get("page") || "0", 10);
    const size = parseInt(url.searchParams.get("size") || "6", 10);

    const totalPages = Math.ceil(TOTAL_ELEMENTS / size);
    const rooms = getPaginatedRooms(page, size);

    return HttpResponse.json({
      timeStamp: new Date().toISOString(),
      code: "COMMON001",
      message: "요청이 성공적으로 처리되었습니다.",
      result: {
        rooms,
        page,
        size,
        totalElements: TOTAL_ELEMENTS,
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

  // http.post("/api/v1/rooms", () => {
  //   return HttpResponse.json(
  //     {
  //       timeStamp: "2026-01-29T14:23:45.123+09:00",
  //       code: "UNAUTHORIZED",
  //       message: "세션이 만료되었습니다.",
  //       result: null,
  //     },
  //     { status: 401 },
  //   );
  // }),

  // 방 입장 가능 여부 확인 API
  // http.get("/api/v1/rooms/:roomId/availability", ({ params }) => {
  //   return HttpResponse.error();
  // }),

  http.get("/api/v1/rooms/:roomId/availability", ({ params }) => {
    const selectedRoom = findRoomById(Number(params.roomId));

    // 방이 존재하지 않는 경우
    if (!selectedRoom) {
      return HttpResponse.json(
        {
          timeStamp: new Date().toISOString(),
          code: "ROOM001",
          message: "존재하지 않는 방입니다.",
          result: null,
        },
        { status: 404 },
      );
    }

    if (selectedRoom.status === "PLAYING") {
      return HttpResponse.json(
        {
          timeStamp: new Date().toISOString(),
          code: "ROOM002",
          message: "게임이 이미 시작된 방입니다.",
          result: null,
        },
        { status: 409 },
      );
    }

    // 입장 가능 여부: 대기 중이고 정원이 남아있어야 함
    const { playerCount, capacity } = selectedRoom;
    if (playerCount >= capacity) {
      return HttpResponse.json(
        {
          timeStamp: new Date().toISOString(),
          code: "ROOM003",
          message: "정원이 가득 찬 방입니다.",
          result: null,
        },
        { status: 409 },
      );
    }

    return HttpResponse.json({
      timeStamp: new Date().toISOString(),
      code: "COMMON001",
      message: "요청이 성공적으로 처리되었습니다.",
      result: {
        ws: {
          endpoint: "/ws/rooms/1234",
          protocol: "websocket",
          message_format: "json",
        },
      },
    });
  }),
];
