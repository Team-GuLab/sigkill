import { Outlet } from "react-router";
import { Toaster } from "@/ui/sonner";

export default function GameLayout() {
  return (
    <>
      <Toaster position="top-center" />
      <div className="flex h-dvh justify-center bg-white">
        <div className="bg-background flex w-110 flex-col overflow-hidden p-4">
          <Outlet />
        </div>
      </div>
    </>
  );
}
