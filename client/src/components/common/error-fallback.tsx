import { Button } from "@/ui/button";
import { AlertCircleIcon } from "lucide-react";
import type { ReactNode } from "react";

// 공용 에러 폴백
interface ErrorFallbackProps {
  title: string;
  description: string;
  icon?: ReactNode;
  buttonText?: string;
  onButtonClick?: () => void;
}

export function ErrorFallback({
  title,
  description,
  icon,
  buttonText = "다시 시도",
  onButtonClick,
}: ErrorFallbackProps) {
  return (
    <div className="flex min-h-100 items-center justify-center">
      <div className="text-center space-y-4">
        <div className="flex justify-center mb-4">
          <div className="text-destructive">
            {icon ?? <AlertCircleIcon className="h-12 w-12" />}
          </div>
        </div>
        <div>
          <p className="text-destructive font-semibold">{title}</p>
          <p className="text-muted-foreground text-sm mt-2">{description}</p>
        </div>
        {onButtonClick && (
          <Button onClick={onButtonClick} variant="outline" size="sm">
            {buttonText}
          </Button>
        )}
      </div>
    </div>
  );
}
