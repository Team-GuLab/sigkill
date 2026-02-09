import { createBrowserRouter, RouterProvider } from "react-router";
import RoomList from "./pages/room-list";
import DefaultLayout from "./layouts/default";

const router = createBrowserRouter([
  {
    element: <DefaultLayout />,
    children: [
      {
        path: "/",
        element: <RoomList />,
      },
    ],
  },
]);

export default function Router() {
  return <RouterProvider router={router} />;
}
