export const ROUTE_PATHS = Object.freeze({
  HOME: "/",
  ROOM_LIST: "/rooms",
  WAITING_ROOM: "/waiting-room/:roomId",
  GAME_ROOM: "/game/:gameId",
} as const);

export const ROUTE_GENERATORS = Object.freeze({
  WAITING_ROOM: (roomId: string | number) => `/waiting-room/${roomId}`,
  GAME_ROOM: (gameId: string | number) => `/game/${gameId}`,
} as const);
