import type { AppError } from "@/api/axios";

export type UseMutationCallback = {
  onMutate?: () => void;
  onSuccess?: () => void;
  onError?: (error: AppError) => void;
  onSettled?: () => void;
};
