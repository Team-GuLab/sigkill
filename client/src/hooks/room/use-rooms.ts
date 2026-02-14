import { useSuspenseQuery } from "@tanstack/react-query";
import { getRoomList, type RoomListParams } from "@/api/room";
import { QUERY_KEYS } from "@/lib/constants";

export function useRooms(params: RoomListParams = {}) {
  return useSuspenseQuery({
    queryKey: QUERY_KEYS.room.list(params),
    queryFn: () => getRoomList(params),
    // staleTime: 10000,
    gcTime: 0,
    refetchOnMount: true,
  });
}
