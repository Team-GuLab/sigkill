import { toast } from "sonner";
import { router } from "@/routes";
import { useRoomStore } from "@/store/room-store";
import { ROUTE_PATHS } from "@/routes/paths";
import type { ErrorCode, ErrorWebSocketMessage } from "./types";

// 방 목록으로 강제 이동이 필요한 에러 코드
const REDIRECT_TO_ROOM_LIST_CODES = new Set<ErrorCode>([
  "ROOM_NOT_FOUND",
  "ROOM_FULL",
  "ROOM_IN_GAME",
  "ROOM_ALREADY_STARTED",
  "PLAYER_NOT_IN_ANY_ROOM",
  "PLAYER_NOT_IN_ROOM",
]);

export const handleErrorMessage = (message: ErrorWebSocketMessage) => {
  toast.error(message.message);

  if (REDIRECT_TO_ROOM_LIST_CODES.has(message.code)) {
    useRoomStore.getState().reset();
    router.navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
  }
};
