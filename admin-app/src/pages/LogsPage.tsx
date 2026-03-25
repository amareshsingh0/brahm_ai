import { useCallback, useEffect, useState } from "react";
import { aFetch } from "@/lib/api";
import { fmt, ACTION_CLS } from "@/lib/utils";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Pagination } from "@/components/ui/Pagination";
import type { AdminLogEntry } from "@/lib/types";

export default function LogsPage() {
  const [logs,    setLogs]    = useState<AdminLogEntry[]>([]);
  const [page,    setPage]    = useState(1);
  const [pages,   setPages]   = useState(1);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const d = await aFetch<{ items?: AdminLogEntry[]; logs?: AdminLogEntry[]; page: number; pages: number }>(
        `/admin/logs?page=${p}&limit=50`
      );
      setLogs(d.items ?? d.logs ?? []);
      setPage(d.page ?? 1);
      setPages(d.pages ?? 1);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(1); }, [load]);

  const columns: Column<AdminLogEntry>[] = [
    { key: "time",   header: "Time",    render: (l) => <span className="text-muted-foreground whitespace-nowrap">{fmt(l.performed_at)}</span> },
    { key: "admin",  header: "Admin",   render: (l) => <span className="text-foreground/60">{l.admin_name}</span> },
    { key: "action", header: "Action",  render: (l) => <span className={`font-medium ${ACTION_CLS[l.action] ?? "text-foreground/70"}`}>{l.action}</span> },
    { key: "target", header: "Target",  render: (l) => <span className="text-muted-foreground font-mono text-[11px]">{l.target_type}:{l.target_id?.slice(0, 12)}…</span> },
    { key: "detail", header: "Details", render: (l) => <span className="text-muted-foreground max-w-[200px] truncate block">{JSON.stringify(l.details)}</span> },
  ];

  return (
    <div className="space-y-4 animate-fade-in">
      <h1 className="text-xl font-bold text-foreground font-display">Admin Log</h1>
      <DataTable columns={columns} rows={logs} loading={loading} empty="No admin actions yet." />
      <Pagination page={page} pages={pages} onChange={load} />
    </div>
  );
}
