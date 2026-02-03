import { axiosInstance } from "@/api/axios";
import type { APIResponse } from "@/api/types";
import type { CreateRoomParams, RoomItem, RoomListDto } from "./types";
import type { AxiosResponse } from "axios";

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

const PLAYER_COUNT = 0;
const CAPACITY = 10;

export const createRoom = async <T = RoomItem>({
  title,
}: CreateRoomParams): Promise<T> => {
  const response = await axiosInstance.post<
    APIResponse<T>,
    AxiosResponse<APIResponse<T>>,
    Pick<RoomItem, "title" | "playerCount" | "capacity">
  >(`/api/v1/rooms`, {
    title,
    playerCount: PLAYER_COUNT,
    capacity: CAPACITY,
  });

  return response.data.result;
};

type WSSession = {
  ws: {
    endpoint: string;
    protocol: "websocket";
    message_format: "json";
  };
};
export const checkRoomAvailability = async <T = WSSession>(roomId: number) => {
  try {
    const response = await axiosInstance.get<APIResponse<T>>(
      `/api/v1/rooms/${roomId}/availability`,
    );
    return response.data.result;
  } catch (error) {
    throw error;
  }
};
