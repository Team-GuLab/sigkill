import { Outlet } from "react-router";
import Header from "@/widgets/header";
import { Toaster } from "@/ui/sonner";

export default function DefaultLayout() {
  return (
    <>
      <Header />
      <Toaster position="top-center" />
      <div className="bg-background min-h-screen px-4 py-8">
        <div className="mx-auto max-w-md">
          <Outlet />
        </div>
      </div>
    </>
  );
}
