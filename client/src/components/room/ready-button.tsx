import { publishMessage } from "@/app/config/web-socket-client";
import { Button } from "@/ui/button";

export default function ReadyButton({
  roomId,
  myReadyStatus,
}: {
  roomId: string | undefined;
  myReadyStatus: boolean;
}) {
  return (
    <Button
      className="text-md h-full flex-1"
      onClick={() => {
        if (!roomId) return;

        if (myReadyStatus) {
          publishMessage("/app/room/unready", { roomId });
        } else {
          publishMessage("/app/room/ready", { roomId });
        }
      }}
    >
      {myReadyStatus ? "준비 취소" : "준비"}
    </Button>
  );
}
