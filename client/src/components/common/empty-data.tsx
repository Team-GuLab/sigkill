import { Button } from "@/ui/button";
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/ui/empty";
import type { ReactNode } from "react";

// 빈 데이터 표시
interface EmptyDataProps {
  icon: ReactNode;
  title: string;
  description: string;
  buttonText?: string;
  buttonIcon?: ReactNode;
  onButtonClick?: () => void;
}
export function EmptyData({
  icon,
  title,
  description,
  buttonText,
  buttonIcon,
  onButtonClick,
}: EmptyDataProps) {
  return (
    <Empty className="bg-muted/30 h-full">
      <EmptyHeader>
        <EmptyMedia variant="icon">{icon}</EmptyMedia>
        <EmptyTitle>{title}</EmptyTitle>
        <EmptyDescription className="max-w-xs text-pretty">
          {description}
        </EmptyDescription>
      </EmptyHeader>
      {buttonText && onButtonClick && (
        <EmptyContent>
          <Button variant="outline" onClick={onButtonClick}>
            {buttonIcon}
            {buttonText}
          </Button>
        </EmptyContent>
      )}
    </Empty>
  );
}
