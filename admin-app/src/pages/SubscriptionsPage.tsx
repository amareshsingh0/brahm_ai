import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { aFetch, invalidateCache } from "@/lib/api";
import { fmt, fmtInr } from "@/lib/utils";
import { Pagination } from "@/components/ui/Pagination";
import { Loader } from "@/components/ui/Loader";
import {
  CreditCard, Users, TrendingDown, AlertTriangle,
  IndianRupee, CheckCircle2, XCircle, Clock, Search,
  RotateCcw, ChevronRight, Sparkles, Calendar,
} from "lucide-react";

// ── Types ─────────────────────────────────────────────────────────────────────

interface SubRow {
  id:           string;
  user_id:      string;
  user_name:    string;
  user_phone:   string;
  user_email?:  string;
  plan:         string;
  period:       string;
  status:       string;
  amount_paid:  number;
  started_at:   string;
  expires_at:   string;
  cancelled_at?: string;
  cancel_reason?: string;
  cancelled_by?:  string;
  days_left?:   number | null;
}

interface Summary {
  active:               number;
  new_month:            number;
  cancelled_month:      number;
  expiring_7d:          number;
  total_revenue_paise:  number;
  plan_distribution:    Record<string, number>;
}

interface SubsResponse {
  items:   SubRow[];
  total:   number;
  page:    number;
  pages:   number;
  summary: Summary;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const PLAN_COLOR: Record<string, string> = {
  premium:  "bg-amber-50 text-amber-700 border-amber-300",
  standard: "bg-blue-50 text-blue-700 border-blue-200",
  free:     "bg-muted text-muted-foreground border-border",
  manual:   "bg-purple-50 text-purple-700 border-purple-200",
};

const STATUS_COLOR: Record<string, string> = {
  active:    "bg-green-50 text-green-700 border-green-200",
  cancelled: "bg-red-50 text-red-600 border-red-200",
  expired:   "bg-muted text-muted-foreground border-border",
  pending:   "bg-amber-50 text-amber-700 border-amber-200",
};

const STATUS_ICON: Record<string, React.ElementType> = {
  active:    CheckCircle2,
  cancelled: XCircle,
  expired:   Clock,
};

function PlanBadge({ plan, period }: { plan: string; period: string }) {
  return (
    <div className="flex items-center gap-1.5">
      <span className={`text-xs font-semibold border px-1.5 py-0.5 rounded capitalize ${PLAN_COLOR[plan] ?? PLAN_COLOR.free}`}>
        {plan}
      </span>
      {period && period !== "manual" && (
        <span className="text-xs text-muted-foreground capitalize">{period}</span>
      )}
      {period === "manual" && (
        <span className="text-xs text-purple-600 bg-purple-50 border border-purple-200 px-1 py-0.5 rounded">Admin Grant</span>
      )}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const Icon = STATUS_ICON[status] ?? Clock;
  return (
    <span className={`inline-flex items-center gap-1 text-xs font-medium border px-1.5 py-0.5 rounded capitalize ${STATUS_COLOR[status] ?? STATUS_COLOR.expired}`}>
      <Icon className="w-3 h-3" /> {status}
    </span>
  );
}

function DaysLeftBadge({ days }: { days: number | null | undefined }) {
  if (days === null || days === undefined) return <span className="text-xs text-muted-foreground">—</span>;
  if (days < 0)  return <span className="text-xs text-red-500 font-medium">Expired</span>;
  if (days <= 7) return <span className="text-xs text-amber-600 font-semibold bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded">{days}d left ⚠</span>;
  return <span className="text-xs text-muted-foreground">{days}d left</span>;
}

// ── Action modal (extend / cancel / grant) ────────────────────────────────────

type ActionType = "extend" | "cancel" | "grant";

function ActionModal({
  sub, action, onClose, onDone,
}: {
  sub: SubRow; action: ActionType; onClose: () => void; onDone: () => void;
}) {
  const [days,   setDays]   = useState(30);
  const [plan,   setPlan]   = useState("standard");
  const [reason, setReason] = useState("");
  const [busy,   setBusy]   = useState(false);
  const [err,    setErr]    = useState("");

  const submit = async () => {
    setBusy(true); setErr("");
    try {
      if (action === "extend") {
        await aFetch(`/admin/users/${sub.user_id}/subscription/extend`, {
          method: "POST", body: JSON.stringify({ days, reason: reason || "admin_extend" }),
        });
      } else if (action === "cancel") {
        await aFetch(`/admin/users/${sub.user_id}/subscription/cancel?reason=${encodeURIComponent(reason || "admin_cancel")}`, {
          method: "POST",
        });
      } else {
        await aFetch(`/admin/users/${sub.user_id}/grant-plan`, {
          method: "POST", body: JSON.stringify({ plan, days, reason: reason || "admin_grant" }),
        });
      }
      invalidateCache();
      onDone();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error");
    } finally { setBusy(false); }
  };

  const titles = { extend: "Extend Subscription", cancel: "Cancel Subscription", grant: "Grant Plan" };
  const colors  = { extend: "bg-blue-600", cancel: "bg-red-600", grant: "bg-amber-600" };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <h2 className="text-base font-bold text-foreground">{titles[action]}</h2>
        <p className="text-sm text-muted-foreground">
          {sub.user_name} · <span className="font-mono">{sub.user_phone}</span>
        </p>

        {action === "grant" && (
          <div>
            <label className="text-xs text-muted-foreground mb-1 block">Plan</label>
            <select value={plan} onChange={(e) => setPlan(e.target.value)}
              className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400">
              <option value="standard">Standard</option>
              <option value="premium">Premium</option>
            </select>
          </div>
        )}

        {(action === "extend" || action === "grant") && (
          <div>
            <label className="text-xs text-muted-foreground mb-1 block">Days</label>
            <input type="number" min={1} max={365} value={days} onChange={(e) => setDays(+e.target.value)}
              className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400" />
          </div>
        )}

        <div>
          <label className="text-xs text-muted-foreground mb-1 block">Reason (optional)</label>
          <input type="text" value={reason} onChange={(e) => setReason(e.target.value)}
            placeholder={action === "cancel" ? "e.g. user request" : "e.g. loyalty reward"}
            className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400" />
        </div>

        {err && <p className="text-xs text-red-600">{err}</p>}

        <div className="flex gap-2 pt-1">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted">
            Cancel
          </button>
          <button onClick={submit} disabled={busy}
            className={`flex-1 py-2 rounded-xl text-white text-sm font-semibold disabled:opacity-50 ${colors[action]}`}>
            {busy ? "…" : titles[action]}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Summary cards ─────────────────────────────────────────────────────────────

function SummaryCards({ s }: { s: Summary }) {
  const cards = [
    { label: "Active",         value: s.active,              icon: CheckCircle2,  color: "text-green-600 bg-green-50"  },
    { label: "New This Month", value: s.new_month,           icon: Sparkles,      color: "text-blue-600 bg-blue-50"    },
    { label: "Cancelled (30d)",value: s.cancelled_month,     icon: TrendingDown,  color: "text-red-600 bg-red-50"      },
    { label: "Expiring in 7d", value: s.expiring_7d,         icon: AlertTriangle, color: "text-amber-600 bg-amber-50"  },
    { label: "Total Revenue",  value: fmtInr(s.total_revenue_paise), icon: IndianRupee, color: "text-amber-700 bg-amber-50" },
  ];
  return (
    <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
      {cards.map(({ label, value, icon: Icon, color }) => (
        <div key={label} className="rounded-xl border border-border bg-white p-4 shadow-sm">
          <div className={`inline-flex w-8 h-8 rounded-lg items-center justify-center mb-2 ${color.split(" ")[1]}`}>
            <Icon className={`w-4 h-4 ${color.split(" ")[0]}`} />
          </div>
          <p className="text-xs text-muted-foreground">{label}</p>
          <p className="text-xl font-bold text-foreground mt-0.5">{value}</p>
        </div>
      ))}
    </div>
  );
}

// ── Plan distribution mini-chart ──────────────────────────────────────────────

function PlanDistribution({ dist }: { dist: Record<string, number> }) {
  const total = Object.values(dist).reduce((a, b) => a + b, 0) || 1;
  const LABELS: Record<string, string> = {
    premium_monthly: "Premium Monthly", premium_yearly: "Premium Yearly",
    premium_manual:  "Premium (Admin)",
    standard_monthly:"Standard Monthly", standard_yearly: "Standard Yearly",
    standard_manual: "Standard (Admin)",
  };
  const COLORS: Record<string, string> = {
    premium_monthly: "bg-amber-500", premium_yearly: "bg-amber-700", premium_manual: "bg-purple-500",
    standard_monthly:"bg-blue-400",  standard_yearly: "bg-blue-600",  standard_manual: "bg-indigo-400",
  };
  return (
    <div className="rounded-xl border border-border bg-white shadow-sm overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-3 border-b border-border">
        <CreditCard className="w-4 h-4 text-amber-600" />
        <p className="text-sm font-semibold text-foreground">Active Plan Distribution</p>
      </div>
      <div className="px-5 py-4 space-y-3">
        {Object.entries(dist).sort((a, b) => b[1] - a[1]).map(([key, count]) => {
          const pct = Math.round((count / total) * 100);
          return (
            <div key={key}>
              <div className="flex justify-between text-xs mb-1">
                <span className="text-foreground/80">{LABELS[key] ?? key}</span>
                <span className="font-semibold text-foreground">{count} ({pct}%)</span>
              </div>
              <div className="h-2 bg-muted rounded-full overflow-hidden">
                <div className={`h-full rounded-full ${COLORS[key] ?? "bg-muted-foreground"}`} style={{ width: `${pct}%` }} />
              </div>
            </div>
          );
        })}
        {Object.keys(dist).length === 0 && (
          <p className="text-sm text-muted-foreground text-center py-2">No active subscriptions</p>
        )}
      </div>
    </div>
  );
}

// ── Subscription row ──────────────────────────────────────────────────────────

function SubRow({
  sub, onAction,
}: {
  sub: SubRow;
  onAction: (s: SubRow, a: ActionType) => void;
}) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  return (
    <div className={`rounded-xl border bg-white shadow-sm overflow-hidden ${sub.status === "cancelled" ? "border-red-200" : sub.days_left !== null && sub.days_left !== undefined && sub.days_left <= 7 && sub.status === "active" ? "border-amber-300" : "border-border"}`}>
      {/* Main row */}
      <div className="flex items-center gap-3 px-4 py-3.5 cursor-pointer hover:bg-muted/20" onClick={() => setOpen((v) => !v)}>
        {/* Avatar */}
        <div className="w-9 h-9 rounded-full bg-gradient-to-br from-amber-100 to-amber-200 flex items-center justify-center shrink-0 text-sm font-bold text-amber-700">
          {(sub.user_name || "?")[0].toUpperCase()}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-sm text-foreground">{sub.user_name || "Unknown"}</span>
            <span className="text-xs text-muted-foreground font-mono">{sub.user_phone}</span>
            <PlanBadge plan={sub.plan} period={sub.period} />
            <StatusBadge status={sub.status} />
            {sub.status === "active" && <DaysLeftBadge days={sub.days_left} />}
          </div>
          <div className="flex items-center gap-3 mt-0.5 text-xs text-muted-foreground">
            <span className="flex items-center gap-1">
              <Calendar className="w-3 h-3" />
              Started {fmt(sub.started_at)}
            </span>
            {sub.expires_at && (
              <span className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                Expires {fmt(sub.expires_at)}
              </span>
            )}
            {sub.amount_paid > 0 && (
              <span className="flex items-center gap-1 text-green-700">
                <IndianRupee className="w-3 h-3" />
                {fmtInr(sub.amount_paid)}
              </span>
            )}
          </div>
        </div>

        <div className="shrink-0">
          {open ? <ChevronRight className="w-4 h-4 text-muted-foreground rotate-90" /> : <ChevronRight className="w-4 h-4 text-muted-foreground" />}
        </div>
      </div>

      {/* Expanded detail + actions */}
      {open && (
        <div className="border-t border-border/50 bg-muted/10 px-4 py-3 space-y-3">
          {/* Detail grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
            {[
              { label: "User ID",      value: sub.user_id },
              { label: "Email",        value: sub.user_email || "—" },
              { label: "Plan",         value: `${sub.plan} / ${sub.period}` },
              { label: "Status",       value: sub.status },
              { label: "Amount Paid",  value: sub.amount_paid ? fmtInr(sub.amount_paid) : "Free / Admin" },
              { label: "Started",      value: fmt(sub.started_at) },
              { label: "Expires",      value: sub.expires_at ? fmt(sub.expires_at) : "—" },
              { label: "Days Left",    value: sub.days_left !== null && sub.days_left !== undefined ? `${sub.days_left}d` : "—" },
              ...(sub.cancelled_at ? [
                { label: "Cancelled",    value: fmt(sub.cancelled_at) },
                { label: "Cancel By",    value: sub.cancelled_by || "—" },
                { label: "Cancel Reason", value: sub.cancel_reason || "—" },
              ] : []),
            ].map(({ label, value }) => (
              <div key={label} className="bg-white rounded-lg border border-border/50 px-3 py-2">
                <p className="text-muted-foreground mb-0.5">{label}</p>
                <p className="font-medium text-foreground truncate" title={String(value)}>{value}</p>
              </div>
            ))}
          </div>

          {/* Action buttons */}
          <div className="flex gap-2 flex-wrap pt-1">
            <button
              onClick={() => navigate(`/users/${sub.user_id}`)}
              className="px-3 py-1.5 rounded-lg border border-border text-xs text-muted-foreground hover:bg-muted flex items-center gap-1.5"
            >
              <Users className="w-3.5 h-3.5" /> View Profile
            </button>
            <button
              onClick={() => onAction(sub, "grant")}
              className="px-3 py-1.5 rounded-lg border border-amber-300 bg-amber-50 text-amber-700 text-xs font-medium hover:bg-amber-100 flex items-center gap-1.5"
            >
              <Sparkles className="w-3.5 h-3.5" /> Grant Plan
            </button>
            {sub.status === "active" && (
              <>
                <button
                  onClick={() => onAction(sub, "extend")}
                  className="px-3 py-1.5 rounded-lg border border-blue-200 bg-blue-50 text-blue-700 text-xs font-medium hover:bg-blue-100 flex items-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" /> Extend
                </button>
                <button
                  onClick={() => onAction(sub, "cancel")}
                  className="px-3 py-1.5 rounded-lg border border-red-200 bg-red-50 text-red-600 text-xs font-medium hover:bg-red-100 flex items-center gap-1.5"
                >
                  <XCircle className="w-3.5 h-3.5" /> Cancel
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function SubscriptionsPage() {
  const [data,    setData]    = useState<SubsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [page,    setPage]    = useState(1);
  const [status,  setStatus]  = useState("");
  const [plan,    setPlan]    = useState("");
  const [period,  setPeriod]  = useState("");
  const [search,  setSearch]  = useState("");
  const [action,  setAction]  = useState<{ sub: SubRow; type: ActionType } | null>(null);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(p), limit: "30" });
      if (status) params.set("status", status);
      if (plan)   params.set("plan", plan);
      if (period) params.set("period", period);
      if (search) params.set("search", search);
      const d = await aFetch<SubsResponse>(`/admin/subscriptions?${params}`);
      setData(d);
      setPage(d.page);
    } finally { setLoading(false); }
  }, [status, plan, period, search]);

  useEffect(() => { load(1); }, [load]);

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-bold text-foreground font-display">Subscriptions</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {data ? `${data.total} subscriptions` : "Loading…"}
          </p>
        </div>
      </div>

      {/* Summary cards */}
      {data?.summary && <SummaryCards s={data.summary} />}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Plan distribution */}
        {data?.summary?.plan_distribution && (
          <PlanDistribution dist={data.summary.plan_distribution} />
        )}

        {/* Filters */}
        <div className="lg:col-span-2 rounded-xl border border-border bg-white shadow-sm overflow-hidden">
          <div className="flex items-center gap-2 px-5 py-3 border-b border-border">
            <Search className="w-4 h-4 text-amber-600" />
            <p className="text-sm font-semibold text-foreground">Filter Subscriptions</p>
          </div>
          <div className="px-5 py-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
            {/* Search */}
            <div className="sm:col-span-2">
              <input
                type="text"
                placeholder="Search by name or phone…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(1)}
                className="w-full text-sm border border-border rounded-lg px-3 py-2 focus:outline-none focus:border-amber-400"
              />
            </div>

            {/* Status filter */}
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Status</label>
              <div className="flex gap-1.5 flex-wrap">
                {["", "active", "cancelled", "expired"].map((s) => (
                  <button key={s || "all"} onClick={() => setStatus(s)}
                    className={`px-2.5 py-1 rounded-full text-xs font-medium transition-colors ${
                      status === s ? "bg-amber-100 text-amber-700 border border-amber-300" : "bg-muted text-muted-foreground hover:bg-border border border-transparent"
                    }`}>
                    {s || "All"}
                  </button>
                ))}
              </div>
            </div>

            {/* Plan filter */}
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Plan</label>
              <div className="flex gap-1.5 flex-wrap">
                {["", "premium", "standard", "free"].map((p) => (
                  <button key={p || "all"} onClick={() => setPlan(p)}
                    className={`px-2.5 py-1 rounded-full text-xs font-medium transition-colors ${
                      plan === p ? "bg-amber-100 text-amber-700 border border-amber-300" : "bg-muted text-muted-foreground hover:bg-border border border-transparent"
                    }`}>
                    {p || "All"}
                  </button>
                ))}
              </div>
            </div>

            {/* Period filter */}
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Period</label>
              <div className="flex gap-1.5 flex-wrap">
                {["", "monthly", "yearly", "manual"].map((p) => (
                  <button key={p || "all"} onClick={() => setPeriod(p)}
                    className={`px-2.5 py-1 rounded-full text-xs font-medium transition-colors ${
                      period === p ? "bg-amber-100 text-amber-700 border border-amber-300" : "bg-muted text-muted-foreground hover:bg-border border border-transparent"
                    }`}>
                    {p || "All"}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* List */}
      {loading ? <Loader /> : !data?.items.length ? (
        <div className="text-center py-16 text-muted-foreground">
          <CreditCard className="w-10 h-10 mx-auto mb-3 opacity-30" />
          <p className="text-sm">No subscriptions found.</p>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {data.items.map((sub) => (
              <SubRow key={sub.id} sub={sub} onAction={(s, a) => setAction({ sub: s, type: a })} />
            ))}
          </div>
          <Pagination page={page} pages={data.pages} onChange={(p) => { setPage(p); load(p); }} />
        </>
      )}

      {/* Action modal */}
      {action && (
        <ActionModal
          sub={action.sub}
          action={action.type}
          onClose={() => setAction(null)}
          onDone={() => { setAction(null); load(page); }}
        />
      )}
    </div>
  );
}
