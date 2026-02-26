import { Dog, Ghost } from "lucide-react";
import { useGamePlayers } from "@/store/game-store";

export default function PlayerField() {
  const players = useGamePlayers();
  if (players.length === 0) {
    return (
      <div className="flex items-center justify-center">
        <p className="text-gray-500">플레이어 입장을 기다리는 중...</p>
      </div>
    );
  }
  return (
    <div className="grid grid-cols-3 gap-12">
      {players.length > 0 &&
        players.map(player => (
          <div key={player.userId}>
            {player.status === "DEAD" ? (
              <Ghost className="h-12 w-12" />
            ) : (
              <Dog className="h-12 w-12 text-stone-800" />
            )}
          </div>
        ))}
    </div>
  );
}
