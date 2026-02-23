import { Outlet } from "react-router";
import { Toaster } from "@/ui/sonner";

export default function DefaultLayout() {
  return (
    <>
      <Toaster position="top-center" />
      <div className="bg-background min-h-screen px-4 py-8">
        <div className="mx-auto max-w-md">
          <Outlet />
        </div>
      </div>
    </>
  );
}
