import { Outlet } from "react-router";
import Profile from "@/widgets/profile";

export default function WithProfileLayout() {
  return (
    <>
      <div className="h-full px-4">
        <Outlet />
      </div>
      <Profile />
    </>
  );
}
