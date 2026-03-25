import { useCallback, useEffect, useState } from "react";
import { aFetch } from "@/lib/api";
import { fmt, fmtInr, PAY_CLS } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { StatCard } from "@/components/ui/StatCard";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Pagination } from "@/components/ui/Pagination";
import { Loader } from "@/components/ui/Loader";
import type { PaymentRow, Revenue } from "@/lib/types";
import { IndianRupee, BarChart3, Landmark } from "lucide-react";

export default function PaymentsPage() {
  const [payments, setPayments] = useState<PaymentRow[]>([]);
  const [total,    setTotal]    = useState(0);
  const [page,     setPage]     = useState(1);
  const [pages,    setPages]    = useState(1);
  const [statusF,  setStatusF]  = useState("");
  const [loading,  setLoading]  = useState(true);
  const [revenue,  setRevenue]  = useState<Revenue | null>(null);

  const load = useCallback(async (p: number, status: string) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(p), limit: "30" });
      if (status) params.set("status", status);
      const d = await aFetch<{ items?: PaymentRow[]; payments?: PaymentRow[]; total: number; page: number; pages: number }>(
        `/admin/payments?${params}`
      );
      setPayments(d.items ?? d.payments ?? []);
      setTotal(d.total ?? 0);
      setPage(d.page ?? 1);
      setPages(d.pages ?? 1);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => {
    load(1, statusF);
    aFetch<Revenue>("/admin/revenue").then(setRevenue).catch(() => {});
  }, [statusF, load]);

  const handleRefund = async (id: number) => {
    const amt = window.prompt("Refund amount in paise (blank = full refund)?");
    if (!window.confirm("Issue refund?")) return;
    try {
      await aFetch(`/admin/payments/${id}/refund`, {
        method: "POST",
        body: JSON.stringify({ amount: amt ? Number(amt) : undefined, reason: "admin refund" }),
      });
      load(page, statusF);
    } catch (e) { alert(`Error: ${e instanceof Error ? e.message : e}`); }
  };

  const columns: Column<PaymentRow>[] = [
    { key: "user",    header: "User",     render: (p) => <span className="text-foreground/70">{p.user_name ?? "—"}</span> },
    { key: "phone",   header: "Phone",    render: (p) => <span className="font-mono text-muted-foreground">{p.user_phone ?? "—"}</span> },
    { key: "order",   header: "Order ID", render: (p) => <span className="font-mono text-muted-foreground text-[11px] max-w-[120px] truncate block">{p.cashfree_order_id}</span> },
    { key: "amount",  header: "Amount",   render: (p) => <span className="text-foreground font-semibold">{fmtInr(p.amount)}</span> },
    { key: "status",  header: "Status",   render: (p) => <Badge text={p.status} cls={PAY_CLS[p.status] ?? "bg-muted text-muted-foreground"} /> },
    { key: "method",  header: "Method",   render: (p) => <span className="text-muted-foreground">{p.payment_method ?? "—"}</span> },
    { key: "fail",    header: "Fail Reason", render: (p) => <span className="text-red-500 text-xs">{p.fail_reason ?? "—"}</span> },
    { key: "date",    header: "Date",     render: (p) => <span className="text-muted-foreground whitespace-nowrap">{fmt(p.paid_at)}</span> },
    { key: "action",  header: "",         render: (p) => (
      p.status === "SUCCESS" ? (
        <button
          onClick={(e) => { e.stopPropagation(); handleRefund(p.id); }}
          className="px-2 py-0.5 rounded bg-purple-50 text-purple-600 hover:bg-purple-100 border border-purple-200 text-xs"
        >
          Refund
        </button>
      ) : null
    )},
  ];

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-foreground font-display">Payments</h1>
        <span className="text-muted-foreground text-sm">{total} transactions</span>
      </div>

      {/* Revenue summary */}
      {revenue ? (
        <div className="grid grid-cols-3 gap-3">
          <StatCard label="Revenue Today" value={fmtInr(revenue.today)} icon={IndianRupee} />
          <StatCard label="Revenue Month" value={fmtInr(revenue.month)} icon={BarChart3} />
          <StatCard label="Revenue Total" value={fmtInr(revenue.total)} icon={Landmark} />
        </div>
      ) : <Loader />}

      {/* Status filter */}
      <div className="flex gap-2 flex-wrap">
        {["", "SUCCESS", "FAILED", "PENDING", "REFUNDED"].map((s) => (
          <button key={s || "all"} onClick={() => { setStatusF(s); setPage(1); }}
            className={`px-3 py-1 rounded-full text-xs transition-colors ${
              statusF === s ? "bg-amber-100 text-amber-700 font-medium" : "bg-muted text-muted-foreground hover:bg-border"
            }`}>
            {s || "All"}
          </button>
        ))}
      </div>

      <DataTable columns={columns} rows={payments} loading={loading} empty="No payments." />
      <Pagination page={page} pages={pages} onChange={(p) => { setPage(p); load(p, statusF); }} />
    </div>
  );
}
