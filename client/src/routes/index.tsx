import { createBrowserRouter, RouterProvider } from "react-router";
import RoomListPage from "./pages/room-list-page";
import DefaultLayout from "./layouts/default";
import WaitingRoom from "./pages/waiting-room";
import EnterPage from "./pages/enter-page";
import { ROUTE_PATHS } from "./paths";

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
        element: <WaitingRoom />,
      },
    ],
  },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
