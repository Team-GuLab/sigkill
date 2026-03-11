import { guestLogin } from "@/api/user";
import { useLogin } from "@/store/user-store";
import { Button } from "@/ui/button";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { ROUTE_PATHS } from "@/routes/paths";

export default function EnterPage() {
  const navigate = useNavigate();
  const login = useLogin();

  const handleEnter = async () => {
    try {
      const user = await guestLogin();
      login(user);
      navigate(ROUTE_PATHS.ROOM_LIST);
    } catch (error) {
      console.error(error);
      toast.error("로그인에 실패했습니다. 잠시후 다시 시도해주세요.");
    }
  };

  return (
    <div className="relative flex flex-1 flex-col items-center justify-center">
      <header className="animate-in fade-in slide-in-from-bottom-4 flex flex-col items-center gap-4 duration-1000">
        <h1 className="title-typing text-primary text-6xl font-extrabold tracking-tight">
          SIGKILL
        </h1>
        <p className="text-muted-foreground text-xl">
          개발자들을 위한 실시간 퀴즈 배틀
        </p>
      </header>

      {/* 하단 중앙 입장 버튼 */}
      <div className="absolute bottom-0 left-1/2 -translate-x-1/2 transform">
        <Button
          onClick={handleEnter}
          className="h-12 w-full min-w-fit rounded-lg px-12"
        >
          <span className="mr-2">Game Start</span>
          <svg
            className="h-6 w-6 transition-transform duration-300 group-hover:translate-x-1"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2.5}
              d="M13 7l5 5m0 0l-5 5m5-5H6"
            />
          </svg>
        </Button>
      </div>
    </div>
  );
}
