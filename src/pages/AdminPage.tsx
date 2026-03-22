/**
 * Brahm AI — Admin Panel
 * Route: /admin
 * Auth:  X-Admin-Key header (set ADMIN_SECRET env var on the VM)
 *        Key is stored in sessionStorage — clears on browser close.
 */
import { useState, useEffect, useCallback } from "react";

// ─── Types ───────────────────────────────────────────────────────────────────

interface Stats {
  total_users: number;
  total_requests: number;
  requests_today: number;
  active_users_today: number;
  top_endpoints: { endpoint: string; count: number }[];
}

interface UserRow {
  session_id: string;
  name: string;
  place: string;
  date: string;
  rashi: string;
  nakshatra: string;
  language: string;
  plan: string;
}

interface LogRow {
  id: number;
  ts: string;
  method: string;
  endpoint: string;
  session_id: string;
  status_code: number;
  duration_ms: number;
}

// ─── API helpers ─────────────────────────────────────────────────────────────

const BASE =
  (import.meta.env.VITE_API_URL ?? "https://brahmasmi.bimoraai.com/api")
    .replace(/\/$/, "");

async function adminFetch(path: string, opts: RequestInit = {}) {
  const key = sessionStorage.getItem("admin-key") ?? "";
  const res = await fetch(`${BASE}${path}`, {
    ...opts,
    headers: {
      "Content-Type": "application/json",
      "X-Admin-Key": key,
      ...(opts.headers ?? {}),
    },
  });
  if (res.status === 401) {
    sessionStorage.removeItem("admin-key");
    window.location.reload();
  }
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ─── Sub-components ──────────────────────────────────────────────────────────

function StatCard({
  label,
  value,
  icon,
  color,
}: {
  label: string;
  value: string | number;
  icon: string;
  color: string;
}) {
  return (
    <div className={`rounded-xl border p-5 flex items-center gap-4 bg-[#0f0f1a] border-white/10`}>
      <div
        className={`w-12 h-12 rounded-lg flex items-center justify-center text-2xl ${color}`}
      >
        {icon}
      </div>
      <div>
        <p className="text-xs text-white/50 uppercase tracking-wider">{label}</p>
        <p className="text-2xl font-bold text-white mt-0.5">{value}</p>
      </div>
    </div>
  );
}

const PLAN_COLORS: Record<string, string> = {
  free:      "bg-white/10 text-white/60",
  jyotishi:  "bg-blue-500/20 text-blue-300",
  acharya:   "bg-amber-500/20 text-amber-300",
};

const METHOD_COLORS: Record<string, string> = {
  GET:    "bg-emerald-500/20 text-emerald-300",
  POST:   "bg-blue-500/20 text-blue-300",
  PUT:    "bg-amber-500/20 text-amber-300",
  DELETE: "bg-red-500/20 text-red-300",
};

function Badge({ text, colorClass }: { text: string; colorClass: string }) {
  return (
    <span className={`px-2 py-0.5 rounded text-xs font-medium ${colorClass}`}>
      {text}
    </span>
  );
}

function Pagination({
  page,
  pages,
  onChange,
}: {
  page: number;
  pages: number;
  onChange: (p: number) => void;
}) {
  if (pages <= 1) return null;
  return (
    <div className="flex items-center gap-2 mt-4">
      <button
        disabled={page === 1}
        onClick={() => onChange(page - 1)}
        className="px-3 py-1 rounded bg-white/10 text-white/70 disabled:opacity-30 text-sm hover:bg-white/20"
      >
        ← Prev
      </button>
      <span className="text-white/50 text-sm">
        {page} / {pages}
      </span>
      <button
        disabled={page === pages}
        onClick={() => onChange(page + 1)}
        className="px-3 py-1 rounded bg-white/10 text-white/70 disabled:opacity-30 text-sm hover:bg-white/20"
      >
        Next →
      </button>
    </div>
  );
}

// ─── Tabs ─────────────────────────────────────────────────────────────────────

function DashboardTab() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    adminFetch("/admin/stats")
      .then(setStats)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="text-white/40 mt-8">Loading stats…</p>;
  if (!stats)  return <p className="text-red-400 mt-8">Failed to load stats.</p>;

  return (
    <div className="space-y-8">
      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Users"       value={stats.total_users}        icon="👤" color="bg-purple-500/20" />
        <StatCard label="Total Requests"    value={stats.total_requests}     icon="📡" color="bg-blue-500/20" />
        <StatCard label="Requests Today"    value={stats.requests_today}     icon="📈" color="bg-emerald-500/20" />
        <StatCard label="Active Users Today" value={stats.active_users_today} icon="🌟" color="bg-amber-500/20" />
      </div>

      {/* Top endpoints */}
      <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-hidden">
        <div className="px-5 py-4 border-b border-white/10">
          <h3 className="text-sm font-semibold text-white/80">Top Endpoints</h3>
        </div>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/10">
              <th className="text-left px-5 py-3 text-white/40 font-medium">#</th>
              <th className="text-left px-5 py-3 text-white/40 font-medium">Endpoint</th>
              <th className="text-right px-5 py-3 text-white/40 font-medium">Requests</th>
            </tr>
          </thead>
          <tbody>
            {stats.top_endpoints.map((ep, i) => (
              <tr key={ep.endpoint} className="border-b border-white/5 hover:bg-white/5">
                <td className="px-5 py-3 text-white/30">{i + 1}</td>
                <td className="px-5 py-3 font-mono text-white/80">{ep.endpoint}</td>
                <td className="px-5 py-3 text-right text-amber-300 font-semibold">{ep.count}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function EditPlanModal({
  user,
  onClose,
  onSaved,
}: {
  user: UserRow;
  onClose: () => void;
  onSaved: (updated: UserRow) => void;
}) {
  const [plan, setPlan] = useState(user.plan);
  const [name, setName] = useState(user.name);
  const [saving, setSaving] = useState(false);

  const save = async () => {
    setSaving(true);
    try {
      const updated = await adminFetch(`/admin/users/${user.session_id}`, {
        method: "PUT",
        body: JSON.stringify({ plan, name }),
      });
      onSaved({ ...user, plan: updated.plan ?? plan, name: updated.name ?? name });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-[#0f0f1a] border border-white/15 rounded-2xl p-6 w-full max-w-sm space-y-4">
        <h3 className="text-white font-semibold">Edit User</h3>
        <p className="text-white/40 text-xs font-mono break-all">{user.session_id}</p>
        <div className="space-y-3">
          <div>
            <label className="text-xs text-white/50 block mb-1">Name</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-white/30"
            />
          </div>
          <div>
            <label className="text-xs text-white/50 block mb-1">Plan</label>
            <select
              value={plan}
              onChange={(e) => setPlan(e.target.value)}
              className="w-full bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-white/30"
            >
              <option value="free">Free</option>
              <option value="jyotishi">Jyotishi</option>
              <option value="acharya">Acharya</option>
            </select>
          </div>
        </div>
        <div className="flex gap-3 pt-2">
          <button
            onClick={onClose}
            className="flex-1 py-2 rounded-lg bg-white/10 text-white/70 text-sm hover:bg-white/15"
          >
            Cancel
          </button>
          <button
            onClick={save}
            disabled={saving}
            className="flex-1 py-2 rounded-lg bg-amber-500/80 text-black font-semibold text-sm hover:bg-amber-400 disabled:opacity-50"
          >
            {saving ? "Saving…" : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}

function UsersTab() {
  const [users, setUsers]   = useState<UserRow[]>([]);
  const [total, setTotal]   = useState(0);
  const [page, setPage]     = useState(1);
  const [pages, setPages]   = useState(1);
  const [search, setSearch] = useState("");
  const [query, setQuery]   = useState("");
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<UserRow | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);

  const load = useCallback(async (p: number, q: string) => {
    setLoading(true);
    try {
      const data = await adminFetch(
        `/admin/users?page=${p}&limit=20&search=${encodeURIComponent(q)}`
      );
      setUsers(data.users);
      setTotal(data.total);
      setPage(data.page);
      setPages(data.pages);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(1, query); }, [query, load]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setQuery(search);
  };

  const handleDelete = async (sid: string) => {
    if (!window.confirm("Delete this user? This cannot be undone.")) return;
    setDeleting(sid);
    try {
      await adminFetch(`/admin/users/${sid}`, { method: "DELETE" });
      setUsers((u) => u.filter((x) => x.session_id !== sid));
      setTotal((t) => t - 1);
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div className="space-y-4">
      {/* Search + count */}
      <div className="flex items-center gap-3">
        <form onSubmit={handleSearch} className="flex gap-2 flex-1 max-w-md">
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, place, rashi…"
            className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-white/30"
          />
          <button
            type="submit"
            className="px-4 py-2 rounded-lg bg-amber-500/80 text-black text-sm font-semibold hover:bg-amber-400"
          >
            Search
          </button>
          {query && (
            <button
              type="button"
              onClick={() => { setSearch(""); setQuery(""); }}
              className="px-3 py-2 rounded-lg bg-white/10 text-white/60 text-sm hover:bg-white/15"
            >
              Clear
            </button>
          )}
        </form>
        <span className="text-white/40 text-sm ml-auto">{total} users</span>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/10">
              {["Name", "Place", "Birth Date", "Rashi", "Nakshatra", "Language", "Plan", "Actions"].map((h) => (
                <th key={h} className="text-left px-4 py-3 text-white/40 font-medium whitespace-nowrap">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-white/30">
                  Loading…
                </td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-white/30">
                  No users found.
                </td>
              </tr>
            ) : (
              users.map((u) => (
                <tr key={u.session_id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="px-4 py-3 text-white font-medium">{u.name || <span className="text-white/30">—</span>}</td>
                  <td className="px-4 py-3 text-white/70">{u.place || "—"}</td>
                  <td className="px-4 py-3 text-white/70 font-mono text-xs">{u.date || "—"}</td>
                  <td className="px-4 py-3 text-white/80">{u.rashi || "—"}</td>
                  <td className="px-4 py-3 text-white/70 text-xs">{u.nakshatra || "—"}</td>
                  <td className="px-4 py-3 text-white/60 capitalize">{u.language}</td>
                  <td className="px-4 py-3">
                    <Badge
                      text={u.plan || "free"}
                      colorClass={PLAN_COLORS[u.plan] ?? PLAN_COLORS.free}
                    />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        onClick={() => setEditing(u)}
                        className="px-2 py-1 rounded bg-blue-500/20 text-blue-300 text-xs hover:bg-blue-500/30"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(u.session_id)}
                        disabled={deleting === u.session_id}
                        className="px-2 py-1 rounded bg-red-500/20 text-red-300 text-xs hover:bg-red-500/30 disabled:opacity-40"
                      >
                        {deleting === u.session_id ? "…" : "Del"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={page} pages={pages} onChange={(p) => load(p, query)} />

      {editing && (
        <EditPlanModal
          user={editing}
          onClose={() => setEditing(null)}
          onSaved={(updated) => {
            setUsers((us) =>
              us.map((u) => (u.session_id === updated.session_id ? updated : u))
            );
            setEditing(null);
          }}
        />
      )}
    </div>
  );
}

function LogsTab() {
  const [logs, setLogs]     = useState<LogRow[]>([]);
  const [total, setTotal]   = useState(0);
  const [page, setPage]     = useState(1);
  const [pages, setPages]   = useState(1);
  const [filter, setFilter] = useState("");
  const [query, setQuery]   = useState("");
  const [loading, setLoading] = useState(true);
  const [clearing, setClearing] = useState(false);

  const load = useCallback(async (p: number, q: string) => {
    setLoading(true);
    try {
      const data = await adminFetch(
        `/admin/logs?page=${p}&limit=50&endpoint_filter=${encodeURIComponent(q)}`
      );
      setLogs(data.logs);
      setTotal(data.total);
      setPage(data.page);
      setPages(data.pages);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(1, query); }, [query, load]);

  const handleClear = async () => {
    if (!window.confirm("Clear ALL activity logs? This cannot be undone.")) return;
    setClearing(true);
    try {
      await adminFetch("/admin/logs", { method: "DELETE" });
      setLogs([]);
      setTotal(0);
      setPages(1);
    } finally {
      setClearing(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <form
          onSubmit={(e) => { e.preventDefault(); setQuery(filter); }}
          className="flex gap-2 flex-1 max-w-md"
        >
          <input
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder="Filter by endpoint…"
            className="flex-1 bg-white/5 border border-white/10 rounded-lg px-3 py-2 text-white text-sm focus:outline-none focus:border-white/30"
          />
          <button
            type="submit"
            className="px-4 py-2 rounded-lg bg-amber-500/80 text-black text-sm font-semibold hover:bg-amber-400"
          >
            Filter
          </button>
          {query && (
            <button
              type="button"
              onClick={() => { setFilter(""); setQuery(""); }}
              className="px-3 py-2 rounded-lg bg-white/10 text-white/60 text-sm hover:bg-white/15"
            >
              Clear
            </button>
          )}
        </form>
        <span className="text-white/40 text-sm">{total} logs</span>
        <button
          onClick={handleClear}
          disabled={clearing}
          className="px-3 py-2 rounded-lg bg-red-500/20 text-red-300 text-sm hover:bg-red-500/30 disabled:opacity-40 ml-auto"
        >
          {clearing ? "Clearing…" : "Clear All Logs"}
        </button>
      </div>

      <div className="rounded-xl border border-white/10 bg-[#0f0f1a] overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-white/10">
              {["Time", "Method", "Endpoint", "Session", "Status", "ms"].map((h) => (
                <th key={h} className="text-left px-4 py-3 text-white/40 font-medium whitespace-nowrap">
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-white/30">Loading…</td>
              </tr>
            ) : logs.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-white/30">No logs yet.</td>
              </tr>
            ) : (
              logs.map((l) => (
                <tr key={l.id} className="border-b border-white/5 hover:bg-white/5">
                  <td className="px-4 py-2 text-white/50 font-mono text-xs whitespace-nowrap">{l.ts}</td>
                  <td className="px-4 py-2">
                    <Badge text={l.method} colorClass={METHOD_COLORS[l.method] ?? "bg-white/10 text-white/50"} />
                  </td>
                  <td className="px-4 py-2 font-mono text-white/70 text-xs">{l.endpoint}</td>
                  <td className="px-4 py-2 font-mono text-white/40 text-xs max-w-[120px] truncate">
                    {l.session_id || "—"}
                  </td>
                  <td className="px-4 py-2">
                    <span className={`text-xs font-mono ${l.status_code >= 400 ? "text-red-400" : "text-emerald-400"}`}>
                      {l.status_code || "—"}
                    </span>
                  </td>
                  <td className="px-4 py-2 text-white/40 text-xs">{l.duration_ms || "—"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <Pagination page={page} pages={pages} onChange={(p) => load(p, query)} />
    </div>
  );
}

// ─── Login screen ────────────────────────────────────────────────────────────

function LoginScreen({ onLogin }: { onLogin: () => void }) {
  const [key, setKey]   = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    sessionStorage.setItem("admin-key", key);
    try {
      await adminFetch("/admin/stats");
      onLogin();
    } catch {
      setError("Wrong admin key. Try again.");
      sessionStorage.removeItem("admin-key");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#080810] flex items-center justify-center p-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <div className="text-5xl mb-3">🔐</div>
          <h1 className="text-white text-2xl font-bold">Admin Panel</h1>
          <p className="text-white/40 text-sm mt-1">Brahm AI — Internal Dashboard</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <input
            type="password"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            placeholder="Admin secret key"
            autoFocus
            className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-white/30 focus:outline-none focus:border-amber-500/50"
          />
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <button
            type="submit"
            disabled={loading || !key}
            className="w-full py-3 rounded-xl bg-amber-500/80 text-black font-bold hover:bg-amber-400 disabled:opacity-40"
          >
            {loading ? "Checking…" : "Enter"}
          </button>
        </form>
      </div>
    </div>
  );
}

// ─── Main admin layout ───────────────────────────────────────────────────────

type Tab = "dashboard" | "users" | "logs";

const TABS: { id: Tab; label: string; icon: string }[] = [
  { id: "dashboard", label: "Dashboard",  icon: "📊" },
  { id: "users",     label: "Users",      icon: "👥" },
  { id: "logs",      label: "Activity",   icon: "📋" },
];

export default function AdminPage() {
  const [authed, setAuthed] = useState(false);
  const [tab, setTab]       = useState<Tab>("dashboard");

  // Check if already logged in
  useEffect(() => {
    const key = sessionStorage.getItem("admin-key");
    if (key) {
      adminFetch("/admin/stats")
        .then(() => setAuthed(true))
        .catch(() => sessionStorage.removeItem("admin-key"));
    }
  }, []);

  if (!authed) return <LoginScreen onLogin={() => setAuthed(true)} />;

  return (
    <div className="min-h-screen bg-[#080810] text-white flex flex-col">
      {/* Header */}
      <header className="border-b border-white/10 px-6 py-4 flex items-center gap-4">
        <span className="text-xl">⚙️</span>
        <h1 className="font-bold text-lg">Brahm AI — Admin</h1>
        <div className="ml-auto flex items-center gap-3">
          <a
            href="/dashboard"
            className="text-xs text-white/40 hover:text-white/70 underline"
          >
            ← Back to App
          </a>
          <button
            onClick={() => {
              sessionStorage.removeItem("admin-key");
              setAuthed(false);
            }}
            className="text-xs px-3 py-1.5 rounded bg-white/10 text-white/60 hover:bg-white/15"
          >
            Logout
          </button>
        </div>
      </header>

      <div className="flex flex-1">
        {/* Sidebar */}
        <nav className="w-48 border-r border-white/10 p-4 space-y-1 shrink-0">
          {TABS.map((t) => (
            <button
              key={t.id}
              onClick={() => setTab(t.id)}
              className={`w-full text-left px-3 py-2.5 rounded-lg text-sm flex items-center gap-2.5 transition-colors ${
                tab === t.id
                  ? "bg-amber-500/20 text-amber-300 font-semibold"
                  : "text-white/50 hover:bg-white/5 hover:text-white/80"
              }`}
            >
              <span>{t.icon}</span>
              {t.label}
            </button>
          ))}
        </nav>

        {/* Content */}
        <main className="flex-1 p-6 overflow-auto">
          <h2 className="text-xl font-bold text-white mb-6">
            {TABS.find((t) => t.id === tab)?.label}
          </h2>
          {tab === "dashboard" && <DashboardTab />}
          {tab === "users"     && <UsersTab />}
          {tab === "logs"      && <LogsTab />}
        </main>
      </div>
    </div>
  );
}
