import { fmt, fmtInr, PAY_CLS } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Loader, Empty } from "@/components/ui/Loader";
import type { PaymentRow } from "@/lib/types";

export function PaymentsTab({ items, loading }: { items: PaymentRow[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No payment records." />;
  return (
    <div className="space-y-2">
      {items.map((p) => (
        <div key={p.id} className="rounded-lg bg-muted/50 border border-border/50 px-4 py-3 text-xs space-y-1">
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-amber-700 font-mono">#{p.id}</span>
            <span className="font-semibold text-foreground">{fmtInr(p.amount)}</span>
            <Badge text={p.status} cls={PAY_CLS[p.status] ?? "bg-muted text-muted-foreground"} />
            {p.payment_method && (
              <Badge text={p.payment_method} cls="bg-muted text-muted-foreground" />
            )}
            <span className="text-muted-foreground ml-auto">{fmt(p.paid_at)}</span>
          </div>
          <div className="flex gap-3 flex-wrap text-muted-foreground">
            <span className="font-mono">{p.cashfree_order_id}</span>
            {p.fail_reason && (
              <span className="text-red-500">{p.fail_reason}</span>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
