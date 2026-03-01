import { LoaderCircle } from "lucide-react";

export function GameStartOverlay() {
  return (
    <div className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-black/70">
      <LoaderCircle className="h-12 w-12 animate-spin text-white" />
      <p className="mt-4 text-lg font-semibold text-white">
        곧 게임이 시작됩니다!
      </p>
    </div>
  );
}
