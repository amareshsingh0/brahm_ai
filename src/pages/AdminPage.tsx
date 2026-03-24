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

// ─── API + Cache ──────────────────────────────────────────────────────────────

const BASE = (import.meta.env.VITE_API_URL ?? "https://brahmasmi.bimoraai.com/api").replace(/\/$/, "");

// Module-level cache: path → { data, ts }. GET requests served from cache for 5 min.
const _cache = new Map<string, { data: unknown; ts: number }>();
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes

export function invalidateAdminCache(path?: string) {
  if (path) _cache.delete(path);
  else _cache.clear();
}

async function aFetch(path: string, opts: RequestInit = {}) {
  const key = sessionStorage.getItem("admin-key") ?? "";
  const isGet = !opts.method || opts.method === "GET";

  // Serve from cache for GET requests
  if (isGet) {
    const cached = _cache.get(path);
    if (cached && Date.now() - cached.ts < CACHE_TTL) return cached.data;
  }

  const res = await fetch(`${BASE}${path}`, {
    ...opts,
    headers: { "Content-Type": "application/json", "X-Admin-Key": key, ...(opts.headers ?? {}) },
  });
  if (res.status === 401) { sessionStorage.removeItem("admin-key"); window.location.reload(); }
  if (!res.ok) throw new Error(await res.text());
  const data = await res.json();

  // Store in cache for GET requests, invalidate on mutations
  if (isGet) {
    _cache.set(path, { data, ts: Date.now() });
  } else {
    _cache.clear(); // any mutation clears all cached data
  }
  return data;
}

// Preload all tabs in background — called once on login so tabs open instantly
function preloadAll() {
  const paths = [
    "/api/admin/stats",
    "/api/admin/users?page=1&limit=25",
    "/api/admin/payments?page=1&limit=30",
    "/api/admin/revenue",
    "/api/admin/chats?page=1&limit=40",
    "/api/admin/logs?page=1&limit=50",
  ];
  paths.forEach((p) => aFetch(p).catch(() => {}));
}

// ─── Shared UI ────────────────────────────────────────────────────────────────

const PLAN_CLS: Record<string, string> = {
  free:      "bg-gray-100 text-gray-500",
  jyotishi:  "bg-blue-50 text-blue-600",
  acharya:   "bg-amber-50 text-amber-700",
};
const STATUS_CLS: Record<string, string> = {
  active:    "bg-emerald-50 text-emerald-700",
  suspended: "bg-yellow-50 text-yellow-700",
  banned:    "bg-red-50 text-red-600",
  deleted:   "bg-gray-100 text-gray-400",
};
const PAY_CLS: Record<string, string> = {
  SUCCESS:  "bg-emerald-50 text-emerald-700",
  FAILED:   "bg-red-50 text-red-600",
  PENDING:  "bg-yellow-50 text-yellow-700",
  REFUNDED: "bg-purple-50 text-purple-600",
};

function Badge({ text, cls }: { text: string; cls?: string }) {
  return <span className={`px-2 py-0.5 rounded text-xs font-medium ${cls ?? "bg-gray-100 text-gray-500"}`}>{text}</span>;
}

function StatCard({ label, value, icon, sub }: { label: string; value: string | number; icon: string; sub?: string }) {
  return (
    <div className="rounded-xl border border-border bg-white p-4 flex items-start gap-3 shadow-sm">
      <span className="text-2xl mt-0.5">{icon}</span>
      <div>
        <p className="text-xs text-muted-foreground uppercase tracking-wider">{label}</p>
        <p className="text-2xl font-bold text-foreground">{value}</p>
        {sub && <p className="text-xs text-muted-foreground mt-0.5">{sub}</p>}
      </div>
    </div>
  );
}

function Pg({ page, pages, onChange }: { page: number; pages: number; onChange: (p: number) => void }) {
  if (pages <= 1) return null;
  return (
    <div className="flex items-center gap-2 mt-4 text-sm">
      <button disabled={page === 1} onClick={() => onChange(page - 1)}
        className="px-3 py-1 rounded bg-muted text-muted-foreground disabled:opacity-30 hover:bg-border">← Prev</button>
      <span className="text-muted-foreground">{page} / {pages}</span>
      <button disabled={page === pages} onClick={() => onChange(page + 1)}
        className="px-3 py-1 rounded bg-muted text-muted-foreground disabled:opacity-30 hover:bg-border">Next →</button>
    </div>
  );
}

function Loader() { return <p className="text-muted-foreground py-8 text-center">Loading…</p>; }
function Empty({ msg = "No data." }: { msg?: string }) { return <p className="text-muted-foreground py-8 text-center">{msg}</p>; }

function fmt(ts?: string) {
  if (!ts) return "—";
  return new Date(ts).toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}
function fmtInr(paise?: number) { return paise ? `₹${(paise / 100).toLocaleString("en-IN")}` : "₹0"; }

// ─── Dashboard Tab ────────────────────────────────────────────────────────────

function DashboardTab() {
  const cached = _cache.get("/api/admin/stats");
  const [s, setS] = useState<Stats | null>((cached?.data as Stats) ?? null);
  const [loading, setLoading] = useState(!cached);

  useEffect(() => {
    aFetch("/api/admin/stats").then((d) => setS(d as Stats)).finally(() => setLoading(false));
  }, []);

  if (loading) return <Loader />;
  if (!s) return <Empty msg="Failed to load stats." />;

  return (
    <div className="space-y-6">
      {/* Row 1 — Users */}
      <div>
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Users</p>
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
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Revenue</p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Revenue Today"  value={fmtInr(s.revenue_today)}  icon="💰" />
          <StatCard label="Revenue Month"  value={fmtInr(s.revenue_month)}  icon="📊" />
          <StatCard label="Revenue Total"  value={fmtInr(s.revenue_total)}  icon="🏦" />
          <StatCard label="Paid Users"     value={s.paid_users}              icon="💳" />
        </div>
      </div>
      {/* Row 3 — Activity */}
      <div>
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Today's Activity</p>
        <div className="grid grid-cols-3 gap-3">
          <StatCard label="AI Chats"     value={s.chats_today}    icon="💬" />
          <StatCard label="Kundalis"     value={s.kundalis_today} icon="⭐" />
          <StatCard label="Palm Readings" value={s.palm_today}    icon="🖐" />
        </div>
      </div>
      {/* Row 4 — Subscriptions breakdown */}
      <div>
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Active Subscriptions</p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Jyotishi Monthly" value={s.active_subscriptions?.jyotishi_monthly ?? 0} icon="🌙" />
          <StatCard label="Jyotishi Yearly"  value={s.active_subscriptions?.jyotishi_yearly  ?? 0} icon="🌙" />
          <StatCard label="Acharya Monthly"  value={s.active_subscriptions?.acharya_monthly  ?? 0} icon="⚡" />
          <StatCard label="Acharya Yearly"   value={s.active_subscriptions?.acharya_yearly   ?? 0} icon="⚡" />
        </div>
      </div>
      {/* Top Endpoints */}
      {s.top_endpoints?.length > 0 && (
        <div className="rounded-xl border border-border bg-white overflow-hidden shadow-sm">
          <p className="px-5 py-3 border-b border-border text-sm font-semibold text-foreground">Top Endpoints</p>
          <table className="w-full text-sm">
            <thead><tr className="border-b border-border bg-muted/50">
              <th className="text-left px-5 py-2 text-muted-foreground font-medium">#</th>
              <th className="text-left px-5 py-2 text-muted-foreground font-medium">Endpoint</th>
              <th className="text-right px-5 py-2 text-muted-foreground font-medium">Hits</th>
            </tr></thead>
            <tbody>
              {s.top_endpoints.map((ep, i) => (
                <tr key={ep.endpoint} className="border-b border-border/50 hover:bg-muted/40">
                  <td className="px-5 py-2 text-muted-foreground">{i + 1}</td>
                  <td className="px-5 py-2 font-mono text-foreground/70">{ep.endpoint}</td>
                  <td className="px-5 py-2 text-right text-amber-700 font-semibold">{ep.count}</td>
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
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-2 sm:p-4" onClick={onClose}>
      <div
        className="bg-background border border-border rounded-2xl w-full max-w-4xl max-h-[92vh] flex flex-col overflow-hidden shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-border shrink-0 bg-white">
          <div>
            {loading ? <p className="text-muted-foreground">Loading…</p> : (
              <div className="flex items-center gap-3 flex-wrap">
                <span className="text-foreground font-semibold">{user?.name || "Unknown"}</span>
                <span className="text-muted-foreground text-sm font-mono">{user?.phone}</span>
                <Badge text={user?.plan ?? "free"} cls={PLAN_CLS[user?.plan ?? "free"]} />
                <Badge text={user?.status ?? "active"} cls={STATUS_CLS[user?.status ?? "active"]} />
                {user?.role === "admin" && <Badge text="ADMIN" cls="bg-purple-50 text-purple-600" />}
              </div>
            )}
          </div>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground text-xl px-2">✕</button>
        </div>

        {/* Action bar */}
        {user && (
          <div className="px-5 py-2.5 border-b border-border flex flex-wrap gap-2 shrink-0 bg-muted/30">
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
              ? <ActionBtn label="Suspend" color="yellow" onClick={() => {
                  const r = window.prompt("Suspend reason?");
                  if (r !== null) action(`/api/admin/users/${userId}/suspend`, "POST", {}, "Suspend this user?");
                }} />
              : <ActionBtn label="Unsuspend" color="green" onClick={() => action(`/api/admin/users/${userId}/unsuspend`, "POST", {})} />
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
            {actionMsg && <span className={`text-xs px-2 py-1 rounded ${actionMsg.startsWith("Error") ? "bg-red-50 text-red-600" : "bg-emerald-50 text-emerald-700"}`}>{actionMsg}</span>}
          </div>
        )}

        {/* Sub-tabs */}
        <div className="flex gap-0 border-b border-border overflow-x-auto shrink-0 bg-white">
          {DETAIL_TABS.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`px-4 py-2.5 text-xs font-medium whitespace-nowrap transition-colors border-b-2 ${
                tab === t.id ? "border-amber-600 text-amber-700" : "border-transparent text-muted-foreground hover:text-foreground"
              }`}>
              {t.icon} {t.label}
            </button>
          ))}
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-5 bg-background">
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
  const cls = { red: "bg-red-50 text-red-600 hover:bg-red-100 border border-red-200", green: "bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200",
    blue: "bg-blue-50 text-blue-600 hover:bg-blue-100 border border-blue-200", yellow: "bg-yellow-50 text-yellow-700 hover:bg-yellow-100 border border-yellow-200" }[color];
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
        <div key={k} className="flex justify-between py-1.5 border-b border-border/60">
          <dt className="text-muted-foreground text-xs">{k}</dt>
          <dd className="text-foreground text-xs font-mono text-right max-w-[55%] truncate">{v}</dd>
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
            className={`px-3 py-1 rounded-full text-xs transition-colors ${ctx === c ? "bg-amber-100 text-amber-700 font-medium" : "bg-muted text-muted-foreground hover:bg-border"}`}>
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
                {m.confidence && <Badge text={m.confidence} cls={m.confidence === "HIGH" ? "bg-emerald-50 text-emerald-700" : m.confidence === "MEDIUM" ? "bg-yellow-50 text-yellow-700" : "bg-red-50 text-red-600"} />}
                {m.tokens_used && <span className="text-muted-foreground">{m.tokens_used} tokens</span>}
                {m.response_ms && <span className="text-muted-foreground">{m.response_ms}ms</span>}
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
      <Pg page={page} pages={pages} onChange={onPage} />
    </div>
  );
}

function KundalisSubTab({ items, loading }: { items: KundaliEntry[]; loading: boolean }) {
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
            <pre className="mt-2 text-muted-foreground text-[10px] overflow-x-auto max-h-48">
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

function PaymentsSubTab({ items, loading, userId: _uid, onRefund }: { items: PaymentRow[]; loading: boolean; userId: string; onRefund: (id: number) => void }) {
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No payment history." />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead><tr className="border-b border-border bg-muted/50">
          {["Order ID", "Amount", "Status", "Method", "Fail Reason", "Date", ""].map((h) => (
            <th key={h} className="text-left px-3 py-2 text-muted-foreground font-medium whitespace-nowrap">{h}</th>
          ))}
        </tr></thead>
        <tbody>
          {items.map((p) => (
            <tr key={p.id} className="border-b border-border/50 hover:bg-muted/40">
              <td className="px-3 py-2 font-mono text-muted-foreground max-w-[120px] truncate">{p.cashfree_order_id}</td>
              <td className="px-3 py-2 text-foreground font-semibold">{fmtInr(p.amount)}</td>
              <td className="px-3 py-2"><Badge text={p.status} cls={PAY_CLS[p.status] ?? "bg-muted text-muted-foreground"} /></td>
              <td className="px-3 py-2 text-muted-foreground">{p.payment_method ?? "—"}</td>
              <td className="px-3 py-2 text-red-500">{p.fail_reason ?? "—"}</td>
              <td className="px-3 py-2 text-muted-foreground whitespace-nowrap">{fmt(p.paid_at)}</td>
              <td className="px-3 py-2">
                {p.status === "SUCCESS" && (
                  <button onClick={() => onRefund(p.id)} className="px-2 py-0.5 rounded bg-purple-50 text-purple-600 hover:bg-purple-100 border border-purple-200">Refund</button>
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
        <div key={u.feature} className="flex items-center gap-3 bg-muted/50 border border-border/50 rounded-lg px-4 py-2.5 text-xs">
          <span className="text-foreground/70 capitalize flex-1">{u.feature.replace(/_/g, " ")}</span>
          <span className="text-amber-700 font-bold text-base">{u.count}</span>
        </div>
      ))}
    </div>
  );
}

function LoginsSubTab({ items, loading }: { items: LoginEntry[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No login records." />;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead><tr className="border-b border-border bg-muted/50">
          {["Time", "Device", "IP", "Status", "Fail Reason"].map((h) => (
            <th key={h} className="text-left px-3 py-2 text-muted-foreground font-medium">{h}</th>
          ))}
        </tr></thead>
        <tbody>
          {items.map((l) => (
            <tr key={l.id} className="border-b border-border/50 hover:bg-muted/40">
              <td className="px-3 py-2 text-muted-foreground whitespace-nowrap">{fmt(l.logged_at)}</td>
              <td className="px-3 py-2 text-foreground/60">{l.device}</td>
              <td className="px-3 py-2 font-mono text-muted-foreground">{l.ip}</td>
              <td className="px-3 py-2">
                <span className={l.success ? "text-emerald-700 font-medium" : "text-red-600 font-medium"}>
                  {l.success ? "✓ OK" : "✗ Failed"}
                </span>
              </td>
              <td className="px-3 py-2 text-red-500">{l.fail_reason ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ─── Users Tab ────────────────────────────────────────────────────────────────

function UsersTab() {
  const _ckey = "/api/admin/users?page=1&limit=25";
  const _cd = (_cache.get(_ckey)?.data as { users: UserRow[]; total: number; page: number; pages: number }) ?? null;
  const [users, setUsers]     = useState<UserRow[]>(_cd?.users ?? []);
  const [total, setTotal]     = useState(_cd?.total ?? 0);
  const [page, setPage]       = useState(1);
  const [pages, setPages]     = useState(_cd?.pages ?? 1);
  const [search, setSearch]   = useState("");
  const [query, setQuery]     = useState("");
  const [planFilter, setPlan] = useState("");
  const [statusFilter, setSt] = useState("");
  const [loading, setLoading] = useState(!_cd);
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
            className="flex-1 bg-white border border-border rounded-lg px-3 py-1.5 text-foreground text-sm focus:outline-none focus:border-amber-400" />
          <button type="submit" className="px-3 py-1.5 rounded-lg bg-amber-600 text-white text-sm font-semibold hover:bg-amber-700">🔍</button>
          {query && <button type="button" onClick={() => { setSearch(""); setQuery(""); }}
            className="px-3 py-1.5 rounded-lg bg-muted text-muted-foreground text-sm hover:bg-border">✕</button>}
        </form>
        <select value={planFilter} onChange={(e) => { setPlan(e.target.value); setPage(1); }}
          className="bg-white border border-border rounded-lg px-3 py-1.5 text-foreground text-sm focus:outline-none">
          <option value="">All Plans</option>
          <option value="free">Free</option>
          <option value="jyotishi">Jyotishi</option>
          <option value="acharya">Acharya</option>
        </select>
        <select value={statusFilter} onChange={(e) => { setSt(e.target.value); setPage(1); }}
          className="bg-white border border-border rounded-lg px-3 py-1.5 text-foreground text-sm focus:outline-none">
          <option value="">All Status</option>
          <option value="active">Active</option>
          <option value="suspended">Suspended</option>
          <option value="banned">Banned</option>
        </select>
        <span className="text-muted-foreground text-sm ml-auto">{total} users</span>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-border bg-white overflow-x-auto shadow-sm">
        <table className="w-full text-xs">
          <thead><tr className="border-b border-border bg-muted/50">
            {["Name", "Phone", "Plan", "Status", "Joined", "Chats", "Kundalis", "Paid", ""].map((h) => (
              <th key={h} className="text-left px-4 py-3 text-muted-foreground font-medium whitespace-nowrap">{h}</th>
            ))}
          </tr></thead>
          <tbody>
            {loading
              ? <tr><td colSpan={9} className="py-8 text-center text-muted-foreground">Loading…</td></tr>
              : users.length === 0
              ? <tr><td colSpan={9} className="py-8 text-center text-muted-foreground">No users found.</td></tr>
              : users.map((u) => (
                <tr key={u.id} className="border-b border-border/50 hover:bg-muted/40 cursor-pointer" onClick={() => setDetail(u.id)}>
                  <td className="px-4 py-2.5 text-foreground font-medium">{u.name || <span className="text-muted-foreground">—</span>}</td>
                  <td className="px-4 py-2.5 font-mono text-foreground/60">{u.phone}</td>
                  <td className="px-4 py-2.5"><Badge text={u.plan ?? "free"} cls={PLAN_CLS[u.plan] ?? PLAN_CLS.free} /></td>
                  <td className="px-4 py-2.5"><Badge text={u.status ?? "active"} cls={STATUS_CLS[u.status] ?? STATUS_CLS.active} /></td>
                  <td className="px-4 py-2.5 text-muted-foreground whitespace-nowrap">{fmt(u.created_at)}</td>
                  <td className="px-4 py-2.5 text-foreground/50">{u.total_chats}</td>
                  <td className="px-4 py-2.5 text-foreground/50">{u.total_kundalis}</td>
                  <td className="px-4 py-2.5 text-emerald-700 font-mono font-semibold">{fmtInr(u.lifetime_paid_inr * 100)}</td>
                  <td className="px-4 py-2.5">
                    <button onClick={(e) => { e.stopPropagation(); setDetail(u.id); }}
                      className="px-2 py-1 rounded bg-amber-50 text-amber-700 hover:bg-amber-100 border border-amber-200 text-xs font-medium">View</button>
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
  const _ckey = "/api/admin/payments?page=1&limit=30";
  const _cd = (_cache.get(_ckey)?.data as { items: PaymentRow[]; total: number; pages: number }) ?? null;
  const _rev = (_cache.get("/api/admin/revenue")?.data as { today: number; month: number; total: number }) ?? null;
  const [payments, setPayments] = useState<PaymentRow[]>(_cd?.items ?? []);
  const [total, setTotal]       = useState(_cd?.total ?? 0);
  const [page, setPage]         = useState(1);
  const [pages, setPages]       = useState(_cd?.pages ?? 1);
  const [statusF, setStatusF]   = useState("");
  const [loading, setLoading]   = useState(!_cd);
  const [revenue, setRevenue]   = useState<{ today: number; month: number; total: number } | null>(_rev);

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
            className={`px-3 py-1 rounded-full text-xs transition-colors ${statusF === s ? "bg-amber-100 text-amber-700 font-medium" : "bg-muted text-muted-foreground hover:bg-border"}`}>
            {s || "All"}
          </button>
        ))}
        <span className="text-muted-foreground text-sm ml-auto">{total} transactions</span>
      </div>
      {/* Table */}
      <div className="rounded-xl border border-border bg-white overflow-x-auto shadow-sm">
        <table className="w-full text-xs">
          <thead><tr className="border-b border-border bg-muted/50">
            {["User", "Phone", "Order ID", "Amount", "Status", "Method", "Fail Reason", "Date", ""].map((h) => (
              <th key={h} className="text-left px-4 py-3 text-muted-foreground font-medium whitespace-nowrap">{h}</th>
            ))}
          </tr></thead>
          <tbody>
            {loading
              ? <tr><td colSpan={9} className="py-8 text-center text-muted-foreground">Loading…</td></tr>
              : payments.length === 0
              ? <tr><td colSpan={9} className="py-8 text-center text-muted-foreground">No payments.</td></tr>
              : payments.map((p) => (
                <tr key={p.id} className="border-b border-border/50 hover:bg-muted/40">
                  <td className="px-4 py-2.5 text-foreground/70">{p.user_name ?? "—"}</td>
                  <td className="px-4 py-2.5 font-mono text-muted-foreground">{p.user_phone ?? "—"}</td>
                  <td className="px-4 py-2.5 font-mono text-muted-foreground max-w-[120px] truncate">{p.cashfree_order_id}</td>
                  <td className="px-4 py-2.5 text-foreground font-semibold">{fmtInr(p.amount)}</td>
                  <td className="px-4 py-2.5"><Badge text={p.status} cls={PAY_CLS[p.status] ?? "bg-muted text-muted-foreground"} /></td>
                  <td className="px-4 py-2.5 text-muted-foreground">{p.payment_method ?? "—"}</td>
                  <td className="px-4 py-2.5 text-red-500">{p.fail_reason ?? "—"}</td>
                  <td className="px-4 py-2.5 text-muted-foreground whitespace-nowrap">{fmt(p.paid_at)}</td>
                  <td className="px-4 py-2.5">
                    {p.status === "SUCCESS" && (
                      <button onClick={() => handleRefund(p.id)} className="px-2 py-1 rounded bg-purple-50 text-purple-600 hover:bg-purple-100 border border-purple-200">Refund</button>
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
  const _ckey = "/api/admin/chats?page=1&limit=40";
  const _cd = (_cache.get(_ckey)?.data as { items: (ChatMsg & { user_name?: string; user_phone?: string })[]; pages: number }) ?? null;
  const [mode, setMode] = useState<"recent" | "flagged" | "analytics">("recent");
  const [chats, setChats] = useState<(ChatMsg & { user_name?: string; user_phone?: string })[]>(_cd?.items ?? []);
  const [analytics, setAnalytics] = useState<{ top_questions: { content: string; times: number }[]; context_dist: { page_context: string; count: number }[] } | null>(null);
  const [loading, setLoading] = useState(!_cd);
  const [page, setPage]   = useState(1);
  const [pages, setPages] = useState(_cd?.pages ?? 1);

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
            className={`px-4 py-1.5 rounded-full text-xs capitalize transition-colors ${mode === m ? "bg-amber-100 text-amber-700 font-medium" : "bg-muted text-muted-foreground hover:bg-border"}`}>
            {m === "flagged" ? "⚑ Flagged" : m === "analytics" ? "📊 Analytics" : "🕐 Recent"}
          </button>
        ))}
      </div>

      {loading ? <Loader /> : mode === "analytics" && analytics ? (
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
      ) : (
        <div className="space-y-2">
          {!chats?.length ? <Empty /> : chats.map((m) => (
            <div key={m.id} className={`rounded-lg px-3 py-2.5 text-xs ${m.flagged ? "border border-red-200 bg-red-50" : "bg-muted/50 border border-border/50"}`}>
              <div className="flex items-center gap-2 flex-wrap mb-1">
                <span className="text-foreground font-medium">{m.user_name ?? "Unknown"}</span>
                <span className="text-muted-foreground font-mono">{m.user_phone}</span>
                <Badge text={m.page_context} cls="bg-muted text-muted-foreground" />
                <span className={m.role === "user" ? "text-blue-600" : "text-amber-700"}>{m.role}</span>
                {m.flagged && <span className="text-red-600">⚑ {m.flag_reason}</span>}
                <span className="text-muted-foreground ml-auto">{fmt(m.created_at)}</span>
              </div>
              <p className="text-foreground/70 leading-relaxed">{m.content.slice(0, 300)}{m.content.length > 300 ? "…" : ""}</p>
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
  const _ckey = "/api/admin/logs?page=1&limit=50";
  const _cd = (_cache.get(_ckey)?.data as { items: AdminLogEntry[]; pages: number }) ?? null;
  const [logs, setLogs]   = useState<AdminLogEntry[]>(_cd?.items ?? []);
  const [page, setPage]   = useState(1);
  const [pages, setPages] = useState(_cd?.pages ?? 1);
  const [loading, setLoading] = useState(!_cd);

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
    ban_user: "text-red-600", delete_user: "text-red-700", refund: "text-purple-600",
    change_plan: "text-blue-600", extend_subscription: "text-emerald-700",
    flag_message: "text-yellow-700", cancel_subscription: "text-yellow-700",
  };

  return (
    <div className="space-y-4">
      {loading ? <Loader /> : logs.length === 0 ? <Empty msg="No admin actions yet." /> : (
        <div className="rounded-xl border border-border bg-white overflow-x-auto shadow-sm">
          <table className="w-full text-xs">
            <thead><tr className="border-b border-border bg-muted/50">
              {["Time", "Admin", "Action", "Target", "Details"].map((h) => (
                <th key={h} className="text-left px-4 py-3 text-muted-foreground font-medium">{h}</th>
              ))}
            </tr></thead>
            <tbody>
              {logs.map((l) => (
                <tr key={l.id} className="border-b border-border/50 hover:bg-muted/40">
                  <td className="px-4 py-2.5 text-muted-foreground whitespace-nowrap">{fmt(l.performed_at)}</td>
                  <td className="px-4 py-2.5 text-foreground/60">{l.admin_name}</td>
                  <td className={`px-4 py-2.5 font-medium ${ACTION_CLS[l.action] ?? "text-foreground/70"}`}>{l.action}</td>
                  <td className="px-4 py-2.5 text-muted-foreground font-mono">{l.target_type}:{l.target_id?.slice(0, 12)}…</td>
                  <td className="px-4 py-2.5 text-muted-foreground max-w-[200px] truncate">{JSON.stringify(l.details)}</td>
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
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <div className="text-5xl mb-3">🔐</div>
          <h1 className="text-foreground text-2xl font-bold">Brahm AI Admin</h1>
          <p className="text-muted-foreground text-sm mt-1">Internal Dashboard</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <input type="password" value={key} onChange={(e) => setKey(e.target.value)}
            placeholder="Admin secret key" autoFocus
            className="w-full bg-white border border-border rounded-xl px-4 py-3 text-foreground placeholder-muted-foreground focus:outline-none focus:border-amber-400" />
          {err && <p className="text-red-600 text-sm">{err}</p>}
          <button type="submit" disabled={loading || !key}
            className="w-full py-3 rounded-xl bg-amber-600 text-white font-bold hover:bg-amber-700 disabled:opacity-40">
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
    if (key) {
      aFetch("/api/admin/stats")
        .then(() => { setAuthed(true); preloadAll(); })
        .catch(() => sessionStorage.removeItem("admin-key"));
    }
  }, []);

  const handleLogin = () => { setAuthed(true); preloadAll(); };

  if (!authed) return <LoginScreen onLogin={handleLogin} />;

  return (
    <div className="min-h-screen bg-background text-foreground flex flex-col">
      <header className="border-b border-border bg-white px-5 py-3.5 flex items-center gap-3 shrink-0 shadow-sm">
        <span className="text-xl">⚙️</span>
        <h1 className="font-bold text-foreground">Brahm AI — Admin</h1>
        <div className="ml-auto flex items-center gap-3">
          <a href="/dashboard" className="text-xs text-muted-foreground hover:text-foreground underline">← App</a>
          <button onClick={() => { sessionStorage.removeItem("admin-key"); setAuthed(false); }}
            className="text-xs px-3 py-1.5 rounded bg-muted text-muted-foreground hover:bg-border border border-border">Logout</button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        <nav className="w-44 border-r border-border bg-white p-3 space-y-1 shrink-0 hidden sm:block">
          {TABS.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`w-full text-left px-3 py-2.5 rounded-lg text-sm flex items-center gap-2.5 transition-colors ${
                tab === t.id ? "bg-amber-50 text-amber-700 font-semibold border border-amber-200" : "text-muted-foreground hover:bg-muted hover:text-foreground"
              }`}>
              <span>{t.icon}</span>{t.label}
            </button>
          ))}
        </nav>
        {/* Mobile tab bar */}
        <div className="sm:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-border flex z-40 shadow-md">
          {TABS.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`flex-1 py-3 flex flex-col items-center gap-0.5 text-[10px] transition-colors ${
                tab === t.id ? "text-amber-700 font-semibold" : "text-muted-foreground"
              }`}>
              <span className="text-base">{t.icon}</span>{t.label.split(" ")[0]}
            </button>
          ))}
        </div>

        {/* Content */}
        <main className="flex-1 p-4 sm:p-6 overflow-y-auto pb-20 sm:pb-6 bg-background">
          <h2 className="text-lg font-bold text-foreground mb-5">{TABS.find((t) => t.id === tab)?.label}</h2>
          <div className={tab === "dashboard" ? "" : "hidden"}><DashboardTab /></div>
          <div className={tab === "users"     ? "" : "hidden"}><UsersTab /></div>
          <div className={tab === "payments"  ? "" : "hidden"}><PaymentsTab /></div>
          <div className={tab === "chat"      ? "" : "hidden"}><ChatMonitorTab /></div>
          <div className={tab === "adminlog"  ? "" : "hidden"}><AdminLogTab /></div>
        </main>
      </div>
    </div>
  );
}
