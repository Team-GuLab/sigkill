import { useState } from "react";

type ErrorMode = "none" | "server" | "unavailable" | "network";

// 개발 환경에서 서버 및 네트워크 에러를 시뮬레이션
export function DevTools() {
  const [isOpen, setIsOpen] = useState(false);
  const [errorMode, setErrorMode] = useState<ErrorMode>(
    (sessionStorage.getItem("msw-error-mode") as ErrorMode) || "none",
  );

  // 개발 환경에서만 표시
  if (import.meta.env.PROD) return null;

  const handleErrorModeChange = (mode: ErrorMode) => {
    setErrorMode(mode);
    if (mode === "none") {
      sessionStorage.removeItem("msw-error-mode");
    } else {
      sessionStorage.setItem("msw-error-mode", mode);
    }
    // 페이지 새로고침으로 적용
    window.location.reload();
  };

  return (
    <div className="fixed bottom-4 left-4 z-50">
      {!isOpen ? (
        <button
          onClick={() => setIsOpen(true)}
          className="bg-gray-800 text-white px-4 py-2 rounded-lg shadow-sm hover:bg-gray-700 text-sm font-mono"
        >
          🛠️ Dev Tools
        </button>
      ) : (
        <div className="bg-gray-800 text-white p-4 rounded-lg shadow-xl min-w-70">
          <div className="flex justify-between items-center mb-3">
            <h3 className="font-bold text-sm">MSW Error Mode</h3>
            <button
              onClick={() => setIsOpen(false)}
              className="text-gray-400 hover:text-white"
            >
              ✕
            </button>
          </div>

          <div className="space-y-2">
            {[
              { value: "none", label: "정상 응답" },
              { value: "server", label: "500 Server Error" },
              { value: "unavailable", label: "503 Service Unavailable" },
              { value: "network", label: "Network Error" },
            ].map((option) => (
              <label
                key={option.value}
                className="flex items-center space-x-2 cursor-pointer hover:bg-gray-700 p-2 rounded"
              >
                <input
                  type="radio"
                  name="error-mode"
                  value={option.value}
                  checked={errorMode === option.value}
                  onChange={() =>
                    handleErrorModeChange(option.value as ErrorMode)
                  }
                  className="cursor-pointer"
                />
                <span className="text-sm">{option.label}</span>
              </label>
            ))}
          </div>

          <div className="mt-3 pt-3 border-t border-gray-700">
            <p className="text-xs text-gray-400">
              선택 후 자동으로 새로고침됩니다
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
