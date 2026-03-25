import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { aFetch } from "@/lib/api";
import { fmt, PLAN_CLS, STATUS_CLS } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Pagination } from "@/components/ui/Pagination";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { fmtInr } from "@/lib/utils";
import type { UserRow } from "@/lib/types";

export default function UsersPage() {
  const navigate = useNavigate();

  const [users,  setUsers]  = useState<UserRow[]>([]);
  const [total,  setTotal]  = useState(0);
  const [page,   setPage]   = useState(1);
  const [pages,  setPages]  = useState(1);
  const [search, setSearch] = useState("");
  const [query,  setQuery]  = useState("");
  const [plan,   setPlan]   = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (p: number, q: string, planF: string, statusF: string) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(p), limit: "25" });
      if (q)       params.set("search", q);
      if (planF)   params.set("plan",   planF);
      if (statusF) params.set("status", statusF);
      const d = await aFetch<{ users?: UserRow[]; items?: UserRow[]; total: number; page: number; pages: number }>(
        `/admin/users?${params}`
      );
      setUsers(d.users ?? d.items ?? []);
      setTotal(d.total ?? 0);
      setPage(d.page ?? 1);
      setPages(d.pages ?? 1);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(1, query, plan, status); }, [query, plan, status, load]);

  const columns: Column<UserRow>[] = [
    { key: "name",    header: "Name",    render: (u) => <span className="text-foreground font-medium">{u.name || <span className="text-muted-foreground">—</span>}</span> },
    { key: "phone",   header: "Phone",   render: (u) => <span className="font-mono text-foreground/60">{u.phone}</span> },
    { key: "plan",    header: "Plan",    render: (u) => <Badge text={u.plan ?? "free"} cls={PLAN_CLS[u.plan] ?? PLAN_CLS.free} /> },
    { key: "status",  header: "Status",  render: (u) => <Badge text={u.status ?? "active"} cls={STATUS_CLS[u.status] ?? STATUS_CLS.active} /> },
    { key: "joined",  header: "Joined",  render: (u) => <span className="text-muted-foreground whitespace-nowrap">{fmt(u.created_at)}</span> },
    { key: "chats",   header: "Chats",   render: (u) => <span className="text-foreground/50">{u.total_chats}</span> },
    { key: "paid",    header: "Paid",    render: (u) => <span className="text-emerald-700 font-mono font-semibold">{fmtInr(u.lifetime_paid_inr * 100)}</span> },
    { key: "action",  header: "",        render: (u) => (
      <button
        onClick={(e) => { e.stopPropagation(); navigate(`/users/${u.id}`); }}
        className="px-2 py-1 rounded bg-amber-50 text-amber-700 hover:bg-amber-100 border border-amber-200 text-xs font-medium"
      >
        View
      </button>
    )},
  ];

  return (
    <div className="space-y-4 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-foreground font-display">Users</h1>
        <span className="text-muted-foreground text-sm">{total} total</span>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-2 items-center">
        <form
          onSubmit={(e) => { e.preventDefault(); setQuery(search); setPage(1); }}
          className="flex gap-2 flex-1 min-w-[200px] max-w-sm"
        >
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search phone, name…"
            className="flex-1 bg-white border border-border rounded-lg px-3 py-1.5 text-foreground text-sm focus:outline-none focus:border-amber-400"
          />
          <button type="submit" className="px-3 py-1.5 rounded-lg bg-amber-600 text-white text-sm font-semibold hover:bg-amber-700">
            Search
          </button>
          {query && (
            <button type="button" onClick={() => { setSearch(""); setQuery(""); setPage(1); }}
              className="px-3 py-1.5 rounded-lg bg-muted text-muted-foreground text-sm hover:bg-border">
              ✕
            </button>
          )}
        </form>

        <select value={plan} onChange={(e) => { setPlan(e.target.value); setPage(1); }}
          className="bg-white border border-border rounded-lg px-3 py-1.5 text-foreground text-sm focus:outline-none">
          <option value="">All Plans</option>
          <option value="free">Free</option>
          <option value="jyotishi">Jyotishi</option>
          <option value="acharya">Acharya</option>
        </select>

        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(1); }}
          className="bg-white border border-border rounded-lg px-3 py-1.5 text-foreground text-sm focus:outline-none">
          <option value="">All Status</option>
          <option value="active">Active</option>
          <option value="suspended">Suspended</option>
          <option value="banned">Banned</option>
        </select>
      </div>

      <DataTable
        columns={columns}
        rows={users}
        loading={loading}
        empty="No users found."
        onRowClick={(u) => navigate(`/users/${u.id}`)}
      />
      <Pagination page={page} pages={pages} onChange={(p) => { setPage(p); load(p, query, plan, status); }} />
    </div>
  );
}
