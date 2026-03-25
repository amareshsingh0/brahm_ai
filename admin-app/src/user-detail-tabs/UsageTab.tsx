import { Loader, Empty } from "@/components/ui/Loader";

interface UsageRow { feature: string; count: number }

export function UsageTab({ items, loading }: { items: UsageRow[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No usage data for today." />;
  const max = Math.max(...items.map((r) => r.count), 1);
  return (
    <div className="space-y-2">
      {items.map((r) => (
        <div key={r.feature} className="flex items-center gap-3 text-xs">
          <span className="w-32 shrink-0 text-muted-foreground capitalize">{r.feature.replace(/_/g, " ")}</span>
          <div className="flex-1 bg-muted rounded-full h-2 overflow-hidden">
            <div
              className="h-full bg-amber-400 rounded-full transition-all"
              style={{ width: `${(r.count / max) * 100}%` }}
            />
          </div>
          <span className="w-8 text-right font-mono text-foreground/70">{r.count}</span>
        </div>
      ))}
    </div>
  );
}
