/**
 * UsageMeter — shows today's message usage vs limit.
 * Shown in chat input area and profile settings.
 */
import { useSubscription } from "@/hooks/useSubscription";
import { Zap } from "lucide-react";
import { cn } from "@/lib/utils";

interface UsageMeterProps {
  compact?: boolean; // small single-line version for chat input area
}

export function UsageMeter({ compact = false }: UsageMeterProps) {
  const { info, usageFraction, messagesRemaining, isPaid } = useSubscription();

  // Unlimited plan — show nothing
  if (!info.daily_message_limit || info.daily_message_limit === 0) return null;

  const used    = info.messages_used_today;
  const limit   = info.daily_message_limit;
  const pct     = Math.round(usageFraction * 100);
  const barColor =
    usageFraction >= 1   ? "bg-red-500"
    : usageFraction >= 0.8 ? "bg-orange-400"
    : "bg-amber-500";

  if (compact) {
    return (
      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
        <Zap className={cn("w-3 h-3", usageFraction >= 0.8 ? "text-orange-500" : "text-amber-500")} />
        <span className={usageFraction >= 1 ? "text-red-600 font-medium" : ""}>
          {used}/{limit} today
        </span>
        <div className="w-16 h-1.5 rounded-full bg-muted overflow-hidden">
          <div className={cn("h-full rounded-full transition-all", barColor)} style={{ width: `${pct}%` }} />
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-border bg-card p-4 space-y-2">
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-foreground">Daily Messages</span>
        <span className={cn("font-semibold", usageFraction >= 1 ? "text-red-600" : "text-amber-700")}>
          {used} / {limit}
        </span>
      </div>
      <div className="w-full h-2 rounded-full bg-muted overflow-hidden">
        <div
          className={cn("h-full rounded-full transition-all duration-500", barColor)}
          style={{ width: `${pct}%` }}
        />
      </div>
      <p className="text-xs text-muted-foreground">
        {usageFraction >= 1
          ? "Daily limit reached — resets at midnight IST"
          : messagesRemaining !== null
            ? `${messagesRemaining} messages remaining today`
            : "Unlimited"
        }
      </p>
      {!isPaid && (
        <a href="/subscription" className="text-xs text-amber-700 font-medium hover:underline flex items-center gap-1">
          <Zap className="w-3 h-3" /> Upgrade for more messages
        </a>
      )}
    </div>
  );
}
