import type { GamePlayer } from "@/api/game/types";
import { Dog, Ghost } from "lucide-react";

export default function PlayerInGame({ player }: { player: GamePlayer }) {
  return (
    <>
      {player.status === "DEAD" ? (
        <Ghost className="h-12 w-12 text-gray-500/50" />
      ) : (
        <Dog className="text-primary h-12 w-12" />
      )}
    </>
  );
}
