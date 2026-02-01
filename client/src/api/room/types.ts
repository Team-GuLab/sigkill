export interface RoomDto {
  room_id: number;
  room_title: string;
  player_count: number;
  capacity: number;
  status: "WAITING" | "PLAYING";
}

export interface RoomListDto {
  rooms: RoomDto[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
  has_next: boolean;
}

export interface RoomItem {
  roomId: number;
  title: string;
  playerCount: number;
  capacity: number;
  status: "WAITING" | "PLAYING";
}
