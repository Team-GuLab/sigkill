import axios, { AxiosError, type AxiosResponse } from "axios";
import type { APIErrorResponse, APIResponse } from "./types";

export class AppError extends Error {
  code?: string;
  status?: number;
  constructor(message: string, code?: string, status?: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
});

axiosInstance.interceptors.request.use();

axiosInstance.interceptors.response.use(
  (response: AxiosResponse<APIResponse<unknown>>) => {
    return response;
  },
  (error: AxiosError<APIErrorResponse>) => {
    // axios 에러가 아닌 경우 그대로 reject
    if (!axios.isAxiosError(error)) {
      console.error("❎ Not Axios error occurred", error);
      return Promise.reject(error);
    }

    if (error.response) {
      console.error("🅾️ Axios error occurred", error);
      const { status, data } = error.response;
      // 애플리케이션 에러로 변환하여 reject
      const appError = new AppError(
        data?.message ?? "요청에 실패했습니다.",
        data?.code ?? "UNKNOWN_ERROR",
        status,
      );
      return Promise.reject(appError);
    }

    // 알 수 없는 서버측 에러
    console.error("🤯 An unexpected error occurred", error.toJSON());
    return Promise.reject(error);
  },
);
