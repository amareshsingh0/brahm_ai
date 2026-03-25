import { type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface StatCardProps {
  label:    string;
  value:    string | number;
  icon:     LucideIcon;
  sub?:     string;
  trend?:   { value: string; positive: boolean };
  className?: string;
}

export function StatCard({ label, value, icon: Icon, sub, trend, className }: StatCardProps) {
  return (
    <div className={cn("rounded-xl border border-border bg-white p-5 flex items-start gap-3 shadow-sm", className)}>
      <div className="p-2 rounded-lg bg-amber-50 shrink-0">
        <Icon className="w-5 h-5 text-amber-700" />
      </div>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground uppercase tracking-wider leading-tight">{label}</p>
        <p className="text-2xl font-bold text-foreground mt-0.5">{value}</p>
        {sub   && <p className="text-xs text-muted-foreground mt-0.5">{sub}</p>}
        {trend && (
          <p className={cn("text-xs font-medium mt-1", trend.positive ? "text-emerald-600" : "text-red-500")}>
            {trend.positive ? "↑" : "↓"} {trend.value}
          </p>
        )}
      </div>
    </div>
  );
}
