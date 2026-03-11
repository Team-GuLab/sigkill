import { Outlet } from "react-router";
import Profile from "@/widgets/profile";

export default function WithProfileLayout() {
  return (
    <>
      <Outlet />
      <Profile />
    </>
  );
}
