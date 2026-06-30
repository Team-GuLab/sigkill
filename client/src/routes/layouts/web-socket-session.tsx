import { useEffect, useState } from "react";
import { Outlet, useNavigate } from "react-router";
import { WebSocketSessionProvider } from "@/app/provider/web-socket-session-provider";
import { getTabSync } from "@/app/config/tab-sync";
import DuplicateTabModal from "@/components/common/duplicate-tab-modal";
import { Spinner } from "@/ui/spinner";
import { ROUTE_PATHS } from "@/routes/paths";

/**
 * Provider 내부에서 context를 소비하면서 탭 중복 감지를 처리
 */
function TabSyncGuard() {
  const navigate = useNavigate();
  const [isDuplicate, setIsDuplicate] = useState<boolean | null>(null);

  useEffect(() => {
    const tabSync = getTabSync();
    // 세션 진입 선언 (다른 탭의 QUERY에 응답 시작)
    tabSync.setResponding(true);

    tabSync.queryOtherTabs().then(hasDuplicate => {
      if (hasDuplicate) {
        // 중복 차단된 탭은 응답 중단 (자신은 세션 비활성)
        tabSync.setResponding(false);
      }
      setIsDuplicate(hasDuplicate);
    });

    return () => {
      // 세션 이탈 시 응답 중단 (다른 탭이 진입 가능하도록)
      tabSync.setResponding(false);
    };
  }, []);

  const handleClose = () => {
    navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
  };

  // 중복 여부 확인 중에는 스피너 표시
  if (isDuplicate === null)
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner />
      </div>
    );

  return (
    <>
      {isDuplicate && <DuplicateTabModal onClose={handleClose} />}
      {!isDuplicate && <Outlet />}
    </>
  );
}

/**
 * WebSocket 세션이 필요한 라우트(WaitingRoom, GameRoom)의 공통 부모
 */
export default function WebSocketSessionLayout() {
  return (
    <WebSocketSessionProvider>
      <TabSyncGuard />
    </WebSocketSessionProvider>
  );
}
