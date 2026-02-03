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
    <div className="min-h-screen bg-background px-4 py-8">
      <div className="mx-auto max-w-md">
        <header className="mb-6 flex items-center justify-between">
          <h1 className="text-xl font-bold text-foreground">방 목록</h1>
          <Button className="cursor-pointer" onClick={handleButtonClick}>
            방 생성
          </Button>
        </header>
        <ErrorBoundary fallbackRender={renderErrorFallback} onReset={reset}>
          <Suspense
            fallback={
              <div className="h-100 flex items-center justify-center">
                <Spinner />
              </div>
            }
          >
            <Rooms />
          </Suspense>
        </ErrorBoundary>

        {showModal &&
          createPortal(
            <RoomCreateModal
              open={showModal}
              onOpenChange={() => setShowModal(false)}
            />,
            document.getElementById("modal-root")!,
          )}
      </div>
    </div>
  );
}
