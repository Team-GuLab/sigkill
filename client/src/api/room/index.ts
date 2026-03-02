import { axiosInstance } from "@/api/axios";
import type { APIResponse } from "@/api/types";
import type {
  CreateRoomParams,
  CreateRoomResponse,
  RoomListDto,
} from "./types";
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

export interface ReserverRoomJoinResponse {
  joinTxId: string;
  expiresAt: number;
  ttlMillis: number;
}
export const reserverRoomJoin = async <T = ReserverRoomJoinResponse>(
  roomId: string,
): Promise<T> => {
  const response = await axiosInstance.post<
    APIResponse<T>,
    AxiosResponse<APIResponse<T>>,
    void
  >(`/api/v1/rooms/${roomId}/join`);

  return response.data.result;
};
