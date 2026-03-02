import { toast } from "sonner";
import type { ErrorWebSocketMessage } from "./types";

export const handleErrorMessage = (message: ErrorWebSocketMessage) => {
  switch (message.code) {
    case "ROOM_NOT_FOUND":
      toast.error(message.message);
      break;

    case "ROOM_JOIN_RESERVATION_NOT_FOUND":
      toast.error(message.message);
      break;
  }
};
