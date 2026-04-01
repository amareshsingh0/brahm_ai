import { useCallback, useEffect, useState } from "react";
import { aFetch, invalidateCache } from "@/lib/api";
import { Loader } from "@/components/ui/Loader";
import {
  Flag, Plus, Edit2, Trash2, XCircle, Info,
} from "lucide-react";

// ── Types ──────────────────────────────────────────────────────────────────────

interface FeatureFlag {
  key:         string;
  name:        string;
  description: string;
  category:    string;
  is_enabled:  boolean;
}

// ── Helpers ────────────────────────────────────────────────────────────────────

const CATEGORY_LABELS: Record<string, string> = {
  ai:        "AI Features",
  astrology: "Astrology Tools",
  tools:     "Tools",
  general:   "General",
};

const CATEGORY_COLORS: Record<string, string> = {
  ai:        "bg-blue-50 text-blue-700 border-blue-200",
  astrology: "bg-amber-50 text-amber-700 border-amber-200",
  tools:     "bg-green-50 text-green-700 border-green-200",
  general:   "bg-muted text-muted-foreground border-border",
};

// ── Toggle switch ──────────────────────────────────────────────────────────────

function Toggle({ on, onChange, disabled }: { on: boolean; onChange: () => void; disabled?: boolean }) {
  return (
    <button
      onClick={onChange}
      disabled={disabled}
      title={on ? "Feature ON — click to disable globally" : "Feature OFF globally — click to enable"}
      className={`relative w-10 h-6 rounded-full transition-colors focus:outline-none disabled:opacity-50 ${on ? "bg-green-500" : "bg-red-400"}`}
    >
      <span className={`absolute top-1 left-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${on ? "translate-x-4" : "translate-x-0"}`} />
    </button>
  );
}

// ── Flag row ───────────────────────────────────────────────────────────────────

function FlagRow({
  flag,
  onEdit,
  onToggle,
  onDelete,
}: {
  flag:     FeatureFlag;
  onEdit:   (f: FeatureFlag) => void;
  onToggle: (f: FeatureFlag) => void;
  onDelete: (f: FeatureFlag) => void;
}) {
  return (
    <div className={`flex items-center gap-3 px-4 py-3 bg-white rounded-xl border shadow-sm ${flag.is_enabled ? "border-border" : "border-red-200 bg-red-50/30"}`}>
      {/* Kill switch indicator */}
      <div className={`w-2 h-2 rounded-full shrink-0 ${flag.is_enabled ? "bg-green-500" : "bg-red-500"}`} />

      {/* Key */}
      <code className="text-xs font-mono text-foreground/70 bg-muted px-1.5 py-0.5 rounded border border-border shrink-0">
        {flag.key}
      </code>

      {/* Name + description */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-sm font-semibold text-foreground">{flag.name}</span>
          <span className={`text-xs border px-1.5 py-0.5 rounded-full ${CATEGORY_COLORS[flag.category] ?? CATEGORY_COLORS.general}`}>
            {CATEGORY_LABELS[flag.category] ?? flag.category}
          </span>
          {!flag.is_enabled && (
            <span className="text-xs bg-red-50 text-red-600 border border-red-200 px-1.5 py-0.5 rounded-full font-medium">
              Disabled Globally
            </span>
          )}
        </div>
        {flag.description && (
          <p className="text-xs text-muted-foreground mt-0.5 truncate">{flag.description}</p>
        )}
      </div>

      {/* Kill switch toggle */}
      <div className="flex items-center gap-3 shrink-0">
        <Toggle on={flag.is_enabled} onChange={() => onToggle(flag)} />
        <button
          onClick={() => onEdit(flag)}
          className="p-1.5 rounded-lg border border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100 transition-colors"
          title="Edit flag"
        >
          <Edit2 className="w-3.5 h-3.5" />
        </button>
        <button
          onClick={() => onDelete(flag)}
          className="p-1.5 rounded-lg border border-red-200 bg-red-50 text-red-600 hover:bg-red-100 transition-colors"
          title="Delete flag"
        >
          <Trash2 className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}

// ── Flag form modal ────────────────────────────────────────────────────────────

const EMPTY_FLAG: FeatureFlag = {
  key:         "",
  name:        "",
  description: "",
  category:    "general",
  is_enabled:  true,
};

function FlagModal({
  flag,
  onClose,
  onSaved,
}: {
  flag:    FeatureFlag | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const isEdit = !!flag;
  const [form, setForm] = useState<FeatureFlag>(flag ?? { ...EMPTY_FLAG });
  const [busy, setBusy] = useState(false);
  const [err,  setErr]  = useState("");

  const set = <K extends keyof FeatureFlag>(k: K, v: FeatureFlag[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const save = async () => {
    if (!form.key.trim())  { setErr("Flag key is required"); return; }
    if (!form.name.trim()) { setErr("Flag name is required"); return; }
    setBusy(true); setErr("");
    try {
      if (isEdit) {
        await aFetch(`/admin/feature-flags/${form.key}`, {
          method: "PATCH", body: JSON.stringify(form),
        });
      } else {
        await aFetch("/admin/feature-flags", {
          method: "POST", body: JSON.stringify(form),
        });
      }
      invalidateCache();
      onSaved();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error saving flag");
    } finally { setBusy(false); }
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div
        className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-foreground">
            {isEdit ? `Edit Flag — ${form.key}` : "New Feature Flag"}
          </h2>
          <button onClick={onClose} className="text-muted-foreground hover:text-foreground">
            <XCircle className="w-5 h-5" />
          </button>
        </div>

        {/* Key */}
        <div>
          <label className="text-xs text-muted-foreground mb-1 block">Key * (snake_case)</label>
          <input
            type="text"
            value={form.key}
            onChange={(e) => set("key", e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, ""))}
            disabled={isEdit}
            placeholder="e.g. ai_chat"
            className="w-full border border-border rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:border-amber-400 disabled:bg-muted disabled:text-muted-foreground"
          />
          {isEdit && <p className="text-xs text-muted-foreground mt-0.5">Key cannot be changed after creation</p>}
        </div>

        {/* Name */}
        <div>
          <label className="text-xs text-muted-foreground mb-1 block">Name *</label>
          <input
            type="text"
            value={form.name}
            onChange={(e) => set("name", e.target.value)}
            placeholder="e.g. AI Chat"
            className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
          />
        </div>

        {/* Description */}
        <div>
          <label className="text-xs text-muted-foreground mb-1 block">Description</label>
          <textarea
            value={form.description}
            onChange={(e) => set("description", e.target.value)}
            rows={2}
            placeholder="What does this flag control?"
            className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400 resize-none"
          />
        </div>

        {/* Category */}
        <div>
          <label className="text-xs text-muted-foreground mb-1 block">Category</label>
          <select
            value={form.category}
            onChange={(e) => set("category", e.target.value)}
            className="w-full border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400"
          >
            <option value="ai">AI Features</option>
            <option value="astrology">Astrology Tools</option>
            <option value="tools">Tools</option>
            <option value="general">General</option>
          </select>
        </div>

        {/* Globally enabled */}
        <div className="flex items-center justify-between rounded-xl border border-border px-4 py-3">
          <div>
            <p className="text-sm font-medium text-foreground">Globally Enabled</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              When OFF, disables this feature for ALL users regardless of plan
            </p>
          </div>
          <Toggle on={form.is_enabled} onChange={() => set("is_enabled", !form.is_enabled)} />
        </div>

        {err && (
          <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
            <XCircle className="w-4 h-4 shrink-0" /> {err}
          </div>
        )}

        <div className="flex gap-2 pt-1">
          <button onClick={onClose} className="flex-1 py-2 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted transition-colors">
            Cancel
          </button>
          <button onClick={save} disabled={busy}
            className="flex-1 py-2 rounded-xl bg-amber-600 text-white text-sm font-semibold hover:bg-amber-700 disabled:opacity-50 transition-colors">
            {busy ? "Saving…" : isEdit ? "Save Changes" : "Create Flag"}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Delete confirm ─────────────────────────────────────────────────────────────

function ConfirmDeleteFlag({ flag, onClose, onDeleted }: { flag: FeatureFlag; onClose: () => void; onDeleted: () => void }) {
  const [busy, setBusy] = useState(false);
  const [err,  setErr]  = useState("");

  const confirm = async () => {
    setBusy(true); setErr("");
    try {
      await aFetch(`/admin/feature-flags/${flag.key}`, { method: "DELETE" });
      invalidateCache();
      onDeleted();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Error deleting flag");
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
            <h2 className="text-base font-bold text-foreground">Delete Flag?</h2>
            <p className="text-sm text-muted-foreground">This cannot be undone.</p>
          </div>
        </div>
        <p className="text-sm text-foreground">
          Delete flag <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">{flag.key}</code>?
          Plans referencing this flag will lose access to it.
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

// ── Main page ──────────────────────────────────────────────────────────────────

export default function FeatureFlagsPage() {
  const [flags,   setFlags]   = useState<FeatureFlag[]>([]);
  const [loading, setLoading] = useState(true);
  const [err,     setErr]     = useState("");
  const [modal,   setModal]   = useState<FeatureFlag | null | "new">(null);
  const [delFlag, setDelFlag] = useState<FeatureFlag | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setErr("");
    try {
      const d = await aFetch<{ flags: FeatureFlag[] }>("/admin/feature-flags");
      setFlags(d.flags ?? []);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Failed to load feature flags");
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleToggle = async (flag: FeatureFlag) => {
    const updated = { ...flag, is_enabled: !flag.is_enabled };
    // Optimistic update
    setFlags((prev) => prev.map((f) => f.key === flag.key ? updated : f));
    try {
      const res = await aFetch<{ flag: FeatureFlag }>(`/admin/feature-flags/${flag.key}`, {
        method: "PATCH",
        body:   JSON.stringify({ is_enabled: updated.is_enabled }),
      });
      setFlags((prev) => prev.map((f) => f.key === flag.key ? res.flag : f));
      invalidateCache();
    } catch (e) {
      // Revert
      setFlags((prev) => prev.map((f) => f.key === flag.key ? flag : f));
      setErr(e instanceof Error ? e.message : "Failed to toggle flag");
    }
  };

  // Group by category
  const categories = [...new Set(flags.map((f) => f.category))];
  const enabledCount  = flags.filter((f) => f.is_enabled).length;
  const disabledCount = flags.length - enabledCount;

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-xl font-bold text-foreground font-display flex items-center gap-2">
            <Flag className="w-5 h-5 text-amber-600" /> Feature Flags
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {flags.length} flags · {enabledCount} enabled · {disabledCount > 0 && (
              <span className="text-red-600 font-medium">{disabledCount} disabled</span>
            )}
          </p>
        </div>
        <button
          onClick={() => setModal("new")}
          className="flex items-center gap-1.5 px-4 py-2 rounded-xl bg-amber-600 text-white text-sm font-semibold hover:bg-amber-700 transition-colors shadow-sm"
        >
          <Plus className="w-4 h-4" /> New Flag
        </button>
      </div>

      {/* Kill switch info banner */}
      <div className="flex items-start gap-2.5 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-sm text-amber-800">
        <Info className="w-4 h-4 shrink-0 mt-0.5 text-amber-600" />
        <p>
          <span className="font-semibold">Global kill switches:</span> Turning OFF a flag disables the feature for{" "}
          <span className="font-semibold">ALL users</span> regardless of their plan. Use for emergency shutdowns.
          Individual plan feature access is controlled in the <a href="/admin/plans" className="underline">Plans</a> page.
        </p>
      </div>

      {/* Error */}
      {err && (
        <div className="flex items-center gap-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-4 py-3">
          <XCircle className="w-4 h-4 shrink-0" /> {err}
        </div>
      )}

      {/* Flags list grouped by category */}
      {loading ? (
        <Loader />
      ) : flags.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          <Flag className="w-10 h-10 mx-auto mb-3 opacity-30" />
          <p className="text-sm">No feature flags yet. Create your first flag.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {categories.map((cat) => {
            const catFlags = flags.filter((f) => f.category === cat);
            return (
              <div key={cat}>
                <div className="flex items-center gap-2 mb-2">
                  <h2 className="text-xs font-bold text-muted-foreground uppercase tracking-wider">
                    {CATEGORY_LABELS[cat] ?? cat}
                  </h2>
                  <span className="text-xs text-muted-foreground">({catFlags.length})</span>
                  <div className="flex-1 h-px bg-border" />
                </div>
                <div className="space-y-2">
                  {catFlags.map((flag) => (
                    <FlagRow
                      key={flag.key}
                      flag={flag}
                      onEdit={(f) => setModal(f)}
                      onToggle={handleToggle}
                      onDelete={(f) => setDelFlag(f)}
                    />
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Legend */}
      {!loading && flags.length > 0 && (
        <div className="flex items-center gap-5 text-xs text-muted-foreground">
          <span className="flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-green-500 inline-block" /> Enabled globally</span>
          <span className="flex items-center gap-1.5"><span className="w-2 h-2 rounded-full bg-red-500 inline-block" /> Disabled globally (emergency off)</span>
        </div>
      )}

      {/* Create/Edit modal */}
      {modal !== null && (
        <FlagModal
          flag={modal === "new" ? null : modal}
          onClose={() => setModal(null)}
          onSaved={() => { setModal(null); load(); }}
        />
      )}

      {/* Delete confirm */}
      {delFlag && (
        <ConfirmDeleteFlag
          flag={delFlag}
          onClose={() => setDelFlag(null)}
          onDeleted={() => { setDelFlag(null); load(); }}
        />
      )}
    </div>
  );
}
