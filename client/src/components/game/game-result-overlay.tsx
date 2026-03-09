import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { Dog, Ghost } from "lucide-react";
import { useGameEnd, useGamePlayers, useGameRoomId } from "@/store/game-store";
import { useUser } from "@/store/user-store";
import { ROUTE_GENERATORS } from "@/routes/paths";
import { Button } from "@/ui/button";
import { cn } from "@/lib/utils";

const CONFETTI_COLORS = [
  "#ff6b6b",
  "#ffd93d",
  "#6bcb77",
  "#4d96ff",
  "#ff922b",
  "#cc5de8",
  "#f06595",
  "#74c0fc",
];

function Confetti() {
  const pieces = useMemo(
    () =>
      Array.from({ length: 50 }, (_, i) => ({
        id: i,
        left: `${(i * 2 + 1) % 100}%`,
        // 음수 딜레이로 애니메이션이 즉시 화면 전체에 퍼지게
        delay: `-${((i * 3) / 50).toFixed(2)}s`,
        duration: `${2.5 + (i % 5) * 0.3}s`,
        color: CONFETTI_COLORS[i % CONFETTI_COLORS.length],
        wide: i % 3 === 0,
      })),
    [],
  );

  return (
    <div className="pointer-events-none fixed inset-0 overflow-hidden">
      {pieces.map(p => (
        <div
          key={p.id}
          className="animate-confetti-fall absolute top-0"
          style={{
            left: p.left,
            animationDelay: p.delay,
            animationDuration: p.duration,
            backgroundColor: p.color,
            width: p.wide ? "10px" : "6px",
            height: p.wide ? "6px" : "14px",
          }}
        />
      ))}
    </div>
  );
}

const RANK_MEDAL: Record<number, string> = { 1: "🥇", 2: "🥈", 3: "🥉" };

export default function GameResultOverlay() {
  const gameEnd = useGameEnd();
  const players = useGamePlayers();
  const roomId = useGameRoomId();
  const user = useUser();
  const navigate = useNavigate();

  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!gameEnd) {
      setVisible(false);
      return;
    }
    const timer = setTimeout(() => setVisible(true), 3000);
    return () => clearTimeout(timer);
  }, [gameEnd]);

  if (!visible || !gameEnd) return null;

  const sorted = [...gameEnd.rankings].sort((a, b) => a.rank - b.rank);

  const handleClickConfirm = () => {
    navigate(ROUTE_GENERATORS.WAITING_ROOM(roomId ?? ""));
  };

  return (
    <main className="fixed inset-0 z-50 flex flex-col bg-black/80 pb-6">
      <Confetti />

      {/* 순위표 */}
      <section className="relative z-10 flex flex-1 flex-col overflow-auto p-6">
        <h2 className="mb-4 text-xl font-bold text-white">경기 결과</h2>

        {/* 헤더 */}
        <div className="mb-1 grid grid-cols-[3rem_1fr_3.5rem] gap-2 border-b border-white/20 pb-2 text-sm text-white/50">
          <span>#</span>
          <span>플레이어</span>
          <span className="text-right">점수</span>
        </div>

        {/* 행 */}
        <div className="flex flex-col gap-1">
          {sorted.map(ranking => {
            const player = players.find(p => p.userId === ranking.userId);
            const isAlive = player?.status === "ALIVE";
            const isMe = ranking.userId === user?.userId;
            const medal = RANK_MEDAL[ranking.rank];

            return (
              <div
                key={ranking.userId}
                className={cn(
                  "grid grid-cols-[3rem_1fr_3.5rem] items-center gap-2 rounded-md px-2 py-2.5",
                  isMe ? "bg-white/15" : "bg-white/5",
                )}
              >
                {/* 순위 */}
                <span className="text-sm font-bold text-white">
                  {medal ?? ranking.rank}
                </span>

                {/* 플레이어 */}
                <div className="flex min-w-0 items-center gap-2">
                  {isAlive ? (
                    <Dog className="text-primary h-5 w-5 shrink-0" />
                  ) : (
                    <Ghost className="h-5 w-5 shrink-0 text-white/40" />
                  )}
                  <span
                    className={cn(
                      "truncate text-sm",
                      isMe
                        ? "font-semibold text-white underline underline-offset-2"
                        : "text-white/90",
                    )}
                  >
                    {ranking.nickname}
                  </span>
                  <span
                    className={cn(
                      "shrink-0 rounded px-1 py-0.5 text-xs font-bold",
                      isAlive
                        ? "bg-green-500/30 text-green-400"
                        : "bg-red-500/30 text-red-400",
                    )}
                  >
                    {isAlive ? "생존" : "탈락"}
                  </span>
                </div>

                {/* 점수 */}
                <span className="text-right text-sm font-medium text-white">
                  {ranking.score}
                </span>
              </div>
            );
          })}
        </div>
      </section>

      {/* 확인 버튼 */}
      <div className="relative z-10 flex w-full justify-end p-4">
        <div className="flex-end flex w-full max-w-60">
          <Button
            size="lg"
            className="text-md w-full tracking-widest"
            onClick={handleClickConfirm}
          >
            확인
          </Button>
        </div>
      </div>
    </main>
  );
}
