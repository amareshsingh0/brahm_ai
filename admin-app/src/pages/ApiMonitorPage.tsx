import { useCallback, useEffect, useState } from "react";
import { aFetch } from "@/lib/api";
import { Loader, Empty } from "@/components/ui/Loader";
import { Zap, AlertTriangle, TrendingUp, Activity, Clock, CheckCircle2 } from "lucide-react";

// ── Types ─────────────────────────────────────────────────────────────────────

interface EndpointRow {
  endpoint:   string;
  count:      number;
  avg_ms:     number;
  errors:     number;
  error_rate: number;
}

interface ErrorRow {
  endpoint:    string;
  status_code: number;
  count:       number;
}

interface SlowRow {
  endpoint: string;
  avg_ms:   number;
  count:    number;
}

interface TimelineItem {
  label: string;
  count: number;
}

interface ApiStats {
  period:           string;
  total_requests:   number;
  top_endpoints:    EndpointRow[];
  errors_by_endpoint: ErrorRow[];
  method_breakdown: { method: string; count: number }[];
  status_distribution: { status: number; count: number }[];
  slowest_endpoints: SlowRow[];
  timeline:         TimelineItem[];
  client_breakdown: { client: string; count: number }[];
}

type Period = "today" | "7d" | "30d";

// ── Helpers ───────────────────────────────────────────────────────────────────

function msColor(ms: number) {
  if (ms < 300)  return "text-green-600";
  if (ms < 1000) return "text-amber-600";
  return "text-red-600";
}

function errColor(rate: number) {
  if (rate === 0) return "text-green-600";
  if (rate < 5)  return "text-amber-600";
  return "text-red-600";
}

function statusBadge(code: number) {
  if (code < 300) return "bg-green-50 text-green-700 border-green-200";
  if (code < 400) return "bg-blue-50 text-blue-700 border-blue-200";
  if (code < 500) return "bg-amber-50 text-amber-700 border-amber-200";
  return "bg-red-50 text-red-700 border-red-200";
}

/** Shorten endpoint path for display */
function short(ep: string) {
  return ep.replace(/^\/api\/v?\d*\/?/, "/").replace(/\/[0-9a-f-]{8,}/g, "/{id}");
}

// ── Mini bar chart (inline SVG) ───────────────────────────────────────────────

function MiniBarChart({ data }: { data: TimelineItem[] }) {
  if (!data.length) return null;
  const max = Math.max(...data.map((d) => d.count), 1);
  const W = 600; const H = 80; const BAR = W / data.length - 2;
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-20" preserveAspectRatio="none">
      {data.map((d, i) => {
        const h = Math.max(2, (d.count / max) * (H - 16));
        const x = i * (W / data.length) + 1;
        return (
          <g key={i}>
            <rect x={x} y={H - h - 14} width={BAR} height={h} rx="2" fill="#f59e0b" opacity="0.8" />
            {data.length <= 24 && (
              <text x={x + BAR / 2} y={H - 2} textAnchor="middle" fontSize="7" fill="#9ca3af">
                {d.label.slice(-5)}
              </text>
            )}
          </g>
        );
      })}
    </svg>
  );
}

// ── Section card ──────────────────────────────────────────────────────────────

function Section({ title, icon: Icon, children }: { title: string; icon: React.ElementType; children: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-border bg-white shadow-sm overflow-hidden">
      <div className="flex items-center gap-2 px-5 py-3 border-b border-border bg-muted/20">
        <Icon className="w-4 h-4 text-amber-600" />
        <p className="text-sm font-semibold text-foreground">{title}</p>
      </div>
      {children}
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function ApiMonitorPage() {
  const [period,  setPeriod]  = useState<Period>("today");
  const [data,    setData]    = useState<ApiStats | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async (p: Period) => {
    setLoading(true);
    try {
      const d = await aFetch<ApiStats>(`/admin/api-stats?period=${p}`);
      setData(d);
    } catch { setData(null); }
    finally  { setLoading(false); }
  }, []);

  useEffect(() => { load(period); }, [period, load]);

  const totalErrors = data?.top_endpoints.reduce((s, e) => s + e.errors, 0) ?? 0;
  const avgLatency  = data?.top_endpoints.length
    ? Math.round(data.top_endpoints.reduce((s, e) => s + e.avg_ms * e.count, 0) / (data.total_requests || 1))
    : 0;
  const successRate = data?.total_requests
    ? Math.round(((data.total_requests - totalErrors) / data.total_requests) * 100)
    : 100;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-bold text-foreground font-display">API Monitor</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {data ? `${data.total_requests.toLocaleString()} requests` : "Loading…"}
          </p>
        </div>

        {/* Period switcher */}
        <div className="flex gap-2">
          {(["today", "7d", "30d"] as const).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-4 py-1.5 rounded-full text-xs font-medium transition-colors ${
                period === p
                  ? "bg-amber-100 text-amber-700 border border-amber-300"
                  : "bg-muted text-muted-foreground hover:bg-border border border-transparent"
              }`}
            >
              {p === "today" ? "Today" : p === "7d" ? "Last 7 Days" : "Last 30 Days"}
            </button>
          ))}
        </div>
      </div>

      {loading ? <Loader /> : !data ? <Empty msg="No API data available." /> : (
        <>
          {/* Summary cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            {[
              { label: "Total Requests", value: data.total_requests.toLocaleString(), icon: Activity, color: "text-blue-600 bg-blue-50" },
              { label: "Avg Latency",    value: `${avgLatency}ms`,                    icon: Clock,    color: `${msColor(avgLatency)} bg-muted` },
              { label: "Total Errors",   value: totalErrors.toLocaleString(),          icon: AlertTriangle, color: totalErrors ? "text-red-600 bg-red-50" : "text-green-600 bg-green-50" },
              { label: "Success Rate",   value: `${successRate}%`,                    icon: CheckCircle2,  color: successRate >= 99 ? "text-green-600 bg-green-50" : "text-amber-600 bg-amber-50" },
            ].map(({ label, value, icon: Icon, color }) => (
              <div key={label} className="rounded-xl border border-border bg-white p-4 shadow-sm">
                <div className={`inline-flex items-center justify-center w-8 h-8 rounded-lg mb-2 ${color.split(" ")[1]}`}>
                  <Icon className={`w-4 h-4 ${color.split(" ")[0]}`} />
                </div>
                <p className="text-xs text-muted-foreground">{label}</p>
                <p className="text-xl font-bold text-foreground mt-0.5">{value}</p>
              </div>
            ))}
          </div>

          {/* Timeline */}
          {data.timeline.length > 0 && (
            <Section title={`Request Volume (${period})`} icon={TrendingUp}>
              <div className="px-5 py-4">
                <MiniBarChart data={data.timeline} />
              </div>
            </Section>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Top endpoints */}
            <Section title="Top Endpoints by Hits" icon={Activity}>
              <div className="divide-y divide-border/50 max-h-[360px] overflow-y-auto">
                <div className="grid grid-cols-[1fr_56px_64px_56px] gap-2 px-4 py-2 text-xs text-muted-foreground font-medium bg-muted/30 sticky top-0">
                  <span>Endpoint</span><span className="text-right">Hits</span>
                  <span className="text-right">Avg ms</span><span className="text-right">Errors</span>
                </div>
                {data.top_endpoints.map((ep, i) => (
                  <div key={i} className="grid grid-cols-[1fr_56px_64px_56px] gap-2 px-4 py-2.5 items-center hover:bg-muted/20 text-xs">
                    <span className="font-mono text-foreground/70 truncate" title={ep.endpoint}>
                      {short(ep.endpoint)}
                    </span>
                    <span className="text-right font-semibold text-foreground">{ep.count}</span>
                    <span className={`text-right font-medium ${msColor(ep.avg_ms)}`}>{ep.avg_ms}ms</span>
                    <span className={`text-right font-medium ${errColor(ep.error_rate)}`}>
                      {ep.errors > 0 ? `${ep.errors} (${ep.error_rate}%)` : "—"}
                    </span>
                  </div>
                ))}
              </div>
            </Section>

            {/* Slowest endpoints */}
            <Section title="Slowest Endpoints (Avg)" icon={Clock}>
              <div className="divide-y divide-border/50 max-h-[360px] overflow-y-auto">
                <div className="grid grid-cols-[1fr_72px_56px] gap-2 px-4 py-2 text-xs text-muted-foreground font-medium bg-muted/30 sticky top-0">
                  <span>Endpoint</span><span className="text-right">Avg ms</span><span className="text-right">Calls</span>
                </div>
                {data.slowest_endpoints.map((ep, i) => (
                  <div key={i} className="grid grid-cols-[1fr_72px_56px] gap-2 px-4 py-2.5 items-center hover:bg-muted/20 text-xs">
                    <span className="font-mono text-foreground/70 truncate" title={ep.endpoint}>
                      {short(ep.endpoint)}
                    </span>
                    <span className={`text-right font-bold ${msColor(ep.avg_ms)}`}>{ep.avg_ms}ms</span>
                    <span className="text-right text-muted-foreground">{ep.count}</span>
                  </div>
                ))}
              </div>
            </Section>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Status distribution */}
            <Section title="Response Status Distribution" icon={CheckCircle2}>
              <div className="divide-y divide-border/50">
                {data.status_distribution.map((s) => {
                  const pct = Math.round((s.count / data.total_requests) * 100);
                  return (
                    <div key={s.status} className="flex items-center gap-3 px-4 py-3">
                      <span className={`text-xs font-mono font-bold border px-1.5 py-0.5 rounded ${statusBadge(s.status)}`}>
                        {s.status}
                      </span>
                      <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${s.status < 300 ? "bg-green-400" : s.status < 400 ? "bg-blue-400" : s.status < 500 ? "bg-amber-400" : "bg-red-400"}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <span className="text-xs text-muted-foreground w-20 text-right">{s.count} ({pct}%)</span>
                    </div>
                  );
                })}
              </div>
            </Section>

            {/* Method breakdown + client breakdown + errors */}
            <div className="space-y-4">
              <Section title="Method Breakdown" icon={Zap}>
                <div className="flex gap-4 px-4 py-4 flex-wrap">
                  {data.method_breakdown.map((m) => (
                    <div key={m.method} className="text-center">
                      <p className="text-xs font-mono text-muted-foreground">{m.method}</p>
                      <p className="text-lg font-bold text-foreground">{m.count}</p>
                    </div>
                  ))}
                </div>
              </Section>

              {data.client_breakdown?.length > 0 && (
                <Section title="Client Breakdown" icon={Zap}>
                  <div className="flex gap-6 px-4 py-4 flex-wrap">
                    {data.client_breakdown.map((c) => {
                      const pct = Math.round((c.count / data.total_requests) * 100);
                      const color = c.client === "web" ? "text-blue-600" : c.client === "android" ? "text-green-600" : "text-muted-foreground";
                      const icon  = c.client === "web" ? "🌐" : c.client === "android" ? "📱" : "❓";
                      return (
                        <div key={c.client} className="text-center">
                          <p className="text-sm mb-1">{icon}</p>
                          <p className={`text-xs font-semibold capitalize ${color}`}>{c.client}</p>
                          <p className="text-lg font-bold text-foreground">{c.count.toLocaleString()}</p>
                          <p className="text-xs text-muted-foreground">{pct}%</p>
                        </div>
                      );
                    })}
                  </div>
                </Section>
              )}

              {data.errors_by_endpoint.length > 0 && (
                <Section title="Errors by Endpoint" icon={AlertTriangle}>
                  <div className="divide-y divide-border/50 max-h-48 overflow-y-auto">
                    {data.errors_by_endpoint.map((e, i) => (
                      <div key={i} className="flex items-center gap-3 px-4 py-2.5 text-xs hover:bg-muted/20">
                        <span className={`font-mono font-bold border px-1.5 py-0.5 rounded ${statusBadge(e.status_code)}`}>
                          {e.status_code}
                        </span>
                        <span className="font-mono text-foreground/70 flex-1 truncate">{short(e.endpoint)}</span>
                        <span className="text-red-600 font-semibold shrink-0">{e.count}×</span>
                      </div>
                    ))}
                  </div>
                </Section>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
