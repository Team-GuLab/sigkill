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
  hostId: string;
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
  playerId: string;
  nickname: string;
  status: "READY" | "NOT_READY";
}

export interface RoomJoinResponse {
  type: "PLAYER_JOIN";
  room: Omit<RoomItem, "playerCount" | "canJoin">;
  players: Player[];
}
