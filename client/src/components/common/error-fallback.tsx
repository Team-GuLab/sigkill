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

export default function ErrorFallback({
  title,
  description,
  icon,
  buttonText = "다시 시도",
  onButtonClick,
}: ErrorFallbackProps) {
  return (
    <div className="flex min-h-100 items-center justify-center">
      <div className="space-y-4 text-center">
        <div className="mb-4 flex justify-center">
          <div className="text-destructive">
            {icon ?? <AlertCircleIcon className="h-12 w-12" />}
          </div>
        </div>
        <div>
          <p className="text-destructive font-semibold">{title}</p>
          <p className="text-muted-foreground mt-2 text-sm">{description}</p>
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
