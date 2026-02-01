import { axiosInstance } from "@/api/axios";
import type { APIResponse } from "@/api/types";
import type { RoomListDto } from "./types";

export interface RoomListParams {
  page?: number;
  size?: number;
}

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
