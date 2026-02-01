import { useSuspenseQuery } from "@tanstack/react-query";
import { getRoomList, toRoomItemModel, type RoomListParams } from "@/api/room";
import { QUERY_KEYS } from "@/lib/constants";

export function useRoomList(params: RoomListParams = {}) {
  return useSuspenseQuery({
    queryKey: QUERY_KEYS.room.list(params),
    queryFn: () => getRoomList(params),
    select: (data) => ({
      ...data,
      rooms: (data?.rooms ?? []).map(toRoomItemModel),
    }),
    staleTime: 0,
    refetchOnMount: true,
  });
}
