export interface RoomListDto {
  rooms: RoomItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface RoomItem {
  roomId: number;
  title: string;
  playerCount: number;
  capacity: number;
  status: "WAITING" | "PLAYING";
}
