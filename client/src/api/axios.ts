import axios from "axios";

export interface ServerError {
  code: string;
  message: string;
  timeStamp: string;
}

export const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
});

axiosInstance.interceptors.request.use();

axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // axios 에러가 아닌 경우 그대로 reject
    if (!axios.isAxiosError(error)) {
      console.error("An unexpected error occurred", error);
      return Promise.reject(error);
    }

    const { data, status } = error.response!;

    switch (status) {
      case 400:
        // TODO: handle 400 error
        console.error(data);
        break;

      case 401:
        // TODO: handle 401 error
        console.error("unauthorised");
        break;

      case 404:
        // TODO: handle 404 error
        console.error("/not-found");
        break;

      case 500:
        // TODO: handle 500 error
        console.error("/server-error");
        break;
    }

    return Promise.reject(error);
  },
);
