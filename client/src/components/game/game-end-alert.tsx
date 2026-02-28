import { useGameEnd, useGamePlayers } from "@/store/game-store";
import { useUser } from "@/store/user-store";
import GameAlert from "./game-alert";

/**
 * 게임 종료 사유에 따른 결과 알림
 */
export default function GameEndAlert() {
  const gameEnd = useGameEnd();
  const players = useGamePlayers();
  const user = useUser();

  if (!gameEnd) return null;

  const { reason, rankings } = gameEnd;
  const myRank = rankings.find(r => r.userId === user?.userId)?.rank;

  // ALL_DEAD: 전원 탈락
  if (reason === "ALL_DEAD") {
    return (
      <GameAlert
        title="전원탈락"
        description="아무도 생존하지 못했어요."
        variant="destructive"
      />
    );
  }

  // ONE_SURVIVOR: 생존자 1명
  if (reason === "ONE_SURVIVOR") {
    const survivor = rankings.find(r => r.rank === 1);
    const description =
      myRank === 1
        ? "축하드려요! 마지막까지 생존하여 우승했어요."
        : `${survivor?.nickname ?? ""}님이 끝까지 생존했어요.`;
    return <GameAlert title="생존자 1명 확인" description={description} />;
  }

  // QUIZ_EXHAUSTED: 마지막 문제까지 진행 (생존자 2명 이상)
  const survivorCount = players.filter(p => p.status === "ALIVE").length;
  const description = myRank === 1 ? "축하드려요! 마지막까지 생존했어요." : "";
  return (
    <GameAlert
      title={`생존자 ${survivorCount}명 확인`}
      description={description}
    />
  );
}
