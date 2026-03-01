import { Alert, AlertDescription, AlertTitle } from "@/ui/alert";
import { AlertCircleIcon, AlertTriangleIcon } from "lucide-react";

interface GameAlertProps {
  title: string;
  description: string;
  variant?: "default" | "destructive" | "warning";
}

export default function GameAlert({
  title,
  description,
  variant = "default",
}: GameAlertProps) {
  return (
    <Alert variant={variant}>
      {variant === "destructive" && <AlertCircleIcon className="h-4 w-4" />}
      {variant === "warning" && <AlertTriangleIcon className="h-4 w-4" />}
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription>{description}</AlertDescription>
    </Alert>
  );
}
