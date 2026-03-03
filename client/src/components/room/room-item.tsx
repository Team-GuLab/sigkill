import { checkRoomAvailability } from "@/api/room";
import type { RoomItem } from "@/api/room/types";
import { Item, ItemContent, ItemTitle, ItemActions } from "@/ui/item";
import { Badge } from "@/ui/badge";
import { Users } from "lucide-react";
import { AppError } from "@/api/axios";
import { toast } from "sonner";
import { useNavigate } from "react-router";
import { ROUTE_GENERATORS } from "@/routes/paths";

// 방 목록 내 단건 방
interface RoomItemProps extends RoomItem {}
export default function RoomItem({
  roomId,
  roomTitle,
  playerCount,
  capacity,
  status,
}: RoomItemProps) {
  const disabled = status === "PLAYING";

  const navigate = useNavigate();

  const handleRoomItemClick = async () => {
    try {
      await checkRoomAvailability(roomId);

      navigate(ROUTE_GENERATORS.WAITING_ROOM(roomId), { replace: true });
    } catch (error) {
      if (error instanceof AppError) {
        toast.error(error.message);
        return;
      }
      toast.error("방 입장 중 에러가 발생했습니다. 잠시후 다시 시도해주세요.");
    }
  };

  return (
    <Item
      variant="outline"
      className={`bg-card rounded-full shadow-sm ${disabled ? "cursor-not-allowed opacity-50" : "cursor-pointer"}`}
      onClick={handleRoomItemClick}
    >
      <ItemContent className="min-w-0">
        <ItemTitle className="flex min-w-0 items-center gap-2">
          <span className="text-muted-foreground w-16 shrink-0">#{roomId}</span>

          <span className="text-foreground min-w-0 overflow-hidden font-semibold text-ellipsis">
            {roomTitle}
          </span>
        </ItemTitle>
      </ItemContent>
      <ItemActions>
        <div className="text-muted-foreground flex items-center gap-1 text-sm">
          <Users className="size-4" />
          <span>
            {playerCount}/{capacity}
          </span>
        </div>
        <Badge
          variant="secondary"
          className={
            status === "WAITING"
              ? "bg-secondary text-secondary-foreground"
              : "bg-accent text-accent-foreground"
          }
        >
          {status === "WAITING" ? "대기중" : "게임 중"}
        </Badge>
      </ItemActions>
    </Item>
  );
}
