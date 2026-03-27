import { useState } from "react";
import { fmt } from "@/lib/utils";
import { Loader, Empty } from "@/components/ui/Loader";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/ui/Pagination";

// ── Types ─────────────────────────────────────────────────────────────────────

interface ActivityEvent {
  type:   "login" | "kundali" | "saved_kundali" | "palmistry" | "payment" | "chat";
  ts:     string;
  icon:   string;
  title:  string;
  detail: Record<string, unknown>;
}

interface ActivityTabProps {
  events:  ActivityEvent[];
  loading: boolean;
  page:    number;
  pages:   number;
  total:   number;
  days:    number;
  onDaysChange: (d: number) => void;
  onPage:  (p: number) => void;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function dateKey(ts: string) {
  return ts ? ts.slice(0, 10) : "Unknown";
}

function fmtDate(key: string) {
  try {
    return new Date(key).toLocaleDateString("en-IN", {
      weekday: "long", day: "numeric", month: "long", year: "numeric",
    });
  } catch { return key; }
}

const TYPE_COLORS: Record<string, string> = {
  login:         "bg-slate-50 border-slate-200 text-slate-700",
  kundali:       "bg-amber-50 border-amber-200 text-amber-700",
  saved_kundali: "bg-emerald-50 border-emerald-200 text-emerald-700",
  palmistry:     "bg-purple-50 border-purple-200 text-purple-700",
  payment:       "bg-blue-50 border-blue-200 text-blue-700",
  chat:          "bg-background border-border text-foreground",
};

const TYPE_BADGE: Record<string, string> = {
  login:         "bg-slate-100 text-slate-600",
  kundali:       "bg-amber-100 text-amber-700",
  saved_kundali: "bg-emerald-100 text-emerald-700",
  palmistry:     "bg-purple-100 text-purple-700",
  payment:       "bg-blue-100 text-blue-700",
  chat:          "bg-muted text-muted-foreground",
};

const DAYS_OPTIONS = [7, 30, 60, 90, 180, 365];

// ── Chat detail ───────────────────────────────────────────────────────────────

function ChatDetail({ detail }: { detail: Record<string, unknown> }) {
  const [open, setOpen] = useState(false);
  const question   = detail.question    as string;
  const reply      = detail.reply       as string | undefined;
  const conf       = detail.confidence  as string | undefined;
  const tokensUsed = detail.tokens_used as number | undefined;
  const responseMs = detail.response_ms as number | undefined;
  const flagged    = detail.flagged     as boolean | undefined;
  const pageCtx    = detail.page_context as string | undefined;
  return (
    <div className="mt-2 space-y-1.5">
      <div className="flex items-center gap-2 flex-wrap text-[10px] text-muted-foreground">
        {pageCtx && <span className="font-medium text-blue-600">{pageCtx}</span>}
        {conf && (
          <Badge text={conf} cls={
            conf === "HIGH"   ? "bg-emerald-50 text-emerald-600" :
            conf === "MEDIUM" ? "bg-yellow-50 text-yellow-600"   : "bg-red-50 text-red-500"
          } />
        )}
        {tokensUsed && <span>{tokensUsed} tokens</span>}
        {responseMs && <span>{responseMs}ms</span>}
        {flagged    && <span className="text-red-500 font-semibold">⚑ Flagged</span>}
        <button onClick={() => setOpen(!open)}
          className="ml-auto text-muted-foreground hover:text-foreground transition-colors">
          {open ? "▲ hide" : "▼ show full"}
        </button>
      </div>

      {/* Question */}
      <div className="rounded bg-blue-50 border border-blue-100 px-3 py-2 text-xs text-blue-900 leading-relaxed">
        <span className="font-semibold text-blue-600 mr-1">You:</span>
        {open ? question : question.slice(0, 200) + (question.length > 200 ? "…" : "")}
      </div>

      {/* Reply */}
      {reply && (
        <div className="rounded bg-amber-50 border border-amber-100 px-3 py-2 text-xs text-amber-900 leading-relaxed">
          <span className="font-semibold text-amber-600 mr-1">Brahm:</span>
          {open ? reply : reply.slice(0, 300) + (reply.length > 300 ? "…" : "")}
        </div>
      )}
    </div>
  );
}

// ── Generic detail ────────────────────────────────────────────────────────────

function GenericDetail({ detail }: { detail: Record<string, unknown> }) {
  return (
    <div className="mt-2 grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-1 text-[10px]">
      {Object.entries(detail)
        .filter(([, v]) => v != null && v !== "" && v !== false)
        .map(([k, v]) => (
          <div key={k} className="flex gap-1.5">
            <span className="text-muted-foreground capitalize shrink-0">{k.replace(/_/g, " ")}:</span>
            <span className="text-foreground/80 truncate">{String(v as string | number | boolean)}</span>
          </div>
        ))}
    </div>
  );
}

// ── Event card ────────────────────────────────────────────────────────────────

function EventCard({ ev }: { ev: ActivityEvent }) {
  const [open, setOpen] = useState(false);
  const isChat    = ev.type === "chat";
  const colorCls  = TYPE_COLORS[ev.type] ?? "bg-background border-border";
  const badgeCls  = TYPE_BADGE[ev.type]  ?? "bg-muted text-muted-foreground";
  const isFlagged = isChat && Boolean(ev.detail.flagged);

  return (
    <div className={`rounded-lg border px-3 py-2.5 text-xs ${isFlagged ? "bg-red-50 border-red-200" : colorCls}`}>
      <div className="flex items-start gap-2 flex-wrap">
        {/* Left: icon + type badge */}
        <span className="text-base leading-none mt-0.5">{ev.icon}</span>
        <Badge text={ev.type.replace("_", " ")} cls={badgeCls} />

        {/* Title */}
        <span className="flex-1 min-w-0 text-foreground/80 font-medium leading-relaxed break-words">
          {ev.title}
          {isFlagged && <span className="ml-1 text-red-500">⚑</span>}
        </span>

        {/* Time */}
        <span className="text-muted-foreground text-[10px] shrink-0 ml-auto">{fmt(ev.ts)}</span>

        {/* Expand toggle (not for chat — it has its own) */}
        {!isChat && (
          <button onClick={() => setOpen(!open)}
            className="text-muted-foreground hover:text-foreground text-[10px]">
            {open ? "▲" : "▼"}
          </button>
        )}
      </div>

      {/* Expanded detail */}
      {isChat
        ? <ChatDetail detail={ev.detail} />
        : open && <GenericDetail detail={ev.detail} />
      }
    </div>
  );
}

// ── Main Tab ──────────────────────────────────────────────────────────────────

export function ActivityTab({
  events, loading, page, pages, total, days, onDaysChange, onPage,
}: ActivityTabProps) {

  // Group events by date
  const grouped: [string, ActivityEvent[]][] = [];
  const seen: Record<string, ActivityEvent[]> = {};
  for (const ev of events) {
    const key = dateKey(ev.ts);
    if (!seen[key]) { seen[key] = []; grouped.push([key, seen[key]]); }
    seen[key].push(ev);
  }

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex items-center gap-3 flex-wrap">
        <span className="text-xs text-muted-foreground">Show last:</span>
        <div className="flex gap-1">
          {DAYS_OPTIONS.map((d) => (
            <button key={d} onClick={() => onDaysChange(d)}
              className={`px-2.5 py-1 rounded text-xs transition-colors ${
                days === d
                  ? "bg-amber-100 text-amber-700 font-medium"
                  : "bg-muted text-muted-foreground hover:bg-border"
              }`}>
              {d}d
            </button>
          ))}
        </div>
        <span className="text-xs text-muted-foreground ml-auto">
          {total} events total
        </span>
      </div>

      {loading ? <Loader /> : !events.length ? (
        <Empty msg={`No activity in the last ${days} days.`} />
      ) : (
        <div className="space-y-6">
          {grouped.map(([dateK, dayEvents]) => (
            <div key={dateK}>
              {/* Day header */}
              <div className="flex items-center gap-3 mb-2">
                <span className="text-xs font-semibold text-amber-700 whitespace-nowrap">
                  {fmtDate(dateK)}
                </span>
                <span className="text-[10px] text-muted-foreground">
                  {dayEvents.length} event{dayEvents.length !== 1 ? "s" : ""}
                </span>
                <div className="flex-1 h-px bg-border/50" />
              </div>

              {/* Events for this day */}
              <div className="space-y-2 pl-2 border-l-2 border-amber-100">
                {dayEvents.map((ev, i) => (
                  <EventCard key={`${ev.type}-${ev.ts}-${i}`} ev={ev} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      <Pagination page={page} pages={pages} onChange={onPage} />
    </div>
  );
}
