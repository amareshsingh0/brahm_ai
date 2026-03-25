import { fmt } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/ui/Pagination";
import { Loader, Empty } from "@/components/ui/Loader";
import type { ChatMsg } from "@/lib/types";

const CTX_OPTIONS = ["", "kundali", "panchang", "sky", "compatibility", "horoscope", "general"];

interface ChatsTabProps {
  chats:       ChatMsg[];
  loading:     boolean;
  page:        number;
  pages:       number;
  ctx:         string;
  onCtxChange: (c: string) => void;
  onPage:      (p: number) => void;
  onFlag:      (id: string) => void;
}

export function ChatsTab({ chats, loading, page, pages, ctx, onCtxChange, onPage, onFlag }: ChatsTabProps) {
  return (
    <div className="space-y-3">
      <div className="flex gap-2 flex-wrap">
        {CTX_OPTIONS.map((c) => (
          <button key={c || "all"} onClick={() => onCtxChange(c)}
            className={`px-3 py-1 rounded-full text-xs transition-colors ${
              ctx === c ? "bg-amber-100 text-amber-700 font-medium" : "bg-muted text-muted-foreground hover:bg-border"
            }`}>
            {c || "All"}
          </button>
        ))}
      </div>

      {loading ? <Loader /> : !chats?.length ? <Empty msg="No chat messages." /> : (
        <div className="space-y-2">
          {chats.map((m) => (
            <div key={m.id} className={`rounded-lg px-3 py-2.5 text-xs ${m.flagged ? "border border-red-200 bg-red-50" : "bg-muted/50 border border-border/50"}`}>
              <div className="flex items-center gap-2 mb-1 flex-wrap">
                <span className={m.role === "user" ? "text-blue-600 font-semibold" : "text-amber-700 font-semibold"}>{m.role}</span>
                <span className="text-muted-foreground">{m.page_context}</span>
                {m.confidence && (
                  <Badge text={m.confidence} cls={
                    m.confidence === "HIGH"   ? "bg-emerald-50 text-emerald-700" :
                    m.confidence === "MEDIUM" ? "bg-yellow-50 text-yellow-700"  : "bg-red-50 text-red-600"
                  } />
                )}
                {m.tokens_used   && <span className="text-muted-foreground">{m.tokens_used} tokens</span>}
                {m.response_ms   && <span className="text-muted-foreground">{m.response_ms}ms</span>}
                <span className="text-muted-foreground ml-auto">{fmt(m.created_at)}</span>
                {!m.flagged && (
                  <button onClick={() => onFlag(m.id)} className="text-muted-foreground hover:text-red-500 text-xs">⚑</button>
                )}
                {m.flagged && <span className="text-red-600 text-xs">⚑ {m.flag_reason}</span>}
              </div>
              <p className="text-foreground/70 leading-relaxed whitespace-pre-wrap">{m.content}</p>
            </div>
          ))}
        </div>
      )}
      <Pagination page={page} pages={pages} onChange={onPage} />
    </div>
  );
}
