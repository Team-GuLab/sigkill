import type { RoomItem } from "@/api/room/types";
import { Item, ItemContent, ItemTitle, ItemActions } from "@/ui/item";
import { Users } from "lucide-react";
import { Badge } from "@/ui/badge";

// 방 목록 내 단건 방
interface RoomItemProps extends RoomItem {}
export default function RoomItem({
  roomId,
  title,
  playerCount,
  capacity,
  status,
}: RoomItemProps) {
  const disabled = status === "PLAYING" || playerCount >= capacity;

  const handleRoomItemClick = async () => {};

  return (
    <Item
      variant="outline"
      className={`rounded-full bg-card shadow-sm ${disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer"}`}
      onClick={handleRoomItemClick}
    >
      <ItemContent className="min-w-0">
        <ItemTitle className="flex items-center gap-2 min-w-0">
          <span className="shrink-0 w-16 text-muted-foreground">#{roomId}</span>

          <span className="min-w-0 text-foreground font-semibold overflow-hidden text-ellipsis">
            {title}
          </span>
        </ItemTitle>
      </ItemContent>
      <ItemActions>
        <div className="flex items-center gap-1 text-sm text-muted-foreground">
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
