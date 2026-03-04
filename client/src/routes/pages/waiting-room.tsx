import { useMemo } from "react";
import { useParams } from "react-router";
import PlayerList from "@/components/room/player-list";
import { MAX_CAPACITY } from "@/constants/room";
import { useUser } from "@/store/user-store";
import GameStartButton from "@/components/room/game-start-button";
import ReadyButton from "@/components/room/ready-button";
import { useRoomSocket } from "@/hooks/room/use-room-socket";
import { WaitingRoomSkeleton } from "@/components/room/waiting-room-skeleton";
import { GameStartOverlay } from "@/components/room/game-start-overlay";
import { useRoomInfo, usePlayers } from "@/store/room-store";
import LeaveButton from "@/components/room/leave-button";

export default function WaitingRoom() {
  const { roomId } = useParams<{ roomId: string }>();

  const user = useUser();
  const myUserId = user!.userId;

  const { isPending: isRoomSocketPending, isGameStarting } = useRoomSocket({
    roomId,
    myUserId,
  });

  const roomInfo = useRoomInfo();
  const players = usePlayers();

  const isHost = myUserId
    ? players.find(p => p.userId === myUserId)?.role === "HOST"
    : false;

  const myReadyStatus = myUserId
    ? players.find(p => p.userId === myUserId)?.status === "READY"
    : false;

  // 모든 게스트가 READY 상태인지 확인
  const isAllGuestsReady = useMemo(() => {
    const guests = players.filter(p => p.role === "GUEST");
    if (guests.length === 0) return false;
    return guests.every(p => p.status === "READY");
  }, [players]);

  if (isRoomSocketPending) {
    return <WaitingRoomSkeleton />;
  }

  return (
    <>
      {isGameStarting && <GameStartOverlay />}
      <div className="scrollbar-hide flex h-[calc(100vh-8rem)] flex-col p-2 pt-6">
        <div className="mb-4 flex-none">
          <div className="flex items-center gap-2">
            <div className="flex-1">
              <h1 className="text-lg font-semibold">
                {roomInfo?.roomTitle || "대기방"}
              </h1>
              <p className="text-sm text-gray-500">Room ID: {roomId}</p>
            </div>
          </div>
        </div>

        {/* 플레이어 목록 */}
        <section className="flex-1">
          <h2 className="bg-background sticky top-0 z-10 mb-3 py-2 text-sm font-semibold">
            참가자 ({players.length}/{roomInfo?.capacity || 0}명)
          </h2>
          <PlayerList
            players={players}
            capacity={roomInfo?.capacity || MAX_CAPACITY}
          />
        </section>

        {/* 버튼 */}
        <div className="bg-background sticky bottom-0 flex-none">
          <div className="flex h-10 items-center gap-2">
            <LeaveButton roomId={roomId || ""} />
            {isHost ? (
              <GameStartButton
                roomId={roomId}
                players={players}
                canStart={isAllGuestsReady}
              />
            ) : (
              <ReadyButton roomId={roomId} myReadyStatus={myReadyStatus} />
            )}
          </div>
        </div>
      </div>
    </>
  );
}
