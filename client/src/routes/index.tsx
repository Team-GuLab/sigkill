import { createBrowserRouter, RouterProvider } from "react-router";
import RoomListPage from "./pages/room-list-page";
import DefaultLayout from "./layouts/default";

const router = createBrowserRouter([
  {
    element: <DefaultLayout />,
    children: [
      {
        path: "/",
        element: <RoomListPage />,
      },
    ],
  },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
