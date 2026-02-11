import { createBrowserRouter, RouterProvider } from "react-router";
import RoomListPage from "./pages/room-list-page";
import DefaultLayout from "./layouts/default";
import WaitingRoom from "./pages/waiting-room";

const router = createBrowserRouter([
  {
    element: <DefaultLayout />,
    children: [
      {
        path: "/",
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
