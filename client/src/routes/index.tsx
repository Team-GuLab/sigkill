import { createBrowserRouter, RouterProvider } from "react-router";
import RoomListPage from "./pages/room-list-page";
import DefaultLayout from "./layouts/default";
import WaitingRoom from "./pages/waiting-room";
import EnterPage from "./pages/enter-page";

const router = createBrowserRouter([
  {
    path: "/",
    element: <EnterPage />,
  },
  {
    element: <DefaultLayout />,
    children: [
      {
        path: "/rooms",
        element: <RoomListPage />,
      },
      {
        path: "/waiting-room/:roomId",
        element: <WaitingRoom />,
      },
    ],
  },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
