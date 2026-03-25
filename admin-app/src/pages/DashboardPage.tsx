import { useEffect, useState } from "react";
import { aFetch } from "@/lib/api";
import { fmtInr } from "@/lib/utils";
import { StatCard } from "@/components/ui/StatCard";
import { Loader, Empty } from "@/components/ui/Loader";
import type { Stats } from "@/lib/types";
import {
  Users, Sparkles, CalendarDays, Activity, TrendingUp,
  IndianRupee, BarChart3, Landmark, CreditCard,
  MessageSquare, Star, Hand,
  Moon, Zap,
} from "lucide-react";

export default function DashboardPage() {
  const [s,       setS]       = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    aFetch<Stats>("/admin/stats")
      .then(setS)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Loader />;
  if (!s)      return <Empty msg="Failed to load stats." />;

  return (
    <div className="space-y-8 animate-fade-in">
      <h1 className="text-xl font-bold text-foreground font-display">Dashboard</h1>

      {/* Users */}
      <section>
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Users</p>
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
          <StatCard label="Total Users"   value={s.total_users} icon={Users} />
          <StatCard label="New Today"     value={s.new_today}   icon={Sparkles} />
          <StatCard label="New This Week" value={s.new_week}    icon={CalendarDays} />
          <StatCard label="DAU"           value={s.dau}         icon={Activity} />
          <StatCard label="MAU"           value={s.mau}         icon={TrendingUp} />
        </div>
      </section>

      {/* Revenue */}
      <section>
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Revenue</p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Revenue Today"  value={fmtInr(s.revenue_today)}  icon={IndianRupee} />
          <StatCard label="Revenue Month"  value={fmtInr(s.revenue_month)}  icon={BarChart3} />
          <StatCard label="Revenue Total"  value={fmtInr(s.revenue_total)}  icon={Landmark} />
          <StatCard label="Paid Users"     value={s.paid_users}             icon={CreditCard} />
        </div>
      </section>

      {/* Today's Activity */}
      <section>
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Today's Activity</p>
        <div className="grid grid-cols-3 gap-3">
          <StatCard label="AI Chats"      value={s.chats_today}    icon={MessageSquare} />
          <StatCard label="Kundalis"      value={s.kundalis_today} icon={Star} />
          <StatCard label="Palm Readings" value={s.palm_today}     icon={Hand} />
        </div>
      </section>

      {/* Subscriptions */}
      <section>
        <p className="text-xs text-muted-foreground uppercase tracking-wider mb-3">Active Subscriptions</p>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatCard label="Jyotishi Monthly" value={s.active_subscriptions?.jyotishi_monthly ?? 0} icon={Moon} />
          <StatCard label="Jyotishi Yearly"  value={s.active_subscriptions?.jyotishi_yearly  ?? 0} icon={Moon} />
          <StatCard label="Acharya Monthly"  value={s.active_subscriptions?.acharya_monthly  ?? 0} icon={Zap} />
          <StatCard label="Acharya Yearly"   value={s.active_subscriptions?.acharya_yearly   ?? 0} icon={Zap} />
        </div>
      </section>

      {/* Top Endpoints */}
      {s.top_endpoints?.length > 0 && (
        <section>
          <div className="rounded-xl border border-border bg-white overflow-hidden shadow-sm">
            <p className="px-5 py-3 border-b border-border text-sm font-semibold text-foreground">
              Top Endpoints (Today)
            </p>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="text-left px-5 py-2 text-muted-foreground font-medium w-8">#</th>
                  <th className="text-left px-5 py-2 text-muted-foreground font-medium">Endpoint</th>
                  <th className="text-right px-5 py-2 text-muted-foreground font-medium">Hits</th>
                </tr>
              </thead>
              <tbody>
                {s.top_endpoints.map((ep, i) => (
                  <tr key={ep.endpoint} className="border-b border-border/50 hover:bg-muted/40">
                    <td className="px-5 py-2 text-muted-foreground">{i + 1}</td>
                    <td className="px-5 py-2 font-mono text-foreground/70 text-xs">{ep.endpoint}</td>
                    <td className="px-5 py-2 text-right text-amber-700 font-semibold">{ep.count}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
