import { fmt } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";
import { Loader, Empty } from "@/components/ui/Loader";
import type { LoginEntry } from "@/lib/types";
import {
  Smartphone, Monitor, Tablet, Globe, Phone, Chrome,
  MapPin, Wifi, Clock, CheckCircle2, XCircle,
} from "lucide-react";

// ── Helpers ───────────────────────────────────────────────────────────────────

function countryFlag(code?: string) {
  if (!code || code.length !== 2) return "🌍";
  // Convert country code to flag emoji (regional indicator symbols)
  return String.fromCodePoint(
    ...[...code.toUpperCase()].map(c => 0x1F1E6 - 65 + c.charCodeAt(0))
  );
}

function methodLabel(method: string) {
  switch (method) {
    case "google":    return { label: "Google",    cls: "bg-blue-50 text-blue-700 border border-blue-200" };
    case "apple":     return { label: "Apple",     cls: "bg-gray-100 text-gray-700 border border-gray-300" };
    case "phone_otp": return { label: "Phone OTP", cls: "bg-amber-50 text-amber-700 border border-amber-200" };
    default:          return { label: method || "Unknown", cls: "bg-muted text-muted-foreground border border-border" };
  }
}

function clientLabel(client: string) {
  switch (client) {
    case "android": return { label: "Android App", Icon: Smartphone, cls: "text-green-700" };
    case "ios":     return { label: "iOS App",     Icon: Smartphone, cls: "text-blue-700"  };
    case "web":     return { label: "Web",         Icon: Monitor,    cls: "text-muted-foreground" };
    default:        return { label: client || "Web", Icon: Globe,    cls: "text-muted-foreground" };
  }
}

function deviceIcon(device: string) {
  const d = (device || "").toLowerCase();
  if (d.includes("mobile") || d.includes("android") || d.includes("iphone")) return Smartphone;
  if (d.includes("tablet") || d.includes("ipad")) return Tablet;
  return Monitor;
}

// ── Main component ────────────────────────────────────────────────────────────

export function LoginsTab({ items, loading }: { items: LoginEntry[]; loading: boolean }) {
  if (loading) return <Loader />;
  if (!items?.length) return <Empty msg="No login history." />;

  return (
    <div className="space-y-3">
      {items.map((l) => {
        const method  = methodLabel(l.login_method);
        const cli     = clientLabel(l.client);
        const DevIcon = deviceIcon(l.device ?? "");
        const flag    = countryFlag(l.country_code);
        const location = [l.city, l.region, l.country].filter(Boolean).join(", ") || null;

        return (
          <div
            key={l.id}
            className={`rounded-xl border p-4 text-sm transition-colors ${
              l.success
                ? "bg-white border-border hover:border-amber-200"
                : "bg-red-50 border-red-200"
            }`}
          >
            {/* Top row — status + method + client + time */}
            <div className="flex items-center gap-2 flex-wrap mb-3">
              {l.success
                ? <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                : <XCircle      className="w-4 h-4 text-red-500 shrink-0" />
              }
              <span className={`text-xs font-semibold ${l.success ? "text-emerald-700" : "text-red-600"}`}>
                {l.success ? "Login successful" : `Failed — ${l.fail_reason || "unknown reason"}`}
              </span>

              {/* Method badge */}
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${method.cls}`}>
                {l.login_method === "google" ? "🔵" : l.login_method === "phone_otp" ? "📱" : "🍎"} {method.label}
              </span>

              {/* Client badge */}
              <span className={`flex items-center gap-1 text-xs font-medium ${cli.cls}`}>
                <cli.Icon className="w-3 h-3" />
                {cli.label}
              </span>

              {/* Time */}
              <span className="ml-auto flex items-center gap-1 text-xs text-muted-foreground whitespace-nowrap">
                <Clock className="w-3 h-3" />
                {fmt(l.logged_at)}
              </span>
            </div>

            {/* Detail grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {/* Device */}
              {l.device && (
                <InfoRow Icon={DevIcon} label="Device">
                  {l.device}
                </InfoRow>
              )}

              {/* IP address */}
              {l.ip && (
                <InfoRow Icon={Wifi} label="IP Address">
                  <span className="font-mono">{l.ip}</span>
                </InfoRow>
              )}

              {/* Location */}
              {location && (
                <InfoRow Icon={MapPin} label="Location">
                  <span>{flag} {location}</span>
                </InfoRow>
              )}

              {/* ISP */}
              {l.isp && (
                <InfoRow Icon={Globe} label="ISP / Network">
                  {l.isp}
                </InfoRow>
              )}
            </div>

            {/* Map link if lat/lon available */}
            {l.lat && l.lon && (
              <a
                href={`https://www.google.com/maps?q=${l.lat},${l.lon}`}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-2 inline-flex items-center gap-1 text-xs text-amber-700 hover:underline"
              >
                <MapPin className="w-3 h-3" />
                View on map ({l.lat.toFixed(3)}, {l.lon.toFixed(3)})
              </a>
            )}
          </div>
        );
      })}
    </div>
  );
}

// ── Small helper ──────────────────────────────────────────────────────────────

function InfoRow({ Icon, label, children }: {
  Icon: React.ComponentType<{ className?: string }>;
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex items-start gap-2 text-xs">
      <Icon className="w-3.5 h-3.5 text-muted-foreground mt-0.5 shrink-0" />
      <span className="text-muted-foreground shrink-0">{label}:</span>
      <span className="text-foreground break-all">{children}</span>
    </div>
  );
}
