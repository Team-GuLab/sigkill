import { Outlet } from "react-router";
import { Toaster } from "@/ui/sonner";

export default function DefaultLayout() {
  return (
    <>
      <Toaster position="top-center" />
      <div className="flex h-screen justify-center bg-white">
        <div className="bg-background flex w-110 flex-col overflow-hidden pt-8">
          <Outlet />
        </div>
      </div>
    </>
  );
}
