import { Outlet } from "react-router";
import Header from "@/widgets/header";
import { Toaster } from "@/ui/sonner";

export default function DefaultLayout() {
  return (
    <>
      <Header />
      <Toaster />
      <Outlet />
    </>
  );
}
