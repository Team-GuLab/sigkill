import { Outlet } from "react-router";
import { Toaster } from "@/ui/sonner";

export default function DefaultLayout() {
  return (
    <>
      <Toaster position="top-center" />
      <div className="bg-background flex h-screen justify-center">
        <div className="flex w-[480px] flex-col overflow-hidden px-4 pt-8 pb-20">
          <Outlet />
        </div>
      </div>
    </>
  );
}
