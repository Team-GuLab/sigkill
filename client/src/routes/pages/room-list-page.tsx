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

export default function RoomListPage() {
  const [showModal, setShowModal] = useState(false);
  const { reset } = useQueryErrorResetBoundary();

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

  return (
    <div className="relative flex h-full flex-col">
      <header className="mb-6 flex items-center justify-between">
        <h1 className="text-foreground text-lg font-bold">방 목록</h1>
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

      <div className="mt-4 flex justify-end">
        <Button
          className="text-md h-10 min-w-28 cursor-pointer rounded-lg"
          onClick={handleButtonClick}
        >
          방 생성
        </Button>
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
