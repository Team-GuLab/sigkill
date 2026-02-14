import { subscribeManager } from "@/app/config/stomp-subscribe-manager";
import { useEffect } from "react";
import { useParams } from "react-router";

export default function WaitingRoom() {
  const { roomId } = useParams<{ roomId: string }>();

  useEffect(() => {
    const unsubscribe = subscribeManager(`/topic/room/${roomId}`, data => {});
  }, [roomId]);

  return (
    <div>
      <h1>Waiting Room</h1>
    </div>
  );
}
