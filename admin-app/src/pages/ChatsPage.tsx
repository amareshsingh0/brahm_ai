import { useCallback, useEffect, useState } from "react";
import { aFetch } from "@/lib/api";
import { fmt } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/ui/Pagination";
import { Loader, Empty } from "@/components/ui/Loader";
import type { ChatMsg, ChatAnalytics } from "@/lib/types";

type Mode = "recent" | "flagged" | "analytics";

export default function ChatsPage() {
  const [mode,      setMode]      = useState<Mode>("recent");
  const [chats,     setChats]     = useState<ChatMsg[]>([]);
  const [analytics, setAnalytics] = useState<ChatAnalytics | null>(null);
  const [loading,   setLoading]   = useState(true);
  const [page,      setPage]      = useState(1);
  const [pages,     setPages]     = useState(1);

  const load = useCallback(async (p: number, m: Mode) => {
    setLoading(true);
    try {
      if (m === "analytics") {
        const d = await aFetch<ChatAnalytics>("/admin/analytics/chat");
        setAnalytics(d);
      } else {
        const d = await aFetch<{ items?: ChatMsg[]; messages?: ChatMsg[]; page: number; pages: number }>(
          `/admin/chats?page=${p}&limit=40${m === "flagged" ? "&flagged=true" : ""}`
        );
        setChats(d.items ?? d.messages ?? []);
        setPage(d.page ?? 1);
        setPages(d.pages ?? 1);
      }
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(1, mode); }, [mode, load]);

  return (
    <div className="space-y-4 animate-fade-in">
      <h1 className="text-xl font-bold text-foreground font-display">Chat Monitor</h1>

      {/* Mode switcher */}
      <div className="flex gap-2">
        {(["recent", "flagged", "analytics"] as const).map((m) => (
          <button key={m} onClick={() => { setMode(m); setPage(1); }}
            className={`px-4 py-1.5 rounded-full text-xs capitalize transition-colors ${
              mode === m ? "bg-amber-100 text-amber-700 font-medium" : "bg-muted text-muted-foreground hover:bg-border"
            }`}>
            {m === "flagged" ? "⚑ Flagged" : m === "analytics" ? "📊 Analytics" : "🕐 Recent"}
          </button>
        ))}
      </div>

      {loading ? <Loader /> : mode === "analytics" && analytics ? (
        <AnalyticsView analytics={analytics} />
      ) : (
        <>
          {!chats.length ? <Empty /> : (
            <div className="space-y-2">
              {chats.map((m) => <ChatCard key={m.id} msg={m} />)}
            </div>
          )}
          <Pagination page={page} pages={pages} onChange={(p) => { setPage(p); load(p, mode); }} />
        </>
      )}
    </div>
  );
}

function ChatCard({ msg: m }: { msg: ChatMsg }) {
  return (
    <div className={`rounded-lg px-3 py-2.5 text-xs ${m.flagged ? "border border-red-200 bg-red-50" : "bg-muted/50 border border-border/50"}`}>
      <div className="flex items-center gap-2 flex-wrap mb-1">
        <span className="text-foreground font-medium">{m.user_name ?? "Unknown"}</span>
        <span className="text-muted-foreground font-mono">{m.user_phone}</span>
        <Badge text={m.page_context} cls="bg-muted text-muted-foreground" />
        <span className={m.role === "user" ? "text-blue-600" : "text-amber-700"}>{m.role}</span>
        {m.flagged && <span className="text-red-600">⚑ {m.flag_reason}</span>}
        <span className="text-muted-foreground ml-auto">{fmt(m.created_at)}</span>
      </div>
      <p className="text-foreground/70 leading-relaxed">
        {m.content.slice(0, 300)}{m.content.length > 300 ? "…" : ""}
      </p>
    </div>
  );
}

function AnalyticsView({ analytics }: { analytics: ChatAnalytics }) {
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className="rounded-xl border border-border bg-white overflow-hidden shadow-sm">
        <p className="px-4 py-3 border-b border-border text-sm text-foreground font-semibold">Top Questions (30d)</p>
        <div className="divide-y divide-border/50">
          {analytics.top_questions?.slice(0, 15).map((q, i) => (
            <div key={i} className="flex gap-3 px-4 py-2.5 text-xs">
              <span className="text-muted-foreground w-5 shrink-0">{i + 1}</span>
              <span className="text-foreground/70 flex-1">{q.content}</span>
              <span className="text-amber-700 font-bold shrink-0">{q.times}×</span>
            </div>
          ))}
        </div>
      </div>
      <div className="rounded-xl border border-border bg-white overflow-hidden shadow-sm">
        <p className="px-4 py-3 border-b border-border text-sm text-foreground font-semibold">Page Context Distribution</p>
        <div className="divide-y divide-border/50">
          {analytics.context_dist?.map((c) => (
            <div key={c.page_context} className="flex gap-3 px-4 py-2.5 text-xs">
              <span className="text-foreground/70 flex-1 capitalize">{c.page_context}</span>
              <span className="text-amber-700 font-bold">{c.count}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
