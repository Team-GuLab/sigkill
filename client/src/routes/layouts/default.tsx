import { Outlet } from "react-router";
import Header from "@/widgets/header";
import { Toaster } from "@/ui/sonner";
import { DevTools } from "@/shared/dev-tools";

export default function DefaultLayout() {
  return (
    <>
      <Header />
      <Toaster />
      <Outlet />
      <DevTools />
    </>
  );
}
