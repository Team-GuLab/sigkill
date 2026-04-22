import { toast } from "sonner";
import { router } from "@/routes";
import { useRoomStore } from "@/store/room-store";
import { ROUTE_PATHS } from "@/routes/paths";
import type { ErrorWebSocketMessage } from "./types";

export const handleErrorMessage = (message: ErrorWebSocketMessage) => {
  toast.error(message.message);

  switch (message.code) {
    // 현재 방에 더 이상 있을 수 없는 경우 → 상태 초기화 후 방 목록으로 강제 이동
    case "ROOM_NOT_FOUND":
    case "ROOM_FULL":
    case "ROOM_IN_GAME":
    case "ROOM_ALREADY_STARTED":
    case "PLAYER_NOT_IN_ANY_ROOM":
    case "PLAYER_NOT_IN_ROOM":
      useRoomStore.getState().reset();
      router.navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
      break;
  }
};
