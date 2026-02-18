export interface RoomListDto {
  rooms: RoomItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface RoomItem {
  roomId: string;
  roomTitle: string;
  playerCount: number;
  capacity: number;
  status: "WAITING" | "PLAYING";
  canJoin: boolean;
}

// 방 생성 시 필요한 데이터
export interface CreateRoomParams {
  roomTitle: RoomItem["roomTitle"];
}

export interface Player {
  userId: number;
  nickname: string;
  status: "READY" | "NOT_READY";
  role: "HOST" | "GUEST";
}

// 웹소켓 메시지 타입들
export interface PlayerJoinMessage {
  type: "PLAYER_JOIN";
  room: Omit<RoomItem, "playerCount" | "canJoin">;
  players: Player[];
}

export interface PlayerLeaveMessage {
  type: "PLAYER_LEFT";
  player: Player;
}

export interface PlayerReadyMessage {
  type: "PLAYER_READY";
  player: Player;
  allReady: boolean;
}

export interface PlayerUnreadyMessage {
  type: "PLAYER_UNREADY";
  player: Player;
}

export interface HostChangedMessage {
  type: "HOST_CHANGED";
  newHost: Player;
  oldHost: Player;
  reason: "HOST_LEFT";
}

// 모든 웹소켓 메시지 타입
export type RoomWebSocketMessage =
  | PlayerJoinMessage
  | PlayerLeaveMessage
  | PlayerReadyMessage
  | PlayerUnreadyMessage
  | HostChangedMessage;

export type PlayerJoinResponse = PlayerJoinMessage;
