import { Suspense, useState } from "react";
import { RoomCreateModal } from "@/components/room/room-create-modal";
import { createPortal } from "react-dom";
import { Button } from "@/ui/button";
import { Spinner } from "@/ui/spinner";
import { ErrorBoundary, type FallbackProps } from "react-error-boundary";
import { useQueryErrorResetBoundary } from "@tanstack/react-query";
import { AppError } from "@/api/axios";
import Rooms from "@/components/room/rooms";
import ErrorFallback from "@/components/common/error-fallback";
import { Avatar, AvatarFallback, AvatarImage } from "@/ui/avatar";
import { useUser } from "@/store/user-store";

export default function RoomListPage() {
  const [showModal, setShowModal] = useState(false);
  const { reset } = useQueryErrorResetBoundary();
  const user = useUser();

  const handleButtonClick = () => {
    setShowModal(true);
  };

  const renderErrorFallback = ({
    error,
    resetErrorBoundary,
  }: FallbackProps) => {
    const errorMessage =
      error instanceof AppError
        ? error.message
        : "데이터를 불러오는 중 오류가 발생했습니다.";

    return (
      <ErrorFallback
        title="방 목록을 불러올 수 없습니다"
        description={errorMessage}
        onButtonClick={resetErrorBoundary}
      />
    );
  };

  const userInitial = user?.nickname
    ? user.nickname.charAt(0).toUpperCase()
    : "?";

  return (
    <div className="relative flex h-full flex-col pb-20">
      <header className="mb-6 flex items-center justify-between">
        <h1 className="text-foreground text-xl font-bold">방 목록</h1>
        <Button className="cursor-pointer" onClick={handleButtonClick}>
          방 생성
        </Button>
      </header>
      <div className="flex-1 overflow-y-auto">
        <ErrorBoundary fallbackRender={renderErrorFallback} onReset={reset}>
          <Suspense
            fallback={
              <div className="flex h-100 items-center justify-center">
                <Spinner />
              </div>
            }
          >
            <Rooms />
          </Suspense>
        </ErrorBoundary>
      </div>

      {/* 하단 사용자 프로필 영역 */}
      <div className="bg-background fixed right-0 bottom-0 left-0 flex items-center gap-3 border-t p-4">
        <Avatar>
          <AvatarImage src="" alt={user?.nickname || "User"} />
          <AvatarFallback>{userInitial}</AvatarFallback>
        </Avatar>
        <div className="flex flex-col">
          <span className="text-sm font-semibold">
            {user?.nickname || "Guest"}
          </span>
          <span className="text-muted-foreground text-xs">
            User ID: {user?.userId || "Unknown"}
          </span>
        </div>
      </div>

      {showModal &&
        createPortal(
          <RoomCreateModal
            open={showModal}
            onOpenChange={() => setShowModal(false)}
          />,
          document.getElementById("modal-root")!,
        )}
    </div>
  );
}
