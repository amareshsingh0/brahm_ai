import { fmt, fmtInr } from "@/lib/utils";
import type { UserDetail } from "@/lib/types";

export function ProfileTab({ user }: { user: UserDetail }) {
  const rows: [string, string][] = [
    ["ID",             user.id],
    ["Phone",          user.phone ?? "—"],
    ["Email",          user.email ?? "—"],
    ["Name",           user.name ?? "—"],
    ["Role",           user.role],
    ["Status",         user.status],
    ["Language",       user.lang_pref],
    ["Birth Date",     user.birth_date ?? "—"],
    ["Birth Time",     user.birth_time ?? "—"],
    ["Birth City",     user.birth_city ?? "—"],
    ["Current City",   user.city ?? "—"],
    ["Plan",           user.plan],
    ["Joined",         fmt(user.created_at)],
    ["Last Login",     fmt(user.last_login)],
    ["Total Chats",    String(user.total_chats)],
    ["Total Kundalis", String(user.total_kundalis)],
    ["Palm Readings",  String(user.total_palm)],
    ["Lifetime Paid",  fmtInr(user.lifetime_paid_inr * 100)],
  ];

  if (user.subscription) {
    rows.push(
      ["Sub Plan",     user.subscription.plan + " / " + user.subscription.period],
      ["Sub Status",   user.subscription.status],
      ["Sub Started",  fmt(user.subscription.started_at)],
      ["Sub Expires",  fmt(user.subscription.expires_at)],
      ["Sub Amount",   fmtInr(user.subscription.amount_paid)],
      ["Cashfree ID",  user.subscription.cashfree_order_id ?? "—"],
    );
  }

  return (
    <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-1">
      {rows.map(([k, v]) => (
        <div key={k} className="flex justify-between py-1.5 border-b border-border/60">
          <dt className="text-muted-foreground text-xs">{k}</dt>
          <dd className="text-foreground text-xs font-mono text-right max-w-[55%] truncate">{v}</dd>
        </div>
      ))}
    </dl>
  );
}
