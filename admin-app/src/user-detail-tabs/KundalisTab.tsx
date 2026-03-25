import { useState } from "react";
import { fmt } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Loader, Empty } from "@/components/ui/Loader";
import type { KundaliEntry } from "@/lib/types";

export function KundalisTab({ items, loading }: { items: KundaliEntry[]; loading: boolean }) {
  const [expanded, setExpanded] = useState<number | null>(null);
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No kundali calculations." />;
  return (
    <div className="space-y-2">
      {items.map((k) => (
        <div key={k.id} className="rounded-lg bg-muted/50 border border-border/50 px-4 py-3 text-xs">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-amber-700 font-mono">#{k.id}</span>
            <span className="text-foreground/70">{k.birth_date} {k.birth_time}</span>
            <span className="text-muted-foreground">{k.birth_city}</span>
            <Badge text={k.source} cls="bg-muted text-muted-foreground" />
            {k.is_saved && <Badge text="Saved" cls="bg-emerald-50 text-emerald-700" />}
            <span className="text-muted-foreground">{k.calc_ms}ms</span>
            <span className="text-muted-foreground ml-auto">{fmt(k.created_at)}</span>
            <button onClick={() => setExpanded(expanded === k.id ? null : k.id)}
              className="text-muted-foreground hover:text-foreground">
              {expanded === k.id ? "▲ less" : "▼ json"}
            </button>
          </div>
          {expanded === k.id && (
            <pre className="mt-2 text-muted-foreground text-[10px] overflow-x-auto max-h-48 bg-muted/30 p-2 rounded">
              {JSON.stringify(k, null, 2)}
            </pre>
          )}
        </div>
      ))}
    </div>
  );
}
