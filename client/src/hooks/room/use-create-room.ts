import { createRoom } from "@/api/room";
import type { CreateRoomParams, CreateRoomResponse } from "@/api/room/types";
import { useMutation } from "@tanstack/react-query";
import type { UseMutationCallback } from "@/shared/types";

export function useCreateRoom(
  callbacks?: UseMutationCallback<CreateRoomResponse, CreateRoomParams>,
) {
  return useMutation<CreateRoomResponse, Error, CreateRoomParams>({
    mutationFn: (params: CreateRoomParams) => createRoom(params),
    onSuccess: callbacks?.onSuccess,
    onError: callbacks?.onError,
  });
}
