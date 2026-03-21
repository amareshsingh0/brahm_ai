import { useState, useCallback, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { KundliChart } from "@/components/charts/KundliChart";
import { samplePlanets, useKundliStore, type PlanetData } from "@/store/kundliStore";
import { useKundali } from "@/hooks/useKundali";
import { Link } from "react-router-dom";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Zap, FileDown, ChevronDown, ChevronUp, Star, Calendar,
  Layers, Home, Loader2, Edit2, User, MapPin,
} from "lucide-react";
import PageBot from "@/components/PageBot";
import { searchCities, getCities, type City } from "@/lib/cities";
import type { KundaliResponse, VargaChartData, AntardashaData, UpagrahaEntry } from "@/types/api";

// ── Constants ──────────────────────────────────────────────────────────────────
const GRAHA_SYMBOLS: Record<string, string> = {
  Surya:"☉", Chandra:"☽", Mangal:"♂", Budh:"☿",
  Guru:"♃", Shukra:"♀", Shani:"♄", Rahu:"☊", Ketu:"☋",
};
const GRAHA_EN: Record<string, string> = {
  Surya:"Sun", Chandra:"Moon", Mangal:"Mars", Budh:"Mercury",
  Guru:"Jupiter", Shukra:"Venus", Shani:"Saturn", Rahu:"Rahu", Ketu:"Ketu",
};
const GRAHA_COLOR: Record<string, string> = {
  Surya:"hsl(42 90% 64%)", Chandra:"hsl(220 20% 85%)", Mangal:"hsl(0 70% 60%)",
  Budh:"hsl(140 55% 55%)", Guru:"hsl(42 70% 58%)", Shukra:"hsl(292 75% 65%)",
  Shani:"hsl(220 30% 55%)", Rahu:"hsl(225 35% 60%)", Ketu:"hsl(30 60% 50%)",
};
const RASHI_SYMBOLS: Record<string, string> = {
  Mesha:"♈", Vrishabha:"♉", Mithuna:"♊", Karka:"♋",
  Simha:"♌", Kanya:"♍", Tula:"♎", Vrischika:"♏",
  Dhanu:"♐", Makara:"♑", Kumbha:"♒", Meena:"♓",
};
const STATUS_BADGE: Record<string, { cls: string; label: string }> = {
  "Uchcha (Exalted)":       { cls:"bg-emerald-500/20 text-emerald-400 border-emerald-500/30", label:"Exalted" },
  "Neecha (Debilitated)":   { cls:"bg-red-500/20 text-red-400 border-red-500/30",             label:"Debilitated" },
  "Svakshetra (Own)":       { cls:"bg-amber-500/20 text-amber-400 border-amber-500/30",       label:"Own Sign" },
  "Normal":                 { cls:"bg-muted/20 text-muted-foreground border-border/20",       label:"Normal" },
};
const RELATIONSHIP_BADGE: Record<string, { cls: string; label: string }> = {
  "Own":     { cls:"bg-amber-500/20 text-amber-400", label:"Own" },
  "Friend":  { cls:"bg-emerald-500/15 text-emerald-400", label:"Friend's House" },
  "Enemy":   { cls:"bg-red-500/15 text-red-400", label:"Enemy's House" },
  "Neutral": { cls:"bg-muted/15 text-muted-foreground", label:"Neutral" },
};
const DASHA_COLORS: Record<string, string> = {
  Ketu:"#f97316", Shukra:"#a855f7", Surya:"#f59e0b", Chandra:"#94a3b8",
  Mangal:"#ef4444", Rahu:"#6366f1", Guru:"#eab308", Shani:"#64748b", Budh:"#22c55e",
};
const YOGA_CATEGORY_COLORS: Record<string, string> = {
  Power: "text-amber-400", Wealth: "text-emerald-400", Intellect: "text-blue-400",
  Spiritual: "text-purple-400", Marriage: "text-pink-400", Adversity: "text-red-400",
};

// All varga charts D-1 to D-60
const VARGA_QUICK = [
  { div: 1,  code: "D-1",  name: "Rashi",        desc: "Body, Personality" },
  { div: 2,  code: "D-2",  name: "Hora",         desc: "Wealth" },
  { div: 3,  code: "D-3",  name: "Drekkana",     desc: "Siblings" },
  { div: 4,  code: "D-4",  name: "Chaturthamsha",desc: "Fortune" },
  { div: 7,  code: "D-7",  name: "Saptamsha",    desc: "Children" },
  { div: 9,  code: "D-9",  name: "Navamsha",     desc: "Marriage, Soul" },
  { div: 10, code: "D-10", name: "Dashamsha",    desc: "Career" },
  { div: 12, code: "D-12", name: "Dwadashamsha", desc: "Parents" },
  { div: 16, code: "D-16", name: "Shodashamsha", desc: "Vehicles" },
  { div: 20, code: "D-20", name: "Vimsamsha",    desc: "Spirituality" },
  { div: 24, code: "D-24", name: "Chaturvimsha", desc: "Education" },
  { div: 27, code: "D-27", name: "Nakshatramsha",desc: "Strength" },
  { div: 30, code: "D-30", name: "Trimsamsha",   desc: "Evils, Karma" },
  { div: 40, code: "D-40", name: "Khavedamsha",  desc: "Auspicious" },
  { div: 45, code: "D-45", name: "Akshavedamsha",desc: "Character" },
  { div: 60, code: "D-60", name: "Shashtiamsha", desc: "Past Life" },
];

const GRAHA_ORDER = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];

// ── Helpers ────────────────────────────────────────────────────────────────────
function isCurrentDasha(start: string, end: string): boolean {
  const now = new Date();
  return new Date(start) <= now && now <= new Date(end);
}
function formatDate(d: string): string {
  try { return new Date(d).toLocaleDateString("en-IN", { day:"2-digit", month:"short", year:"numeric" }); } catch { return d; }
}
function toPlanetData(gname: string, g: any): PlanetData {
  return {
    name: GRAHA_EN[gname] ?? gname,
    symbol: GRAHA_SYMBOLS[gname] ?? "?",
    rashi: g.rashi, house: g.house,
    degree: `${g.degree?.toFixed?.(1) ?? g.degree ?? 0}°`,
    nakshatra: g.nakshatra ?? "",
    color: GRAHA_COLOR[gname] ?? "hsl(0 0% 60%)",
    sanskritName: gname,
  };
}

// ── PDF Generator ─────────────────────────────────────────────────────────────
function generateKundaliPDF(data: KundaliResponse) {
  const planetRows = GRAHA_ORDER.map(gn => {
    const g = data.grahas[gn];
    if (!g) return "";
    const retro = g.retro ? " ℞" : "";
    const statusColor = g.status.includes("Exalted") ? "#10b981"
      : g.status.includes("Debilitated") ? "#ef4444"
      : g.status.includes("Own") ? "#f59e0b" : "#6b7280";
    const relColor = g.relationship === "Friend" ? "#10b981" : g.relationship === "Enemy" ? "#ef4444" : "#6b7280";
    return `<tr>
      <td>${GRAHA_SYMBOLS[gn] ?? ""} ${gn} (${GRAHA_EN[gn]})</td>
      <td>${g.rashi} ${RASHI_SYMBOLS[g.rashi] ?? ""} ${g.rashi_en ? `<span style="color:#9ca3af;font-size:9px">${g.rashi_en}</span>` : ""}</td>
      <td style="text-align:center">H${g.house}</td>
      <td style="text-align:center">${g.degree.toFixed(2)}°${g.longitude ? ` <span style="color:#9ca3af;font-size:8px">(${g.longitude.toFixed(2)}°)</span>` : ""}</td>
      <td>${g.nakshatra} (${g.nakshatra_lord ?? ""})</td>
      <td style="text-align:center">P${g.pada}${retro}</td>
      <td style="color:${statusColor};font-weight:600">${g.status.split(" ")[0]}</td>
      <td style="color:${relColor};font-size:9px">${g.relationship ?? ""}</td>
    </tr>`;
  }).join("");

  const navRows = GRAHA_ORDER.map(gn => {
    const g = data.navamsha?.[gn];
    if (!g) return "";
    const statusColor = g.status.includes("Exalted") ? "#10b981"
      : g.status.includes("Debilitated") ? "#ef4444"
      : g.status.includes("Own") ? "#f59e0b" : "#6b7280";
    return `<tr>
      <td>${GRAHA_SYMBOLS[gn] ?? ""} ${gn}</td>
      <td>${g.rashi} ${RASHI_SYMBOLS[g.rashi] ?? ""}</td>
      <td style="text-align:center">H${g.house}</td>
      <td style="color:${statusColor};font-weight:600">${g.status.split(" ")[0]}</td>
      <td>${g.retro ? "℞" : ""}</td>
    </tr>`;
  }).join("");

  const dashaRows = data.dashas.map(d => {
    const cur = isCurrentDasha(d.start, d.end);
    const antarRows = d.antardashas?.map(a => {
      const aCur = isCurrentDasha(a.start, a.end);
      return `<tr style="font-size:9px;${aCur ? "background:#fffbeb;" : ""}">
        <td style="padding-left:24px">↳ ${a.lord}</td>
        <td>${formatDate(a.start)}</td><td>${formatDate(a.end)}</td>
        <td style="text-align:center">${a.years}y</td>
        <td style="color:${aCur ? "#b45309" : "#6b7280"}">${aCur ? "Active" : ""}</td>
      </tr>`;
    }).join("") ?? "";
    return `<tr style="${cur ? "background:#fffbeb;font-weight:600" : ""}">
      <td>${cur ? "★ " : ""}${d.lord} (${GRAHA_EN[d.lord] ?? d.lord})</td>
      <td>${formatDate(d.start)}</td><td>${formatDate(d.end)}</td>
      <td style="text-align:center">${d.years} yrs</td>
      <td style="color:${cur ? "#b45309" : "#6b7280"}">${cur ? "Active Now" : ""}</td>
    </tr>${antarRows}`;
  }).join("");

  const yogaRows = data.yogas.map(y => {
    const strengthColor = y.strength === "Very Strong" ? "#10b981" : y.strength === "Strong" ? "#f59e0b" : "#6b7280";
    return `<tr>
      <td style="font-weight:600">${y.name}</td>
      <td style="color:${strengthColor}">${y.strength}</td>
      <td style="font-size:10px;color:#6b7280">${y.category ?? ""}</td>
      <td style="font-size:10px">${y.desc}</td>
    </tr>`;
  }).join("");

  const houseRows = data.houses.map(h => `<tr>
    <td style="text-align:center;font-weight:600">H${h.house}</td>
    <td>${h.rashi} ${RASHI_SYMBOLS[h.rashi] ?? ""} ${h.rashi_en ? `<span style="color:#9ca3af;font-size:9px">(${h.rashi_en})</span>` : ""}</td>
    <td>${h.lord} ${h.lord_en ? `(${h.lord_en})` : ""}</td>
    <td style="font-size:9px;color:#6b7280">${h.planets?.join(", ") ?? ""}</td>
    <td style="font-size:9px;color:#6b7280">${h.signification ?? ""}</td>
  </tr>`).join("");

  const ayan = data.ayanamsha_label ?? "Lahiri";

  const html = `<!DOCTYPE html><html><head><meta charset="UTF-8">
<title>Janam Kundali — ${data.name || "Birth Chart"}</title>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family: Georgia, 'Times New Roman', serif; background:#fff; color:#1a1a2e; font-size:11px; line-height:1.4; }
  .page { width:210mm; min-height:297mm; margin:0 auto; padding:12mm 14mm; }
  h2 { font-size:13px; color:#6d28d9; border-bottom:2px solid #ede9fe; padding-bottom:3px; margin:14px 0 6px; }
  .header-bar { background:linear-gradient(135deg,#1e1b4b,#312e81); color:#fff; padding:16px 20px; border-radius:10px; text-align:center; margin-bottom:14px; }
  .header-bar h1 { color:#fbbf24; font-size:20px; margin-bottom:3px; }
  .header-bar .sub { color:#c4b5fd; font-size:10px; }
  .meta-row { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; margin-bottom:12px; }
  .meta-card { border:1px solid #e5e7eb; border-radius:6px; padding:8px 10px; }
  .meta-label { font-size:9px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; }
  .meta-val { font-size:12px; font-weight:600; color:#1a1a2e; margin-top:1px; }
  table { width:100%; border-collapse:collapse; font-size:10px; margin-bottom:4px; }
  th { background:#ede9fe; color:#4c1d95; padding:4px 6px; text-align:left; font-size:9px; font-weight:600; }
  td { padding:3px 6px; border-bottom:1px solid #f3f4f6; vertical-align:top; }
  tr:nth-child(even) td { background:#fafafa; }
  .lagna-row { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; margin:8px 0 12px; }
  .lagna-card { border:2px solid #fbbf24; border-radius:8px; padding:8px; text-align:center; }
  .lagna-main { font-size:18px; font-weight:bold; color:#b45309; }
  .lagna-sub { font-size:9px; color:#6b7280; margin-top:1px; }
  .two-col { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
  .footer { margin-top:14px; padding-top:8px; border-top:1px solid #e5e7eb; font-size:8px; color:#9ca3af; text-align:center; }
  @media print { body { -webkit-print-color-adjust:exact; print-color-adjust:exact; } .page { padding:8mm; } }
  .page-break { page-break-before:always; }
</style>
</head><body><div class="page">

<div class="header-bar">
  <h1>☽ Janam Kundali — Vedic Birth Chart</h1>
  <div class="sub">${ayan} Ayanamsha · Whole Sign Houses · ${data.rahu_mode === "true" ? "True" : "Mean"} Rahu/Ketu</div>
  <div style="margin-top:6px;font-size:16px;color:#fbbf24;font-weight:bold">${data.name || "—"}</div>
  <div style="font-size:10px;color:#c4b5fd;margin-top:2px">${data.place || ""} · ${data.birth_date}</div>
</div>

<div class="meta-row">
  <div class="meta-card">
    <div class="meta-label">Coordinates</div>
    <div class="meta-val">${data.lat.toFixed(4)}°N, ${data.lon.toFixed(4)}°E</div>
  </div>
  <div class="meta-card">
    <div class="meta-label">Timezone</div>
    <div class="meta-val">UTC +${data.tz}</div>
  </div>
  <div class="meta-card">
    <div class="meta-label">Ayanamsha (${ayan})</div>
    <div class="meta-val">${data.ayanamsha}°</div>
  </div>
  <div class="meta-card">
    <div class="meta-label">Lagna Degree</div>
    <div class="meta-val">${data.lagna.degree}° ${data.lagna.rashi}</div>
  </div>
</div>

<h2>Lagna &amp; Key Positions</h2>
<div class="lagna-row">
  <div class="lagna-card">
    <div class="lagna-sub">Lagna (Ascendant)</div>
    <div class="lagna-main">${data.lagna.rashi} ${RASHI_SYMBOLS[data.lagna.rashi] ?? ""}</div>
    <div class="lagna-sub">${data.lagna.nakshatra} · Pada ${data.lagna.pada ?? ""} · ${data.lagna.degree}°</div>
  </div>
  <div class="lagna-card" style="border-color:#6366f1">
    <div class="lagna-sub">Moon (Chandra)</div>
    <div class="lagna-main" style="color:#4f46e5">${data.grahas["Chandra"]?.rashi ?? ""}</div>
    <div class="lagna-sub">${data.grahas["Chandra"]?.nakshatra ?? ""} · P${data.grahas["Chandra"]?.pada ?? ""}</div>
  </div>
  <div class="lagna-card" style="border-color:#f59e0b">
    <div class="lagna-sub">Sun (Surya)</div>
    <div class="lagna-main" style="color:#b45309">${data.grahas["Surya"]?.rashi ?? ""}</div>
    <div class="lagna-sub">${data.grahas["Surya"]?.nakshatra ?? ""} · ${data.grahas["Surya"]?.status?.split(" ")[0] ?? ""}</div>
  </div>
  <div class="lagna-card" style="border-color:#eab308">
    <div class="lagna-sub">Jupiter (Guru)</div>
    <div class="lagna-main" style="color:#a16207">${data.grahas["Guru"]?.rashi ?? ""}</div>
    <div class="lagna-sub">H${data.grahas["Guru"]?.house} · ${data.grahas["Guru"]?.status?.split(" ")[0] ?? ""}</div>
  </div>
</div>

<h2>All Planet Positions (D-1 Rashi Chart)</h2>
<table>
  <thead><tr>
    <th>Planet</th><th>Rashi</th><th style="text-align:center">House</th>
    <th style="text-align:center">Degree</th><th>Nakshatra (Lord)</th>
    <th style="text-align:center">Pada/℞</th><th>Status</th><th>Relation</th>
  </tr></thead>
  <tbody>${planetRows}</tbody>
</table>

<h2>Bhava Chart (Houses)</h2>
<table>
  <thead><tr><th style="text-align:center">House</th><th>Rashi</th><th>Lord</th><th>Planets</th><th>Signification</th></tr></thead>
  <tbody>${houseRows}</tbody>
</table>

<div class="page-break"></div>

<h2>Navamsha Chart (D-9) — ${data.navamsha_lagna?.rashi ?? ""} Lagna</h2>
<table>
  <thead><tr><th>Planet</th><th>Rashi</th><th style="text-align:center">House</th><th>Status</th><th>Retro</th></tr></thead>
  <tbody>${navRows || "<tr><td colspan='5' style='color:#9ca3af'>Not available</td></tr>"}</tbody>
</table>

<h2>Vimshottari Dasha (with Antardasha)</h2>
<table>
  <thead><tr><th>Dasha Lord</th><th>Start</th><th>End</th><th style="text-align:center">Duration</th><th>Status</th></tr></thead>
  <tbody>${dashaRows}</tbody>
</table>

<h2>Active Yogas</h2>
<table>
  <thead><tr><th>Yoga</th><th>Strength</th><th>Category</th><th>Description</th></tr></thead>
  <tbody>${yogaRows || "<tr><td colspan='4' style='color:#9ca3af'>No yogas detected</td></tr>"}</tbody>
</table>

<div class="footer">
  Generated by Brahm AI · ${ayan} Ayanamsha · ${data.rahu_mode === "true" ? "True" : "Mean"} Rahu/Ketu · Whole Sign Houses · pyswisseph DE431 ·
  ${new Date().toLocaleDateString("en-IN", { day:"2-digit", month:"long", year:"numeric" })}<br>
  This chart is for reference. Consult a qualified Jyotishi for complete life guidance.
</div>

</div></body></html>`;

  const win = window.open("", "_blank");
  if (!win) return;
  win.document.write(html);
  win.document.close();
  setTimeout(() => win.print(), 800);
}

// ── PlanetRow ──────────────────────────────────────────────────────────────────
function PlanetRow({ gname, g, onClick, isSelected }: {
  gname: string;
  g: NonNullable<KundaliResponse["grahas"][string]>;
  onClick: () => void;
  isSelected: boolean;
}) {
  const sb = STATUS_BADGE[g.status] ?? STATUS_BADGE["Normal"];
  const rb = RELATIONSHIP_BADGE[g.relationship ?? "Neutral"] ?? RELATIONSHIP_BADGE["Neutral"];
  const color = GRAHA_COLOR[gname] ?? "hsl(0 0% 60%)";
  return (
    <motion.button
      initial={{ opacity: 0, x: -4 }}
      animate={{ opacity: 1, x: 0 }}
      onClick={onClick}
      className={`w-full flex items-center gap-3 p-2.5 rounded-lg hover:bg-muted/30 transition-colors text-left ${isSelected ? "bg-primary/10 border border-primary/20" : ""}`}
    >
      <span className="text-xl shrink-0 w-7 text-center" style={{ color }}>
        {GRAHA_SYMBOLS[gname] ?? "?"}
      </span>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="text-sm font-medium text-foreground">{GRAHA_EN[gname] ?? gname}</span>
          <span className="text-xs text-muted-foreground/60">({gname})</span>
          {g.retro && <span className="text-[10px] text-amber-400 border border-amber-500/30 px-1 rounded">℞</span>}
          {g.combust && <span className="text-[10px] text-orange-400 border border-orange-500/30 px-1 rounded">Combust</span>}
        </div>
        <div className="text-xs text-muted-foreground mt-0.5">
          {g.rashi} · H{g.house} · {g.nakshatra} P{g.pada}
        </div>
      </div>
      <div className="flex flex-col items-end gap-1 shrink-0">
        <span className="text-xs text-muted-foreground">{g.degree.toFixed(1)}°</span>
        <span className={`text-[10px] px-1.5 py-0.5 rounded border ${sb.cls}`}>{sb.label}</span>
      </div>
    </motion.button>
  );
}

// ── Antardasha Expansion ───────────────────────────────────────────────────────
function AntardashaList({ antardashas }: { antardashas: AntardashaData[] }) {
  const [expandedAntar, setExpandedAntar] = useState<string | null>(null);
  return (
    <div className="ml-6 mt-1 space-y-0.5 border-l border-border/20 pl-3">
      {antardashas.map((a, i) => {
        const cur = isCurrentDasha(a.start, a.end);
        const hasPratyantar = a.pratyantardashas && a.pratyantardashas.length > 0;
        const isExp = expandedAntar === `${i}`;
        return (
          <div key={i}>
            <div
              onClick={() => hasPratyantar && setExpandedAntar(isExp ? null : `${i}`)}
              className={`flex items-center gap-2 text-xs py-0.5 px-1.5 rounded ${cur ? "bg-amber-500/10 text-amber-300" : "text-muted-foreground"} ${hasPratyantar ? "cursor-pointer hover:bg-muted/20" : ""}`}
            >
              <div className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: DASHA_COLORS[a.lord] ?? "#888" }} />
              <span className="font-medium w-14">{a.lord}</span>
              <span>{formatDate(a.start)} → {formatDate(a.end)}</span>
              <span className="text-muted-foreground/60">{a.years}y</span>
              {cur && <span className="text-[10px] text-amber-400 font-medium">★ Active</span>}
              {hasPratyantar && <span className="ml-auto text-[10px] opacity-50">{isExp ? "▲" : "▼"}</span>}
            </div>
            {isExp && hasPratyantar && (
              <div className="ml-4 mt-0.5 space-y-0.5 border-l border-border/10 pl-2">
                {a.pratyantardashas!.map((p, j) => {
                  const pCur = isCurrentDasha(p.start, p.end);
                  return (
                    <div key={j} className={`flex items-center gap-1.5 text-[10px] py-0.5 px-1 rounded ${pCur ? "bg-amber-500/10 text-amber-300" : "text-muted-foreground/70"}`}>
                      <div className="w-1 h-1 rounded-full shrink-0" style={{ background: DASHA_COLORS[p.lord] ?? "#888" }} />
                      <span className="font-medium w-12">{p.lord}</span>
                      <span>{formatDate(p.start)} → {formatDate(p.end)}</span>
                      <span className="opacity-60">{p.days}d</span>
                      {pCur && <span className="text-amber-400">★</span>}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

// ── Birth Form ────────────────────────────────────────────────────────────────
interface LocalForm { name: string; date: string; time: string; place: string }

function KundaliBirthForm({
  initial, loading, error, onGenerate,
}: {
  initial: LocalForm;
  loading: boolean;
  error: string | null;
  onGenerate: (form: LocalForm, city: City | null) => void;
}) {
  const [form, setForm] = useState<LocalForm>(initial);
  const [city, setCity] = useState<City | null>(null);
  const [suggestions, setSuggestions] = useState<City[]>([]);
  const wrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => { getCities().catch(() => {}); }, []);
  useEffect(() => {
    const h = (e: MouseEvent) => { if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setSuggestions([]); };
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);

  const handleCityInput = async (v: string) => {
    setForm(f => ({ ...f, place: v }));
    setCity(null);
    if (v.length >= 2) { const res = await searchCities(v); setSuggestions(res); }
    else setSuggestions([]);
  };

  const handleSelect = (c: City) => {
    setForm(f => ({ ...f, place: c.name }));
    setCity(c); setSuggestions([]);
  };

  const canSubmit = !!form.date && !!form.time;

  return (
    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
      className="max-w-lg mx-auto">
      <div className="cosmic-card rounded-2xl p-5 space-y-4">
        <div className="flex items-center gap-2 mb-1">
          <Star className="h-4 w-4 text-primary" />
          <h2 className="font-display text-base text-foreground">Birth Details</h2>
        </div>

        {/* Name */}
        <div>
          <Label className="text-xs text-muted-foreground">Name <span className="opacity-50">(optional)</span></Label>
          <div className="relative mt-1">
            <User className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground/50" />
            <Input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
              placeholder="Your name" className="pl-8 bg-muted/20 border-border/30" />
          </div>
        </div>

        {/* Date + Time */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label className="text-xs text-muted-foreground">Date of Birth <span className="text-red-400">*</span></Label>
            <Input type="date" value={form.date} onChange={e => setForm(f => ({ ...f, date: e.target.value }))}
              className="bg-muted/20 border-border/30 mt-1" />
          </div>
          <div>
            <Label className="text-xs text-muted-foreground">Time of Birth <span className="text-red-400">*</span></Label>
            <Input type="time" value={form.time} onChange={e => setForm(f => ({ ...f, time: e.target.value }))}
              className="bg-muted/20 border-border/30 mt-1" />
          </div>
        </div>

        {/* Place with autocomplete */}
        <div ref={wrapRef} className="relative">
          <Label className="text-xs text-muted-foreground">Birth Place</Label>
          <div className="relative mt-1">
            <MapPin className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground/50" />
            <Input value={form.place} onChange={e => handleCityInput(e.target.value)}
              placeholder="Search city..." autoComplete="off"
              className={`pl-8 bg-muted/20 border-border/30 ${city ? "border-primary/50" : ""}`} />
            {city && (
              <span className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-primary/70">
                ✓ {city.lat.toFixed(1)}°, {city.lon.toFixed(1)}°
              </span>
            )}
          </div>
          {suggestions.length > 0 && (
            <div className="absolute z-50 w-full mt-1 rounded-xl border border-border/30 bg-background/95 backdrop-blur shadow-xl overflow-hidden">
              {suggestions.map(c => (
                <button key={`${c.name}-${c.lat}`} onMouseDown={() => handleSelect(c)}
                  className="w-full text-left px-3 py-2.5 text-xs hover:bg-muted/40 transition-colors border-b border-border/10 last:border-0">
                  <span className="font-medium text-foreground">{c.name}</span>
                  <span className="text-muted-foreground ml-2">{c.lat.toFixed(1)}°N, {c.lon.toFixed(1)}°E</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {!city && form.place && (
          <p className="text-[10px] text-amber-400/80">Select a city from the dropdown for accurate coordinates.</p>
        )}

        {error && <p className="text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{error}</p>}

        <button
          onClick={() => onGenerate(form, city)}
          disabled={!canSubmit || loading}
          className="w-full py-2.5 rounded-xl text-sm font-semibold bg-primary/20 hover:bg-primary/30 text-primary border border-primary/30 transition-colors disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> Calculating...</> : <><Star className="h-4 w-4" /> Generate Kundali</>}
        </button>
      </div>

      <p className="text-[10px] text-muted-foreground/40 text-center mt-3">
        Lahiri Ayanamsha · Whole Sign Houses · pyswisseph DE431
      </p>
    </motion.div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function KundliPage() {
  const kundaliData = useKundliStore((s) => s.kundaliData);
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const setBirthDetails = useKundliStore((s) => s.setBirthDetails);
  const settings = useKundliStore((s) => s.kundaliSettings);
  const setSettings = useKundliStore((s) => s.setKundaliSettings);
  const nameLang = settings.nameLang;

  // View: "input" = show form, "result" = show kundali
  const [view, setView] = useState<"input" | "result">(kundaliData ? "result" : "input");
  const [formError, setFormError] = useState<string | null>(null);

  // Local form — pre-fill from store if available
  const [localForm, setLocalForm] = useState<LocalForm>({
    name: birthDetails?.name ?? "",
    date: birthDetails?.dateOfBirth ?? "",
    time: birthDetails?.timeOfBirth ?? "",
    place: birthDetails?.birthPlace ?? "",
  });

  const [selectedPlanet, setSelectedPlanet] = useState<PlanetData | null>(null);
  const [activeTab, setActiveTab] = useState<"charts" | "dasha" | "houses" | "chalit">("charts");
  const [showAllYogas, setShowAllYogas] = useState(false);
  const [expandedDasha, setExpandedDasha] = useState<string | null>(null);
  const [selectedVarga, setSelectedVarga] = useState(1); // Default D-1
  const [loadingVarga, setLoadingVarga] = useState(false);
  const [vargaCache, setVargaCache] = useState<Record<number, VargaChartData>>({});

  const kundaliMutation = useKundali();

  const handleGenerate = useCallback(async (form: LocalForm, city: { lat: number; lon: number; tz: number } | null) => {
    setFormError(null);
    const DEF = { lat: 25.317, lon: 83.013, tz: 5.5 }; // Varanasi fallback (select city from dropdown for accuracy)
    const loc = city ?? DEF;
    const details = {
      name: form.name || "Unknown",
      dateOfBirth: form.date,
      timeOfBirth: form.time || "12:00",
      birthPlace: form.place || "India",
      lat: loc.lat, lon: loc.lon, tz: loc.tz,
    };
    setLocalForm(form);
    setBirthDetails(details);
    try {
      await kundaliMutation.mutateAsync(details as any);
      setView("result");
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : "Calculation failed. Please check birth details.");
    }
  }, [kundaliMutation, setBirthDetails]);

  // Recalculate handler for settings changes (ayanamsha/rahu mode)
  const handleRecalculate = useCallback(() => {
    if (!birthDetails) return;
    kundaliMutation.mutate({
      name: birthDetails.name,
      dateOfBirth: birthDetails.dateOfBirth,
      timeOfBirth: birthDetails.timeOfBirth,
      birthPlace: birthDetails.birthPlace,
      lat: birthDetails.lat,
      lon: birthDetails.lon,
      tz: birthDetails.tz,
    });
  }, [birthDetails, kundaliMutation]);

  // Map kundaliData → PlanetData[] for chart
  const planets: PlanetData[] = kundaliData
    ? GRAHA_ORDER.map(gn => {
        const g = kundaliData.grahas[gn];
        if (!g) return null;
        return toPlanetData(gn, g);
      }).filter(Boolean) as PlanetData[]
    : samplePlanets;

  // Navamsha planets
  const navPlanets: PlanetData[] = kundaliData?.navamsha
    ? GRAHA_ORDER.map(gn => {
        const g = kundaliData.navamsha![gn];
        if (!g) return null;
        return { ...toPlanetData(gn, g), degree: "0°", nakshatra: "" };
      }).filter(Boolean) as PlanetData[]
    : [];

  // Varga chart planets (from varga_charts or navamsha for D-9)
  const getVargaPlanets = (div: number): PlanetData[] => {
    if (div === 1) return planets;
    if (div === 9) return navPlanets;
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache[div];
    if (!vc) return [];
    return GRAHA_ORDER.map(gn => {
      const g = vc.grahas[gn];
      if (!g) return null;
      return { ...toPlanetData(gn, g), degree: "0°", nakshatra: "" };
    }).filter(Boolean) as PlanetData[];
  };

  const getVargaLagna = (div: number): string => {
    if (div === 1) return kundaliData?.lagna.rashi ?? "Tula";
    if (div === 9) return kundaliData?.navamsha_lagna?.rashi ?? kundaliData?.lagna.rashi ?? "Tula";
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache[div];
    return vc?.lagna?.rashi ?? kundaliData?.lagna.rashi ?? "Tula";
  };

  const lagnaRashi = kundaliData?.lagna.rashi ?? "Tula";
  const navLagnaRashi = kundaliData?.navamsha_lagna?.rashi ?? lagnaRashi;
  const yogas = kundaliData?.yogas ?? [];
  const visibleYogas = showAllYogas ? yogas : yogas.slice(0, 4);
  const currentDasha = kundaliData?.dashas.find(d => isCurrentDasha(d.start, d.end));

  // Load varga chart on-demand
  const loadVarga = async (div: number) => {
    if (div === 1 || div === 9 || vargaCache[div] || kundaliData?.varga_charts?.[`D-${div}`]) return;
    if (!kundaliData) return; // no data yet, skip
    if (!birthDetails) return;
    setLoadingVarga(true);
    try {
      const { api } = await import("@/lib/api");
      const resp = await api.post<KundaliResponse>("/api/kundali", {
        name: birthDetails.name,
        date: birthDetails.dateOfBirth,
        time: birthDetails.timeOfBirth,
        place: birthDetails.birthPlace,
        lat: birthDetails.lat,
        lon: birthDetails.lon,
        tz: birthDetails.tz,
        ayanamsha: kundaliData?.ayanamsha_mode ?? "lahiri",
        rahu_mode: kundaliData?.rahu_mode ?? "mean",
        calc_options: [`d${div}`],
      });
      if (resp.varga_charts?.[`D-${div}`]) {
        setVargaCache(prev => ({ ...prev, [div]: resp.varga_charts![`D-${div}`] }));
      }
    } catch (e) {
      console.error("Failed to load varga chart:", e);
    }
    setLoadingVarga(false);
  };

  const handleVargaSelect = (div: number) => {
    setSelectedVarga(div);
    loadVarga(div);
  };

  return (
    <div className="p-3 md:p-5 space-y-4 overflow-x-hidden">
      {/* Header — always shown */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <h1 className="font-display text-2xl text-foreground text-glow-gold mb-0.5">
            {view === "result" && kundaliData?.name ? `${kundaliData.name}'s Kundali` : "Janam Kundali"}
          </h1>
          {view === "result" && kundaliData ? (
            <p className="text-xs text-muted-foreground">
              {kundaliData.place} · {kundaliData.birth_date} · {kundaliData.ayanamsha_label ?? "Lahiri"} {kundaliData.ayanamsha}°
              {kundaliData.rahu_mode === "true" ? " · True Rahu" : ""}
            </p>
          ) : (
            <p className="text-xs text-muted-foreground">Vedic birth chart · Divisional charts · Dasha · Yogas</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {view === "result" && (
            <button onClick={() => setView("input")}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs text-muted-foreground hover:text-foreground border border-border/20 hover:border-border/40 transition-colors">
              <Edit2 className="h-3 w-3" /> New / Edit
            </button>
          )}
          {view === "result" && kundaliData && (
            <button onClick={() => generateKundaliPDF(kundaliData)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-primary/20 hover:bg-primary/30 text-primary transition-colors border border-primary/20">
              <FileDown className="h-3.5 w-3.5" /> PDF
            </button>
          )}
        </div>
      </motion.div>

      {/* INPUT VIEW — Birth Form */}
      <AnimatePresence mode="wait">
        {view === "input" && (
          <motion.div key="form" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}>
            <KundaliBirthForm
              initial={localForm}
              loading={kundaliMutation.isPending}
              error={formError}
              onGenerate={handleGenerate}
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* RESULT VIEW — Full Kundali */}
      {view === "result" && (
        <>

      {/* Inline Settings Strip */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.05 }}
        className="cosmic-card rounded-xl p-3 flex flex-wrap gap-4 items-start">
        {/* Ayanamsha */}
        <div className="flex-1 min-w-[180px]">
          <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Ayanamsha</p>
          <div className="flex gap-1 flex-wrap">
            {([ ["lahiri","Lahiri"], ["raman","Raman"], ["kp","KP"], ["true_citra","True Citra"] ] as const).map(([val, lbl]) => (
              <button key={val}
                onClick={() => { setSettings({ ayanamsha: val as any }); setTimeout(handleRecalculate, 100); }}
                className={`px-2 py-0.5 rounded text-xs transition-colors ${settings.ayanamsha === val ? "bg-primary/20 text-primary border border-primary/30" : "text-muted-foreground border border-border/20 hover:text-foreground"}`}>
                {lbl}
              </button>
            ))}
          </div>
        </div>
        {/* Rahu/Ketu */}
        <div>
          <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Rahu/Ketu</p>
          <div className="flex gap-1">
            {([ ["mean","Mean"], ["true","True"] ] as const).map(([val, lbl]) => (
              <button key={val}
                onClick={() => { setSettings({ rahuMode: val as any }); setTimeout(handleRecalculate, 100); }}
                className={`px-2 py-0.5 rounded text-xs transition-colors ${settings.rahuMode === val ? "bg-primary/20 text-primary border border-primary/30" : "text-muted-foreground border border-border/20 hover:text-foreground"}`}>
                {lbl}
              </button>
            ))}
          </div>
        </div>
        {/* Name Language */}
        <div>
          <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Names</p>
          <div className="flex gap-1">
            {([ ["vedic","Vedic"], ["en","English"] ] as const).map(([val, lbl]) => (
              <button key={val}
                onClick={() => setSettings({ nameLang: val as any })}
                className={`px-2 py-0.5 rounded text-xs transition-colors ${settings.nameLang === val ? "bg-primary/20 text-primary border border-primary/30" : "text-muted-foreground border border-border/20 hover:text-foreground"}`}>
                {lbl}
              </button>
            ))}
          </div>
        </div>
      </motion.div>

      {/* Key positions strip */}
      {kundaliData && (
        <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
          className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {[
            { label: "Lagna", value: kundaliData.lagna.rashi, sub: `${kundaliData.lagna.nakshatra} P${kundaliData.lagna.pada ?? ""}`, color: "text-primary", icon: RASHI_SYMBOLS[kundaliData.lagna.rashi] },
            { label: "Moon", value: kundaliData.grahas["Chandra"]?.rashi ?? "—", sub: kundaliData.grahas["Chandra"]?.nakshatra ?? "", color: "text-blue-400", icon: "☽" },
            { label: "Sun", value: kundaliData.grahas["Surya"]?.rashi ?? "—", sub: kundaliData.grahas["Surya"]?.status?.split(" ")[0] ?? "", color: "text-amber-400", icon: "☉" },
            { label: "Jupiter", value: kundaliData.grahas["Guru"]?.rashi ?? "—", sub: `H${kundaliData.grahas["Guru"]?.house} · ${kundaliData.grahas["Guru"]?.status?.split(" ")[0] ?? ""}`, color: "text-yellow-400", icon: "♃" },
          ].map(item => (
            <div key={item.label} className="cosmic-card rounded-xl p-3">
              <div className="flex items-center gap-1.5 mb-0.5">
                <span className="text-base opacity-60">{item.icon}</span>
                <p className="text-xs text-muted-foreground uppercase tracking-wide">{item.label}</p>
              </div>
              <p className={`text-sm font-semibold ${item.color}`}>{item.value}</p>
              <p className="text-xs text-muted-foreground/70">{item.sub}</p>
            </div>
          ))}
        </motion.div>
      )}

      {/* Current Mahadasha strip */}
      {currentDasha && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.15 }}
          className="cosmic-card rounded-xl p-3 flex items-center gap-3">
          <div className="w-2 h-2 rounded-full animate-pulse" style={{ background: DASHA_COLORS[currentDasha.lord] ?? "#888" }} />
          <div className="flex-1">
            <span className="text-xs text-muted-foreground uppercase tracking-wide">Current Mahadasha · </span>
            <span className="text-sm font-semibold text-foreground">{currentDasha.lord} ({GRAHA_EN[currentDasha.lord]})</span>
            <span className="text-xs text-muted-foreground ml-2">{formatDate(currentDasha.start)} → {formatDate(currentDasha.end)}</span>
          </div>
          {/* Find current antardasha */}
          {currentDasha.antardashas && (() => {
            const curAntar = currentDasha.antardashas.find(a => isCurrentDasha(a.start, a.end));
            if (!curAntar) return null;
            return (
              <div className="text-right">
                <p className="text-[10px] text-muted-foreground uppercase">Antardasha</p>
                <p className="text-xs font-medium text-foreground">{curAntar.lord} <span className="text-muted-foreground">({formatDate(curAntar.start)} → {formatDate(curAntar.end)})</span></p>
              </div>
            );
          })()}
        </motion.div>
      )}

      {/* Main grid */}
      <div className="grid lg:grid-cols-[1fr_360px] gap-4">

        {/* LEFT: Chart + Tabs */}
        <div className="cosmic-card rounded-xl p-3 md:p-4 min-w-0 overflow-hidden">
          {/* Tab bar */}
          <div className="flex gap-1 mb-3 border-b border-border/30 pb-2.5">
            {[
              { id:"charts", label:"Charts",  icon: <Layers className="h-3 w-3" /> },
              { id:"dasha",  label:"Dashas",  icon: <Calendar className="h-3 w-3" /> },
              { id:"houses", label:"Bhavas",  icon: <Home className="h-3 w-3" /> },
              { id:"chalit", label:"Chalit",  icon: <Star className="h-3 w-3" /> },
            ].map(tab => (
              <button key={tab.id}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
                className={`flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full transition-colors ${
                  activeTab === tab.id ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {tab.icon} {tab.label}
              </button>
            ))}
          </div>

          <AnimatePresence mode="wait">
            {/* Unified Charts Tab — D-1 to D-60 + Chart Style */}
            {activeTab === "charts" && (
              <motion.div key="charts" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="min-w-0">

                {/* Varga selector grid — flex-wrap, no horizontal scroll */}
                <div className="flex flex-wrap gap-1.5 mb-3">
                  {VARGA_QUICK.map(v => (
                    <button key={v.div}
                      onClick={() => handleVargaSelect(v.div)}
                      title={`${v.name} — ${v.desc}`}
                      className={`px-2 py-1 rounded-lg text-xs font-medium transition-colors ${
                        selectedVarga === v.div
                          ? "bg-primary/20 text-primary border border-primary/30"
                          : "text-muted-foreground hover:text-foreground border border-border/20 hover:border-border/40"
                      }`}
                    >
                      {v.code}
                      <span className="hidden sm:inline text-[10px] opacity-60 ml-1">{v.name.split(/[,\s]/)[0]}</span>
                    </button>
                  ))}
                </div>

                {/* Selected varga info */}
                {(() => {
                  const sel = VARGA_QUICK.find(v => v.div === selectedVarga);
                  return (
                    <div className="flex items-center justify-between mb-2 px-0.5">
                      <div>
                        <span className="text-sm font-semibold text-foreground">{sel?.code ?? `D-${selectedVarga}`}</span>
                        <span className="text-xs text-muted-foreground ml-2">{sel?.name}</span>
                        <span className="text-xs text-muted-foreground/50 ml-1">— {sel?.desc}</span>
                      </div>
                      <span className="text-xs text-muted-foreground">Lagna: <span className="text-foreground font-medium">{getVargaLagna(selectedVarga)}</span></span>
                    </div>
                  );
                })()}

                {loadingVarga ? (
                  <div className="text-center py-12 text-muted-foreground text-sm animate-pulse">Loading chart...</div>
                ) : getVargaPlanets(selectedVarga).length > 0 ? (
                  <>
                    {/* Chart with style toggle (N/S/E/W) built in */}
                    <KundliChart
                      onPlanetClick={setSelectedPlanet}
                      selectedPlanet={selectedPlanet}
                      planets={getVargaPlanets(selectedVarga)}
                      lagnaRashi={getVargaLagna(selectedVarga)}
                      showStyleToggle={true}
                    />
                    <p className="text-xs text-muted-foreground text-center mt-1.5">
                      Click planet for details · {kundaliData?.ayanamsha_label ?? "Lahiri"} · Whole Sign
                    </p>

                    {/* Planet positions table — only for non-D1 or always */}
                    {selectedVarga !== 1 && (
                      <div className="mt-4 space-y-1">
                        <p className="text-xs text-muted-foreground uppercase tracking-wide font-medium mb-2">
                          Planet Positions — {VARGA_QUICK.find(v => v.div === selectedVarga)?.code}
                        </p>
                        {GRAHA_ORDER.map(gn => {
                          const vc = selectedVarga === 9
                            ? kundaliData?.navamsha?.[gn]
                            : (kundaliData?.varga_charts?.[`D-${selectedVarga}`] ?? vargaCache[selectedVarga])?.grahas?.[gn];
                          if (!vc) return null;
                          const sb = STATUS_BADGE[vc.status] ?? STATUS_BADGE["Normal"];
                          return (
                            <div key={gn} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-muted/20">
                              <span className="text-base w-6 text-center" style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                              <span className="text-xs text-foreground flex-1">{gn} <span className="text-muted-foreground/60 text-xs">({GRAHA_EN[gn]})</span></span>
                              <span className="text-xs text-muted-foreground">{vc.rashi} · H{vc.house}</span>
                              {vc.retro && <span className="text-[10px] text-amber-400">℞</span>}
                              <span className={`text-[10px] px-1.5 py-0.5 rounded border ${sb.cls}`}>{sb.label}</span>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </>
                ) : (
                  <div className="text-center py-12 text-muted-foreground text-sm">
                    {kundaliData ? "Loading varga chart..." : "Generate your Kundali first"}
                  </div>
                )}
              </motion.div>
            )}

            {/* Dasha table with Antardasha expansion */}
            {activeTab === "dasha" && (
              <motion.div key="dasha" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Calendar className="h-3.5 w-3.5 text-primary" /> Vimshottari Dasha · Click to expand Antardasha
                </p>
                {(kundaliData?.dashas ?? []).length > 0 ? (
                  <div className="space-y-2">
                    {(kundaliData?.dashas ?? []).map((d, i) => {
                      const cur = isCurrentDasha(d.start, d.end);
                      const color = DASHA_COLORS[d.lord] ?? "#888";
                      const isExpanded = expandedDasha === d.lord;
                      const hasAntar = d.antardashas && d.antardashas.length > 0;
                      return (
                        <motion.div key={i} initial={{ opacity: 0, x: -4 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.03 * i }}>
                          <div
                            onClick={() => hasAntar && setExpandedDasha(isExpanded ? null : d.lord)}
                            className={`rounded-lg p-3 border cursor-pointer ${cur
                              ? "bg-amber-500/10 border-amber-500/30"
                              : "bg-muted/10 border-border/20"} ${hasAntar ? "hover:bg-muted/20" : ""}`}>
                            <div className="flex items-center gap-3">
                              <div className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: color }} />
                              <div className="flex-1">
                                <div className="flex items-center gap-2 flex-wrap">
                                  <span className="text-sm font-medium text-foreground">{d.lord}</span>
                                  <span className="text-xs text-muted-foreground">({GRAHA_EN[d.lord] ?? d.lord})</span>
                                  {cur && <span className="text-[10px] bg-amber-500/20 text-amber-400 px-1.5 py-0.5 rounded border border-amber-500/30 font-medium">Active Now ★</span>}
                                </div>
                                <p className="text-xs text-muted-foreground mt-0.5">
                                  {formatDate(d.start)} → {formatDate(d.end)} · {d.years} years
                                </p>
                              </div>
                              <div className="flex items-center gap-2 shrink-0">
                                <div className="hidden sm:block w-20 h-1.5 bg-muted/30 rounded-full overflow-hidden">
                                  <div className="h-full rounded-full" style={{ background: color, width: `${(d.years / 20) * 100}%` }} />
                                </div>
                                {hasAntar && (
                                  isExpanded
                                    ? <ChevronUp className="h-3.5 w-3.5 text-muted-foreground" />
                                    : <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
                                )}
                              </div>
                            </div>
                          </div>
                          {/* Antardasha expansion */}
                          <AnimatePresence>
                            {isExpanded && d.antardashas && (
                              <motion.div
                                initial={{ opacity: 0, height: 0 }}
                                animate={{ opacity: 1, height: "auto" }}
                                exit={{ opacity: 0, height: 0 }}
                                className="overflow-hidden"
                              >
                                <AntardashaList antardashas={d.antardashas} />
                              </motion.div>
                            )}
                          </AnimatePresence>
                        </motion.div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see dasha sequence</p>
                )}
              </motion.div>
            )}

            {/* Bhava (House) table — enhanced with significations */}
            {activeTab === "houses" && (
              <motion.div key="houses" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Home className="h-3.5 w-3.5 text-primary" /> Bhava Chart — Whole Sign Houses
                </p>
                {(kundaliData?.houses ?? []).length > 0 ? (
                  <div className="space-y-1.5">
                    {(kundaliData?.houses ?? []).map((h) => {
                      const planetsIn = h.planets ?? GRAHA_ORDER.filter(gn => kundaliData?.grahas[gn]?.house === h.house);
                      return (
                        <div key={h.house} className="flex items-start gap-3 px-2 py-2.5 rounded-lg hover:bg-muted/20 transition-colors">
                          <div className="w-8 h-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0 mt-0.5">
                            <span className="text-xs font-bold text-primary">{h.house}</span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="text-sm text-foreground font-medium">
                                {h.rashi} {RASHI_SYMBOLS[h.rashi] ?? ""}
                                {h.rashi_en && <span className="text-xs text-muted-foreground/50 ml-1">({h.rashi_en})</span>}
                              </span>
                              <span className="text-xs text-muted-foreground">Lord: {h.lord} {h.lord_en ? `(${h.lord_en})` : ""}</span>
                            </div>
                            {h.signification && (
                              <p className="text-[10px] text-muted-foreground/60 mt-0.5">{h.signification}</p>
                            )}
                            {planetsIn.length > 0 && (
                              <div className="flex items-center gap-1 mt-1 flex-wrap">
                                {planetsIn.map(gn => (
                                  <span key={gn} className="text-[10px] px-1.5 py-0.5 rounded" style={{ color: GRAHA_COLOR[gn], background: `${GRAHA_COLOR[gn]}22` }}>
                                    {GRAHA_SYMBOLS[gn]} {gn}
                                  </span>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see house chart</p>
                )}
              </motion.div>
            )}

            {/* Bhav Chalit Chart */}
            {activeTab === "chalit" && (
              <motion.div key="chalit" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Star className="h-3.5 w-3.5 text-primary" /> Bhav Chalit — Placidus House Cusps (Sidereal)
                </p>
                {kundaliData?.bhav_chalit ? (() => {
                  const bc = kundaliData.bhav_chalit!;
                  return (
                    <div className="space-y-3">
                      {/* House cusps table */}
                      <div>
                        <p className="text-[10px] text-muted-foreground uppercase tracking-wide mb-2">12 House Cusps</p>
                        <div className="grid grid-cols-2 gap-1.5">
                          {bc.cusps_sid.map((cusp, i) => {
                            const rashiIdx = Math.floor(cusp / 30);
                            const deg = (cusp % 30).toFixed(2);
                            const rashiNames = ["Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya","Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"];
                            const rashi = rashiNames[rashiIdx] ?? "?";
                            return (
                              <div key={i} className="flex items-center gap-2 px-2 py-1.5 rounded-lg bg-muted/20 text-xs">
                                <span className="text-primary font-bold w-5 text-center">{i + 1}</span>
                                <span className="text-foreground">{rashi}</span>
                                <span className="text-muted-foreground ml-auto">{deg}°</span>
                                <span className="text-muted-foreground/50">{RASHI_SYMBOLS[rashi] ?? ""}</span>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                      {/* Planet bhav chalit placements */}
                      <div>
                        <p className="text-[10px] text-muted-foreground uppercase tracking-wide mb-2">Planet Placements (Bhav Chalit)</p>
                        <div className="space-y-1">
                          {GRAHA_ORDER.map(gn => {
                            const chHouse = bc.planets[gn];
                            const rashiHouse = kundaliData.grahas[gn]?.house;
                            if (chHouse === undefined) return null;
                            const shifted = chHouse !== rashiHouse;
                            return (
                              <div key={gn} className={`flex items-center gap-2 px-2 py-1.5 rounded-lg text-xs ${shifted ? "bg-amber-500/10 border border-amber-500/20" : "bg-muted/10"}`}>
                                <span className="text-base w-6 text-center" style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                <span className="text-foreground flex-1">{gn} <span className="text-muted-foreground/50">({GRAHA_EN[gn]})</span></span>
                                <span className="text-muted-foreground">Rashi: H{rashiHouse}</span>
                                <span className={`font-semibold ${shifted ? "text-amber-400" : "text-foreground"}`}>Chalit: H{chHouse}</span>
                                {shifted && <span className="text-[10px] text-amber-400">shifted</span>}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">
                    {kundaliData ? "Bhav Chalit not available" : "Generate Kundali to see Bhav Chalit"}
                  </p>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* RIGHT panel */}
        <div className="space-y-3">

          {/* Planet list */}
          <div className="cosmic-card rounded-xl p-3 md:p-4">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">
              Planet Positions (D-1)
            </h3>
            <div className="space-y-1">
              {kundaliData
                ? GRAHA_ORDER.map(gn => {
                    const g = kundaliData.grahas[gn];
                    if (!g) return null;
                    const pd = toPlanetData(gn, g);
                    return (
                      <PlanetRow key={gn} gname={gn} g={g}
                        onClick={() => setSelectedPlanet(prev => prev?.name === pd.name ? null : pd)}
                        isSelected={selectedPlanet?.name === pd.name}
                      />
                    );
                  })
                : samplePlanets.map(p => (
                    <button key={p.name} onClick={() => setSelectedPlanet(p)}
                      className="w-full flex items-center gap-3 p-2 rounded-lg hover:bg-muted/30 transition-colors text-left">
                      <span className="text-lg" style={{ color: p.color }}>{p.symbol}</span>
                      <div className="flex-1">
                        <span className="text-sm text-foreground">{p.name}</span>
                        <span className="text-xs text-muted-foreground ml-2">{p.rashi} · H{p.house}</span>
                      </div>
                      <span className="text-xs text-muted-foreground">{p.degree}</span>
                    </button>
                  ))
              }
            </div>
          </div>

          {/* Selected planet detail — enhanced */}
          <AnimatePresence>
            {selectedPlanet && kundaliData && (
              <motion.div initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 4 }}
                className="cosmic-card rounded-xl p-3 md:p-4">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-2xl" style={{ color: selectedPlanet.color }}>{selectedPlanet.symbol}</span>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{selectedPlanet.name}</p>
                      <p className="text-[10px] text-muted-foreground">{selectedPlanet.sanskritName} {kundaliData.grahas[selectedPlanet.sanskritName ?? ""]?.graha_hindi ?? ""}</p>
                    </div>
                  </div>
                  <button onClick={() => setSelectedPlanet(null)} className="text-muted-foreground hover:text-foreground text-xs">✕</button>
                </div>
                {(() => {
                  const gn = selectedPlanet.sanskritName ?? "";
                  const g = kundaliData.grahas[gn];
                  if (!g) return null;
                  const sb = STATUS_BADGE[g.status] ?? STATUS_BADGE["Normal"];
                  const rb = RELATIONSHIP_BADGE[g.relationship ?? "Neutral"] ?? RELATIONSHIP_BADGE["Neutral"];
                  return (
                    <div className="space-y-2 text-xs">
                      <div className="grid grid-cols-2 gap-2">
                        {[
                          ["Rashi", `${g.rashi} ${g.rashi_en ? `(${g.rashi_en})` : ""}`],
                          ["House", `H${g.house}`],
                          ["Degree", `${g.degree.toFixed(2)}°${g.longitude ? ` (${g.longitude.toFixed(2)}°)` : ""}`],
                          ["Nakshatra", `${g.nakshatra} ${g.nakshatra_hindi ?? ""}`],
                          ["Pada", `${g.pada}`],
                          ["Nak. Lord", g.nakshatra_lord ?? "—"],
                          ["Retrograde", g.retro ? "Yes ℞" : "No"],
                          ["Speed", g.speed ? `${g.speed.toFixed(4)}°/day` : "—"],
                        ].map(([k, v]) => (
                          <div key={k} className="bg-muted/20 rounded-lg p-2">
                            <p className="text-[10px] text-muted-foreground uppercase tracking-wide">{k}</p>
                            <p className="text-foreground font-medium mt-0.5">{v}</p>
                          </div>
                        ))}
                      </div>
                      <div className="grid grid-cols-2 gap-2">
                        <div className={`rounded-lg p-2 border ${sb.cls} text-center`}>
                          <p className="text-[10px] uppercase tracking-wide opacity-70">Dignity</p>
                          <p className="font-semibold text-xs">{g.status}</p>
                        </div>
                        <div className={`rounded-lg p-2 text-center ${rb.cls}`}>
                          <p className="text-[10px] uppercase tracking-wide opacity-70">Relationship</p>
                          <p className="font-semibold text-xs">{rb.label}</p>
                        </div>
                      </div>
                      {g.karaka && (
                        <div className="bg-primary/5 rounded-lg p-2 border border-primary/10">
                          <p className="text-[10px] text-muted-foreground uppercase tracking-wide">Karaka (Significator)</p>
                          <p className="text-foreground text-xs mt-0.5">{g.karaka}</p>
                        </div>
                      )}
                      {g.ruler_of && g.ruler_of.length > 0 && (
                        <div className="bg-muted/20 rounded-lg p-2">
                          <p className="text-[10px] text-muted-foreground uppercase tracking-wide">Rules Houses</p>
                          <p className="text-foreground text-xs mt-0.5">{g.ruler_of.map(h => `H${h}`).join(", ")}</p>
                        </div>
                      )}
                      {g.combust && (
                        <div className="bg-orange-500/10 rounded-lg p-2 border border-orange-500/20">
                          <p className="text-[10px] text-orange-400 uppercase tracking-wide">Combust (Asta)</p>
                          <p className="text-orange-300 text-xs mt-0.5">Planet is too close to Sun — weakened</p>
                        </div>
                      )}
                      {g.lat_ecl !== undefined && (
                        <div className="bg-muted/20 rounded-lg p-2">
                          <p className="text-[10px] text-muted-foreground uppercase tracking-wide">Ecliptic Latitude</p>
                          <p className="text-foreground text-xs mt-0.5">{g.lat_ecl.toFixed(4)}°</p>
                        </div>
                      )}
                    </div>
                  );
                })()}
              </motion.div>
            )}
          </AnimatePresence>

          {/* Yogas */}
          <div className="cosmic-card rounded-xl p-3 md:p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Zap className="h-3.5 w-3.5 text-primary" />
                <h3 className="text-xs font-semibold text-foreground">Yogas ({yogas.filter(y => y.present !== false).length} active)</h3>
              </div>
              <Link to="/yogas" className="text-xs text-primary hover:underline">All Yogas →</Link>
            </div>
            {yogas.length > 0 ? (
              <>
                <div className="space-y-1.5">
                  {visibleYogas.map((y) => {
                    const isPresent = y.present !== false;
                    const strengthCls = !isPresent ? "text-muted-foreground/40 border-border/10 bg-muted/5"
                      : y.strength === "Very Strong" ? "text-emerald-400 border-emerald-500/30 bg-emerald-500/10"
                      : y.strength === "Strong" ? "text-amber-400 border-amber-500/30 bg-amber-500/10"
                      : "text-muted-foreground border-border/20 bg-muted/10";
                    const catColor = YOGA_CATEGORY_COLORS[y.category ?? ""] ?? "text-muted-foreground";
                    return (
                      <div key={y.name} className={`rounded-lg p-2.5 ${isPresent ? "bg-muted/20" : "bg-muted/5 opacity-60"}`}>
                        <div className="flex items-center justify-between mb-0.5">
                          <div className="flex items-center gap-2">
                            <span className={`text-xs font-bold ${isPresent ? "text-emerald-400" : "text-red-400/50"}`}>{isPresent ? "✓" : "✗"}</span>
                            <p className={`text-xs font-medium ${isPresent ? "text-foreground" : "text-muted-foreground/50"}`}>{y.name}</p>
                            {y.category && <span className={`text-[10px] ${catColor}`}>{y.category}</span>}
                          </div>
                          {isPresent && <span className={`text-[10px] px-1.5 py-0.5 rounded border ${strengthCls}`}>{y.strength}</span>}
                        </div>
                        {isPresent && <p className="text-xs text-muted-foreground">{y.desc}</p>}
                      </div>
                    );
                  })}
                </div>
                {yogas.length > 4 && (
                  <button onClick={() => setShowAllYogas(v => !v)}
                    className="mt-2 w-full flex items-center justify-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors">
                    {showAllYogas ? <><ChevronUp className="h-3 w-3" /> Show Less</> : <><ChevronDown className="h-3 w-3" /> Show {yogas.length - 4} More ({yogas.filter(y => y.present !== false).length} active)</>}
                  </button>
                )}
              </>
            ) : (
              <p className="text-xs text-muted-foreground text-center py-4">
                {kundaliData ? "No major yogas detected" : "Generate Kundali to see yogas"}
              </p>
            )}
          </div>

          {/* Lagna details card */}
          {kundaliData && (
            <div className="cosmic-card rounded-xl p-3 md:p-4">
              <div className="flex items-center gap-2 mb-3">
                <Star className="h-3.5 w-3.5 text-primary" />
                <h3 className="text-xs font-semibold text-foreground">Lagna Details</h3>
              </div>
              <div className="space-y-2 text-xs">
                {[
                  ["Lagna Rashi", `${kundaliData.lagna.rashi} ${kundaliData.lagna.rashi_en ? `(${kundaliData.lagna.rashi_en})` : ""}`],
                  ["Nakshatra", `${kundaliData.lagna.nakshatra} ${kundaliData.lagna.nakshatra_hindi ?? ""} · Pada ${kundaliData.lagna.pada ?? "—"}`],
                  ["Degree", `${kundaliData.lagna.degree}°${kundaliData.lagna.full_degree ? ` (${kundaliData.lagna.full_degree.toFixed(2)}° abs)` : ""}`],
                  ["Navamsha Lagna", kundaliData.navamsha_lagna?.rashi ?? "—"],
                  ["Ayanamsha", `${kundaliData.ayanamsha}° (${kundaliData.ayanamsha_label ?? "Lahiri"})`],
                  ["Rahu/Ketu", kundaliData.rahu_mode === "true" ? "True Node" : "Mean Node"],
                ].map(([k, v]) => (
                  <div key={k} className="flex justify-between items-center py-1 border-b border-border/10 last:border-0">
                    <span className="text-muted-foreground">{k}</span>
                    <span className="font-medium text-foreground text-right">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Birth Panchang */}
          {kundaliData?.birth_panchang && (
            <div className="cosmic-card rounded-xl p-3 md:p-4">
              <div className="flex items-center gap-2 mb-3">
                <Calendar className="h-3.5 w-3.5 text-primary" />
                <h3 className="text-xs font-semibold text-foreground">Birth Panchang</h3>
              </div>
              <div className="space-y-1.5 text-xs">
                {[
                  ["Vara (Weekday)", kundaliData.birth_panchang.vara],
                  ["Tithi", `${kundaliData.birth_panchang.tithi_num} — ${kundaliData.birth_panchang.tithi} (${kundaliData.birth_panchang.paksha})`],
                  ["27 Yoga", kundaliData.birth_panchang.yoga],
                  ["Karana", kundaliData.birth_panchang.karana],
                  ["Sunrise", kundaliData.birth_panchang.sunrise],
                  ["Sunset", kundaliData.birth_panchang.sunset],
                ].map(([k, v]) => (
                  <div key={k} className="flex justify-between items-center py-1 border-b border-border/10 last:border-0">
                    <span className="text-muted-foreground">{k}</span>
                    <span className="font-medium text-foreground text-right">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Personal Characteristics */}
          {kundaliData?.personal && (
            <div className="cosmic-card rounded-xl p-3 md:p-4">
              <div className="flex items-center gap-2 mb-3">
                <User className="h-3.5 w-3.5 text-primary" />
                <h3 className="text-xs font-semibold text-foreground">Personal Characteristics</h3>
              </div>
              <div className="grid grid-cols-2 gap-1.5 text-xs">
                {[
                  ["Nadi", kundaliData.personal.nadi],
                  ["Gana", kundaliData.personal.gana],
                  ["Yoni", kundaliData.personal.yoni],
                  ["Varna", kundaliData.personal.varna],
                  ["Tattva", kundaliData.personal.tattva],
                  ["Vashya", kundaliData.personal.vashya],
                  ["Nak. Paya", kundaliData.personal.nakshatra_paya],
                  ["Rashi Paya", kundaliData.personal.rashi_paya],
                  ["Yunja", kundaliData.personal.yunja],
                ].map(([k, v]) => (
                  <div key={k} className="bg-muted/20 rounded-lg p-2">
                    <p className="text-[10px] text-muted-foreground uppercase tracking-wide">{k}</p>
                    <p className="font-medium text-foreground mt-0.5">{v}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Upagrahas */}
          {kundaliData?.upagraha && Object.keys(kundaliData.upagraha).length > 0 && (
            <div className="cosmic-card rounded-xl p-3 md:p-4">
              <div className="flex items-center gap-2 mb-3">
                <Star className="h-3.5 w-3.5 text-primary" />
                <h3 className="text-xs font-semibold text-foreground">Upagrahas (Sub-Planets)</h3>
              </div>
              <div className="space-y-1 text-xs">
                {Object.entries(kundaliData.upagraha).map(([name, u]: [string, UpagrahaEntry]) => (
                  <div key={name} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-muted/20">
                    <span className="font-medium text-foreground w-24 shrink-0">{name}</span>
                    <span className="text-muted-foreground">{u.rashi_name}</span>
                    <span className="text-muted-foreground/60">{u.dms}</span>
                    <span className="text-muted-foreground/50 text-[10px] ml-auto">{u.nakshatra}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <p className="text-[10px] text-muted-foreground/40 leading-relaxed pb-2">
            Whole Sign Houses · {kundaliData?.ayanamsha_label ?? "Lahiri"} · {kundaliData?.rahu_mode === "true" ? "True" : "Mean"} Rahu/Ketu · pyswisseph DE431 · For guidance consult a qualified Jyotishi.
          </p>
        </div>
      </div>

        </>
      )}

      <PageBot pageContext="kundali" pageData={kundaliData ?? {}} />
    </div>
  );
}
