import { fmt } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Loader, Empty } from "@/components/ui/Loader";
import type { PalmEntry } from "@/lib/types";

export function PalmistryTab({ items, loading }: { items: PalmEntry[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No palmistry readings." />;
  return (
    <div className="space-y-2">
      {items.map((p) => (
        <div key={p.id} className="rounded-lg bg-muted/50 border border-border/50 px-4 py-3 text-xs space-y-1.5">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-amber-700 font-mono">#{p.id}</span>
            {p.confidence && <Badge text={p.confidence} cls="bg-blue-50 text-blue-600" />}
            <span className="text-muted-foreground">{p.tokens_used} tokens</span>
            <span className="text-muted-foreground ml-auto">{fmt(p.created_at)}</span>
          </div>
          {p.lines_found && (
            <div className="flex flex-wrap gap-2 mt-1">
              {Object.entries(p.lines_found).map(([line, val]) => (
                <span key={line} className="bg-muted px-2 py-0.5 rounded text-muted-foreground">
                  <span className="text-foreground/50">{line}:</span> {val}
                </span>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
