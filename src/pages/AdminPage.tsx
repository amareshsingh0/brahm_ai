/**
 * Brahm AI — Admin Panel v2
 * Route: /admin
 * Auth: X-Admin-Key header (sessionStorage — clears on browser close)
 *
 * Tabs: Dashboard | Users | Payments | Chat Monitor | Analytics | Admin Log
 * User Detail: Profile | Chat History | Kundalis | Palmistry | Payments | Usage | Login History
 */
import { useState, useEffect, useCallback, useRef } from "react";

// ─── Types ────────────────────────────────────────────────────────────────────

interface Stats {
  total_users: number;
  new_today: number;
  new_week: number;
  mau: number;
  dau: number;
  paid_users: number;
  revenue_today: number;
  revenue_month: number;
  revenue_total: number;
  chats_today: number;
  kundalis_today: number;
  palm_today: number;
  active_subscriptions: {
    jyotishi_monthly: number;
    jyotishi_yearly: number;
    acharya_monthly: number;
    acharya_yearly: number;
  };
  top_endpoints: { endpoint: string; count: number }[];
}

interface UserRow {
  id: string;
  name: string;
  phone: string;
  email?: string;
  role: string;
  status: string;
  plan: string;
  lang_pref: string;
  birth_city?: string;
  birth_date?: string;
  created_at: string;
  last_login?: string;
  total_chats: number;
  total_kundalis: number;
  total_palm: number;
  lifetime_paid_inr: number;
}

interface UserDetail extends UserRow {
  birth_time?: string;
  city?: string;
  subscription?: {
    plan: string;
    period: string;
    status: string;
    started_at: string;
    expires_at: string;
    amount_paid: number;
    cashfree_order_id: string;
  };
  usage_today: { feature: string; count: number }[];
}

interface ChatMsg {
  id: string;
  session_id: string;
  page_context: string;
  role: string;
  content: string;
  confidence?: string;
  tokens_used?: number;
  response_ms?: number;
  flagged: boolean;
  flag_reason?: string;
  created_at: string;
}

interface KundaliEntry {
  id: number;
  birth_date: string;
  birth_time: string;
  birth_city: string;
  calc_ms: number;
  is_saved: boolean;
  source: string;
  created_at: string;
}

interface PalmEntry {
  id: number;
  lines_found: Record<string, string>;
  confidence: string;
  tokens_used: number;
  created_at: string;
}

interface PaymentRow {
  id: number;
  user_id?: string;
  user_name?: string;
  user_phone?: string;
  cashfree_order_id: string;
  amount: number;
  status: string;
  payment_method?: string;
  fail_reason?: string;
  paid_at: string;
}

interface LoginEntry {
  id: number;
  ip: string;
  device: string;
  success: boolean;
  fail_reason?: string;
  logged_at: string;
}

interface AdminLogEntry {
  id: number;
  admin_name: string;
  action: string;
  target_type: string;
  target_id: string;
  details: Record<string, unknown>;
  performed_at: string;
}

interface Paginated<T> {
  items: T[];
  total: number;
  page: number;
  pages: number;
}

// ─── API ──────────────────────────────────────────────────────────────────────

const BASE = (import.meta.env.VITE_API_URL ?? "https://brahmasmi.bimoraai.com/api").replace(/\/$/, "");

async function aFetch(path: string, opts: RequestInit = {}) {
  const key = sessionStorage.getItem("admin-key") ?? "";
  const res = await fetch(`${BASE}${path}`, {
    ...opts,
    headers: { "Content-Type": "application/json", "X-Admin-Key": key, ...(opts.headers ?? {}) },
  });
  if (res.status === 401) { sessionStorage.removeItem("admin-key"); window.location.reload(); }
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ─── Shared UI ────────────────────────────────────────────────────────────────

const PLAN_CLS: Record<string, string> = {
  free:      "bg-white/10 text-white/50",
  jyotishi:  "bg-blue-500/20 text-blue-300",
  acharya:   "bg-amber-500/20 text-amber-300",
};
const STATUS_CLS: Record<string, string> = {
  active:    "bg-emerald-500/20 text-emerald-300",
  suspended: "bg-yellow-500/20 text-yellow-300",
  banned:    "bg-red-500/20 text-red-400",
  deleted:   "bg-white/10 text-white/30",
};
const PAY_CLS: Record<string, string> = {
  SUCCESS:  "bg-emerald-500/20 text-emerald-300",
  FAILED:   "bg-red-500/20 text-red-300",
  PENDING:  "bg-yellow-500/20 text-yellow-300",
  REFUNDED: "bg-purple-500/20 text-purple-300",
};

function Badge({ text, cls }: { text: string; cls?: string }) {
  return <span className={`px-2 py-0.5 rounded text-xs font-medium ${cls ?? "bg-white/10 text-white/50"}`}>{text}</span>;
}

function StatCard({ label, value, icon, sub }: { label: string; value: string | number; icon: string; sub?: string }) {
  return (
    <div className="rounded-xl border border-white/10 bg-[#0f0f1a] p-4 flex items-start gap-3">
      <span className="text-2xl mt-0.5">{icon}</span>
      <div>
        <p className="text-xs text-white/40 uppercase tracking-wider">{label}</p>
        <p className="text-2xl font-bold text-white">{value}</p>
        {sub && <p className="text-xs text-white/30 mt-0.5">{sub}</p>}
      </div>
    </div>
  );
}

function Pg({ page, pages, onChange }: { page: number; pages: number; onChange: (p: number) => void }) {
  if (pages <= 1) return null;
  return (
    <div className="flex items-center gap-2 mt-4 text-sm">
      <button disabled={page === 1} onClick={() => onChange(page - 1)}
        className="px-3 py-1 rounded bg-white/10 text-white/60 disabled:opacity-30 hover:bg-white/20">← Prev</button>
      <span className="text-white/40">{page} / {pages}</span>
      <button disabled={page === pages} onClick={() => onChange(page + 1)}
        className="px-3 py-1 rounded bg-white/10 text-white/60 disabled:opacity-30 hover:bg-white/20">Next →</button>
    </div>
  );
}

function Loader() { return <p className="text-white/30 py-8 text-center">Loading…</p>; }
function Empty({ msg = "No data." }: { msg?: string }) { return <p className="text-white/20 py-8 text-center">{msg}</p>; }

function fmt(ts?: string) {
  if (!ts) return "—";
  return new Date(ts).toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}
function fmtInr(paise?: number) { return paise ? `₹${(paise / 100).toLocaleString("en-IN")}` : "₹0"; }

// ─── Dashboard Tab ────────────────────────────────────────────────────────────

function DashboardTab() {
  const [s, setS] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => { aFetch("/api/admin/stats").then(setS).finally(() => setLoading(false)); }, []);

  if (loading) return <Loader />;
  if (!s) return <Empty msg="Failed to load stats." />;

  return (
    <div className="space-y-6">
      {/* Row 1 — Users */}
      <div>
        <p className="text-xs text-white/30 uppercase tracking-wider mb-3">Users</p>
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
          <StatCard label="Total Users"   value={s.total_users} icon="👤" />
          <StatCard label="New Today"     value={s.new_today}   icon="✨" />
          <StatCard label="New This Week" value={s.new_week}    icon="📅" />
          <StatCard label="DAU"           value={s.dau}         icon="🌟" />
          <StatCard label="MAU"           value={s.mau}         icon="📈" />
        </div>
      </div>
      {/* Row 2 — Revenue */}
      <div>
        <p className="text-xs text-white/30 uppercase tracking-wider mb-3">Revenue</p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Revenue Today"  value={fmtInr(s.revenue_today)}  icon="💰" />
          <StatCard label="Revenue Month"  value={fmtInr(s.revenue_month)}  icon="📊" />
          <StatCard label="Revenue Total"  value={fmtInr(s.revenue_total)}  icon="🏦" />
          <StatCard label="Paid Users"     value={s.paid_users}              icon="💳" />
        </div>
      </div>
      {/* Row 3 — Activity */}
      <div>
        <p className="text-xs text-white/30 uppercase tracking-wider mb-3">Today's Activity</p>
        <div className="grid grid-cols-3 gap-3">
          <StatCard label="AI Chats"     value={s.chats_today}    icon="💬" />
          <StatCard label="Kundalis"     value={s.kundalis_today} icon="⭐" />
          <StatCard label="Palm Readings" value={s.palm_today}    icon="🖐" />
        </div>
      </div>
      {/* Row 4 — Subscriptions breakdown */}
      <div>
        <p className="text-xs text-white/30 uppercase tracking-wider mb-3">Active Subscriptions</p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Jyotishi Monthly" value={s.active_subscriptions?.jyotishi_monthly ?? 0} icon="🌙" />
          <StatCard label="Jyotishi Yearly"  value={s.active_subscriptions?.jyotishi_yearly  ?? 0} icon="🌙" />
          <StatCard label="Acharya Monthly"  value={s.active_subscriptions?.acharya_monthly  ?? 0} icon="⚡" />
          <StatCard label="Acharya Yearly"   value={s.active_subscriptions?.acharya_yearly   ?? 0} icon="⚡" />
        </div>
      </div>
      {/* Top Endpoints */}
      {s.top_endpoints?.length > 0 && (
        <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-hidden">
          <p className="px-5 py-3 border-b border-white/10 text-sm font-semibold text-white/70">Top Endpoints</p>
          <table className="w-full text-sm">
            <thead><tr className="border-b border-white/10">
              <th className="text-left px-5 py-2 text-white/30 font-medium">#</th>
              <th className="text-left px-5 py-2 text-white/30 font-medium">Endpoint</th>
              <th className="text-right px-5 py-2 text-white/30 font-medium">Hits</th>
            </tr></thead>
            <tbody>
              {s.top_endpoints.map((ep, i) => (
                <tr key={ep.endpoint} className="border-b border-white/5 hover:bg-white/5">
                  <td className="px-5 py-2 text-white/30">{i + 1}</td>
                  <td className="px-5 py-2 font-mono text-white/70">{ep.endpoint}</td>
                  <td className="px-5 py-2 text-right text-amber-300 font-semibold">{ep.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

// ─── User Detail Modal ────────────────────────────────────────────────────────

type DetailTab = "profile" | "chats" | "kundalis" | "palmistry" | "payments" | "usage" | "logins";

function UserDetailModal({ userId, onClose }: { userId: string; onClose: () => void }) {
  const [tab, setTab] = useState<DetailTab>("profile");
  const [user, setUser] = useState<UserDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionMsg, setActionMsg] = useState("");

  // Sub-data states
  const [chats, setChats] = useState<ChatMsg[]>([]);
  const [chatPage, setChatPage] = useState(1);
  const [chatPages, setChatPages] = useState(1);
  const [chatCtx, setChatCtx] = useState("");

  const [kundalis, setKundalis] = useState<KundaliEntry[]>([]);
  const [palmistry, setPalmistry] = useState<PalmEntry[]>([]);
  const [payments, setPayments] = useState<PaymentRow[]>([]);
  const [logins, setLogins] = useState<LoginEntry[]>([]);
  const [subLoading, setSubLoading] = useState(false);

  useEffect(() => {
    aFetch(`/api/admin/users/${userId}`).then(setUser).finally(() => setLoading(false));
  }, [userId]);

  const loadChats = useCallback(async (page: number, ctx: string) => {
    setSubLoading(true);
    try {
      const d = await aFetch(`/api/admin/users/${userId}/chats?page=${page}&limit=30${ctx ? `&page_context=${ctx}` : ""}`);
      setChats(d.items ?? d.messages ?? []);
      setChatPage(d.page ?? 1);
      setChatPages(d.pages ?? 1);
    } finally { setSubLoading(false); }
  }, [userId]);

  const loadKundalis = useCallback(async () => {
    setSubLoading(true);
    const d = await aFetch(`/api/admin/users/${userId}/kundalis`).finally(() => setSubLoading(false));
    setKundalis(d.items ?? d);
  }, [userId]);

  const loadPalmistry = useCallback(async () => {
    setSubLoading(true);
    const d = await aFetch(`/api/admin/users/${userId}/palmistry`).finally(() => setSubLoading(false));
    setPalmistry(d.items ?? d);
  }, [userId]);

  const loadPayments = useCallback(async () => {
    setSubLoading(true);
    const d = await aFetch(`/api/admin/users/${userId}/payments`).finally(() => setSubLoading(false));
    setPayments(d.items ?? d);
  }, [userId]);

  const loadLogins = useCallback(async () => {
    setSubLoading(true);
    const d = await aFetch(`/api/admin/users/${userId}/logins`).finally(() => setSubLoading(false));
    setLogins(d.items ?? d);
  }, [userId]);

  useEffect(() => {
    if (tab === "chats")    loadChats(1, "");
    if (tab === "kundalis") loadKundalis();
    if (tab === "palmistry") loadPalmistry();
    if (tab === "payments") loadPayments();
    if (tab === "logins")   loadLogins();
  }, [tab, loadChats, loadKundalis, loadPalmistry, loadPayments, loadLogins]);

  const action = async (endpoint: string, method = "POST", body: Record<string, unknown> = {}, confirm_msg?: string) => {
    if (confirm_msg && !window.confirm(confirm_msg)) return;
    try {
      await aFetch(endpoint, { method, body: JSON.stringify(body) });
      setActionMsg("Done ✓");
      aFetch(`/api/admin/users/${userId}`).then(setUser);
      setTimeout(() => setActionMsg(""), 3000);
    } catch (e) {
      setActionMsg(`Error: ${e instanceof Error ? e.message : String(e)}`);
    }
  };

  const DETAIL_TABS: { id: DetailTab; label: string; icon: string }[] = [
    { id: "profile",   label: "Profile",   icon: "👤" },
    { id: "chats",     label: "Chats",     icon: "💬" },
    { id: "kundalis",  label: "Kundalis",  icon: "⭐" },
    { id: "palmistry", label: "Palmistry", icon: "🖐" },
    { id: "payments",  label: "Payments",  icon: "💳" },
    { id: "usage",     label: "Usage",     icon: "📊" },
    { id: "logins",    label: "Logins",    icon: "🔐" },
  ];

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-2 sm:p-4" onClick={onClose}>
      <div
        className="bg-[#0c0c18] border border-white/15 rounded-2xl w-full max-w-4xl max-h-[92vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-white/10 shrink-0">
          <div>
            {loading ? <p className="text-white/40">Loading…</p> : (
              <div className="flex items-center gap-3 flex-wrap">
                <span className="text-white font-semibold">{user?.name || "Unknown"}</span>
                <span className="text-white/40 text-sm font-mono">{user?.phone}</span>
                <Badge text={user?.plan ?? "free"} cls={PLAN_CLS[user?.plan ?? "free"]} />
                <Badge text={user?.status ?? "active"} cls={STATUS_CLS[user?.status ?? "active"]} />
                {user?.role === "admin" && <Badge text="ADMIN" cls="bg-purple-500/30 text-purple-300" />}
              </div>
            )}
          </div>
          <button onClick={onClose} className="text-white/40 hover:text-white text-xl px-2">✕</button>
        </div>

        {/* Action bar */}
        {user && (
          <div className="px-5 py-2.5 border-b border-white/10 flex flex-wrap gap-2 shrink-0">
            <ActionBtn label="Change Plan" color="blue" onClick={() => {
              const plan = window.prompt("New plan (free/jyotishi/acharya):", user.plan);
              if (plan) action(`/api/admin/users/${userId}`, "PATCH", { plan, note: "admin override" });
            }} />
            <ActionBtn label="Grant Free Days" color="green" onClick={() => {
              const days = window.prompt("Grant how many free days?");
              if (days) action(`/api/admin/users/${userId}/grant-plan`, "POST", { plan: user.plan || "jyotishi", days: Number(days), reason: "admin grant" });
            }} />
            <ActionBtn label="Extend Sub" color="green" onClick={() => {
              const days = window.prompt("Extend by how many days?");
              if (days) action(`/api/admin/users/${userId}/subscription/extend`, "POST", { days: Number(days), reason: "admin" });
            }} />
            <ActionBtn label="Cancel Sub" color="yellow" onClick={() => {
              const reason = window.prompt("Cancel reason?", "admin_request");
              if (reason) action(`/api/admin/users/${userId}/subscription/cancel`, "POST", { reason }, "Cancel this user's subscription?");
            }} />
            {user.status === "active"
              ? <ActionBtn label="Suspend" color="yellow" onClick={() => action(`/api/admin/users/${userId}`, "PATCH", { status: "suspended", note: "admin suspend" }, "Suspend this user?")} />
              : <ActionBtn label="Unsuspend" color="green" onClick={() => action(`/api/admin/users/${userId}`, "PATCH", { status: "active", note: "admin unsuspend" })} />
            }
            {user.role !== "banned"
              ? <ActionBtn label="Ban" color="red" onClick={() => {
                  const r = window.prompt("Ban reason?");
                  if (r) action(`/api/admin/users/${userId}/ban`, "POST", { reason: r }, "Ban this user?");
                }} />
              : <ActionBtn label="Unban" color="green" onClick={() => action(`/api/admin/users/${userId}/unban`, "POST", {})} />
            }
            <ActionBtn label="Clear Chats" color="red" onClick={() => action(`/api/admin/users/${userId}/chats`, "DELETE", {}, "Delete ALL chat history for this user?")} />
            <ActionBtn label="Delete Account" color="red" onClick={() => {
              const r = window.prompt("Delete reason?", "user_request");
              if (r) action(`/api/admin/users/${userId}`, "DELETE", { reason: r }, "PERMANENTLY delete this account? This cannot be undone.");
            }} />
            {actionMsg && <span className={`text-xs px-2 py-1 rounded ${actionMsg.startsWith("Error") ? "text-red-400" : "text-emerald-400"}`}>{actionMsg}</span>}
          </div>
        )}

        {/* Sub-tabs */}
        <div className="flex gap-0 border-b border-white/10 overflow-x-auto shrink-0">
          {DETAIL_TABS.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`px-4 py-2.5 text-xs font-medium whitespace-nowrap transition-colors border-b-2 ${
                tab === t.id ? "border-amber-400 text-amber-300" : "border-transparent text-white/40 hover:text-white/70"
              }`}>
              {t.icon} {t.label}
            </button>
          ))}
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5">
          {tab === "profile" && user && <ProfileTab user={user} />}
          {tab === "chats" && (
            <ChatsSubTab chats={chats} loading={subLoading} page={chatPage} pages={chatPages}
              ctx={chatCtx} onCtxChange={(c) => { setChatCtx(c); loadChats(1, c); }}
              onPage={(p) => { setChatPage(p); loadChats(p, chatCtx); }}
              onFlag={(id) => action(`/api/admin/chats/${id}/flag`, "POST", { reason: "admin flag" })}
            />
          )}
          {tab === "kundalis"  && <KundalisSubTab items={kundalis} loading={subLoading} />}
          {tab === "palmistry" && <PalmSubTab items={palmistry} loading={subLoading} />}
          {tab === "payments"  && <PaymentsSubTab items={payments} loading={subLoading} userId={userId} onRefund={(id) => {
            const amt = window.prompt("Refund amount in paise (blank = full)?");
            action(`/api/admin/payments/${id}/refund`, "POST", { amount: amt ? Number(amt) : undefined, reason: "admin refund" }, "Issue refund?");
          }} />}
          {tab === "usage"     && user && <UsageSubTab usage={user.usage_today} />}
          {tab === "logins"    && <LoginsSubTab items={logins} loading={subLoading} />}
        </div>
      </div>
    </div>
  );
}

function ActionBtn({ label, color, onClick }: { label: string; color: "red"|"green"|"blue"|"yellow"; onClick: () => void }) {
  const cls = { red: "bg-red-500/20 text-red-300 hover:bg-red-500/35", green: "bg-emerald-500/20 text-emerald-300 hover:bg-emerald-500/35",
    blue: "bg-blue-500/20 text-blue-300 hover:bg-blue-500/35", yellow: "bg-yellow-500/20 text-yellow-300 hover:bg-yellow-500/35" }[color];
  return <button onClick={onClick} className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${cls}`}>{label}</button>;
}

function ProfileTab({ user }: { user: UserDetail }) {
  const rows: [string, string][] = [
    ["ID", user.id],
    ["Phone", user.phone ?? "—"],
    ["Email", user.email ?? "—"],
    ["Name", user.name ?? "—"],
    ["Role", user.role],
    ["Status", user.status],
    ["Language", user.lang_pref],
    ["Birth Date", user.birth_date ?? "—"],
    ["Birth Time", user.birth_time ?? "—"],
    ["Birth City", user.birth_city ?? "—"],
    ["Current City", user.city ?? "—"],
    ["Plan", user.plan],
    ["Joined", fmt(user.created_at)],
    ["Last Login", fmt(user.last_login)],
    ["Total Chats", String(user.total_chats)],
    ["Total Kundalis", String(user.total_kundalis)],
    ["Palm Readings", String(user.total_palm)],
    ["Lifetime Paid", fmtInr(user.lifetime_paid_inr * 100)],
  ];
  if (user.subscription) {
    rows.push(
      ["Sub Plan", user.subscription.plan + " / " + user.subscription.period],
      ["Sub Status", user.subscription.status],
      ["Sub Started", fmt(user.subscription.started_at)],
      ["Sub Expires", fmt(user.subscription.expires_at)],
      ["Sub Amount", fmtInr(user.subscription.amount_paid)],
      ["Cashfree Order", user.subscription.cashfree_order_id ?? "—"],
    );
  }
  return (
    <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-2">
      {rows.map(([k, v]) => (
        <div key={k} className="flex justify-between py-1.5 border-b border-white/5">
          <dt className="text-white/40 text-xs">{k}</dt>
          <dd className="text-white/80 text-xs font-mono text-right max-w-[55%] truncate">{v}</dd>
        </div>
      ))}
    </dl>
  );
}

function ChatsSubTab({ chats, loading, page, pages, ctx, onCtxChange, onPage, onFlag }:
  { chats: ChatMsg[]; loading: boolean; page: number; pages: number; ctx: string;
    onCtxChange: (c: string) => void; onPage: (p: number) => void; onFlag: (id: string) => void }) {
  const ctxOptions = ["", "kundali", "panchang", "sky", "compatibility", "horoscope", "general"];
  return (
    <div className="space-y-3">
      <div className="flex gap-2 flex-wrap">
        {ctxOptions.map((c) => (
          <button key={c || "all"} onClick={() => onCtxChange(c)}
            className={`px-3 py-1 rounded-full text-xs transition-colors ${ctx === c ? "bg-amber-500/30 text-amber-300" : "bg-white/5 text-white/40 hover:bg-white/10"}`}>
            {c || "All"}
          </button>
        ))}
      </div>
      {loading ? <Loader /> : chats.length === 0 ? <Empty msg="No chat messages." /> : (
        <div className="space-y-2">
          {chats.map((m) => (
            <div key={m.id} className={`rounded-lg px-3 py-2.5 text-xs ${m.flagged ? "border border-red-500/30 bg-red-500/5" : "bg-white/5"}`}>
              <div className="flex items-center gap-2 mb-1 flex-wrap">
                <span className={m.role === "user" ? "text-blue-300 font-semibold" : "text-amber-300 font-semibold"}>{m.role}</span>
                <span className="text-white/30">{m.page_context}</span>
                {m.confidence && <Badge text={m.confidence} cls={m.confidence === "HIGH" ? "bg-emerald-500/20 text-emerald-300" : m.confidence === "MEDIUM" ? "bg-yellow-500/20 text-yellow-300" : "bg-red-500/20 text-red-300"} />}
                {m.tokens_used && <span className="text-white/20">{m.tokens_used} tokens</span>}
                {m.response_ms && <span className="text-white/20">{m.response_ms}ms</span>}
                <span className="text-white/20 ml-auto">{fmt(m.created_at)}</span>
                {!m.flagged && (
                  <button onClick={() => onFlag(m.id)} className="text-white/20 hover:text-red-400 text-xs">⚑</button>
                )}
                {m.flagged && <span className="text-red-400 text-xs">⚑ {m.flag_reason}</span>}
              </div>
              <p className="text-white/70 leading-relaxed whitespace-pre-wrap">{m.content}</p>
            </div>
          ))}
        </div>
      )}
      <Pg page={page} pages={pages} onChange={onPage} />
    </div>
  );
}

function KundalisSubTab({ items, loading }: { items: KundaliEntry[]; loading: boolean }) {
  const [expanded, setExpanded] = useState<number | null>(null);
  if (loading) return <Loader />;
  if (!items.length) return <Empty msg="No kundali calculations." />;
  return (
    <div className="space-y-2">
      {items.map((k) => (
        <div key={k.id} className="rounded-lg bg-white/5 px-4 py-3 text-xs">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-amber-300 font-mono">#{k.id}</span>
            <span className="text-white/70">{k.birth_date} {k.birth_time}</span>
            <span className="text-white/50">{k.birth_city}</span>
            <Badge text={k.source} cls="bg-white/10 text-white/40" />
            {k.is_saved && <Badge text="Saved" cls="bg-emerald-500/20 text-emerald-300" />}
            <span className="text-white/30">{k.calc_ms}ms</span>
            <span className="text-white/20 ml-auto">{fmt(k.created_at)}</span>
            <button onClick={() => setExpanded(expanded === k.id ? null : k.id)}
              className="text-white/30 hover:text-white/60">
              {expanded === k.id ? "▲ less" : "▼ json"}
            </button>
          </div>
          {expanded === k.id && (
            <pre className="mt-2 text-white/40 text-[10px] overflow-x-auto max-h-48">
              {JSON.stringify(k, null, 2)}
            </pre>
          )}
        </div>
      ))}
    </div>
  );
}

function PalmSubTab({ items, loading }: { items: PalmEntry[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items.length) return <Empty msg="No palmistry readings." />;
  return (
    <div className="space-y-2">
      {items.map((p) => (
        <div key={p.id} className="rounded-lg bg-white/5 px-4 py-3 text-xs space-y-1.5">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-amber-300 font-mono">#{p.id}</span>
            {p.confidence && <Badge text={p.confidence} cls="bg-blue-500/20 text-blue-300" />}
            <span className="text-white/30">{p.tokens_used} tokens</span>
            <span className="text-white/20 ml-auto">{fmt(p.created_at)}</span>
          </div>
          {p.lines_found && (
            <div className="flex flex-wrap gap-2 mt-1">
              {Object.entries(p.lines_found).map(([line, val]) => (
                <span key={line} className="bg-white/5 px-2 py-0.5 rounded text-white/50">
                  <span className="text-white/30">{line}:</span> {val}
                </span>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function PaymentsSubTab({ items, loading, userId: _uid, onRefund }: { items: PaymentRow[]; loading: boolean; userId: string; onRefund: (id: number) => void }) {
  if (loading) return <Loader />;
  if (!items.length) return <Empty msg="No payment history." />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead><tr className="border-b border-white/10">
          {["Order ID", "Amount", "Status", "Method", "Fail Reason", "Date", ""].map((h) => (
            <th key={h} className="text-left px-3 py-2 text-white/30 font-medium whitespace-nowrap">{h}</th>
          ))}
        </tr></thead>
        <tbody>
          {items.map((p) => (
            <tr key={p.id} className="border-b border-white/5 hover:bg-white/5">
              <td className="px-3 py-2 font-mono text-white/50 max-w-[120px] truncate">{p.cashfree_order_id}</td>
              <td className="px-3 py-2 text-white/80 font-semibold">{fmtInr(p.amount)}</td>
              <td className="px-3 py-2"><Badge text={p.status} cls={PAY_CLS[p.status] ?? "bg-white/10 text-white/40"} /></td>
              <td className="px-3 py-2 text-white/50">{p.payment_method ?? "—"}</td>
              <td className="px-3 py-2 text-red-300/70">{p.fail_reason ?? "—"}</td>
              <td className="px-3 py-2 text-white/30 whitespace-nowrap">{fmt(p.paid_at)}</td>
              <td className="px-3 py-2">
                {p.status === "SUCCESS" && (
                  <button onClick={() => onRefund(p.id)} className="px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 hover:bg-purple-500/30">Refund</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function UsageSubTab({ usage }: { usage: { feature: string; count: number }[] }) {
  if (!usage?.length) return <Empty msg="No usage data." />;
  return (
    <div className="space-y-2">
      {usage.map((u) => (
        <div key={u.feature} className="flex items-center gap-3 bg-white/5 rounded-lg px-4 py-2.5 text-xs">
          <span className="text-white/60 capitalize flex-1">{u.feature.replace(/_/g, " ")}</span>
          <span className="text-amber-300 font-bold text-base">{u.count}</span>
        </div>
      ))}
    </div>
  );
}

function LoginsSubTab({ items, loading }: { items: LoginEntry[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items.length) return <Empty msg="No login records." />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead><tr className="border-b border-white/10">
          {["Time", "Device", "IP", "Status", "Fail Reason"].map((h) => (
            <th key={h} className="text-left px-3 py-2 text-white/30 font-medium">{h}</th>
          ))}
        </tr></thead>
        <tbody>
          {items.map((l) => (
            <tr key={l.id} className="border-b border-white/5 hover:bg-white/5">
              <td className="px-3 py-2 text-white/40 whitespace-nowrap">{fmt(l.logged_at)}</td>
              <td className="px-3 py-2 text-white/50">{l.device}</td>
              <td className="px-3 py-2 font-mono text-white/40">{l.ip}</td>
              <td className="px-3 py-2">
                <span className={l.success ? "text-emerald-400" : "text-red-400"}>
                  {l.success ? "✓ OK" : "✗ Failed"}
                </span>
              </td>
              <td className="px-3 py-2 text-red-300/60">{l.fail_reason ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ─── Users Tab ────────────────────────────────────────────────────────────────

function UsersTab() {
  const [users, setUsers]     = useState<UserRow[]>([]);
  const [total, setTotal]     = useState(0);
  const [page, setPage]       = useState(1);
  const [pages, setPages]     = useState(1);
  const [search, setSearch]   = useState("");
  const [query, setQuery]     = useState("");
  const [planFilter, setPlan] = useState("");
  const [statusFilter, setSt] = useState("");
  const [loading, setLoading] = useState(true);
  const [detail, setDetail]   = useState<string | null>(null);

  const load = useCallback(async (p: number, q: string, plan: string, status: string) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(p), limit: "25" });
      if (q) params.set("search", q);
      if (plan) params.set("plan", plan);
      if (status) params.set("status", status);
      const d = await aFetch(`/api/admin/users?${params}`);
      setUsers(d.users ?? d.items ?? []);
      setTotal(d.total ?? 0);
      setPage(d.page ?? 1);
      setPages(d.pages ?? 1);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(1, query, planFilter, statusFilter); }, [query, planFilter, statusFilter, load]);

  return (
    <div className="space-y-4">
      {/* Filters */}
      <div className="flex flex-wrap gap-2 items-center">
        <form onSubmit={(e) => { e.preventDefault(); setQuery(search); }} className="flex gap-2 flex-1 min-w-[200px] max-w-sm">
          <input value={search} onChange={(e) => setSearch(e.target.value)}
            placeholder="Search phone, name…"
            className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-white text-sm focus:outline-none focus:border-white/30" />
          <button type="submit" className="px-3 py-1.5 rounded-lg bg-amber-500/80 text-black text-sm font-semibold">🔍</button>
          {query && <button type="button" onClick={() => { setSearch(""); setQuery(""); }}
            className="px-3 py-1.5 rounded-lg bg-white/10 text-white/50 text-sm">✕</button>}
        </form>
        <select value={planFilter} onChange={(e) => { setPlan(e.target.value); setPage(1); }}
          className="bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-white/70 text-sm focus:outline-none">
          <option value="">All Plans</option>
          <option value="free">Free</option>
          <option value="jyotishi">Jyotishi</option>
          <option value="acharya">Acharya</option>
        </select>
        <select value={statusFilter} onChange={(e) => { setSt(e.target.value); setPage(1); }}
          className="bg-white/5 border border-white/10 rounded-lg px-3 py-1.5 text-white/70 text-sm focus:outline-none">
          <option value="">All Status</option>
          <option value="active">Active</option>
          <option value="suspended">Suspended</option>
          <option value="banned">Banned</option>
        </select>
        <span className="text-white/30 text-sm ml-auto">{total} users</span>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-x-auto">
        <table className="w-full text-xs">
          <thead><tr className="border-b border-white/10">
            {["Name", "Phone", "Plan", "Status", "Joined", "Chats", "Kundalis", "Paid", ""].map((h) => (
              <th key={h} className="text-left px-4 py-3 text-white/30 font-medium whitespace-nowrap">{h}</th>
            ))}
          </tr></thead>
          <tbody>
            {loading
              ? <tr><td colSpan={9} className="py-8 text-center text-white/20">Loading…</td></tr>
              : users.length === 0
              ? <tr><td colSpan={9} className="py-8 text-center text-white/20">No users found.</td></tr>
              : users.map((u) => (
                <tr key={u.id} className="border-b border-white/5 hover:bg-white/5 cursor-pointer" onClick={() => setDetail(u.id)}>
                  <td className="px-4 py-2.5 text-white font-medium">{u.name || <span className="text-white/20">—</span>}</td>
                  <td className="px-4 py-2.5 font-mono text-white/60">{u.phone}</td>
                  <td className="px-4 py-2.5"><Badge text={u.plan ?? "free"} cls={PLAN_CLS[u.plan] ?? PLAN_CLS.free} /></td>
                  <td className="px-4 py-2.5"><Badge text={u.status ?? "active"} cls={STATUS_CLS[u.status] ?? STATUS_CLS.active} /></td>
                  <td className="px-4 py-2.5 text-white/30 whitespace-nowrap">{fmt(u.created_at)}</td>
                  <td className="px-4 py-2.5 text-white/50">{u.total_chats}</td>
                  <td className="px-4 py-2.5 text-white/50">{u.total_kundalis}</td>
                  <td className="px-4 py-2.5 text-emerald-300/80 font-mono">{fmtInr(u.lifetime_paid_inr * 100)}</td>
                  <td className="px-4 py-2.5">
                    <button onClick={(e) => { e.stopPropagation(); setDetail(u.id); }}
                      className="px-2 py-1 rounded bg-amber-500/20 text-amber-300 hover:bg-amber-500/30 text-xs">View</button>
                  </td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>
      <Pg page={page} pages={pages} onChange={(p) => load(p, query, planFilter, statusFilter)} />
      {detail && <UserDetailModal userId={detail} onClose={() => setDetail(null)} />}
    </div>
  );
}

// ─── Payments Tab ─────────────────────────────────────────────────────────────

function PaymentsTab() {
  const [payments, setPayments] = useState<PaymentRow[]>([]);
  const [total, setTotal]       = useState(0);
  const [page, setPage]         = useState(1);
  const [pages, setPages]       = useState(1);
  const [statusF, setStatusF]   = useState("");
  const [loading, setLoading]   = useState(true);
  const [revenue, setRevenue]   = useState<{ today: number; month: number; total: number } | null>(null);

  const load = useCallback(async (p: number, status: string) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(p), limit: "30" });
      if (status) params.set("status", status);
      const d = await aFetch(`/api/admin/payments?${params}`);
      setPayments(d.items ?? d.payments ?? []);
      setTotal(d.total ?? 0);
      setPage(d.page ?? 1);
      setPages(d.pages ?? 1);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => {
    load(1, statusF);
    aFetch("/api/admin/revenue").then(setRevenue).catch(() => {});
  }, [statusF, load]);

  const handleRefund = async (id: number) => {
    const amt = window.prompt("Refund amount in paise (blank = full)?");
    if (!window.confirm("Issue refund?")) return;
    try {
      await aFetch(`/api/admin/payments/${id}/refund`, { method: "POST", body: JSON.stringify({ amount: amt ? Number(amt) : undefined, reason: "admin refund" }) });
      load(page, statusF);
    } catch (e) { alert(`Error: ${e instanceof Error ? e.message : e}`); }
  };

  return (
    <div className="space-y-5">
      {/* Revenue summary */}
      {revenue && (
        <div className="grid grid-cols-3 gap-3">
          <StatCard label="Revenue Today" value={fmtInr(revenue.today)}  icon="💰" />
          <StatCard label="Revenue Month" value={fmtInr(revenue.month)}  icon="📊" />
          <StatCard label="Revenue Total" value={fmtInr(revenue.total)}  icon="🏦" />
        </div>
      )}
      {/* Filter */}
      <div className="flex gap-2 flex-wrap items-center">
        {["", "SUCCESS", "FAILED", "PENDING", "REFUNDED"].map((s) => (
          <button key={s || "all"} onClick={() => { setStatusF(s); setPage(1); }}
            className={`px-3 py-1 rounded-full text-xs transition-colors ${statusF === s ? "bg-amber-500/30 text-amber-300" : "bg-white/5 text-white/40 hover:bg-white/10"}`}>
            {s || "All"}
          </button>
        ))}
        <span className="text-white/30 text-sm ml-auto">{total} transactions</span>
      </div>
      {/* Table */}
      <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-x-auto">
        <table className="w-full text-xs">
          <thead><tr className="border-b border-white/10">
            {["User", "Phone", "Order ID", "Amount", "Status", "Method", "Fail Reason", "Date", ""].map((h) => (
              <th key={h} className="text-left px-4 py-3 text-white/30 font-medium whitespace-nowrap">{h}</th>
            ))}
          </tr></thead>
          <tbody>
            {loading
              ? <tr><td colSpan={9} className="py-8 text-center text-white/20">Loading…</td></tr>
              : payments.length === 0
              ? <tr><td colSpan={9} className="py-8 text-center text-white/20">No payments.</td></tr>
              : payments.map((p) => (
                <tr key={p.id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="px-4 py-2.5 text-white/70">{p.user_name ?? "—"}</td>
                  <td className="px-4 py-2.5 font-mono text-white/50">{p.user_phone ?? "—"}</td>
                  <td className="px-4 py-2.5 font-mono text-white/40 max-w-[120px] truncate">{p.cashfree_order_id}</td>
                  <td className="px-4 py-2.5 text-white/80 font-semibold">{fmtInr(p.amount)}</td>
                  <td className="px-4 py-2.5"><Badge text={p.status} cls={PAY_CLS[p.status] ?? "bg-white/10 text-white/40"} /></td>
                  <td className="px-4 py-2.5 text-white/50">{p.payment_method ?? "—"}</td>
                  <td className="px-4 py-2.5 text-red-300/60">{p.fail_reason ?? "—"}</td>
                  <td className="px-4 py-2.5 text-white/30 whitespace-nowrap">{fmt(p.paid_at)}</td>
                  <td className="px-4 py-2.5">
                    {p.status === "SUCCESS" && (
                      <button onClick={() => handleRefund(p.id)} className="px-2 py-1 rounded bg-purple-500/20 text-purple-300 hover:bg-purple-500/30">Refund</button>
                    )}
                  </td>
                </tr>
              ))
            }
          </tbody>
        </table>
      </div>
      <Pg page={page} pages={pages} onChange={(p) => load(p, statusF)} />
    </div>
  );
}

// ─── Chat Monitor Tab ─────────────────────────────────────────────────────────

function ChatMonitorTab() {
  const [mode, setMode] = useState<"recent" | "flagged" | "analytics">("recent");
  const [chats, setChats] = useState<(ChatMsg & { user_name?: string; user_phone?: string })[]>([]);
  const [analytics, setAnalytics] = useState<{ top_questions: { content: string; times: number }[]; context_dist: { page_context: string; count: number }[] } | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage]   = useState(1);
  const [pages, setPages] = useState(1);

  const load = useCallback(async (p: number, m: string) => {
    setLoading(true);
    try {
      if (m === "analytics") {
        const d = await aFetch("/api/admin/analytics/chat");
        setAnalytics(d);
      } else {
        const d = await aFetch(`/api/admin/chats?page=${p}&limit=40${m === "flagged" ? "&flagged=true" : ""}`);
        setChats(d.items ?? d.messages ?? []);
        setPage(d.page ?? 1);
        setPages(d.pages ?? 1);
      }
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(1, mode); }, [mode, load]);

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        {(["recent", "flagged", "analytics"] as const).map((m) => (
          <button key={m} onClick={() => setMode(m)}
            className={`px-4 py-1.5 rounded-full text-xs capitalize transition-colors ${mode === m ? "bg-amber-500/30 text-amber-300" : "bg-white/5 text-white/40 hover:bg-white/10"}`}>
            {m === "flagged" ? "⚑ Flagged" : m === "analytics" ? "📊 Analytics" : "🕐 Recent"}
          </button>
        ))}
      </div>

      {loading ? <Loader /> : mode === "analytics" && analytics ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-hidden">
            <p className="px-4 py-3 border-b border-white/10 text-sm text-white/60">Top Questions (30d)</p>
            <div className="divide-y divide-white/5">
              {analytics.top_questions?.slice(0, 15).map((q, i) => (
                <div key={i} className="flex gap-3 px-4 py-2.5 text-xs">
                  <span className="text-white/20 w-5 shrink-0">{i + 1}</span>
                  <span className="text-white/60 flex-1">{q.content}</span>
                  <span className="text-amber-300 font-bold shrink-0">{q.times}×</span>
                </div>
              ))}
            </div>
          </div>
          <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-hidden">
            <p className="px-4 py-3 border-b border-white/10 text-sm text-white/60">Page Context Distribution</p>
            <div className="divide-y divide-white/5">
              {analytics.context_dist?.map((c) => (
                <div key={c.page_context} className="flex gap-3 px-4 py-2.5 text-xs">
                  <span className="text-white/60 flex-1 capitalize">{c.page_context}</span>
                  <span className="text-amber-300 font-bold">{c.count}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="space-y-2">
          {chats.length === 0 ? <Empty /> : chats.map((m) => (
            <div key={m.id} className={`rounded-lg px-3 py-2.5 text-xs ${m.flagged ? "border border-red-500/30 bg-red-500/5" : "bg-white/5"}`}>
              <div className="flex items-center gap-2 flex-wrap mb-1">
                <span className="text-white/70 font-medium">{m.user_name ?? "Unknown"}</span>
                <span className="text-white/30 font-mono">{m.user_phone}</span>
                <Badge text={m.page_context} cls="bg-white/10 text-white/40" />
                <span className={m.role === "user" ? "text-blue-300" : "text-amber-300"}>{m.role}</span>
                {m.flagged && <span className="text-red-400">⚑ {m.flag_reason}</span>}
                <span className="text-white/20 ml-auto">{fmt(m.created_at)}</span>
              </div>
              <p className="text-white/60 leading-relaxed">{m.content.slice(0, 300)}{m.content.length > 300 ? "…" : ""}</p>
            </div>
          ))}
          <Pg page={page} pages={pages} onChange={(p) => { setPage(p); load(p, mode); }} />
        </div>
      )}
    </div>
  );
}

// ─── Admin Log Tab ────────────────────────────────────────────────────────────

function AdminLogTab() {
  const [logs, setLogs]   = useState<AdminLogEntry[]>([]);
  const [page, setPage]   = useState(1);
  const [pages, setPages] = useState(1);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const d = await aFetch(`/api/admin/logs?page=${p}&limit=50`);
      setLogs(d.items ?? d.logs ?? []);
      setPage(d.page ?? 1);
      setPages(d.pages ?? 1);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(1); }, [load]);

  const ACTION_CLS: Record<string, string> = {
    ban_user: "text-red-300", delete_user: "text-red-400", refund: "text-purple-300",
    change_plan: "text-blue-300", extend_subscription: "text-emerald-300",
    flag_message: "text-yellow-300", cancel_subscription: "text-yellow-300",
  };

  return (
    <div className="space-y-4">
      {loading ? <Loader /> : logs.length === 0 ? <Empty msg="No admin actions yet." /> : (
        <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-x-auto">
          <table className="w-full text-xs">
            <thead><tr className="border-b border-white/10">
              {["Time", "Admin", "Action", "Target", "Details"].map((h) => (
                <th key={h} className="text-left px-4 py-3 text-white/30 font-medium">{h}</th>
              ))}
            </tr></thead>
            <tbody>
              {logs.map((l) => (
                <tr key={l.id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="px-4 py-2.5 text-white/30 whitespace-nowrap">{fmt(l.performed_at)}</td>
                  <td className="px-4 py-2.5 text-white/60">{l.admin_name}</td>
                  <td className={`px-4 py-2.5 font-medium ${ACTION_CLS[l.action] ?? "text-white/70"}`}>{l.action}</td>
                  <td className="px-4 py-2.5 text-white/40 font-mono">{l.target_type}:{l.target_id?.slice(0, 12)}…</td>
                  <td className="px-4 py-2.5 text-white/30 max-w-[200px] truncate">{JSON.stringify(l.details)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Pg page={page} pages={pages} onChange={load} />
    </div>
  );
}

// ─── Login Screen ─────────────────────────────────────────────────────────────

function LoginScreen({ onLogin }: { onLogin: () => void }) {
  const [key, setKey] = useState("");
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setErr(""); setLoading(true);
    sessionStorage.setItem("admin-key", key);
    try {
      await aFetch("/api/admin/stats");
      onLogin();
    } catch {
      setErr("Invalid key.");
      sessionStorage.removeItem("admin-key");
    } finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen bg-[#080810] flex items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <div className="text-5xl mb-3">🔐</div>
          <h1 className="text-white text-2xl font-bold">Brahm AI Admin</h1>
          <p className="text-white/30 text-sm mt-1">Internal Dashboard</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <input type="password" value={key} onChange={(e) => setKey(e.target.value)}
            placeholder="Admin secret key" autoFocus
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-white/20 focus:outline-none focus:border-amber-500/50" />
          {err && <p className="text-red-400 text-sm">{err}</p>}
          <button type="submit" disabled={loading || !key}
            className="w-full py-3 rounded-xl bg-amber-500/80 text-black font-bold hover:bg-amber-400 disabled:opacity-40">
            {loading ? "Checking…" : "Enter"}
          </button>
        </form>
      </div>
    </div>
  );
}

// ─── Main ─────────────────────────────────────────────────────────────────────

type Tab = "dashboard" | "users" | "payments" | "chat" | "adminlog";

const TABS: { id: Tab; label: string; icon: string }[] = [
  { id: "dashboard", label: "Dashboard",    icon: "📊" },
  { id: "users",     label: "Users",        icon: "👥" },
  { id: "payments",  label: "Payments",     icon: "💳" },
  { id: "chat",      label: "Chat Monitor", icon: "💬" },
  { id: "adminlog",  label: "Admin Log",    icon: "📋" },
];

export default function AdminPage() {
  const [authed, setAuthed] = useState(false);
  const [tab, setTab]       = useState<Tab>("dashboard");

  useEffect(() => {
    const key = sessionStorage.getItem("admin-key");
    if (key) aFetch("/api/admin/stats").then(() => setAuthed(true)).catch(() => sessionStorage.removeItem("admin-key"));
  }, []);

  if (!authed) return <LoginScreen onLogin={() => setAuthed(true)} />;

  return (
    <div className="min-h-screen bg-[#080810] text-white flex flex-col">
      <header className="border-b border-white/10 px-5 py-3.5 flex items-center gap-3 shrink-0">
        <span className="text-xl">⚙️</span>
        <h1 className="font-bold">Brahm AI — Admin</h1>
        <div className="ml-auto flex items-center gap-3">
          <a href="/dashboard" className="text-xs text-white/30 hover:text-white/60 underline">← App</a>
          <button onClick={() => { sessionStorage.removeItem("admin-key"); setAuthed(false); }}
            className="text-xs px-3 py-1.5 rounded bg-white/10 text-white/50 hover:bg-white/15">Logout</button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        <nav className="w-44 border-r border-white/10 p-3 space-y-1 shrink-0 hidden sm:block">
          {TABS.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`w-full text-left px-3 py-2.5 rounded-lg text-sm flex items-center gap-2.5 transition-colors ${
                tab === t.id ? "bg-amber-500/20 text-amber-300 font-semibold" : "text-white/40 hover:bg-white/5 hover:text-white/70"
              }`}>
              <span>{t.icon}</span>{t.label}
            </button>
          ))}
        </nav>
        {/* Mobile tab bar */}
        <div className="sm:hidden fixed bottom-0 left-0 right-0 bg-[#0c0c18] border-t border-white/10 flex z-40">
          {TABS.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`flex-1 py-3 flex flex-col items-center gap-0.5 text-[10px] transition-colors ${
                tab === t.id ? "text-amber-300" : "text-white/30"
              }`}>
              <span className="text-base">{t.icon}</span>{t.label.split(" ")[0]}
            </button>
          ))}
        </div>

        {/* Content */}
        <main className="flex-1 p-4 sm:p-6 overflow-y-auto pb-20 sm:pb-6">
          <h2 className="text-lg font-bold text-white mb-5">{TABS.find((t) => t.id === tab)?.label}</h2>
          {tab === "dashboard" && <DashboardTab />}
          {tab === "users"     && <UsersTab />}
          {tab === "payments"  && <PaymentsTab />}
          {tab === "chat"      && <ChatMonitorTab />}
          {tab === "adminlog"  && <AdminLogTab />}
        </main>
      </div>
    </div>
  );
}
