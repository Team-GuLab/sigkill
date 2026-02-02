// 서버 api 응답 공통 타입
export type APIResponse<T> = {
  timeStamp: string;
  code: string;
  message: string;
  result: T;
};

export type APIErrorResponse = APIResponse<null>;
