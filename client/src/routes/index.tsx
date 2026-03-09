import { useRef } from "react";
import { createBrowserRouter, RouterProvider, useNavigate } from "react-router";
import { ErrorBoundary, type FallbackProps } from "react-error-boundary";
import RoomListPage from "./pages/room-list-page";
import DefaultLayout from "./layouts/default";
import WaitingRoom from "./pages/waiting-room";
import EnterPage from "./pages/enter-page";
import GameRoom from "./pages/game-room";
import NotFound from "@/components/common/not-found";
import ErrorFallback from "@/components/common/error-fallback";
import { AppError } from "@/api/axios";
import { useGameRoomId } from "@/store/game-store";
import { ROUTE_PATHS, ROUTE_GENERATORS } from "./paths";
import GameLayout from "./layouts/game";

function WaitingRoomRoute() {
  const navigate = useNavigate();

  const renderError = ({ error }: FallbackProps) => (
    <ErrorFallback
      title="오류가 발생했습니다"
      description={
        error instanceof AppError
          ? error.message
          : "알 수 없는 오류가 발생했습니다."
      }
      buttonText="방 목록으로 이동"
      onButtonClick={() => navigate(ROUTE_PATHS.ROOM_LIST, { replace: true })}
    />
  );

  return (
    <ErrorBoundary fallbackRender={renderError}>
      <WaitingRoom />
    </ErrorBoundary>
  );
}

function GameRoomRoute() {
  const navigate = useNavigate();
  const roomId = useGameRoomId();
  // 에러 발생 시 game-store가 리셋되어 roomId가 null이 되므로 ref로 보존
  const savedRoomIdRef = useRef<string | null>(null);
  if (roomId) {
    savedRoomIdRef.current = roomId;
  }

  const renderError = ({ error }: FallbackProps) => (
    <ErrorFallback
      title="오류가 발생했습니다"
      description={
        error instanceof AppError
          ? error.message
          : "알 수 없는 오류가 발생했습니다."
      }
      buttonText="대기방으로 이동"
      onButtonClick={() => {
        if (savedRoomIdRef.current) {
          navigate(ROUTE_GENERATORS.WAITING_ROOM(savedRoomIdRef.current), {
            replace: true,
          });
        } else {
          navigate(ROUTE_PATHS.ROOM_LIST, { replace: true });
        }
      }}
    />
  );

  return (
    <ErrorBoundary fallbackRender={renderError}>
      <GameRoom />
    </ErrorBoundary>
  );
}

const router = createBrowserRouter([
  {
    path: ROUTE_PATHS.HOME,
    element: <EnterPage />,
  },
  {
    element: <DefaultLayout />,
    children: [
      {
        path: ROUTE_PATHS.ROOM_LIST,
        element: <RoomListPage />,
      },
      {
        path: ROUTE_PATHS.WAITING_ROOM,
        element: <WaitingRoomRoute />,
      },
    ],
  },
  {
    element: <GameLayout />,
    children: [
      {
        path: ROUTE_PATHS.GAME_ROOM,
        element: <GameRoomRoute />,
      },
    ],
  },
  {
    path: "*",
    element: <NotFound />,
  },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
