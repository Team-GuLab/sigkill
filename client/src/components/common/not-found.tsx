import { useNavigate } from "react-router";
import { ROUTE_PATHS } from "@/routes/paths";
import ErrorFallback from "./error-fallback";

export default function NotFound() {
  const navigate = useNavigate();

  return (
    <div className="flex h-screen items-center justify-center">
      <ErrorFallback
        title="페이지를 찾을 수 없습니다"
        description="요청하신 페이지가 존재하지 않습니다."
        buttonText="홈으로 이동"
        onButtonClick={() => navigate(ROUTE_PATHS.LANDING, { replace: true })}
      />
    </div>
  );
}
