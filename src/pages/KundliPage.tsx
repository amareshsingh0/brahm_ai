import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { KundliChart } from "@/components/charts/KundliChart";
import { samplePlanets, useKundliStore, type PlanetData } from "@/store/kundliStore";
import { Link } from "react-router-dom";
import { Zap, FileDown, ChevronDown, ChevronUp, Star, Calendar, Info } from "lucide-react";
import PageBot from "@/components/PageBot";
import type { KundaliResponse } from "@/types/api";

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
const DASHA_COLORS: Record<string, string> = {
  Ketu:"#f97316", Shukra:"#a855f7", Surya:"#f59e0b", Chandra:"#94a3b8",
  Mangal:"#ef4444", Rahu:"#6366f1", Guru:"#eab308", Shani:"#64748b", Budh:"#22c55e",
};

// ── Helper ────────────────────────────────────────────────────────────────────
function isCurrentDasha(start: string, end: string): boolean {
  const now = new Date();
  return new Date(start) <= now && now <= new Date(end);
}

function formatDate(d: string): string {
  try {
    return new Date(d).toLocaleDateString("en-IN", { day:"2-digit", month:"short", year:"numeric" });
  } catch { return d; }
}

// ── PDF Generator ─────────────────────────────────────────────────────────────
function generateKundaliPDF(data: KundaliResponse) {
  const graha_order = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];

  const planetRows = graha_order.map(gn => {
    const g = data.grahas[gn];
    if (!g) return "";
    const retro = g.retro ? " ℞" : "";
    const statusColor = g.status.includes("Exalted") ? "#10b981"
      : g.status.includes("Debilitated") ? "#ef4444"
      : g.status.includes("Own") ? "#f59e0b" : "#6b7280";
    return `<tr>
      <td>${GRAHA_SYMBOLS[gn] ?? ""} ${gn} / ${GRAHA_EN[gn]}</td>
      <td>${g.rashi} ${RASHI_SYMBOLS[g.rashi] ?? ""}</td>
      <td style="text-align:center">H${g.house}</td>
      <td style="text-align:center">${g.degree.toFixed(2)}°</td>
      <td>${g.nakshatra} (${g.nakshatra_lord ?? ""})</td>
      <td style="text-align:center">${g.pada}${retro}</td>
      <td style="color:${statusColor};font-weight:600">${g.status.split(" ")[0]}</td>
    </tr>`;
  }).join("");

  const navRows = graha_order.map(gn => {
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
    </tr>`;
  }).join("");

  const dashaRows = data.dashas.map(d => {
    const cur = isCurrentDasha(d.start, d.end);
    return `<tr style="${cur ? "background:#fffbeb;font-weight:600" : ""}">
      <td>${cur ? "★ " : ""}${d.lord} (${GRAHA_EN[d.lord] ?? d.lord})</td>
      <td>${formatDate(d.start)}</td>
      <td>${formatDate(d.end)}</td>
      <td style="text-align:center">${d.years} yrs</td>
      <td style="color:${cur ? "#b45309" : "#6b7280"}">${cur ? "Active Now" : ""}</td>
    </tr>`;
  }).join("");

  const yogaRows = data.yogas.map(y => {
    const strengthColor = y.strength === "Very Strong" ? "#10b981" : y.strength === "Strong" ? "#f59e0b" : "#6b7280";
    return `<tr>
      <td style="font-weight:600">${y.name}</td>
      <td style="color:${strengthColor}">${y.strength}</td>
      <td style="font-size:11px">${y.desc}</td>
    </tr>`;
  }).join("");

  const houseRows = data.houses.map(h => `<tr>
    <td style="text-align:center">H${h.house}</td>
    <td>${h.rashi} ${RASHI_SYMBOLS[h.rashi] ?? ""}</td>
    <td>${h.lord}</td>
  </tr>`).join("");

  const html = `<!DOCTYPE html><html><head><meta charset="UTF-8">
<title>Janam Kundali — ${data.name || "Birth Chart"}</title>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family: Georgia, 'Times New Roman', serif; background:#fff; color:#1a1a2e; font-size:12px; line-height:1.5; }
  .page { width:210mm; min-height:297mm; margin:0 auto; padding:14mm 14mm; }
  h1 { font-size:24px; color:#7c3aed; text-align:center; }
  h2 { font-size:14px; color:#6d28d9; border-bottom:2px solid #ede9fe; padding-bottom:3px; margin:16px 0 8px; }
  .header-bar { background:linear-gradient(135deg,#1e1b4b,#312e81); color:#fff; padding:18px 22px; border-radius:10px; text-align:center; margin-bottom:16px; }
  .header-bar h1 { color:#fbbf24; font-size:22px; margin-bottom:4px; }
  .header-bar .sub { color:#c4b5fd; font-size:11px; }
  .meta-row { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; margin-bottom:14px; }
  .meta-card { border:1px solid #e5e7eb; border-radius:8px; padding:10px 12px; }
  .meta-label { font-size:10px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; }
  .meta-val { font-size:13px; font-weight:600; color:#1a1a2e; margin-top:2px; }
  table { width:100%; border-collapse:collapse; font-size:11px; margin-bottom:6px; }
  th { background:#ede9fe; color:#4c1d95; padding:5px 7px; text-align:left; font-size:10px; font-weight:600; }
  td { padding:4px 7px; border-bottom:1px solid #f3f4f6; vertical-align:top; }
  tr:nth-child(even) td { background:#fafafa; }
  .lagna-row { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; margin:10px 0 14px; }
  .lagna-card { border:2px solid #fbbf24; border-radius:8px; padding:10px; text-align:center; }
  .lagna-main { font-size:20px; font-weight:bold; color:#b45309; }
  .lagna-sub { font-size:10px; color:#6b7280; margin-top:2px; }
  .two-col { display:grid; grid-template-columns:1fr 1fr; gap:12px; }
  .footer { margin-top:16px; padding-top:10px; border-top:1px solid #e5e7eb; font-size:9px; color:#9ca3af; text-align:center; }
  @media print { body { -webkit-print-color-adjust:exact; print-color-adjust:exact; } .page { padding:10mm; } }
</style>
</head><body><div class="page">

<div class="header-bar">
  <h1>☽ Janam Kundali</h1>
  <div class="sub">Vedic Birth Chart · Lahiri Ayanamsha · Whole Sign Houses</div>
  <div style="margin-top:8px;font-size:14px;color:#fbbf24;font-weight:bold">${data.name || "—"}</div>
  <div style="font-size:11px;color:#c4b5fd;margin-top:2px">${data.place || ""} · ${data.birth_date}</div>
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
    <div class="meta-label">Ayanamsha (Lahiri)</div>
    <div class="meta-val">${data.ayanamsha}°</div>
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
    <div class="lagna-sub">${data.grahas["Chandra"]?.nakshatra ?? ""} · Pada ${data.grahas["Chandra"]?.pada ?? ""}</div>
  </div>
  <div class="lagna-card" style="border-color:#f59e0b">
    <div class="lagna-sub">Sun (Surya)</div>
    <div class="lagna-main" style="color:#b45309">${data.grahas["Surya"]?.rashi ?? ""}</div>
    <div class="lagna-sub">${data.grahas["Surya"]?.nakshatra ?? ""} · ${data.grahas["Surya"]?.status?.split(" ")[0] ?? ""}</div>
  </div>
</div>

<h2>All Planet Positions (D-1 Rashi Chart)</h2>
<table>
  <thead><tr>
    <th>Planet</th><th>Rashi</th><th style="text-align:center">House</th>
    <th style="text-align:center">Degree</th><th>Nakshatra (Lord)</th>
    <th style="text-align:center">Pada / ℞</th><th>Status</th>
  </tr></thead>
  <tbody>${planetRows}</tbody>
</table>

<div class="two-col">
  <div>
    <h2>House Chart (Bhava)</h2>
    <table>
      <thead><tr><th style="text-align:center">House</th><th>Rashi</th><th>Lord</th></tr></thead>
      <tbody>${houseRows}</tbody>
    </table>
  </div>
  <div>
    <h2>Navamsha Chart (D-9)</h2>
    <table>
      <thead><tr><th>Planet</th><th>Rashi</th><th style="text-align:center">House</th><th>Status</th></tr></thead>
      <tbody>${navRows || "<tr><td colspan='4' style='color:#9ca3af'>Not available</td></tr>"}</tbody>
    </table>
  </div>
</div>

<h2>Vimshottari Dasha Sequence</h2>
<table>
  <thead><tr><th>Mahadasha Lord</th><th>Start</th><th>End</th><th style="text-align:center">Duration</th><th>Status</th></tr></thead>
  <tbody>${dashaRows}</tbody>
</table>

<h2>Active Yogas</h2>
<table>
  <thead><tr><th>Yoga Name</th><th>Strength</th><th>Description</th></tr></thead>
  <tbody>${yogaRows || "<tr><td colspan='3' style='color:#9ca3af'>No yogas detected</td></tr>"}</tbody>
</table>

<div class="footer">
  Generated by Brahm AI · Lahiri Ayanamsha · Whole Sign Houses · pyswisseph DE431 ·
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

// ── PlanetRow (collapsible detail row) ────────────────────────────────────────
function PlanetRow({ gname, g, onClick, isSelected }: {
  gname: string;
  g: NonNullable<KundaliResponse["grahas"][string]>;
  onClick: () => void;
  isSelected: boolean;
}) {
  const sb = STATUS_BADGE[g.status] ?? STATUS_BADGE["Normal"];
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
          <span className="text-[10px] text-muted-foreground/60">({gname})</span>
          {g.retro && <span className="text-[9px] text-amber-400 border border-amber-500/30 px-1 rounded">℞</span>}
        </div>
        <div className="text-[11px] text-muted-foreground mt-0.5">
          {g.rashi} · H{g.house} · {g.nakshatra} P{g.pada}
        </div>
      </div>
      <div className="flex flex-col items-end gap-1 shrink-0">
        <span className="text-[10px] text-muted-foreground">{g.degree.toFixed(1)}°</span>
        <span className={`text-[9px] px-1.5 py-0.5 rounded border ${sb.cls}`}>{sb.label}</span>
      </div>
    </motion.button>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function KundliPage() {
  const kundaliData = useKundliStore((s) => s.kundaliData);
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const [selectedPlanet, setSelectedPlanet] = useState<PlanetData | null>(null);
  const [activeTab, setActiveTab] = useState<"janam" | "navamsha" | "dasha" | "houses">("janam");
  const [showAllYogas, setShowAllYogas] = useState(false);

  const graha_order = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];

  // Map API kundaliData to PlanetData[] for chart display
  const planets: PlanetData[] = kundaliData
    ? graha_order.map((gname) => {
        const g = kundaliData.grahas[gname];
        if (!g) return null;
        return {
          name: GRAHA_EN[gname] ?? gname,
          symbol: GRAHA_SYMBOLS[gname] ?? "?",
          rashi: g.rashi,
          house: g.house,
          degree: `${g.degree.toFixed(1)}°`,
          nakshatra: g.nakshatra,
          color: GRAHA_COLOR[gname] ?? "hsl(0 0% 60%)",
          sanskritName: gname,
        } as PlanetData;
      }).filter(Boolean) as PlanetData[]
    : samplePlanets;

  // Navamsha planets for chart
  const navPlanets: PlanetData[] = kundaliData?.navamsha
    ? graha_order.map((gname) => {
        const g = kundaliData.navamsha![gname];
        if (!g) return null;
        return {
          name: GRAHA_EN[gname] ?? gname,
          symbol: GRAHA_SYMBOLS[gname] ?? "?",
          rashi: g.rashi,
          house: g.house,
          degree: "0°",
          nakshatra: "",
          color: GRAHA_COLOR[gname] ?? "hsl(0 0% 60%)",
          sanskritName: gname,
        } as PlanetData;
      }).filter(Boolean) as PlanetData[]
    : [];

  const lagnaRashi = kundaliData?.lagna.rashi ?? "Tula";
  const navLagnaRashi = kundaliData?.navamsha_lagna?.rashi ?? lagnaRashi;

  const yogas = kundaliData?.yogas ?? [];
  const visibleYogas = showAllYogas ? yogas : yogas.slice(0, 4);

  const currentDasha = kundaliData?.dashas.find(d => isCurrentDasha(d.start, d.end));

  return (
    <div className="p-4 md:p-6 space-y-5">
      {/* Header */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <h1 className="font-display text-2xl text-foreground text-glow-gold mb-0.5">
            {kundaliData?.name ? `${kundaliData.name}'s Kundali` : "My Kundli"}
          </h1>
          {kundaliData && (
            <p className="text-xs text-muted-foreground">
              {kundaliData.place} · {kundaliData.birth_date} · Lahiri Ayanamsha {kundaliData.ayanamsha}°
            </p>
          )}
        </div>
        {kundaliData && (
          <button
            onClick={() => generateKundaliPDF(kundaliData)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-primary/20 hover:bg-primary/30 text-primary transition-colors border border-primary/20"
          >
            <FileDown className="h-3.5 w-3.5" /> Download PDF
          </button>
        )}
      </motion.div>

      {/* Key positions strip */}
      {kundaliData && (
        <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
          className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {[
            { label: "Lagna", value: kundaliData.lagna.rashi, sub: `${kundaliData.lagna.nakshatra} P${kundaliData.lagna.pada ?? ""}`, color: "text-primary" },
            { label: "Moon (Chandra)", value: kundaliData.grahas["Chandra"]?.rashi ?? "—", sub: kundaliData.grahas["Chandra"]?.nakshatra ?? "", color: "text-blue-400" },
            { label: "Sun (Surya)", value: kundaliData.grahas["Surya"]?.rashi ?? "—", sub: kundaliData.grahas["Surya"]?.status?.split(" ")[0] ?? "", color: "text-amber-400" },
            { label: "Jupiter (Guru)", value: kundaliData.grahas["Guru"]?.rashi ?? "—", sub: `H${kundaliData.grahas["Guru"]?.house}`, color: "text-yellow-400" },
          ].map(item => (
            <div key={item.label} className="cosmic-card rounded-xl p-3">
              <p className="text-[10px] text-muted-foreground uppercase tracking-wide">{item.label}</p>
              <p className={`text-sm font-semibold ${item.color} mt-0.5`}>{item.value}</p>
              <p className="text-[10px] text-muted-foreground/70">{item.sub}</p>
            </div>
          ))}
        </motion.div>
      )}

      {/* Current Mahadasha strip */}
      {currentDasha && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.15 }}
          className="cosmic-card rounded-xl p-3 flex items-center gap-3">
          <div className="w-2 h-2 rounded-full animate-pulse" style={{ background: DASHA_COLORS[currentDasha.lord] ?? "#888" }} />
          <div>
            <span className="text-[10px] text-muted-foreground uppercase tracking-wide">Current Mahadasha · </span>
            <span className="text-sm font-semibold text-foreground">{currentDasha.lord} ({GRAHA_EN[currentDasha.lord]})</span>
            <span className="text-[11px] text-muted-foreground ml-2">{formatDate(currentDasha.start)} → {formatDate(currentDasha.end)}</span>
          </div>
        </motion.div>
      )}

      {/* Main grid */}
      <div className="grid lg:grid-cols-[1fr_380px] gap-5">

        {/* LEFT: Chart + Tabs */}
        <div className="cosmic-card rounded-xl p-4">
          {/* Tab bar */}
          <div className="flex gap-1 mb-4 border-b border-border/30 pb-3 flex-wrap">
            {[
              { id:"janam",    label:"D-1 Rashi" },
              { id:"navamsha", label:"D-9 Navamsha" },
              { id:"dasha",    label:"Dashas" },
              { id:"houses",   label:"Bhavas" },
            ].map(tab => (
              <button key={tab.id}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
                className={`text-xs px-3 py-1.5 rounded-full transition-colors ${
                  activeTab === tab.id ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <AnimatePresence mode="wait">
            {/* Janam (D-1) chart */}
            {activeTab === "janam" && (
              <motion.div key="janam" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <KundliChart
                  onPlanetClick={setSelectedPlanet}
                  selectedPlanet={selectedPlanet}
                  planets={planets}
                  lagnaRashi={lagnaRashi}
                />
                <p className="text-[10px] text-muted-foreground text-center mt-2">
                  Click on any planet symbol for details · Whole Sign Houses · Lahiri Ayanamsha
                </p>
              </motion.div>
            )}

            {/* Navamsha (D-9) chart */}
            {activeTab === "navamsha" && (
              <motion.div key="navamsha" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                {navPlanets.length > 0 ? (
                  <>
                    <div className="text-center mb-3">
                      <p className="text-xs font-medium text-foreground">Navamsha Chart (D-9)</p>
                      <p className="text-[10px] text-muted-foreground">Lagna: {navLagnaRashi} · Marriage, Dharma &amp; Soul</p>
                    </div>
                    <KundliChart
                      onPlanetClick={setSelectedPlanet}
                      selectedPlanet={selectedPlanet}
                      planets={navPlanets}
                      lagnaRashi={navLagnaRashi}
                    />
                    {/* Navamsha planet table */}
                    <div className="mt-4 space-y-1">
                      <p className="text-[10px] text-muted-foreground uppercase tracking-wide font-medium mb-2">D-9 Planet Positions</p>
                      {graha_order.map(gn => {
                        const g = kundaliData?.navamsha?.[gn];
                        if (!g) return null;
                        const sb = STATUS_BADGE[g.status] ?? STATUS_BADGE["Normal"];
                        return (
                          <div key={gn} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-muted/20">
                            <span className="text-base w-6 text-center" style={{ color: GRAHA_COLOR[gn] }}>
                              {GRAHA_SYMBOLS[gn]}
                            </span>
                            <span className="text-xs text-foreground flex-1">{gn} <span className="text-muted-foreground/60 text-[10px]">({GRAHA_EN[gn]})</span></span>
                            <span className="text-[11px] text-muted-foreground">{g.rashi} · H{g.house}</span>
                            <span className={`text-[9px] px-1.5 py-0.5 rounded border ${sb.cls}`}>{sb.label}</span>
                          </div>
                        );
                      })}
                    </div>
                  </>
                ) : (
                  <div className="text-center py-12 text-muted-foreground text-sm">
                    Generate your Kundali to see D-9 Navamsha chart
                  </div>
                )}
              </motion.div>
            )}

            {/* Dasha table */}
            {activeTab === "dasha" && (
              <motion.div key="dasha" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Calendar className="h-3.5 w-3.5 text-primary" /> Vimshottari Dasha Sequence
                </p>
                {(kundaliData?.dashas ?? []).length > 0 ? (
                  <div className="space-y-2">
                    {(kundaliData?.dashas ?? []).map((d, i) => {
                      const cur = isCurrentDasha(d.start, d.end);
                      const color = DASHA_COLORS[d.lord] ?? "#888";
                      return (
                        <motion.div key={i} initial={{ opacity: 0, x: -4 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.03 * i }}
                          className={`rounded-lg p-3 border ${cur
                            ? "bg-amber-500/10 border-amber-500/30"
                            : "bg-muted/10 border-border/20"}`}>
                          <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: color }} />
                            <div className="flex-1">
                              <div className="flex items-center gap-2 flex-wrap">
                                <span className="text-sm font-medium text-foreground">{d.lord}</span>
                                <span className="text-[10px] text-muted-foreground">({GRAHA_EN[d.lord] ?? d.lord})</span>
                                {cur && <span className="text-[9px] bg-amber-500/20 text-amber-400 px-1.5 py-0.5 rounded border border-amber-500/30 font-medium">Active Now ★</span>}
                              </div>
                              <p className="text-[10px] text-muted-foreground mt-0.5">
                                {formatDate(d.start)} → {formatDate(d.end)} · {d.years} years
                              </p>
                            </div>
                            {/* Duration bar */}
                            <div className="hidden sm:block w-20 h-1.5 bg-muted/30 rounded-full overflow-hidden shrink-0">
                              <div className="h-full rounded-full" style={{ background: color, width: `${(d.years / 20) * 100}%` }} />
                            </div>
                          </div>
                        </motion.div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see dasha sequence</p>
                )}
              </motion.div>
            )}

            {/* Bhava (House) table */}
            {activeTab === "houses" && (
              <motion.div key="houses" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Info className="h-3.5 w-3.5 text-primary" /> Bhava Chart — Whole Sign Houses
                </p>
                {(kundaliData?.houses ?? []).length > 0 ? (
                  <div className="space-y-1.5">
                    {(kundaliData?.houses ?? []).map((h) => {
                      const planetsInHouse = graha_order.filter(gn => kundaliData?.grahas[gn]?.house === h.house);
                      return (
                        <div key={h.house} className="flex items-center gap-3 px-2 py-2 rounded-lg hover:bg-muted/20 transition-colors">
                          <div className="w-8 h-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0">
                            <span className="text-xs font-bold text-primary">{h.house}</span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="text-sm text-foreground font-medium">{h.rashi}</span>
                              <span className="text-[10px] text-muted-foreground">Lord: {h.lord}</span>
                            </div>
                            {planetsInHouse.length > 0 && (
                              <div className="flex items-center gap-1 mt-0.5 flex-wrap">
                                {planetsInHouse.map(gn => (
                                  <span key={gn} className="text-[9px] px-1 py-0.5 rounded" style={{ color: GRAHA_COLOR[gn], background: `${GRAHA_COLOR[gn]}22` }}>
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
          </AnimatePresence>
        </div>

        {/* RIGHT panel */}
        <div className="space-y-4">

          {/* Planet list */}
          <div className="cosmic-card rounded-xl p-4">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">
              Planet Positions (D-1)
            </h3>
            <div className="space-y-1">
              {kundaliData
                ? graha_order.map(gn => {
                    const g = kundaliData.grahas[gn];
                    if (!g) return null;
                    const pd: PlanetData = {
                      name: GRAHA_EN[gn] ?? gn,
                      symbol: GRAHA_SYMBOLS[gn] ?? "?",
                      rashi: g.rashi, house: g.house,
                      degree: `${g.degree.toFixed(1)}°`,
                      nakshatra: g.nakshatra,
                      color: GRAHA_COLOR[gn] ?? "hsl(0 0% 60%)",
                      sanskritName: gn,
                    };
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

          {/* Selected planet detail */}
          <AnimatePresence>
            {selectedPlanet && kundaliData && (
              <motion.div initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 4 }}
                className="cosmic-card rounded-xl p-4">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-2xl" style={{ color: selectedPlanet.color }}>{selectedPlanet.symbol}</span>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{selectedPlanet.name}</p>
                      <p className="text-[10px] text-muted-foreground">{selectedPlanet.sanskritName}</p>
                    </div>
                  </div>
                  <button onClick={() => setSelectedPlanet(null)} className="text-muted-foreground hover:text-foreground text-xs">✕</button>
                </div>
                {(() => {
                  const gn = selectedPlanet.sanskritName ?? "";
                  const g = kundaliData.grahas[gn];
                  if (!g) return null;
                  const sb = STATUS_BADGE[g.status] ?? STATUS_BADGE["Normal"];
                  return (
                    <div className="space-y-2 text-[11px]">
                      <div className="grid grid-cols-2 gap-2">
                        {[
                          ["Rashi", g.rashi], ["House", `H${g.house}`],
                          ["Degree", `${g.degree.toFixed(2)}°`], ["Nakshatra", g.nakshatra],
                          ["Pada", `${g.pada}`], ["Nak. Lord", g.nakshatra_lord ?? "—"],
                          ["Retrograde", g.retro ? "Yes ℞" : "No"], ["House Lord", g.house_lord ?? "—"],
                        ].map(([k, v]) => (
                          <div key={k} className="bg-muted/20 rounded-lg p-2">
                            <p className="text-[9px] text-muted-foreground uppercase tracking-wide">{k}</p>
                            <p className="text-foreground font-medium mt-0.5">{v}</p>
                          </div>
                        ))}
                      </div>
                      <div className={`rounded-lg p-2 border ${sb.cls} text-center`}>
                        <p className="text-[9px] uppercase tracking-wide opacity-70">Status</p>
                        <p className="font-semibold">{g.status}</p>
                      </div>
                    </div>
                  );
                })()}
              </motion.div>
            )}
          </AnimatePresence>

          {/* Yogas */}
          <div className="cosmic-card rounded-xl p-4">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Zap className="h-3.5 w-3.5 text-primary" />
                <h3 className="text-xs font-semibold text-foreground">Active Yogas</h3>
              </div>
              <Link to="/yogas" className="text-[10px] text-primary hover:underline">All Yogas →</Link>
            </div>
            {yogas.length > 0 ? (
              <>
                <div className="space-y-2">
                  {visibleYogas.map((y) => {
                    const strengthCls = y.strength === "Very Strong" ? "text-emerald-400 border-emerald-500/30 bg-emerald-500/10"
                      : y.strength === "Strong" ? "text-amber-400 border-amber-500/30 bg-amber-500/10"
                      : "text-muted-foreground border-border/20 bg-muted/10";
                    return (
                      <div key={y.name} className="bg-muted/20 rounded-lg p-2.5">
                        <div className="flex items-center justify-between mb-0.5">
                          <p className="text-xs font-medium text-foreground">{y.name}</p>
                          <span className={`text-[9px] px-1.5 py-0.5 rounded border ${strengthCls}`}>{y.strength}</span>
                        </div>
                        <p className="text-[10px] text-muted-foreground">{y.desc}</p>
                      </div>
                    );
                  })}
                </div>
                {yogas.length > 4 && (
                  <button onClick={() => setShowAllYogas(v => !v)}
                    className="mt-2 w-full flex items-center justify-center gap-1 text-[10px] text-muted-foreground hover:text-foreground transition-colors">
                    {showAllYogas ? <><ChevronUp className="h-3 w-3" /> Show Less</> : <><ChevronDown className="h-3 w-3" /> Show {yogas.length - 4} More</>}
                  </button>
                )}
              </>
            ) : (
              <p className="text-[11px] text-muted-foreground text-center py-4">
                {kundaliData ? "No major yogas detected" : "Generate Kundali to see yogas"}
              </p>
            )}
          </div>

          {/* Lagna details card */}
          {kundaliData && (
            <div className="cosmic-card rounded-xl p-4">
              <div className="flex items-center gap-2 mb-3">
                <Star className="h-3.5 w-3.5 text-primary" />
                <h3 className="text-xs font-semibold text-foreground">Lagna Details</h3>
              </div>
              <div className="space-y-2 text-[11px]">
                {[
                  ["Lagna Rashi", kundaliData.lagna.rashi],
                  ["Nakshatra", `${kundaliData.lagna.nakshatra} · Pada ${kundaliData.lagna.pada ?? "—"}`],
                  ["Degree", `${kundaliData.lagna.degree}°`],
                  ["Navamsha Lagna", kundaliData.navamsha_lagna?.rashi ?? "—"],
                  ["Ayanamsha", `${kundaliData.ayanamsha}° (Lahiri)`],
                ].map(([k, v]) => (
                  <div key={k} className="flex justify-between items-center py-1 border-b border-border/10 last:border-0">
                    <span className="text-muted-foreground">{k}</span>
                    <span className="font-medium text-foreground">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          <p className="text-[9px] text-muted-foreground/40 leading-relaxed pb-2">
            Whole Sign Houses · Lahiri Ayanamsha · pyswisseph DE431 · For complete guidance consult a qualified Jyotishi.
          </p>
        </div>
      </div>

      <PageBot pageContext="kundali" pageData={kundaliData ?? {}} />
    </div>
  );
}
