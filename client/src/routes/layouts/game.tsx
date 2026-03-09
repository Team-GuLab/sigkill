import { Outlet } from "react-router";
import { Toaster } from "@/ui/sonner";

export default function GameLayout() {
  return (
    <>
      <Toaster position="top-center" />
      <div className="bg-background flex h-screen flex-col px-4 pt-8 pb-6">
        <div className="mx-auto flex w-full max-w-xl flex-1 flex-col overflow-hidden">
          <Outlet />
        </div>
      </div>
    </>
  );
}
