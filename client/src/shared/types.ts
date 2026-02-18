import type { AppError } from "@/api/axios";

export type UseMutationCallback<T, P> = {
  onMutate?: (params: P) => void;
  onSuccess?: (data: T) => void;
  onError?: (error: AppError) => void;
  onSettled?: () => void;
};
