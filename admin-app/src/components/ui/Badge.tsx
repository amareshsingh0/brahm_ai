import { cn } from "@/lib/utils";

interface BadgeProps {
  text: string;
  cls?: string;
  className?: string;
}

export function Badge({ text, cls, className }: BadgeProps) {
  return (
    <span className={cn("px-2 py-0.5 rounded text-xs font-medium", cls ?? "bg-gray-100 text-gray-500", className)}>
      {text}
    </span>
  );
}
