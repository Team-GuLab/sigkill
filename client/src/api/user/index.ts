import { axiosInstance } from "../axios";
import type { APIResponse } from "../types";
import type { GuestLoginResponse } from "./types";

export const guestLogin = async (): Promise<GuestLoginResponse> => {
  const response = await axiosInstance.post<APIResponse<GuestLoginResponse>>(
    "/api/v1/users/guest-login",
  );

  return response.data.result;
};
