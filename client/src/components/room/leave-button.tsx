import { publishMessage } from "@/app/config/web-socket-client";
import { ROUTE_PATHS } from "@/routes/paths";
import { Button } from "@/ui/button";
import { useNavigate } from "react-router";

/**
 * 방 나가기 버튼
 * @param roomId - 방 ID
 */
interface LeaveButtonProps {
  roomId: string;
}
export default function LeaveButton({ roomId }: LeaveButtonProps) {
  const navigate = useNavigate();

  const handleLeaveButtonClick = () => {
    if (!roomId) return;
    publishMessage("/app/room/leave", { roomId });
    navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
  };

  return (
    <Button
      variant="gray"
      className="text-md h-full w-28"
      onClick={handleLeaveButtonClick}
    >
      나가기
    </Button>
  );
}
