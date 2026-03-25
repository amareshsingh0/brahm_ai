import { fmt } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Loader, Empty } from "@/components/ui/Loader";
import type { LoginEntry } from "@/lib/types";

export function LoginsTab({ items, loading }: { items: LoginEntry[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No login history." />;
  return (
    <div className="space-y-2">
      {items.map((l) => (
        <div key={l.id} className={`rounded-lg px-4 py-2.5 text-xs border ${l.success ? "bg-muted/50 border-border/50" : "bg-red-50 border-red-200"}`}>
          <div className="flex items-center gap-3 flex-wrap">
            <Badge
              text={l.success ? "OK" : "FAIL"}
              cls={l.success ? "bg-emerald-50 text-emerald-700" : "bg-red-100 text-red-600"}
            />
            <span className="text-muted-foreground font-mono">{l.ip}</span>
            <span className="text-muted-foreground">{l.device}</span>
            {l.fail_reason && <span className="text-red-500">{l.fail_reason}</span>}
            <span className="text-muted-foreground ml-auto">{fmt(l.logged_at)}</span>
          </div>
        </div>
      ))}
    </div>
  );
}
