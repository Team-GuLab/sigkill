import { createRoom } from "@/api/room";
import type { CreateRoomParams } from "@/api/room/types";
import { useMutation } from "@tanstack/react-query";
import type { UseMutationCallback } from "@/shared/types";

export function useCreateRoom(callbacks?: UseMutationCallback) {
  return useMutation({
    mutationFn: (params: CreateRoomParams) => createRoom(params),
    onSuccess: callbacks?.onSuccess,
    onError: callbacks?.onError,
  });
}
