/**
 * DeletedAccountsPage — Read-only GDPR review panel.
 * Shows accounts deleted in last 30 days. Admin cannot delete data here.
 */
import { useCallback, useEffect, useState } from "react";
import { aFetch } from "@/lib/api";
import { UserX, Clock } from "lucide-react";
import { Pagination } from "@/components/ui/Pagination";
import { Loader, Empty } from "@/components/ui/Loader";

interface DeletedAccount {
  id: string;
  user_id: string;
  name: string | null;
  phone: string | null;
  plan_at_delete: string | null;
  total_chats: number;
  total_payments: number;
  delete_reason: string | null;
  deleted_by: string | null;
  deleted_at: string;
}

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString("en-IN", {
      day: "2-digit", month: "short", year: "numeric",
      hour: "2-digit", minute: "2-digit",
    });
  } catch { return iso; }
}

function daysUntilPurge(deletedAt: string) {
  const diff = 30 - Math.floor((Date.now() - new Date(deletedAt).getTime()) / 86400000);
  return Math.max(0, diff);
}

export default function DeletedAccountsPage() {
  const [accounts, setAccounts] = useState<DeletedAccount[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pages, setPages] = useState(1);
  const [total, setTotal] = useState(0);

  const load = useCallback(async (p: number) => {
    setLoading(true);
    try {
      const d = await aFetch<{ items: DeletedAccount[]; total: number; pages: number }>(
        `/admin/deleted-accounts?page=${p}&limit=30`
      );
      setAccounts(d.items ?? []);
      setTotal(d.total ?? 0);
      setPages(d.pages ?? 1);
      setPage(p);
    } catch {
      setAccounts([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(1); }, [load]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-foreground flex items-center gap-2">
          <UserX className="w-5 h-5 text-red-500" />
          Deleted Accounts
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Accounts deleted in the last 30 days (GDPR review window). Read-only — data auto-purges after 30 days.
          {total > 0 && <span className="ml-1 font-medium">{total} account{total !== 1 ? "s" : ""}.</span>}
        </p>
      </div>

      {loading && <Loader />}
      {!loading && accounts.length === 0 && <Empty message="No deleted accounts in the last 30 days." />}

      {!loading && accounts.length > 0 && (
        <>
          <div className="overflow-x-auto rounded-xl border border-border">
            <table className="w-full text-sm">
              <thead className="bg-muted/30 text-muted-foreground text-xs uppercase">
                <tr>
                  <th className="text-left px-4 py-3">User</th>
                  <th className="text-left px-4 py-3">Plan</th>
                  <th className="text-left px-4 py-3">Chats</th>
                  <th className="text-left px-4 py-3">Payments</th>
                  <th className="text-left px-4 py-3">Reason</th>
                  <th className="text-left px-4 py-3">Deleted By</th>
                  <th className="text-left px-4 py-3">Deleted At</th>
                  <th className="text-left px-4 py-3">Purge In</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {accounts.map((a) => (
                  <tr key={a.id} className="hover:bg-muted/20 transition-colors">
                    <td className="px-4 py-3">
                      <div className="font-medium">{a.name ?? "—"}</div>
                      <div className="text-xs text-muted-foreground">{a.phone ?? a.user_id}</div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full ${
                        a.plan_at_delete === "premium" ? "bg-amber-100 text-amber-700" :
                        a.plan_at_delete === "standard" ? "bg-blue-50 text-blue-600" :
                        "bg-muted text-muted-foreground"
                      }`}>
                        {a.plan_at_delete ?? "free"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-muted-foreground">{a.total_chats}</td>
                    <td className="px-4 py-3 text-muted-foreground">{a.total_payments}</td>
                    <td className="px-4 py-3 text-muted-foreground text-xs">{a.delete_reason ?? "—"}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full ${
                        a.deleted_by === "admin"
                          ? "bg-red-50 text-red-600"
                          : "bg-muted text-muted-foreground"
                      }`}>
                        {a.deleted_by ?? "user"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">{formatDate(a.deleted_at)}</td>
                    <td className="px-4 py-3">
                      <span className="flex items-center gap-1 text-xs text-orange-600">
                        <Clock className="w-3 h-3" />
                        {daysUntilPurge(a.deleted_at)}d
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {pages > 1 && (
            <Pagination page={page} pages={pages} onPage={(p) => load(p)} />
          )}
        </>
      )}
    </div>
  );
}
