import { useCallback, useEffect, useState } from "react";
import { aFetch, invalidateCache } from "@/lib/api";
import { Loader } from "@/components/ui/Loader";
import {
  Layers, Plus, Edit2, Trash2, ToggleLeft, ToggleRight,
  Users, IndianRupee, CheckCircle2, XCircle, ChevronDown, ChevronUp,
} from "lucide-react";

// ── Types ──────────────────────────────────────────────────────────────────────

interface Plan {
  id:                  string;
  name:                string;
  description:         string;
  price_inr:           number;
  duration_days:       number;
  daily_message_limit: number;
  daily_token_limit:   number;
  badge_text?:         string;
  sort_order:          number;
  is_active:           boolean;
  features:            string[];
  subscriber_count?:   number;
}

interface Flag {
  key:              string;
  name:             string;
  description:      string;
  category:         string;
  is_enabled:       boolean;
}

// ── Helpers ────────────────────────────────────────────────────────────────────

function fmtLimit(n: number, unit = "") {
  if (n === 0) return "Unlimited";
  if (n >= 1000) return `${(n / 1000).toFixed(0)}K${unit}`;
  return `${n}${unit}`;
}

function fmtPrice(n: number) {
  return `₹${n.toLocaleString("en-IN")}`;
}

// ── Toggle switch ──────────────────────────────────────────────────────────────

function Toggle({ on, onChange, disabled }: { on: boolean; onChange: () => void; disabled?: boolean }) {
  return (
    <button
      onClick={onChange}
      disabled={disabled}
      className={`relative w-9 h-5 rounded-full transition-colors focus:outline-none disabled:opacity-50 ${on ? "bg-green-500" : "bg-muted-foreground/30"}`}
    >
      <span className={`absolute top-0.5 left-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${on ? "translate-x-4" : "translate-x-0"}`} />
    </button>
  );
}

// ── Plan card ──────────────────────────────────────────────────────────────────

function PlanCard({
  plan,
  onEdit,
  onToggle,
  onDelete,
}: {
  plan:     Plan;
  onEdit:   (p: Plan) => void;
  onToggle: (p: Plan) => void;
  onDelete: (p: Plan) => void;
}) {
  const [showAll, setShowAll] = useState(false);
  const visibleFeatures = showAll ? plan.features : plan.features.slice(0, 5);
  const extraCount = plan.features.length - 5;

  return (
    <div className={`rounded-xl border bg-white shadow-sm overflow-hidden flex flex-col ${plan.is_active ? "border-border" : "border-red-200 opacity-75"}`}>
      {/* Header */}
      <div className="px-4 py-3 border-b border-border flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-bold text-foreground text-sm">{plan.name}</span>
            {plan.badge_text && (
              <span className="text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200 px-1.5 py-0.5 rounded-full">
                {plan.badge_text}
              </span>
            )}
            <span className={`text-xs border px-1.5 py-0.5 rounded-full ${plan.is_active ? "bg-green-50 text-green-700 border-green-200" : "bg-red-50 text-red-600 border-red-200"}`}>
              {plan.is_active ? "Active" : "Inactive"}
            </span>
          </div>
          <p className="text-xs text-muted-foreground mt-0.5 truncate">{plan.description}</p>
        </div>
        <Toggle on={plan.is_active} onChange={() => onToggle(plan)} />
      </div>

      {/* Body */}
      <div className="px-4 py-3 flex-1 space-y-2.5">
        {/* Price */}
        <div className="flex items-baseline gap-1">
          <span className="text-2xl font-extrabold text-amber-700">{fmtPrice(plan.price_inr)}</span>
          <span className="text-xs text-muted-foreground">/month</span>
        </div>

        {/* Limits */}
        <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
          <span>{fmtLimit(plan.daily_message_limit)} msgs/day</span>
          <span>{fmtLimit(plan.daily_token_limit, "K")} tokens/day</span>
          <span>Sort #{plan.sort_order}</span>
        </div>

        {/* Subscribers */}
        {plan.subscriber_count !== undefined && (
          <div className="flex items-center gap-1.5 text-xs text-blue-600 bg-blue-50 border border-blue-100 rounded-lg px-2 py-1 w-fit">
            <Users className="w-3 h-3" />
            <span>{plan.subscriber_count} subscribers</span>
          </div>
        )}

        {/* Features */}
        {plan.features.length > 0 && (
          <div>
            <p className="text-xs text-muted-foreground mb-1.5">Features ({plan.features.length})</p>
            <div className="flex flex-wrap gap-1">
              {visibleFeatures.map((f) => (
                <span key={f} className="text-xs bg-muted text-foreground/70 border border-border px-1.5 py-0.5 rounded">
                  {f}
                </span>
              ))}
              {!showAll && extraCount > 0 && (
                <button
                  onClick={() => setShowAll(true)}
                  className="text-xs bg-amber-50 text-amber-700 border border-amber-200 px-1.5 py-0.5 rounded flex items-center gap-0.5"
                >
                  +{extraCount} more <ChevronDown className="w-3 h-3" />
                </button>
              )}
              {showAll && extraCount > 0 && (
                <button
                  onClick={() => setShowAll(false)}
                  className="text-xs bg-muted text-muted-foreground border border-border px-1.5 py-0.5 rounded flex items-center gap-0.5"
                >
                  Show less <ChevronUp className="w-3 h-3" />
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Footer actions */}
      <div className="px-4 py-2.5 border-t border-border flex items-center gap-2">
        <button
          onClick={() => onEdit(plan)}
          className="flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg border border-amber-200 bg-amber-50 text-amber-700 text-xs font-medium hover:bg-amber-100 transition-colors"
        >
          <Edit2 className="w-3 h-3" /> Edit
        </button>
        <button
          onClick={() => onDelete(plan)}
          disabled={(plan.subscriber_count ?? 0) > 0}
          title={(plan.subscriber_count ?? 0) > 0 ? "Cannot delete plan with active subscribers" : "Delete plan"}
          className="flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg border border-red-200 bg-red-50 text-red-600 text-xs font-medium hover:bg-red-100 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <Trash2 className="w-3 h-3" />
        </button>
      </div>
    </div>
  );
}

// ── Plan Form Modal ────────────────────────────────────────────────────────────

const EMPTY_PLAN: Omit<Plan, "subscriber_count"> = {
  id:                  "",
  name:                "",
  description:         "",
  price_inr:           0,
  duration_days:       30,
  daily_message_limit: 0,
  daily_token_limit:   100000,
  badge_text:          "",
  sort_order:          0,
  is_active:           true,
  features:            [],
};

function PlanModal({
  plan,
  flags,
  onClose,
  onSaved,
}: {
  plan:    Partial<Plan> | null;
  flags:   Flag[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const isEdit = !!(plan && plan.id);

  const [form,  setForm]  = useState<typeof EMPTY_PLAN>(plan ? { ...EMPTY_PLAN, ...plan } : { ...EMPTY_PLAN });
  const [busy,  setBusy]  = useState(false);
  const [err,   setErr]   = useState("");

  const set = <K extends keyof typeof EMPTY_PLAN>(k: K, v: typeof EMPTY_PLAN[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const toggleFeature = (key: string) => {
    setForm((f) => ({
      ...f,
      features: f.features.includes(key)
        ? f.features.filter((x) => x !== key)
        : [...f.features, key],
    }));
  };

  const selectAllInCategory = (cat: string) => {
    const keys = flags.filter((fl) => fl.category === cat).map((fl) => fl.key);
    setForm((f) => ({ ...f, features: [...new Set([...f.features, ...keys])] }));
  };

  const deselectAllInCategory = (cat: string) => {
    const keys = new Set(flags.filter((fl) => fl.category === cat).map((fl) => fl.key));
    setForm((f) => ({ ...f, features: f.features.filter((x) => !keys.has(x)) }));
  };

  const save = async () => {
    if (!form.name.trim()) { setErr("Plan name is required"); return; }
    if (!form.id.trim())   { setErr("Plan ID is required"); return; }
    setBusy(true); setErr("");
    try {
      if (isEdit) {
        await aFetch(`/admin/plans/${form.id}`, {
          method: "PUT", body: JSON.stringify(form),
        });
      } else {
        await aFetch("/admin/plans", {
          method: "POST", body: JSON.stringify(form),
        });
      }
      invalidateCache();
      onSaved();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error saving plan");
    } finally { setBusy(false); }
  };

  // Group flags by category
  const categories = [...new Set(flags.map((f) => f.category))];

  const CAT_LABEL: Record<string, string> = {
    ai:        "AI Features",
    astrology: "Astrology Tools",
    tools:     "Tools",
    general:   "General",
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal header */}
        <div className="px-6 py-4 border-b border-border flex items-center justify-between">
          <h2 className="text-base font-bold text-foreground">
            {isEdit ? `Edit Plan — ${form.name}` : "New Plan"}
          </h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground transition-colors">
            <XCircle className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable body */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
          {/* Plan ID */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Plan ID *</label>
              <input
                type="text"
                value={form.id}
                onChange={(e) => set("id", e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ""))}
                disabled={isEdit}
                placeholder="e.g. basic"
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400 disabled:bg-muted disabled:text-muted-foreground font-mono"
              />
              {isEdit && <p className="text-xs text-muted-foreground mt-0.5">Cannot change ID after creation</p>}
            </div>
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Plan Name *</label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => set("name", e.target.value)}
                placeholder="e.g. Basic Plan"
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
              />
            </div>
          </div>

          <div>
            <label className="text-xs text-muted-foreground mb-1 block">Description</label>
            <textarea
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
              rows={2}
              placeholder="Short plan description…"
              className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400 resize-none"
            />
          </div>

          {/* Price + duration */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Price (₹/month)</label>
              <input
                type="number"
                min={0}
                value={form.price_inr}
                onChange={(e) => set("price_inr", +e.target.value)}
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Duration (days)</label>
              <input
                type="number"
                min={1}
                value={form.duration_days}
                onChange={(e) => set("duration_days", +e.target.value)}
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Msgs/day (0=∞)</label>
              <input
                type="number"
                min={0}
                value={form.daily_message_limit}
                onChange={(e) => set("daily_message_limit", +e.target.value)}
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Tokens/day</label>
              <input
                type="number"
                min={0}
                value={form.daily_token_limit}
                onChange={(e) => set("daily_token_limit", +e.target.value)}
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
              />
            </div>
          </div>

          {/* Badge + sort + active */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 items-end">
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Badge Text (optional)</label>
              <input
                type="text"
                value={form.badge_text ?? ""}
                onChange={(e) => set("badge_text", e.target.value)}
                placeholder="e.g. Popular"
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
              />
            </div>
            <div>
              <label className="text-xs text-muted-foreground mb-1 block">Sort Order</label>
              <input
                type="number"
                min={0}
                value={form.sort_order}
                onChange={(e) => set("sort_order", +e.target.value)}
                className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
              />
            </div>
            <div className="flex items-center gap-2 pb-1">
              <Toggle on={form.is_active} onChange={() => set("is_active", !form.is_active)} />
              <span className="text-sm text-foreground/80">Active</span>
            </div>
          </div>

          {/* Features — flag checklist by category */}
          <div className="border border-border rounded-xl overflow-hidden">
            <div className="px-4 py-2.5 bg-muted/30 border-b border-border">
              <p className="text-sm font-semibold text-foreground">Features</p>
              <p className="text-xs text-muted-foreground mt-0.5">
                Select which feature flags are included in this plan ({form.features.length} selected)
              </p>
            </div>
            <div className="divide-y divide-border">
              {flags.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">No feature flags defined yet</p>
              )}
              {categories.map((cat) => {
                const catFlags = flags.filter((f) => f.category === cat);
                const allSelected = catFlags.every((f) => form.features.includes(f.key));
                return (
                  <div key={cat} className="px-4 py-3 space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-foreground uppercase tracking-wide">
                        {CAT_LABEL[cat] ?? cat}
                      </span>
                      <div className="flex gap-2">
                        <button
                          onClick={() => selectAllInCategory(cat)}
                          className="text-xs text-amber-700 hover:underline"
                        >
                          Select All
                        </button>
                        <span className="text-muted-foreground text-xs">·</span>
                        <button
                          onClick={() => deselectAllInCategory(cat)}
                          className="text-xs text-muted-foreground hover:underline"
                        >
                          Deselect All
                        </button>
                      </div>
                    </div>
                    <div className="space-y-1.5">
                      {catFlags.map((flag) => (
                        <label key={flag.key} className="flex items-start gap-2.5 cursor-pointer group">
                          <input
                            type="checkbox"
                            checked={form.features.includes(flag.key)}
                            onChange={() => toggleFeature(flag.key)}
                            className="mt-0.5 accent-amber-600"
                          />
                          <div>
                            <span className="text-sm text-foreground font-medium group-hover:text-amber-700 transition-colors">
                              {flag.name}
                            </span>
                            {flag.description && (
                              <p className="text-xs text-muted-foreground">{flag.description}</p>
                            )}
                          </div>
                        </label>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-border space-y-2">
          {err && (
            <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              <XCircle className="w-4 h-4 shrink-0" /> {err}
            </div>
          )}
          <div className="flex gap-2">
            <button
              onClick={onClose}
              className="flex-1 py-2 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={save}
              disabled={busy}
              className="flex-1 py-2 rounded-xl bg-amber-600 text-white text-sm font-semibold hover:bg-amber-700 disabled:opacity-50 transition-colors"
            >
              {busy ? "Saving…" : isEdit ? "Save Changes" : "Create Plan"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Delete confirm dialog ──────────────────────────────────────────────────────

function ConfirmDelete({ plan, onClose, onDeleted }: { plan: Plan; onClose: () => void; onDeleted: () => void }) {
  const [busy, setBusy] = useState(false);
  const [err,  setErr]  = useState("");

  const confirm = async () => {
    setBusy(true); setErr("");
    try {
      await aFetch(`/admin/plans/${plan.id}`, { method: "DELETE" });
      invalidateCache();
      onDeleted();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error deleting plan");
    } finally { setBusy(false); }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6 space-y-4" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center">
            <Trash2 className="w-5 h-5 text-red-600" />
          </div>
          <div>
            <h2 className="text-base font-bold text-foreground">Delete Plan?</h2>
            <p className="text-sm text-muted-foreground">This cannot be undone.</p>
          </div>
        </div>
        <p className="text-sm text-foreground">
          You are about to delete <span className="font-semibold">{plan.name}</span>.
          All plan configuration will be permanently removed.
        </p>
        {err && <p className="text-xs text-red-600">{err}</p>}
        <div className="flex gap-2">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted">
            Cancel
          </button>
          <button onClick={confirm} disabled={busy}
            className="flex-1 py-2 rounded-xl bg-red-600 text-white text-sm font-semibold hover:bg-red-700 disabled:opacity-50">
            {busy ? "Deleting…" : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Summary cards ──────────────────────────────────────────────────────────────

function SummaryCards({ plans }: { plans: Plan[] }) {
  const active      = plans.filter((p) => p.is_active).length;
  const totalSubs   = plans.reduce((a, p) => a + (p.subscriber_count ?? 0), 0);
  const estRevenue  = plans.reduce((a, p) => a + (p.subscriber_count ?? 0) * p.price_inr, 0);

  const cards = [
    { label: "Active Plans",    value: active,                     icon: CheckCircle2, color: "text-green-600 bg-green-50"   },
    { label: "Total Subscribers", value: totalSubs,                icon: Users,        color: "text-blue-600 bg-blue-50"     },
    { label: "Est. Monthly Revenue", value: `₹${estRevenue.toLocaleString("en-IN")}`, icon: IndianRupee, color: "text-amber-700 bg-amber-50" },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
      {cards.map(({ label, value, icon: Icon, color }) => (
        <div key={label} className="rounded-xl border border-border bg-white p-4 shadow-sm flex items-center gap-3">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${color.split(" ")[1]}`}>
            <Icon className={`w-5 h-5 ${color.split(" ")[0]}`} />
          </div>
          <div>
            <p className="text-xs text-muted-foreground">{label}</p>
            <p className="text-xl font-bold text-foreground">{value}</p>
          </div>
        </div>
      ))}
    </div>
  );
}

// ── Main page ──────────────────────────────────────────────────────────────────

export default function PlansPage() {
  const [plans,   setPlans]   = useState<Plan[]>([]);
  const [flags,   setFlags]   = useState<Flag[]>([]);
  const [loading, setLoading] = useState(true);
  const [err,     setErr]     = useState("");
  const [modal,   setModal]   = useState<Partial<Plan> | null | "new">(null);
  const [delPlan, setDelPlan] = useState<Plan | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setErr("");
    try {
      const [pd, fd] = await Promise.all([
        aFetch<{ plans: Plan[] }>("/admin/plans"),
        aFetch<{ flags: Flag[] }>("/admin/feature-flags"),
      ]);
      setPlans(pd.plans ?? []);
      setFlags(fd.flags ?? []);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Failed to load plans");
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleToggle = async (plan: Plan) => {
    // Optimistic update
    setPlans((prev) => prev.map((p) => p.id === plan.id ? { ...p, is_active: !p.is_active } : p));
    try {
      const res = await aFetch<{ plan: Plan }>(`/admin/plans/${plan.id}/toggle`, { method: "PATCH" });
      setPlans((prev) => prev.map((p) => p.id === plan.id ? res.plan : p));
      invalidateCache();
    } catch (e) {
      // Revert on error
      setPlans((prev) => prev.map((p) => p.id === plan.id ? plan : p));
      setErr(e instanceof Error ? e.message : "Failed to toggle plan");
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-bold text-foreground font-display flex items-center gap-2">
            <Layers className="w-5 h-5 text-amber-600" /> Plans
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Manage subscription plans and feature access
          </p>
        </div>
        <button
          onClick={() => setModal("new")}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-amber-600 text-white text-sm font-semibold hover:bg-amber-700 transition-colors shadow-sm"
        >
          <Plus className="w-4 h-4" /> New Plan
        </button>
      </div>

      {/* Error */}
      {err && (
        <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-4 py-3">
          <XCircle className="w-4 h-4 shrink-0" /> {err}
        </div>
      )}

      {/* Summary */}
      {!loading && plans.length > 0 && <SummaryCards plans={plans} />}

      {/* Plans grid */}
      {loading ? (
        <Loader />
      ) : plans.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          <Layers className="w-10 h-10 mx-auto mb-3 opacity-30" />
          <p className="text-sm">No plans yet. Create your first plan.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...plans].sort((a, b) => a.sort_order - b.sort_order).map((plan) => (
            <PlanCard
              key={plan.id}
              plan={plan}
              onEdit={(p) => setModal(p)}
              onToggle={handleToggle}
              onDelete={(p) => setDelPlan(p)}
            />
          ))}
        </div>
      )}

      {/* Toggle icon legend */}
      {!loading && plans.length > 0 && (
        <div className="flex items-center gap-4 text-xs text-muted-foreground">
          <span className="flex items-center gap-1"><ToggleRight className="w-4 h-4 text-green-500" /> Active</span>
          <span className="flex items-center gap-1"><ToggleLeft className="w-4 h-4 text-muted-foreground" /> Inactive</span>
        </div>
      )}

      {/* Create/Edit modal */}
      {modal !== null && (
        <PlanModal
          plan={modal === "new" ? null : modal}
          flags={flags}
          onClose={() => setModal(null)}
          onSaved={() => { setModal(null); load(); }}
        />
      )}

      {/* Delete confirm */}
      {delPlan && (
        <ConfirmDelete
          plan={delPlan}
          onClose={() => setDelPlan(null)}
          onDeleted={() => { setDelPlan(null); load(); }}
        />
      )}
    </div>
  );
}
