import { axiosInstance } from "@/api/axios";
import type { APIResponse } from "@/api/types";
import type { RoomDto, RoomListDto, RoomItem } from "./types";

export interface RoomListParams {
  page?: number;
  size?: number;
}

// DTO를 RoomItem 모델(react props용)로 변환
export const toRoomItemModel = (dto: RoomDto): RoomItem => ({
  roomId: dto.room_id,
  title: dto.room_title,
  playerCount: dto.player_count,
  capacity: dto.capacity,
  status: dto.status,
});

export const getRoomList = async <T = RoomListDto>(
  params: RoomListParams = {},
): Promise<T> => {
  const { page = 0, size = 6 } = params;
  const queryParams = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });

  const response = await axiosInstance.get<APIResponse<T>>(
    `/api/v1/rooms?${queryParams}`,
  );
  return response.data.result;
};
