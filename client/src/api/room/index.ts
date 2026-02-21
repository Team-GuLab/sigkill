import { axiosInstance } from "@/api/axios";
import type { APIResponse } from "@/api/types";
import type { CreateRoomParams, CreateRoomResponse, RoomListDto } from "./types";
import type { AxiosResponse } from "axios";
import { MAX_CAPACITY } from "@/constants/room";

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

export const createRoom = async ({
  roomTitle,
}: CreateRoomParams): Promise<CreateRoomResponse> => {
  const response = await axiosInstance.post<
    APIResponse<CreateRoomResponse>,
    AxiosResponse<APIResponse<CreateRoomResponse>>,
    { roomTitle: string; capacity: number }
  >(`/api/v1/rooms`, {
    roomTitle,
    capacity: MAX_CAPACITY,
  });

  return response.data.result;
};

export const checkRoomAvailability = async <
  T = { roomId: string; canJoin: boolean },
>(
  roomId: string,
): Promise<T> => {
  try {
    const response = await axiosInstance.get<APIResponse<T>>(
      `/api/v1/rooms/${roomId}/availability`,
    );

    return response.data.result;
  } catch (error) {
    throw error;
  }
};
