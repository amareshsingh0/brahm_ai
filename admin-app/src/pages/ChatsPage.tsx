import { useCallback, useEffect, useState } from "react";
import { aFetch } from "@/lib/api";
import { fmt } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/ui/Pagination";
import { Loader, Empty } from "@/components/ui/Loader";
import type { ChatMsg, ChatAnalytics } from "@/lib/types";
import {
  ChevronDown, ChevronUp, MessageSquare, Bot, User,
  Clock, Zap, AlertTriangle, BarChart2, Flag, Copy, Check,
} from "lucide-react";

// ── Types ─────────────────────────────────────────────────────────────────────

interface Conversation {
  session_id:   string;
  page_context: string;
  user_name:    string;
  user_phone:   string;
  last_at:      string;
  has_flagged:  boolean;
  turns: { user: ChatMsg; ai: ChatMsg | null }[];
}

type Mode = "recent" | "flagged" | "analytics";

// ── Helpers ───────────────────────────────────────────────────────────────────

const PAGE_ICONS: Record<string, string> = {
  kundali: "🌟", panchang: "📅", compatibility: "💞", palmistry: "🖐",
  horoscope: "♈", chat: "💬", general: "💬", gochar: "🪐",
  prashna: "❓", varshphal: "📆", kp: "🔭", "sade-sati": "⚠️",
  dosha: "🔴", gemstones: "💎", rectification: "🕐",
};

const CONFIDENCE_COLOR: Record<string, string> = {
  HIGH:   "text-green-600 bg-green-50 border-green-200",
  MEDIUM: "text-amber-600 bg-amber-50 border-amber-200",
  LOW:    "text-red-600 bg-red-50 border-red-200",
};

/** Group flat message list → conversations by session_id */
function groupIntoConversations(msgs: ChatMsg[]): Conversation[] {
  const sessMap = new Map<string, { msgs: ChatMsg[]; ctx: string; name: string; phone: string }>();
  // msgs come newest-first from API; reverse to chronological for pairing
  for (const m of [...msgs].reverse()) {
    const sid = m.session_id || "unknown";
    if (!sessMap.has(sid)) {
      sessMap.set(sid, {
        msgs: [], ctx: m.page_context,
        name: m.user_name ?? "Unknown", phone: m.user_phone ?? "",
      });
    }
    sessMap.get(sid)!.msgs.push(m);
  }

  const conversations: Conversation[] = [];
  for (const [sid, { msgs: smsgs, ctx, name, phone }] of sessMap) {
    const turns: { user: ChatMsg; ai: ChatMsg | null }[] = [];
    let i = 0;
    while (i < smsgs.length) {
      if (smsgs[i].role === "user") {
        const userMsg = smsgs[i];
        const aiMsg   = smsgs[i + 1]?.role === "assistant" ? smsgs[i + 1] : null;
        turns.push({ user: userMsg, ai: aiMsg });
        i += aiMsg ? 2 : 1;
      } else {
        // orphan AI message (shouldn't happen, but handle gracefully)
        turns.push({ user: { ...smsgs[i], role: "user", content: "[context missing]" }, ai: smsgs[i] });
        i++;
      }
    }
    const has_flagged = smsgs.some((m) => m.flagged);
    const last_at     = smsgs[smsgs.length - 1]?.created_at ?? "";
    conversations.push({ session_id: sid, page_context: ctx, user_name: name, user_phone: phone, last_at, has_flagged, turns });
  }

  // Sort newest first
  return conversations.sort((a, b) => b.last_at.localeCompare(a.last_at));
}

// ── Copy button ───────────────────────────────────────────────────────────────

function CopyBtn({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  const copy = () => {
    navigator.clipboard.writeText(text).then(() => { setCopied(true); setTimeout(() => setCopied(false), 1500); });
  };
  return (
    <button onClick={copy} className="opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded hover:bg-border">
      {copied ? <Check className="w-3 h-3 text-green-600" /> : <Copy className="w-3 h-3 text-muted-foreground" />}
    </button>
  );
}

// ── Turn (user + AI pair) ─────────────────────────────────────────────────────

function TurnRow({ turn, idx }: { turn: { user: ChatMsg; ai: ChatMsg | null }; idx: number }) {
  const [expanded, setExpanded] = useState(idx === 0); // first turn open by default
  const { user: u, ai } = turn;

  const userPreview = u.content.slice(0, 120) + (u.content.length > 120 ? "…" : "");
  const confidence  = ai?.confidence?.toUpperCase() as string | undefined;

  return (
    <div className={`border rounded-xl overflow-hidden ${u.flagged || ai?.flagged ? "border-red-200" : "border-border/50"}`}>
      {/* Turn header — always visible */}
      <button
        className="w-full flex items-start gap-3 px-4 py-3 hover:bg-muted/30 transition-colors text-left"
        onClick={() => setExpanded((v) => !v)}
      >
        <span className="text-xs font-mono text-muted-foreground w-5 shrink-0 pt-0.5">#{idx + 1}</span>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className="inline-flex items-center gap-1 text-xs text-blue-700 bg-blue-50 border border-blue-200 px-1.5 py-0.5 rounded font-medium">
              <User className="w-3 h-3" /> User
            </span>
            {(u.flagged || ai?.flagged) && (
              <span className="inline-flex items-center gap-1 text-xs text-red-600 bg-red-50 border border-red-200 px-1.5 py-0.5 rounded">
                <AlertTriangle className="w-3 h-3" />
                {u.flag_reason || ai?.flag_reason || "Flagged"}
              </span>
            )}
            {confidence && (
              <span className={`text-xs border px-1.5 py-0.5 rounded font-medium ${CONFIDENCE_COLOR[confidence] ?? ""}`}>
                {confidence}
              </span>
            )}
            {ai?.response_ms && (
              <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
                <Zap className="w-3 h-3" />{ai.response_ms}ms
              </span>
            )}
            {ai?.tokens_used && (
              <span className="text-xs text-muted-foreground">{ai.tokens_used} tok</span>
            )}
            <span className="text-xs text-muted-foreground ml-auto">{fmt(u.created_at)}</span>
          </div>
          {!expanded && (
            <p className="text-sm text-foreground/70 leading-snug truncate">{userPreview}</p>
          )}
        </div>
        <span className="text-muted-foreground shrink-0 pt-0.5">
          {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </span>
      </button>

      {/* Expanded content */}
      {expanded && (
        <div className="border-t border-border/40">
          {/* User message */}
          <div className="group flex gap-3 px-4 py-3 bg-blue-50/40">
            <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center shrink-0 mt-0.5">
              <User className="w-3.5 h-3.5 text-blue-600" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm text-foreground leading-relaxed whitespace-pre-wrap break-words">{u.content}</p>
            </div>
            <CopyBtn text={u.content} />
          </div>

          {/* AI response */}
          {ai ? (
            <div className="group flex gap-3 px-4 py-3 bg-amber-50/30 border-t border-border/30">
              <div className="w-6 h-6 rounded-full bg-amber-100 flex items-center justify-center shrink-0 mt-0.5">
                <Bot className="w-3.5 h-3.5 text-amber-700" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-foreground/85 leading-relaxed whitespace-pre-wrap break-words">{ai.content}</p>
              </div>
              <CopyBtn text={ai.content} />
            </div>
          ) : (
            <div className="px-4 py-2 bg-muted/20 border-t border-border/30">
              <p className="text-xs text-muted-foreground italic">No AI response recorded</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── Conversation Card ─────────────────────────────────────────────────────────

function ConversationCard({ conv }: { conv: Conversation }) {
  const [open, setOpen] = useState(false);
  const icon = PAGE_ICONS[conv.page_context] ?? "💬";
  const totalTurns = conv.turns.length;
  const flaggedTurns = conv.turns.filter((t) => t.user.flagged || t.ai?.flagged).length;

  return (
    <div className={`rounded-xl border bg-white overflow-hidden shadow-sm ${conv.has_flagged ? "border-red-300" : "border-border"}`}>
      {/* Conversation header */}
      <button
        className="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-muted/20 transition-colors text-left"
        onClick={() => setOpen((v) => !v)}
      >
        <div className="w-9 h-9 rounded-full bg-amber-50 border border-amber-200 flex items-center justify-center shrink-0 text-base">
          {icon}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-sm text-foreground">{conv.user_name}</span>
            {conv.user_phone && (
              <span className="text-xs text-muted-foreground font-mono">{conv.user_phone}</span>
            )}
            <Badge text={conv.page_context} cls="bg-muted text-muted-foreground capitalize" />
            {conv.has_flagged && (
              <span className="inline-flex items-center gap-1 text-xs text-red-600 bg-red-50 border border-red-200 px-1.5 py-0.5 rounded">
                <Flag className="w-3 h-3" /> {flaggedTurns} flagged
              </span>
            )}
          </div>
          <div className="flex items-center gap-3 mt-0.5">
            <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
              <MessageSquare className="w-3 h-3" /> {totalTurns} turn{totalTurns !== 1 ? "s" : ""}
            </span>
            <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
              <Clock className="w-3 h-3" /> {fmt(conv.last_at)}
            </span>
            {!open && (
              <p className="text-xs text-muted-foreground truncate max-w-xs hidden sm:block">
                {conv.turns[0]?.user.content.slice(0, 80)}…
              </p>
            )}
          </div>
        </div>

        <span className="text-muted-foreground shrink-0">
          {open ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </span>
      </button>

      {/* All turns */}
      {open && (
        <div className="border-t border-border/50 px-4 py-3 space-y-3 bg-muted/10">
          {conv.turns.map((turn, idx) => (
            <TurnRow key={`${turn.user.id}-${idx}`} turn={turn} idx={idx} />
          ))}
        </div>
      )}
    </div>
  );
}

// ── Analytics View ────────────────────────────────────────────────────────────

function AnalyticsView({ analytics }: { analytics: ChatAnalytics }) {
  const total = analytics.context_dist?.reduce((s, c) => s + c.count, 0) || 1;
  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className="rounded-xl border border-border bg-white shadow-sm overflow-hidden">
        <div className="flex items-center gap-2 px-4 py-3 border-b border-border">
          <BarChart2 className="w-4 h-4 text-amber-600" />
          <p className="text-sm font-semibold text-foreground">Top Questions (30d)</p>
        </div>
        <div className="divide-y divide-border/50 max-h-[420px] overflow-y-auto">
          {analytics.top_questions?.slice(0, 20).map((q, i) => (
            <div key={i} className="flex gap-3 px-4 py-2.5 text-xs hover:bg-muted/20">
              <span className="text-muted-foreground w-6 shrink-0 font-mono">{i + 1}.</span>
              <span className="text-foreground/80 flex-1 leading-relaxed">{q.content}</span>
              <span className="text-amber-700 font-bold shrink-0 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded">
                {q.times}×
              </span>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-xl border border-border bg-white shadow-sm overflow-hidden">
        <div className="flex items-center gap-2 px-4 py-3 border-b border-border">
          <MessageSquare className="w-4 h-4 text-amber-600" />
          <p className="text-sm font-semibold text-foreground">Usage by Page Context</p>
        </div>
        <div className="divide-y divide-border/50">
          {analytics.context_dist?.sort((a, b) => b.count - a.count).map((c) => {
            const pct = Math.round((c.count / total) * 100);
            return (
              <div key={c.page_context} className="px-4 py-3">
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-sm text-foreground capitalize flex items-center gap-1.5">
                    {PAGE_ICONS[c.page_context] ?? "💬"} {c.page_context}
                  </span>
                  <span className="text-xs font-bold text-amber-700">{c.count} ({pct}%)</span>
                </div>
                <div className="h-1.5 rounded-full bg-muted overflow-hidden">
                  <div
                    className="h-full bg-amber-400 rounded-full"
                    style={{ width: `${pct}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function ChatsPage() {
  const [mode,          setMode]          = useState<Mode>("recent");
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [analytics,     setAnalytics]     = useState<ChatAnalytics | null>(null);
  const [loading,       setLoading]       = useState(true);
  const [page,          setPage]          = useState(1);
  const [pages,         setPages]         = useState(1);
  const [total,         setTotal]         = useState(0);
  const [search,        setSearch]        = useState("");

  const load = useCallback(async (p: number, m: Mode) => {
    setLoading(true);
    try {
      if (m === "analytics") {
        const d = await aFetch<ChatAnalytics>("/admin/analytics/chat");
        setAnalytics(d);
      } else {
        const d = await aFetch<{ items?: ChatMsg[]; messages?: ChatMsg[]; page: number; pages: number; total: number }>(
          `/admin/chats?page=${p}&limit=40${m === "flagged" ? "&flagged=true" : ""}`
        );
        const msgs = d.items ?? d.messages ?? [];
        setConversations(groupIntoConversations(msgs));
        setPage(d.page ?? 1);
        setPages(d.pages ?? 1);
        setTotal(d.total ?? msgs.length);
      }
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(1, mode); }, [mode, load]);

  const filtered = search.trim()
    ? conversations.filter((c) =>
        c.user_name.toLowerCase().includes(search.toLowerCase()) ||
        c.user_phone.includes(search) ||
        c.turns.some((t) =>
          t.user.content.toLowerCase().includes(search.toLowerCase()) ||
          t.ai?.content.toLowerCase().includes(search.toLowerCase())
        )
      )
    : conversations;

  return (
    <div className="space-y-5 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-bold text-foreground font-display">Chat Monitor</h1>
          {mode !== "analytics" && !loading && (
            <p className="text-sm text-muted-foreground mt-0.5">
              {total} messages · {conversations.length} conversations on this page
            </p>
          )}
        </div>

        {/* Search */}
        {mode !== "analytics" && (
          <input
            type="text"
            placeholder="Search user, phone, message…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-64 text-sm border border-border rounded-lg px-3 py-1.5 bg-white focus:outline-none focus:border-amber-400"
          />
        )}
      </div>

      {/* Mode switcher */}
      <div className="flex gap-2 flex-wrap">
        {(["recent", "flagged", "analytics"] as const).map((m) => (
          <button
            key={m}
            onClick={() => { setMode(m); setPage(1); }}
            className={`px-4 py-1.5 rounded-full text-xs font-medium transition-colors ${
              mode === m
                ? "bg-amber-100 text-amber-700 border border-amber-300"
                : "bg-muted text-muted-foreground hover:bg-border border border-transparent"
            }`}
          >
            {m === "flagged" ? "🚩 Flagged" : m === "analytics" ? "📊 Analytics" : "🕐 Recent"}
          </button>
        ))}
      </div>

      {/* Content */}
      {loading ? <Loader /> : mode === "analytics" && analytics ? (
        <AnalyticsView analytics={analytics} />
      ) : filtered.length === 0 ? (
        <Empty />
      ) : (
        <>
          <div className="space-y-3">
            {filtered.map((conv) => (
              <ConversationCard key={conv.session_id} conv={conv} />
            ))}
          </div>
          {!search && (
            <Pagination page={page} pages={pages} onChange={(p) => { setPage(p); load(p, mode); }} />
          )}
        </>
      )}
    </div>
  );
}
