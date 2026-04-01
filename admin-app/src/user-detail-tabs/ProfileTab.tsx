import { fmt, fmtInr } from "@/lib/utils";
import type { UserDetail } from "@/lib/types";

// ── Helpers ───────────────────────────────────────────────────────────────────

function authMethodBadge(user: UserDetail) {
  if (user.google_id) return { label: "Google OAuth", emoji: "🔵", cls: "bg-blue-50 text-blue-700 border-blue-200" };
  if (user.apple_id)  return { label: "Apple Sign-In", emoji: "⚫", cls: "bg-gray-100 text-gray-800 border-gray-300" };
  if (user.phone)     return { label: "Phone OTP",    emoji: "📱", cls: "bg-amber-50 text-amber-700 border-amber-200" };
  return               { label: "Unknown",           emoji: "❓", cls: "bg-muted text-muted-foreground border-border" };
}

function deviceBadge(device?: string) {
  if (!device) return null;
  const d = device.toLowerCase();
  if (d === "android") return { label: "Android App", emoji: "🤖" };
  if (d === "ios")     return { label: "iOS App",     emoji: "🍎" };
  return                { label: "Web Browser",       emoji: "🌐" };
}

// ── Component ─────────────────────────────────────────────────────────────────

export function ProfileTab({ user }: { user: UserDetail }) {
  const authMethod = authMethodBadge(user);
  const device     = deviceBadge(user.signup_device);

  return (
    <div className="space-y-5">

      {/* ── Auth & Login Method ─────────────────────────────────────────── */}
      <Section title="Account & Login">
        <div className="flex flex-wrap gap-2 mb-3">
          {/* Auth method */}
          <span className={`inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full border ${authMethod.cls}`}>
            {authMethod.emoji} {authMethod.label}
          </span>
          {/* Phone verified */}
          {user.phone && (
            <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-full border ${
              user.phone_verified
                ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                : "bg-red-50 text-red-600 border-red-200"
            }`}>
              {user.phone_verified ? "✓ Phone Verified" : "✗ Phone Unverified"}
            </span>
          )}
          {/* Signup device */}
          {device && (
            <span className="inline-flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-full border border-border bg-muted text-muted-foreground">
              {device.emoji} Signed up via {device.label}
            </span>
          )}
        </div>
        <Row label="User ID"       value={user.id}               mono />
        {user.google_id && <Row label="Google ID"   value={user.google_id}        mono />}
        {user.apple_id  && <Row label="Apple ID"    value={user.apple_id}         mono />}
        {user.phone     && <Row label="Phone"        value={user.phone}            mono />}
        {user.email     && <Row label="Email"        value={user.email} />}
        <Row label="Signup IP"     value={user.signup_ip ?? "—"} mono />
        <Row label="Joined"        value={fmt(user.created_at)} />
        <Row label="Last Login"    value={fmt(user.last_login)} />
      </Section>

      {/* ── Personal Info ───────────────────────────────────────────────── */}
      <Section title="Personal Info">
        <Row label="Name"         value={user.name ?? "—"} />
        <Row label="Role"         value={user.role} />
        <Row label="Status"       value={user.status} />
        <Row label="Language"     value={user.lang_pref} />
        <Row label="Gender"       value={user.gender ?? "—"} />
        <Row label="Birth Date"   value={user.birth_date ?? "—"} />
        <Row label="Birth Time"   value={user.birth_time ?? "—"} />
        <Row label="Birth City"   value={user.birth_city ?? "—"} />
        <Row label="Current City" value={user.city ?? "—"} />
      </Section>

      {/* ── Subscription ────────────────────────────────────────────────── */}
      <Section title="Subscription">
        <Row label="Plan" value={user.plan} highlight={user.plan !== "free"} />
        {user.subscription ? (
          <>
            <Row label="Period"     value={user.subscription.plan + " / " + user.subscription.period} />
            <Row label="Status"     value={user.subscription.status} />
            <Row label="Started"    value={fmt(user.subscription.started_at)} />
            <Row label="Expires"    value={fmt(user.subscription.expires_at)} />
            <Row label="Amount"     value={fmtInr(user.subscription.amount_paid)} />
            <Row label="Order ID"   value={user.subscription.cashfree_order_id ?? "—"} mono />
          </>
        ) : (
          <p className="text-xs text-muted-foreground py-1">No active subscription</p>
        )}
        <Row label="Lifetime Paid" value={fmtInr(user.lifetime_paid_inr * 100)} highlight />
      </Section>

      {/* ── Activity Summary ────────────────────────────────────────────── */}
      <Section title="Activity Summary">
        <Row label="Total Chats"    value={String(user.total_chats)} />
        <Row label="Total Kundalis" value={String(user.total_kundalis)} />
        <Row label="Palm Readings"  value={String(user.total_palm)} />
        {user.usage_today?.length > 0 && (
          <div className="mt-2">
            <p className="text-xs text-muted-foreground mb-1.5">Today's usage:</p>
            <div className="flex flex-wrap gap-1.5">
              {user.usage_today.map(u => (
                <span key={u.feature} className="text-xs px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-100">
                  {u.feature} × {u.count}
                </span>
              ))}
            </div>
          </div>
        )}
      </Section>

    </div>
  );
}

// ── Sub-components ────────────────────────────────────────────────────────────

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-white rounded-xl border border-border overflow-hidden">
      <div className="px-4 py-2.5 bg-muted/50 border-b border-border">
        <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">{title}</p>
      </div>
      <div className="px-4 py-2 divide-y divide-border/50">
        {children}
      </div>
    </div>
  );
}

function Row({ label, value, mono, highlight }: {
  label:     string;
  value:     string;
  mono?:     boolean;
  highlight?: boolean;
}) {
  return (
    <div className="flex items-center justify-between py-2 gap-4">
      <span className="text-xs text-muted-foreground shrink-0">{label}</span>
      <span className={`text-xs text-right truncate max-w-[60%] ${
        mono      ? "font-mono text-foreground/70" :
        highlight ? "font-semibold text-amber-700"  :
                    "text-foreground"
      }`}>
        {value || "—"}
      </span>
    </div>
  );
}
