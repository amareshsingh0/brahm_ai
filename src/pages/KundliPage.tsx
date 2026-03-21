import React, { useState, useCallback, useEffect, useRef } from "react";
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
import type { KundaliResponse, VargaChartData, AntardashaData, UpagrahaEntry, ShadbalaPlanet, PratyantarData } from "@/types/api";

// ── Constants ──────────────────────────────────────────────────────────────────

// Rashi index (0=Mesha … 11=Meena) → planet lord
const RASHI_LORDS_IDX: Record<number, string> = {
  0:"Mangal",1:"Shukra",2:"Budh",3:"Chandra",4:"Surya",5:"Budh",
  6:"Shukra",7:"Mangal",8:"Guru",9:"Shani",10:"Shani",11:"Guru",
};
const RASHI_LIST_IDX = [
  "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
  "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena",
];
/** Houses ruled by `gn` given the lagna rashi name */
function getRulerOf(gn: string, lagnaRashi: string): number[] {
  const lagnaIdx = RASHI_LIST_IDX.indexOf(lagnaRashi);
  if (lagnaIdx < 0) return [];
  return Object.entries(RASHI_LORDS_IDX)
    .filter(([, lord]) => lord === gn)
    .map(([ri]) => (Number(ri) - lagnaIdx + 12) % 12 + 1);
}

// Extra aspect offsets (all planets have 7th = offset 6)
const SPECIAL_ASPECTS: Record<string, number[]> = {
  Mangal: [3, 7],   // 4th and 8th
  Guru:   [4, 8],   // 5th and 9th
  Shani:  [2, 9],   // 3rd and 10th
};

/** Compute map of house → aspecting planet names from grahaRows */
function computeAspects(rows: Array<{ gn: string; house: number }>): Record<number, string[]> {
  const result: Record<number, string[]> = {};
  for (let h = 1; h <= 12; h++) result[h] = [];
  for (const { gn, house } of rows) {
    const offsets = [6, ...(SPECIAL_ASPECTS[gn] ?? [])];
    for (const off of offsets) {
      const aspH = (house - 1 + off) % 12 + 1;
      result[aspH].push(gn);
    }
  }
  return result;
}

const GRAHA_SYMBOLS: Record<string, string> = {
  Surya:"☉︎", Chandra:"☽︎", Mangal:"♂︎", Budh:"☿︎",
  Guru:"♃︎", Shukra:"♀︎", Shani:"♄︎", Rahu:"☊︎", Ketu:"☋︎",
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
  Mesha:"♈︎", Vrishabha:"♉︎", Mithuna:"♊︎", Karka:"♋︎",
  Simha:"♌︎", Kanya:"♍︎", Tula:"♎︎", Vrischika:"♏︎",
  Dhanu:"♐︎", Makara:"♑︎", Kumbha:"♒︎", Meena:"♓︎",
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

// All varga charts — BC + D-1 to D-60
const VARGA_QUICK = [
  { div: 0,  code: "BC",   name: "Bhav Chalit",  desc: "Placidus House Positions" },
  { div: 1,  code: "D-1",  name: "Rashi",        desc: "Body, Personality" },
  { div: 2,  code: "D-2",  name: "Hora",         desc: "Wealth" },
  { div: 3,  code: "D-3",  name: "Drekkana",     desc: "Siblings" },
  { div: 4,  code: "D-4",  name: "Chaturthamsha",desc: "Fortune & Property" },
  { div: 5,  code: "D-5",  name: "Panchamsha",   desc: "Fame, Power" },
  { div: 6,  code: "D-6",  name: "Shashtiamsha", desc: "Health, Enemies" },
  { div: 7,  code: "D-7",  name: "Saptamsha",    desc: "Children" },
  { div: 8,  code: "D-8",  name: "Ashtamsha",    desc: "Sudden Events" },
  { div: 9,  code: "D-9",  name: "Navamsha",     desc: "Marriage, Soul" },
  { div: 10, code: "D-10", name: "Dashamsha",    desc: "Career" },
  { div: 11, code: "D-11", name: "Ekadashamsha", desc: "Gains, Income" },
  { div: 12, code: "D-12", name: "Dwadashamsha", desc: "Parents" },
  { div: 16, code: "D-16", name: "Shodashamsha", desc: "Vehicles, Comforts" },
  { div: 20, code: "D-20", name: "Vimsamsha",    desc: "Spirituality" },
  { div: 24, code: "D-24", name: "Chaturvimsha", desc: "Education" },
  { div: 27, code: "D-27", name: "Nakshatramsha",desc: "Strength" },
  { div: 30, code: "D-30", name: "Trimsamsha",   desc: "Evils, Karma" },
  { div: 40, code: "D-40", name: "Khavedamsha",  desc: "Auspicious Effects" },
  { div: 45, code: "D-45", name: "Akshavedamsha",desc: "Character" },
  { div: 60, code: "D-60", name: "Shashtiamsha", desc: "Past Life" },
];

// Rashi short names & qualities
const RASHI_SHORT: Record<string, string> = {
  Mesha:"Mesh", Vrishabha:"Vrish", Mithuna:"Mith", Karka:"Kark",
  Simha:"Simh", Kanya:"Kany", Tula:"Tula", Vrischika:"Vris",
  Dhanu:"Dhan", Makara:"Maka", Kumbha:"Kumb", Meena:"Meen",
};
const RASHI_QUALITY: Record<string, string> = {
  Mesha:"Mas, Movable", Vrishabha:"Fem, Fixed", Mithuna:"Mas, Common", Karka:"Fem, Movable",
  Simha:"Mas, Fixed", Kanya:"Fem, Common", Tula:"Mas, Movable", Vrischika:"Fem, Fixed",
  Dhanu:"Mas, Common", Makara:"Fem, Movable", Kumbha:"Mas, Fixed", Meena:"Fem, Common",
};
function degToDMS(degree: number, rashiName: string): string {
  const d = Math.floor(degree);
  const mf = (degree - d) * 60;
  const m = Math.floor(mf);
  const s = Math.floor((mf - m) * 60);
  const rs = RASHI_SHORT[rashiName] ?? rashiName.slice(0, 4);
  return `${d}° ${rs} ${String(m).padStart(2,"0")}' ${String(s).padStart(2,"0")}″`;
}

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
  <h1>☽︎ Janam Kundali — Vedic Birth Chart</h1>
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
              {cur && <span className="text-xs text-amber-400 font-medium">★ Active</span>}
              {hasPratyantar && <span className="ml-auto text-xs opacity-70">{isExp ? "▲" : "▼"}</span>}
            </div>
            {isExp && hasPratyantar && (
              <PratyantarList pratyantardashas={a.pratyantardashas!} />
            )}
          </div>
        );
      })}
    </div>
  );
}

// ── Pratyantar List (with Sukshma expand) ─────────────────────────────────────
function PratyantarList({ pratyantardashas }: { pratyantardashas: PratyantarData[] }) {
  const [expandedP, setExpandedP] = useState<string | null>(null);
  return (
    <div className="ml-4 mt-0.5 space-y-0.5 border-l border-border/10 pl-2">
      {pratyantardashas.map((p, j) => {
        const pCur = isCurrentDasha(p.start, p.end);
        const hasSukshma = p.sukshmadashas && p.sukshmadashas.length > 0;
        const isPExp = expandedP === `${j}`;
        return (
          <div key={j}>
            <div
              onClick={() => hasSukshma && setExpandedP(isPExp ? null : `${j}`)}
              className={`flex items-center gap-1.5 text-xs py-0.5 px-1 rounded ${pCur ? "bg-amber-500/10 text-amber-300" : "text-muted-foreground/70"} ${hasSukshma ? "cursor-pointer hover:bg-muted/10" : ""}`}
            >
              <div className="w-1 h-1 rounded-full shrink-0" style={{ background: DASHA_COLORS[p.lord] ?? "#888" }} />
              <span className="font-medium w-12">{p.lord}</span>
              <span>{formatDate(p.start)} → {formatDate(p.end)}</span>
              <span className="opacity-60">{p.days}d</span>
              {pCur && <span className="text-amber-400">★</span>}
              {hasSukshma && <span className="ml-auto opacity-60">{isPExp ? "▲" : "▼"}</span>}
            </div>
            {isPExp && hasSukshma && (
              <div className="ml-3 mt-0.5 space-y-0.5 border-l border-border/10 pl-2">
                {p.sukshmadashas!.map((s, k) => {
                  const sCur = isCurrentDasha(s.start.split(" ")[0], s.end.split(" ")[0]);
                  return (
                    <div key={k} className={`flex items-center gap-1.5 text-xs py-0.5 px-1 rounded ${sCur ? "text-amber-300" : "text-muted-foreground/70"}`}>
                      <div className="w-0.5 h-0.5 rounded-full shrink-0 opacity-60" style={{ background: DASHA_COLORS[s.lord] ?? "#888" }} />
                      <span className="font-medium w-10">{s.lord}</span>
                      <span>{s.start.slice(0,10)}</span>
                      <span className="opacity-60">{s.hours}h</span>
                      {sCur && <span className="text-amber-400">★</span>}
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
          <Label className="text-xs text-muted-foreground">Name <span className="opacity-70">(optional)</span></Label>
          <div className="relative mt-1">
            <User className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground/70" />
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
            <MapPin className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground/70" />
            <Input value={form.place} onChange={e => handleCityInput(e.target.value)}
              placeholder="Search city..." autoComplete="off"
              className={`pl-8 bg-muted/20 border-border/30 ${city ? "border-primary/50" : ""}`} />
            {city && (
              <span className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-primary/70">
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
          <p className="text-xs text-amber-400/80">Select a city from the dropdown for accurate coordinates.</p>
        )}

        {error && <p className="text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{error}</p>}

        <button
          onClick={() => onGenerate(form, city)}
          disabled={!canSubmit || loading}
          className="w-full py-2.5 rounded-xl text-sm font-semibold bg-primary/20 hover:bg-primary/30 text-primary border border-primary/30 transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> Calculating...</> : <><Star className="h-4 w-4" /> Generate Kundali</>}
        </button>
      </div>

      <p className="text-xs text-muted-foreground/60 text-center mt-3">
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
  const [activeTab, setActiveTab] = useState<"charts" | "grahas" | "dasha" | "houses" | "chalit" | "strength" | "ashtaka" | "upagraha" | "yogas" | "lagna">("charts");
  const [expandedGraha, setExpandedGraha] = useState<{ side: "L"|"R"; gn: string } | null>(null);
  const [showAllYogas, setShowAllYogas] = useState(false);
  const [expandedDasha, setExpandedDasha] = useState<string | null>(null);
  // Dual chart state — div=0 means BC
  const [selectedVargaL, setSelectedVargaL] = useState(1);
  const [selectedVargaR, setSelectedVargaR] = useState(9);
  const [chartTabL, setChartTabL] = useState<"graha" | "bhava">("graha");
  const [chartTabR, setChartTabR] = useState<"graha" | "bhava">("graha");
  const [loadingVargaL, setLoadingVargaL] = useState(false);
  const [loadingVargaR, setLoadingVargaR] = useState(false);
  const [vargaCache, setVargaCache] = useState<Record<number, VargaChartData>>();

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

  // Get BC planets — remap D1 planets to their Bhav Chalit house
  const getBCPlanets = (): PlanetData[] => {
    if (!kundaliData?.bhav_chalit) return planets;
    const rashis = ["Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya","Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"];
    const lagnaIdx = rashis.indexOf(kundaliData.lagna.rashi);
    return planets.map(p => {
      const sName = p.sanskritName ?? "";
      const chHouse = kundaliData.bhav_chalit!.planets[sName];
      if (chHouse === undefined) return p;
      const bcRashiIdx = (lagnaIdx + chHouse - 1) % 12;
      return { ...p, house: chHouse, rashi: rashis[bcRashiIdx] };
    });
  };

  // Varga chart planets (from varga_charts or navamsha for D-9)
  const getVargaPlanets = (div: number): PlanetData[] => {
    if (div === 0) return getBCPlanets();  // BC
    if (div === 1) return planets;
    if (div === 9) return navPlanets;
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
    if (!vc) return [];
    return GRAHA_ORDER.map(gn => {
      const g = vc.grahas[gn];
      if (!g) return null;
      return { ...toPlanetData(gn, g), degree: "0°", nakshatra: "" };
    }).filter(Boolean) as PlanetData[];
  };

  const getVargaLagna = (div: number): string => {
    if (div === 0) return kundaliData?.lagna.rashi ?? "Tula"; // BC uses D1 lagna
    if (div === 1) return kundaliData?.lagna.rashi ?? "Tula";
    if (div === 9) return kundaliData?.navamsha_lagna?.rashi ?? kundaliData?.lagna.rashi ?? "Tula";
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
    return vc?.lagna?.rashi ?? kundaliData?.lagna.rashi ?? "Tula";
  };
  const getVargaHouses = (div: number) => {
    if (div === 0 || div === 1) return kundaliData?.houses ?? [];
    if (div === 9) return kundaliData?.navamsha_houses ?? [];
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
    return vc?.houses ?? [];
  };

  const lagnaRashi = kundaliData?.lagna.rashi ?? "Tula";
  const navLagnaRashi = kundaliData?.navamsha_lagna?.rashi ?? lagnaRashi;
  const yogas = kundaliData?.yogas ?? [];
  const visibleYogas = showAllYogas ? yogas : yogas.slice(0, 4);
  const currentDasha = kundaliData?.dashas.find(d => isCurrentDasha(d.start, d.end));

  // Load varga chart on-demand
  const loadVarga = async (div: number, side: "L" | "R") => {
    if (div === 0 || div === 1 || div === 9) return; // BC/D1/D9 always available
    if (vargaCache?.[div] || kundaliData?.varga_charts?.[`D-${div}`]) return;
    if (!kundaliData || !birthDetails) return;
    const setLoading = side === "L" ? setLoadingVargaL : setLoadingVargaR;
    setLoading(true);
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
    setLoading(false);
  };

  const handleVargaSelectL = (div: number) => { setSelectedVargaL(div); loadVarga(div, "L"); };
  const handleVargaSelectR = (div: number) => { setSelectedVargaR(div); loadVarga(div, "R"); };

  return (
    <div className="space-y-4">
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
        <div className="flex-1 min-w-0">
          <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Ayanamsha</p>
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
          <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Rahu/Ketu</p>
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
          <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Names</p>
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
            { label: "Moon", value: kundaliData.grahas["Chandra"]?.rashi ?? "—", sub: kundaliData.grahas["Chandra"]?.nakshatra ?? "", color: "text-blue-400", icon: "☽︎" },
            { label: "Sun", value: kundaliData.grahas["Surya"]?.rashi ?? "—", sub: kundaliData.grahas["Surya"]?.status?.split(" ")[0] ?? "", color: "text-amber-400", icon: "☉︎" },
            { label: "Jupiter", value: kundaliData.grahas["Guru"]?.rashi ?? "—", sub: `H${kundaliData.grahas["Guru"]?.house} · ${kundaliData.grahas["Guru"]?.status?.split(" ")[0] ?? ""}`, color: "text-yellow-400", icon: "♃︎" },
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
                <p className="text-xs text-muted-foreground uppercase">Antardasha</p>
                <p className="text-xs font-medium text-foreground">{curAntar.lord} <span className="text-muted-foreground">({formatDate(curAntar.start)} → {formatDate(curAntar.end)})</span></p>
              </div>
            );
          })()}
        </motion.div>
      )}

      {/* Main content — full width */}
      <div>

        {/* Charts + Tabs — full width */}
        <div className="cosmic-card rounded-xl p-3 md:p-4 min-w-0 overflow-hidden">
          {/* Tab bar — scrollable within card, no page overflow */}
          <div className="flex gap-1 mb-3 border-b border-border/30 pb-2 overflow-x-auto scrollbar-none">
            {[
              { id:"charts",   label:"Charts",       icon: <Layers className="h-3 w-3" /> },
              { id:"grahas",   label:"Grahas",       icon: <Star className="h-3 w-3" /> },
              { id:"dasha",    label:"Dashas",       icon: <Calendar className="h-3 w-3" /> },
              { id:"houses",   label:"Bhavas",       icon: <Home className="h-3 w-3" /> },
              { id:"chalit",   label:"Chalit",       icon: <Layers className="h-3 w-3" /> },
              { id:"strength", label:"Shadbala",     icon: <Zap className="h-3 w-3" /> },
              { id:"ashtaka",  label:"Ashtakavarga", icon: <Star className="h-3 w-3" /> },
              { id:"upagraha", label:"Upagraha",     icon: <Star className="h-3 w-3" /> },
              { id:"yogas",    label:"Yogas",        icon: <Zap className="h-3 w-3" /> },
              { id:"lagna",    label:"Lagna",        icon: <Home className="h-3 w-3" /> },
            ].map(tab => (
              <button key={tab.id}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
                className={`shrink-0 flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full transition-colors whitespace-nowrap ${
                  activeTab === tab.id ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {tab.icon} {tab.label}
              </button>
            ))}
          </div>

          <AnimatePresence mode="wait">
            {/* Dual Charts Tab */}
            {activeTab === "charts" && (
              <motion.div key="charts" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="min-w-0">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 items-start">
                  {([
                    { div: selectedVargaL, setDiv: handleVargaSelectL, tab: chartTabL, setTab: setChartTabL, loading: loadingVargaL, side: "L" as const },
                    { div: selectedVargaR, setDiv: handleVargaSelectR, tab: chartTabR, setTab: setChartTabR, loading: loadingVargaR, side: "R" as const },
                  ] as const).map(({ div, setDiv, tab, setTab, loading, side }) => {
                    const meta = VARGA_QUICK.find(v => v.div === div);
                    const isBc = div === 0;
                    const chartPlanets = getVargaPlanets(div);
                    const chartLagna = getVargaLagna(div);
                    const vargaHouses = getVargaHouses(div);
                    // Graha detail rows for this chart
                    const grahaRows = kundaliData ? GRAHA_ORDER.map(gn => {
                      // Always get D1 base data for longitude/nakshatra (actual ecliptic position)
                      const d1g = kundaliData.grahas[gn];
                      // ruler_of computed client-side from D1 lagna (reliable, no backend dependency)
                      const d1Lagna = kundaliData.lagna.rashi;
                      const rulerOf = getRulerOf(gn, d1Lagna);
                      if (isBc || div === 1) {
                        if (!d1g) return null;
                        const rashiHouse = d1g.house;
                        const bcHouse = kundaliData.bhav_chalit?.planets[gn] ?? rashiHouse;
                        return { gn, rashi: d1g.rashi, house: rashiHouse, bcHouse, degree: d1g.degree, d1Rashi: d1g.rashi, nakshatra: d1g.nakshatra, nakshatra_lord: d1g.nakshatra_lord, pada: d1g.pada, retro: d1g.retro, combust: d1g.combust, status: d1g.status, ruler_of: rulerOf, isD1: true };
                      }
                      if (div === 9) {
                        const g = kundaliData.navamsha?.[gn];
                        if (!g) return null;
                        return { gn, rashi: g.rashi, house: g.house, bcHouse: g.house, degree: d1g?.degree ?? 0, d1Rashi: d1g?.rashi ?? g.rashi, nakshatra: d1g?.nakshatra ?? "", nakshatra_lord: d1g?.nakshatra_lord ?? "", pada: d1g?.pada ?? 0, retro: g.retro, combust: false, status: g.status, ruler_of: rulerOf, isD1: false };
                      }
                      const vc = kundaliData.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
                      const g = vc?.grahas?.[gn];
                      if (!g) return null;
                      return { gn, rashi: g.rashi, house: g.house, bcHouse: g.house, degree: d1g?.degree ?? 0, d1Rashi: d1g?.rashi ?? g.rashi, nakshatra: d1g?.nakshatra ?? "", nakshatra_lord: d1g?.nakshatra_lord ?? "", pada: d1g?.pada ?? 0, retro: g.retro, combust: false, status: g.status, ruler_of: rulerOf, isD1: false };
                    }).filter(Boolean) : [];

                    return (
                      <div key={side} className={`rounded-xl border bg-card/30 backdrop-blur-sm overflow-hidden ${
                        selectedVargaL === selectedVargaR ? "border-amber-500/30" : "border-border/20"
                      }`}>
                        {/* Chart header — label + selector + lagna */}
                        <div className="flex items-center gap-2 p-2 border-b border-border/15 bg-muted/10">
                          <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded shrink-0 ${
                            side === "L"
                              ? "bg-primary/20 text-primary"
                              : "bg-amber-500/20 text-amber-400"
                          }`}>{side === "L" ? "Chart A" : "Chart B"}</span>
                          <select
                            value={div}
                            onChange={e => setDiv(Number(e.target.value))}
                            className="flex-1 bg-background/60 border border-border/30 rounded-lg px-2 py-1 text-xs text-foreground focus:outline-none focus:border-primary/50 cursor-pointer"
                          >
                            {VARGA_QUICK.map(v => (
                              <option key={v.div} value={v.div}>{v.code} — {v.name}</option>
                            ))}
                          </select>
                          <div className="text-xs text-muted-foreground shrink-0">
                            Lagna: <span className="text-amber-400 font-medium">{chartLagna}</span>
                          </div>
                        </div>

                        {/* Chart desc + same-chart warning */}
                        <div className="px-3 py-1 text-[10px] text-muted-foreground/70 border-b border-border/10 flex items-center gap-2">
                          <span>{isBc ? "Placidus house positions — planets may shift houses vs Whole Sign" : meta?.desc}</span>
                          {isBc && <span className="text-amber-400/80">Bhav Chalit</span>}
                          {selectedVargaL === selectedVargaR && (
                            <span className="ml-auto text-amber-500/80">⚠ Same chart on both sides</span>
                          )}
                        </div>

                        {/* Chart */}
                        <div className="p-1">
                          {loading ? (
                            <div className="flex items-center justify-center py-16 text-muted-foreground text-xs gap-2">
                              <Loader2 className="h-4 w-4 animate-spin" /> Loading chart…
                            </div>
                          ) : chartPlanets.length > 0 ? (
                            <KundliChart
                              onPlanetClick={setSelectedPlanet}
                              selectedPlanet={selectedPlanet}
                              planets={chartPlanets}
                              lagnaRashi={chartLagna}
                              showStyleToggle={true}
                            />
                          ) : (
                            <div className="flex items-center justify-center py-16 text-muted-foreground text-xs">
                              {kundaliData ? "Varga data loading…" : "Generate Kundali first"}
                            </div>
                          )}
                        </div>

                        {/* Detail tabs */}
                        {kundaliData && (
                          <div className="border-t border-border/15">
                            <div className="flex border-b border-border/15">
                              {(["graha","bhava"] as const).map(t => (
                                <button key={t} onClick={() => setTab(t)}
                                  className={`flex-1 py-1.5 text-xs font-medium transition-colors ${tab === t ? "text-primary border-b-2 border-primary bg-primary/5" : "text-muted-foreground hover:text-foreground"}`}>
                                  {t === "graha" ? "Graha Details" : "Bhava Details"}
                                </button>
                              ))}
                            </div>

                            {tab === "graha" && (
                              <div className="overflow-x-auto scrollbar-none">
                                <table className="w-full min-w-[280px] text-xs table-fixed">
                                  <colgroup>
                                    <col style={{ width: "22%" }} />
                                    <col style={{ width: "28%" }} />
                                    <col style={{ width: "22%" }} />
                                    <col style={{ width: "13%" }} />
                                    <col style={{ width: "15%" }} />
                                  </colgroup>
                                  <thead>
                                    <tr className="border-b border-border/15 bg-muted/10">
                                      <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Graha</th>
                                      <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Longitude</th>
                                      <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Nakshatra</th>
                                      <th className="text-center px-2 py-1.5 text-muted-foreground font-medium">Rules</th>
                                      <th className="text-center px-2 py-1.5 text-muted-foreground font-medium">House/Status</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {/* Lagna row — always shown for all charts */}
                                    {kundaliData && (() => {
                                      const isD1full = isBc || div === 1;
                                      const l = kundaliData.lagna;
                                      return (
                                        <tr className="border-b border-border/10 bg-amber-500/5">
                                          <td className="px-2 py-1.5 font-medium text-amber-400">Lagna</td>
                                          <td className="px-2 py-1.5 font-mono text-foreground truncate">
                                            {isD1full ? degToDMS(l.degree, l.rashi) : chartLagna}
                                          </td>
                                          <td className="px-2 py-1.5 text-muted-foreground truncate">
                                            {isD1full ? `${l.nakshatra}${l.pada ? ` P${l.pada}` : ""}` : "—"}
                                          </td>
                                          <td className="px-2 py-1.5 text-center text-muted-foreground">—</td>
                                          <td className="px-2 py-1.5 text-center font-medium text-foreground">H1</td>
                                        </tr>
                                      );
                                    })()}
                                    {grahaRows.map(r => {
                                      if (!r) return null;
                                      const isQ = [1,4,7,10].includes(r.house);
                                      const bcDiff = r.isD1 && r.bcHouse !== r.house;
                                      const sb = STATUS_BADGE[r.status] ?? STATUS_BADGE["Normal"];
                                      const isExpanded = expandedGraha?.side === side && expandedGraha?.gn === r.gn;
                                      // Get full graha data for expanded panel
                                      const fullG = kundaliData.grahas[r.gn];
                                      return (
                                        <React.Fragment key={r.gn}>
                                          <tr
                                            onClick={() => setExpandedGraha(isExpanded ? null : { side, gn: r.gn })}
                                            className={`border-b border-border/10 cursor-pointer transition-colors ${isExpanded ? "bg-primary/8 border-primary/20" : "hover:bg-muted/10"}`}
                                          >
                                            <td className="px-2 py-1.5">
                                              <span className="flex items-center gap-1">
                                                <span style={{ color: GRAHA_COLOR[r.gn] }}>{GRAHA_SYMBOLS[r.gn]}</span>
                                                <span className="font-medium text-foreground">{r.gn}</span>
                                                {r.retro && <span className="text-amber-400 text-[10px]">℞</span>}
                                                {r.combust && <span className="text-orange-400 text-[10px]">🔥</span>}
                                                {isQ && <span className="text-[9px] text-primary/60 ml-0.5">Q</span>}
                                              </span>
                                            </td>
                                            <td className="px-2 py-1.5 font-mono text-foreground text-xs">
                                              {r.degree > 0 ? degToDMS(r.degree, (r as any).d1Rashi ?? r.rashi) : r.rashi}
                                            </td>
                                            <td className="px-2 py-1.5 text-muted-foreground truncate">
                                              {r.nakshatra ? `${r.nakshatra}${r.pada ? ` P${r.pada}` : ""}` : "—"}
                                            </td>
                                            <td className="px-2 py-1.5 text-center text-muted-foreground text-[10px]">
                                              {r.ruler_of?.length ? r.ruler_of.map(h => `H${h}`).join(",") : "—"}
                                            </td>
                                            <td className="px-2 py-1.5 text-center">
                                              <span className="font-medium text-foreground">H{r.house}</span>
                                              {r.isD1 && bcDiff && <span className="ml-0.5 text-amber-400 text-[9px]">→H{r.bcHouse}</span>}
                                              <br />
                                              <span className={`text-[9px] px-1 py-0.5 rounded border ${sb.cls}`}>{sb.label}</span>
                                            </td>
                                          </tr>
                                          {/* Expanded detail row */}
                                          {isExpanded && (
                                            <tr key={`${r.gn}-exp`} className="border-b border-primary/15 bg-primary/5">
                                              <td colSpan={5} className="px-3 py-3">
                                                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                                                  {fullG ? (
                                                    <>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Rashi</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.rashi} {fullG.rashi_en ? `(${fullG.rashi_en})` : ""}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">House</p>
                                                        <p className="text-foreground font-medium mt-0.5">H{fullG.house}{bcDiff ? ` → H${r.bcHouse} (BC)` : ""}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Nakshatra/Pada</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.nakshatra} P{fullG.pada}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Nak. Lord</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.nakshatra_lord ?? "—"}</p>
                                                      </div>
                                                      <div className={`rounded-lg p-2 border ${(STATUS_BADGE[fullG.status] ?? STATUS_BADGE["Normal"]).cls}`}>
                                                        <p className="text-[9px] uppercase tracking-wide opacity-70">Dignity</p>
                                                        <p className="font-medium text-xs mt-0.5">{fullG.status}</p>
                                                      </div>
                                                      <div className={`rounded-lg p-2 ${(RELATIONSHIP_BADGE[fullG.relationship ?? "Neutral"] ?? RELATIONSHIP_BADGE["Neutral"]).cls}`}>
                                                        <p className="text-[9px] uppercase tracking-wide opacity-70">Relationship</p>
                                                        <p className="font-medium text-xs mt-0.5">{(RELATIONSHIP_BADGE[fullG.relationship ?? "Neutral"] ?? RELATIONSHIP_BADGE["Neutral"]).label}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Speed / Lat</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.speed?.toFixed(3) ?? "—"}°/d · {fullG.lat_ecl?.toFixed(2) ?? "0"}°</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Rules Houses</p>
                                                        <p className="text-foreground font-medium mt-0.5">{r.ruler_of?.length ? r.ruler_of.map(h => `H${h}`).join(", ") : "—"}</p>
                                                      </div>
                                                      {fullG.karaka && (
                                                        <div className="col-span-2 sm:col-span-4 bg-primary/10 rounded-lg p-2 border border-primary/15">
                                                          <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Karaka (Significator)</p>
                                                          <p className="text-foreground text-xs mt-0.5">{fullG.karaka}</p>
                                                        </div>
                                                      )}
                                                      {fullG.combust && (
                                                        <div className="col-span-2 sm:col-span-4 bg-orange-500/10 rounded-lg p-2 border border-orange-500/20">
                                                          <p className="text-[9px] text-orange-400 uppercase tracking-wide">Combust (Asta)</p>
                                                          <p className="text-orange-300 text-xs mt-0.5">Too close to Sun — weakened</p>
                                                        </div>
                                                      )}
                                                    </>
                                                  ) : (
                                                    <>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Varga Rashi</p>
                                                        <p className="text-foreground font-medium mt-0.5">{r.rashi}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Varga House</p>
                                                        <p className="text-foreground font-medium mt-0.5">H{r.house}</p>
                                                      </div>
                                                      <div className={`rounded-lg p-2 border ${sb.cls}`}>
                                                        <p className="text-[9px] uppercase tracking-wide opacity-70">Status</p>
                                                        <p className="font-medium text-xs mt-0.5">{r.status}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">D1 Longitude</p>
                                                        <p className="text-foreground font-medium mt-0.5 font-mono text-[10px]">
                                                          {r.degree > 0 ? degToDMS(r.degree, (r as any).d1Rashi ?? r.rashi) : "—"}
                                                        </p>
                                                      </div>
                                                      {r.nakshatra && (
                                                        <div className="bg-muted/30 rounded-lg p-2">
                                                          <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Nakshatra / Pada</p>
                                                          <p className="text-foreground font-medium mt-0.5">{r.nakshatra} P{r.pada}</p>
                                                        </div>
                                                      )}
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Retrograde</p>
                                                        <p className="text-foreground font-medium mt-0.5">{r.retro ? "Yes ℞" : "No"}</p>
                                                      </div>
                                                    </>
                                                  )}
                                                </div>
                                              </td>
                                            </tr>
                                          )}
                                        </React.Fragment>
                                      );
                                    })}
                                  </tbody>
                                </table>
                                <p className="text-[10px] text-muted-foreground/60 px-2 py-1.5">Click any row to expand full details · Q = Kendra (angular house)</p>
                              </div>
                            )}

                            {tab === "bhava" && (
                              <div className="overflow-x-auto scrollbar-none">
                                {(() => {
                                  const validRows = grahaRows.filter(Boolean) as NonNullable<typeof grahaRows[number]>[];
                                  const aspectMap = computeAspects(validRows);
                                  return (
                                    <table className="w-full min-w-[320px] text-xs table-fixed">
                                      <colgroup>
                                        <col style={{ width: "7%" }} />
                                        <col style={{ width: "20%" }} />
                                        <col style={{ width: "11%" }} />
                                        <col style={{ width: "13%" }} />
                                        <col style={{ width: "14%" }} />
                                        <col style={{ width: "35%" }} />
                                      </colgroup>
                                      <thead>
                                        <tr className="border-b border-border/15 bg-muted/10">
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">H</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Residents</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Owner</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Rashi</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Quality</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Aspected By</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        {vargaHouses.map(h => {
                                          const isQ = [1,4,7,10].includes(h.house);
                                          let residents: string[] = [];
                                          if (isBc && kundaliData.bhav_chalit) {
                                            residents = GRAHA_ORDER.filter(gn => kundaliData.bhav_chalit!.planets[gn] === h.house);
                                          } else {
                                            residents = h.planets ?? [];
                                          }
                                          const aspectors = aspectMap[h.house] ?? [];
                                          return (
                                            <tr key={h.house} className={`border-b border-border/10 hover:bg-muted/10 ${isQ ? "bg-primary/3" : ""}`}>
                                              <td className="px-2 py-1.5 font-medium text-foreground">
                                                {h.house}{isQ && <span className="text-[9px] text-primary/60 ml-0.5">Q</span>}
                                              </td>
                                              <td className="px-2 py-1.5 text-muted-foreground truncate">
                                                {residents.length > 0 ? residents.map(g => g.slice(0,3)).join(", ") : <span className="opacity-40">—</span>}
                                              </td>
                                              <td className="px-2 py-1.5 text-foreground truncate">{h.lord?.slice(0,3) ?? "—"}</td>
                                              <td className="px-2 py-1.5 text-muted-foreground truncate">{RASHI_SHORT[h.rashi] ?? h.rashi?.slice(0,3)}</td>
                                              <td className="px-2 py-1.5 text-muted-foreground truncate text-[10px]">
                                                {RASHI_QUALITY[h.rashi] ?? "—"}
                                              </td>
                                              <td className="px-2 py-1.5 text-muted-foreground text-[10px]">
                                                {aspectors.length > 0
                                                  ? aspectors.map(g => (
                                                      <span key={g} className="mr-1 whitespace-nowrap" style={{ color: GRAHA_COLOR[g] }}>
                                                        {GRAHA_SYMBOLS[g]}{g.slice(0,3)}
                                                      </span>
                                                    ))
                                                  : <span className="opacity-40">—</span>}
                                              </td>
                                            </tr>
                                          );
                                        })}
                                      </tbody>
                                    </table>
                                  );
                                })()}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
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
                                  {cur && <span className="text-xs bg-amber-500/20 text-amber-400 px-1.5 py-0.5 rounded border border-amber-500/30 font-medium">Active Now ★</span>}
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
                {(kundaliData?.houses ?? []).length > 0 ? (() => {
                  // Compute D1 aspects client-side
                  const d1AspectMap = computeAspects(
                    GRAHA_ORDER.filter(gn => kundaliData!.grahas[gn]).map(gn => ({
                      gn, house: kundaliData!.grahas[gn].house,
                    }))
                  );
                  return (
                  <div className="space-y-1.5">
                    {(kundaliData?.houses ?? []).map((h) => {
                      const planetsIn = h.planets ?? GRAHA_ORDER.filter(gn => kundaliData?.grahas[gn]?.house === h.house);
                      const aspectors = d1AspectMap[h.house] ?? [];
                      return (
                        <div key={h.house} className="flex items-start gap-3 px-2 py-2.5 rounded-lg hover:bg-muted/20 transition-colors">
                          <div className="w-8 h-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0 mt-0.5">
                            <span className="text-xs font-bold text-primary">{h.house}</span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="text-sm text-foreground font-medium">
                                {h.rashi} {RASHI_SYMBOLS[h.rashi] ?? ""}
                                {h.rashi_en && <span className="text-xs text-muted-foreground/70 ml-1">({h.rashi_en})</span>}
                              </span>
                              <span className="text-xs text-muted-foreground">Lord: {h.lord} {h.lord_en ? `(${h.lord_en})` : ""}</span>
                            </div>
                            {h.signification && (
                              <p className="text-xs text-muted-foreground/60 mt-0.5">{h.signification}</p>
                            )}
                            {(h.gender || h.modality || h.tattva) && (
                              <p className="text-xs text-muted-foreground/70 mt-0.5">
                                {[h.gender, h.modality, h.tattva].filter(Boolean).join(" · ")}
                              </p>
                            )}
                            {aspectors.length > 0 && (
                              <div className="flex items-center gap-1 mt-1 flex-wrap">
                                <span className="text-[10px] text-muted-foreground/60 mr-0.5">Aspected by:</span>
                                {aspectors.map(gn => (
                                  <span key={gn} className="text-[10px] px-1 py-0.5 rounded" style={{ color: GRAHA_COLOR[gn], background: `${GRAHA_COLOR[gn]}22` }}>
                                    {GRAHA_SYMBOLS[gn]} {gn}
                                  </span>
                                ))}
                              </div>
                            )}
                            {planetsIn.length > 0 && (
                              <div className="flex items-center gap-1 mt-1 flex-wrap">
                                {planetsIn.map(gn => (
                                  <span key={gn} className="text-xs px-1.5 py-0.5 rounded" style={{ color: GRAHA_COLOR[gn], background: `${GRAHA_COLOR[gn]}22` }}>
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
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see house chart</p>
                )}
              </motion.div>
            )}

            {/* Grahas Detail Table */}
            {activeTab === "grahas" && (
              <motion.div key="grahas" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Star className="h-3.5 w-3.5 text-primary" /> Graha Details — Full Planetary Data
                </p>
                {kundaliData ? (
                  <div className="overflow-x-auto">
                    <table className="text-xs min-w-[560px] w-full">
                      <thead>
                        <tr className="border-b border-border/20 text-muted-foreground">
                          <th className="text-left py-1.5 px-2">Graha</th>
                          <th className="text-center px-1">R</th>
                          <th className="text-center px-1">C</th>
                          <th className="text-left px-2">Longitude (DMS)</th>
                          <th className="text-left px-2">Nakshatra / Lord</th>
                          <th className="text-center px-1">Pada</th>
                          <th className="text-center px-1">Raw L</th>
                          <th className="text-center px-1">Lat</th>
                          <th className="text-center px-1">RA</th>
                          <th className="text-center px-1">Dec</th>
                          <th className="text-center px-1">Speed</th>
                          <th className="text-left px-2">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {/* Lagna row */}
                        <tr className="border-b border-border/10 bg-primary/5">
                          <td className="py-1.5 px-2 font-semibold text-primary">Lagna (Lg)</td>
                          <td className="text-center px-1">—</td>
                          <td className="text-center px-1">—</td>
                          <td className="px-2 font-medium text-foreground">
                            {(() => {
                              const d = kundaliData.lagna.degree;
                              const deg = Math.floor(d);
                              const minF = (d - deg) * 60;
                              const min = Math.floor(minF);
                              const sec = Math.floor((minF - min) * 60);
                              return `${deg.toString().padStart(2,"0")}° ${min.toString().padStart(2,"0")}' ${sec.toString().padStart(2,"0")}"`;
                            })()}
                          </td>
                          <td className="px-2">{kundaliData.lagna.nakshatra} / {kundaliData.lagna.nakshatra?.split(" ")[0]}</td>
                          <td className="text-center px-1">{kundaliData.lagna.pada ?? "—"}</td>
                          <td className="text-center px-1">{kundaliData.lagna.full_degree?.toFixed(2) ?? "—"}</td>
                          <td className="text-center px-1">0.00</td>
                          <td className="text-center px-1">—</td>
                          <td className="text-center px-1">—</td>
                          <td className="text-center px-1">—</td>
                          <td className="px-2">—</td>
                        </tr>
                        {GRAHA_ORDER.map(gn => {
                          const g = kundaliData.grahas[gn];
                          if (!g) return null;
                          const lon = g.longitude ?? 0;
                          const deg = Math.floor(g.degree);
                          const minF = (g.degree - deg) * 60;
                          const min = Math.floor(minF);
                          const sec = Math.floor((minF - min) * 60);
                          const dms = `${deg.toString().padStart(2,"0")}° ${min.toString().padStart(2,"0")}' ${sec.toString().padStart(2,"0")}"`;
                          const sb = STATUS_BADGE[g.status] ?? STATUS_BADGE["Normal"];
                          return (
                            <tr key={gn} className="border-b border-border/10 hover:bg-muted/20">
                              <td className="py-1.5 px-2">
                                <span style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                <span className="font-medium text-foreground ml-1">{gn}</span>
                              </td>
                              <td className="text-center px-1 text-amber-400">{g.retro ? "℞" : ""}</td>
                              <td className="text-center px-1 text-orange-400">{g.combust ? "C" : ""}</td>
                              <td className="px-2 font-mono text-foreground">
                                {g.rashi} {dms}
                              </td>
                              <td className="px-2 text-muted-foreground">
                                {g.nakshatra} {g.pada}, {g.nakshatra_lord}
                              </td>
                              <td className="text-center px-1 text-foreground">{g.pada}</td>
                              <td className="text-center px-1 text-muted-foreground font-mono">{lon.toFixed(2)}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.lat_ecl?.toFixed(2) ?? "0.00"}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.ra?.toFixed(2) ?? "—"}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.dec?.toFixed(2) ?? "—"}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.speed?.toFixed(2) ?? "—"}</td>
                              <td className="px-2"><span className={`px-1.5 py-0.5 rounded border text-xs ${sb.cls}`}>{sb.label}</span></td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    <p className="text-xs text-muted-foreground/70 mt-2 px-1">
                      R = Retrograde · C = Combust · Raw L = Full longitude (0°–360°) · Lat = Ecliptic Latitude · RA = Right Ascension · Dec = Declination
                    </p>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see graha details</p>
                )}
              </motion.div>
            )}

            {/* Shadbala + Bhavabala */}
            {activeTab === "strength" && (
              <motion.div key="strength" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Zap className="h-3.5 w-3.5 text-primary" /> Shadbala — Six-fold Planetary Strength
                </p>
                {kundaliData?.shadbala?.planets ? (() => {
                  const planets7 = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani"];
                  const sb = kundaliData.shadbala!.planets;
                  return (
                    <div className="space-y-4">
                      {/* Summary grid */}
                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                        {planets7.filter(gn => sb[gn]).map(gn => {
                          const s = sb[gn];
                          const pct = Math.min(100, Math.round(s.ratio * 100));
                          const color = s.is_strong ? "text-emerald-400" : "text-red-400";
                          return (
                            <div key={gn} className="bg-muted/20 rounded-lg p-2 text-center">
                              <div className="flex items-center justify-center gap-1 mb-1">
                                <span className="text-sm" style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                <span className="text-xs text-muted-foreground">{GRAHA_EN[gn]}</span>
                              </div>
                              <p className={`text-sm font-bold ${color}`}>{s.total_rupas}R</p>
                              <p className="text-xs text-muted-foreground">Need {s.required_rupas}R</p>
                              <div className="mt-1 h-1 bg-muted/30 rounded-full">
                                <div className={`h-full rounded-full ${s.is_strong ? "bg-emerald-400" : "bg-red-400"}`} style={{ width: `${pct}%` }} />
                              </div>
                              <p className={`text-xs mt-0.5 font-medium ${color}`}>{s.is_strong ? "✓ Strong" : "✗ Weak"}</p>
                            </div>
                          );
                        })}
                      </div>
                      {/* Detailed table */}
                      <div className="overflow-x-auto">
                        <table className="w-full min-w-[360px] text-xs">
                          <thead>
                            <tr className="border-b border-border/20">
                              <th className="text-left text-muted-foreground py-1 pr-2">Component</th>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <th key={gn} className="text-center text-muted-foreground py-1 px-1">
                                  <span style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {[
                              ["Sthana", "sthana_bala"],
                              ["  ↳ Uccha", null, "uccha"],
                              ["  ↳ Oja-Yugma", null, "oja_yugma"],
                              ["  ↳ Kendradi", null, "kendradi"],
                              ["  ↳ Drekkana", null, "drekkana"],
                              ["Dig", "dig_bala"],
                              ["Kala", "kaala_bala"],
                              ["Chesta", "chesta_bala"],
                              ["Naisargika", "naisargika_bala"],
                              ["Drik", "drik_bala"],
                            ].map(([label, key, subKey]) => (
                              <tr key={`${key ?? subKey}`} className={`border-b border-border/10 ${!key ? "opacity-60" : ""}`}>
                                <td className={`text-muted-foreground py-1 pr-2 ${key ? "font-medium" : "text-xs pl-2"}`}>{label}</td>
                                {planets7.filter(gn => sb[gn]).map(gn => (
                                  <td key={gn} className="text-center text-foreground py-1 px-1 text-xs">
                                    {key
                                      ? (sb[gn] as any)[key as string]
                                      : (sb[gn].sthana_detail as any)?.[subKey as string] ?? "—"
                                    }
                                  </td>
                                ))}
                              </tr>
                            ))}
                            <tr className="border-t border-border/30 font-bold">
                              <td className="text-muted-foreground py-1.5 pr-2">Total (V)</td>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <td key={gn} className="text-center text-foreground py-1.5 px-1">{sb[gn].total_virupas}</td>
                              ))}
                            </tr>
                            <tr>
                              <td className="text-muted-foreground py-1 pr-2">Rupas</td>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <td key={gn} className="text-center text-foreground py-1 px-1">{sb[gn].total_rupas}</td>
                              ))}
                            </tr>
                            <tr>
                              <td className="text-muted-foreground py-1 pr-2">Ratio</td>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <td key={gn} className={`text-center py-1 px-1 font-medium ${sb[gn].is_strong ? "text-emerald-400" : "text-red-400"}`}>
                                  {sb[gn].ratio}
                                </td>
                              ))}
                            </tr>
                          </tbody>
                        </table>
                      </div>
                      {/* Bhavabala */}
                      {kundaliData.bhavabala && (
                        <div>
                          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">Bhavabala — House Strength</p>
                          <div className="grid grid-cols-3 sm:grid-cols-4 gap-1.5">
                            {kundaliData.bhavabala.map(bh => (
                              <div key={bh.house} className="bg-muted/20 rounded-lg p-2 text-center">
                                <div className="flex items-center justify-between mb-0.5">
                                  <span className="text-xs font-bold text-primary">H{bh.house}</span>
                                  <span className="text-xs text-muted-foreground">#{bh.rank}</span>
                                </div>
                                <p className="text-xs text-foreground font-medium">{bh.strength}R</p>
                                <p className="text-xs text-muted-foreground">{bh.lord}</p>
                                <div className="mt-1 h-1 bg-muted/30 rounded-full">
                                  <div className="h-full bg-primary/60 rounded-full" style={{ width: `${Math.min(100, (bh.strength / 12) * 100)}%` }} />
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">
                    {kundaliData ? "Shadbala not calculated. Regenerate with shadbala option." : "Generate Kundali to see Shadbala"}
                  </p>
                )}
              </motion.div>
            )}

            {/* Ashtakavarga */}
            {activeTab === "ashtaka" && (
              <motion.div key="ashtaka" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Star className="h-3.5 w-3.5 text-primary" /> Ashtakavarga — Bindu Points per Rashi
                </p>
                {kundaliData?.ashtakavarga ? (() => {
                  const av = kundaliData.ashtakavarga!;
                  const rashiNames = ["Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya","Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"];
                  const planets7 = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani"];
                  return (
                    <div className="space-y-4">
                      {/* SAV grid */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Sarvashtakavarga (SAV) — Total Bindus per Rashi</p>
                        <div className="grid grid-cols-6 sm:grid-cols-12 gap-1">
                          {rashiNames.map((rashi, i) => {
                            const pts = av.sav.points[i] ?? 0;
                            const isGood = pts >= 30;
                            return (
                              <div key={rashi} className={`rounded-lg p-1.5 text-center ${isGood ? "bg-emerald-500/10 border border-emerald-500/20" : "bg-red-500/10 border border-red-500/20"}`}>
                                <p className="text-xs text-muted-foreground">{RASHI_SYMBOLS[rashi] ?? rashi.slice(0,2)}</p>
                                <p className={`text-sm font-bold ${isGood ? "text-emerald-400" : "text-red-400"}`}>{pts}</p>
                              </div>
                            );
                          })}
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">Total: {av.sav.total} · ≥30 = favourable</p>
                      </div>
                      {/* Reduced SAV */}
                      {av.reduced_sav && (
                        <div>
                          <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Reduced Sarvashtakavarga (after Trikona Shodhana)</p>
                          <div className="grid grid-cols-6 sm:grid-cols-12 gap-1">
                            {rashiNames.map((rashi, i) => {
                              const pts = av.reduced_sav!.points[i] ?? 0;
                              return (
                                <div key={rashi} className="rounded-lg p-1.5 text-center bg-muted/20">
                                  <p className="text-xs text-muted-foreground">{RASHI_SYMBOLS[rashi] ?? rashi.slice(0,2)}</p>
                                  <p className="text-sm font-bold text-foreground">{pts}</p>
                                </div>
                              );
                            })}
                          </div>
                          <p className="text-xs text-muted-foreground mt-1">Total: {av.reduced_sav.total}</p>
                        </div>
                      )}

                      {/* BAV table */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Bhinnashtakavarga (BAV) — Per-planet bindus</p>
                        <div className="overflow-x-auto">
                          <table className="w-full min-w-[420px] text-xs">
                            <thead>
                              <tr className="border-b border-border/20">
                                <th className="text-left text-muted-foreground py-1 pr-2 w-16">Planet</th>
                                {rashiNames.map((r) => (
                                  <th key={r} className="text-center text-muted-foreground py-1 px-0.5 w-6">
                                    {RASHI_SYMBOLS[r] ?? r.slice(0,2)}
                                  </th>
                                ))}
                                <th className="text-center text-muted-foreground py-1 px-1">Σ</th>
                              </tr>
                            </thead>
                            <tbody>
                              {planets7.map(gn => {
                                const bavData = av.bav[gn];
                                if (!bavData) return null;
                                return (
                                  <tr key={gn} className="border-b border-border/10">
                                    <td className="py-1 pr-2">
                                      <span style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                      <span className="text-muted-foreground ml-1">{gn.slice(0,3)}</span>
                                    </td>
                                    {bavData.points.map((pt, i) => (
                                      <td key={i} className={`text-center py-1 px-0.5 ${pt >= 4 ? "text-emerald-400" : pt <= 2 ? "text-red-400/70" : "text-foreground"}`}>
                                        {pt}
                                      </td>
                                    ))}
                                    <td className="text-center py-1 px-1 font-bold text-foreground">{bavData.total}</td>
                                  </tr>
                                );
                              })}
                              <tr className="border-t border-border/30 font-bold">
                                <td className="text-muted-foreground py-1.5 pr-2">SAV</td>
                                {av.sav.points.map((pt, i) => (
                                  <td key={i} className={`text-center py-1.5 px-0.5 font-bold ${pt >= 30 ? "text-emerald-400" : "text-red-400"}`}>{pt}</td>
                                ))}
                                <td className="text-center py-1.5 px-1 text-foreground font-bold">{av.sav.total}</td>
                              </tr>
                              {av.reduced_sav && (
                                <tr className="border-t border-border/20">
                                  <td className="text-muted-foreground py-1 pr-2 text-xs">Red.SAV</td>
                                  {av.reduced_sav.points.map((pt, i) => (
                                    <td key={i} className="text-center py-1 px-0.5 text-muted-foreground/70">{pt}</td>
                                  ))}
                                  <td className="text-center py-1 px-1 text-muted-foreground">{av.reduced_sav.total}</td>
                                </tr>
                              )}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    </div>
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">
                    {kundaliData ? "Ashtakavarga not calculated. Regenerate." : "Generate Kundali to see Ashtakavarga"}
                  </p>
                )}
              </motion.div>
            )}

            {/* Upagraha Tab */}
            {activeTab === "upagraha" && (
              <motion.div key="upagraha" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                {kundaliData?.upagraha && Object.keys(kundaliData.upagraha).length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[380px] text-xs">
                      <thead>
                        <tr className="border-b border-border/20 bg-muted/10">
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium">Upagraha</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium">Rashi</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium">Longitude</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium hidden sm:table-cell">Nakshatra</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium hidden sm:table-cell">Nak Lord</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium hidden md:table-cell">Significance</th>
                        </tr>
                      </thead>
                      <tbody>
                        {Object.entries(kundaliData.upagraha).map(([name, u]: [string, UpagrahaEntry]) => {
                          const isMalefic = ["Dhuma","Vyatipata","Mandi","Gulika","Kala","Mrityu","YamaGhantaka"].includes(name);
                          return (
                            <tr key={name} className="border-b border-border/10 hover:bg-muted/10 transition-colors">
                              <td className="px-3 py-2 font-medium text-foreground">
                                <span className={isMalefic ? "text-red-400/80" : "text-emerald-400/80"}>●</span>
                                <span className="ml-1.5">{name}</span>
                              </td>
                              <td className="px-3 py-2 text-muted-foreground">{u.rashi_name} {RASHI_SYMBOLS[u.rashi_name] ?? ""}</td>
                              <td className="px-3 py-2 font-mono text-foreground">{u.dms}</td>
                              <td className="px-3 py-2 text-muted-foreground hidden sm:table-cell">{u.nakshatra}</td>
                              <td className="px-3 py-2 text-muted-foreground hidden sm:table-cell">{u.nakshatra_lord}</td>
                              <td className="px-3 py-2 text-muted-foreground/70 text-[10px] hidden md:table-cell max-w-xs truncate" title={u.significance}>
                                {u.significance.split(" — ")[1] ?? u.significance}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    <p className="text-[10px] text-muted-foreground/60 mt-3 px-1">
                      Sun-derived: Dhuma, Vyatipata, Parivesha, Indrachapa, Upaketu · Hora-based: Mandi, Gulika, Kala, Mrityu, ArdhaPrahara, YamaGhantaka
                    </p>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-10">
                    {kundaliData ? "Upagraha not calculated. Regenerate Kundali." : "Generate Kundali to see Upagrahas"}
                  </p>
                )}
              </motion.div>
            )}

            {/* Yogas Tab */}
            {activeTab === "yogas" && (
              <motion.div key="yogas" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                {yogas.length > 0 ? (
                  <div className="space-y-1.5">
                    <p className="text-xs text-muted-foreground mb-3">
                      {yogas.filter(y => y.present !== false).length} active of {yogas.length} total yogas
                    </p>
                    {yogas.map(y => {
                      const isPresent = y.present !== false;
                      const strengthCls = !isPresent ? "text-muted-foreground/60 border-border/10 bg-muted/5"
                        : y.strength === "Very Strong" ? "text-emerald-400 border-emerald-500/30 bg-emerald-500/10"
                        : y.strength === "Strong" ? "text-amber-400 border-amber-500/30 bg-amber-500/10"
                        : "text-muted-foreground border-border/20 bg-muted/10";
                      const catColor = YOGA_CATEGORY_COLORS[y.category ?? ""] ?? "text-muted-foreground";
                      return (
                        <div key={y.name} className={`rounded-lg p-2.5 border border-border/10 ${isPresent ? "bg-muted/20" : "bg-muted/5 opacity-50"}`}>
                          <div className="flex items-center justify-between mb-0.5">
                            <div className="flex items-center gap-2">
                              <span className={`text-xs font-bold ${isPresent ? "text-emerald-400" : "text-red-400/50"}`}>{isPresent ? "✓" : "✗"}</span>
                              <p className={`text-xs font-medium ${isPresent ? "text-foreground" : "text-muted-foreground/70"}`}>{y.name}</p>
                              {y.category && <span className={`text-[10px] ${catColor}`}>{y.category}</span>}
                            </div>
                            {isPresent && <span className={`text-[10px] px-1.5 py-0.5 rounded border ${strengthCls}`}>{y.strength}</span>}
                          </div>
                          {isPresent && <p className="text-[11px] text-muted-foreground mt-0.5">{y.desc}</p>}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-10">
                    {kundaliData ? "No major yogas detected" : "Generate Kundali to see Yogas"}
                  </p>
                )}
              </motion.div>
            )}

            {/* Lagna Tab */}
            {activeTab === "lagna" && (
              <motion.div key="lagna" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="space-y-4">
                {kundaliData ? (
                  <>
                    {/* Lagna details */}
                    <div>
                      <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">Lagna (Ascendant)</p>
                      <div className="grid grid-cols-2 gap-2 text-xs">
                        {[
                          ["Rashi", `${kundaliData.lagna.rashi}${kundaliData.lagna.rashi_en ? ` (${kundaliData.lagna.rashi_en})` : ""}`],
                          ["Nakshatra", `${kundaliData.lagna.nakshatra}${kundaliData.lagna.nakshatra_hindi ? ` ${kundaliData.lagna.nakshatra_hindi}` : ""}`],
                          ["Pada", `${kundaliData.lagna.pada ?? "—"}`],
                          ["Degree", `${kundaliData.lagna.degree}°${kundaliData.lagna.full_degree ? ` (${kundaliData.lagna.full_degree.toFixed(2)}° abs)` : ""}`],
                          ["Navamsha Lagna", kundaliData.navamsha_lagna?.rashi ?? "—"],
                          ["Ayanamsha", `${kundaliData.ayanamsha}° (${kundaliData.ayanamsha_label ?? "Lahiri"})`],
                          ["Rahu/Ketu Mode", kundaliData.rahu_mode === "true" ? "True Node" : "Mean Node"],
                          ["Place", kundaliData.place ?? "—"],
                        ].map(([k, v]) => (
                          <div key={k} className="bg-muted/20 rounded-lg p-2">
                            <p className="text-[10px] text-muted-foreground uppercase tracking-wide">{k}</p>
                            <p className="text-foreground font-medium mt-0.5">{v}</p>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Birth Panchang */}
                    {kundaliData.birth_panchang && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">Birth Panchang</p>
                        <div className="grid grid-cols-2 gap-1.5 text-xs">
                          {[
                            ["Tithi", `${kundaliData.birth_panchang.tithi} (${kundaliData.birth_panchang.paksha})`],
                            ["Vara", kundaliData.birth_panchang.vara],
                            ["Nakshatra", kundaliData.birth_panchang.moon_nakshatra ?? kundaliData.birth_panchang.yoga],
                            ["Yoga", kundaliData.birth_panchang.yoga],
                            ["Karana", kundaliData.birth_panchang.karana],
                            ["Sunrise", kundaliData.birth_panchang.sunrise],
                            ["Sunset", kundaliData.birth_panchang.sunset],
                            ["Moon Sign", kundaliData.birth_panchang.moonsign ?? "—"],
                            ["Sun Sign", kundaliData.birth_panchang.sunsign ?? "—"],
                            ["Surya Nak.", kundaliData.birth_panchang.surya_nakshatra ?? "—"],
                          ].map(([k, v]) => (
                            <div key={k} className="flex justify-between items-center py-1 px-2 rounded bg-muted/10 border border-border/10">
                              <span className="text-muted-foreground">{k}</span>
                              <span className="font-medium text-foreground">{v}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Personal characteristics */}
                    {kundaliData.personal && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">Personal Characteristics</p>
                        <div className="grid grid-cols-2 gap-1.5 text-xs">
                          {Object.entries(kundaliData.personal).map(([k, v]) => (
                            <div key={k} className="flex justify-between items-center py-1 px-2 rounded bg-muted/10 border border-border/10">
                              <span className="text-muted-foreground capitalize">{k}</span>
                              <span className="font-medium text-foreground">{v as string}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-10">Generate Kundali to see Lagna details</p>
                )}
              </motion.div>
            )}

            {/* Bhav Chalit Chart */}
            {activeTab === "chalit" && (
              <motion.div key="chalit" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Star className="h-3.5 w-3.5 text-primary" /> Bhav Chalit — Placidus House Cusps (Sidereal)
                </p>
                {kundaliData?.bhav_chalit?.cusps_sid?.length ? (() => {
                  const bc = kundaliData.bhav_chalit!;
                  return (
                    <div className="space-y-3">
                      {/* House cusps table */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">12 House Cusps</p>
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
                                <span className="text-muted-foreground/70">{RASHI_SYMBOLS[rashi] ?? ""}</span>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                      {/* Planet bhav chalit placements */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Planet Placements (Bhav Chalit)</p>
                        <div className="space-y-1">
                          {GRAHA_ORDER.map(gn => {
                            const chHouse = bc.planets[gn];
                            const rashiHouse = kundaliData.grahas[gn]?.house;
                            if (chHouse === undefined) return null;
                            const shifted = chHouse !== rashiHouse;
                            return (
                              <div key={gn} className={`flex items-center gap-2 px-2 py-1.5 rounded-lg text-xs ${shifted ? "bg-amber-500/10 border border-amber-500/20" : "bg-muted/10"}`}>
                                <span className="text-base w-6 text-center" style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                <span className="text-foreground flex-1">{gn} <span className="text-muted-foreground/70">({GRAHA_EN[gn]})</span></span>
                                <span className="text-muted-foreground">Rashi: H{rashiHouse}</span>
                                <span className={`font-semibold ${shifted ? "text-amber-400" : "text-foreground"}`}>Chalit: H{chHouse}</span>
                                {shifted && <span className="text-xs text-amber-400">shifted</span>}
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
          <p className="text-xs text-muted-foreground/60 leading-relaxed pt-3 border-t border-border/15 mt-2">
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
