import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import PageBot from '@/components/PageBot';
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Heart, Star, Loader2, CheckCircle2, AlertTriangle, XCircle,
  ChevronDown, ChevronUp, TrendingUp, TrendingDown, Edit2,
  FileDown, MapPin, Clock, Calendar,
} from "lucide-react";
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer, Tooltip,
} from "recharts";
import { useCompatibility } from "@/hooks/useCompatibility";
import { getCities, searchCities, type City } from "@/lib/cities"; // getCities preloads cache
import type { CompatibilityResponse, GunaScore, DoshaSummary, LifeAreaScore } from "@/types/api";

// ── Varna label map ────────────────────────────────────────────────────────────
const VARNA_LABEL: Record<string, string> = {
  "Brahmin":   "Brahmin (Jnana)",
  "Kshatriya": "Kshatriya (Karma)",
  "Vaishya":   "Vaishya (Artha)",
  "Shudra":    "Shudra (Seva)",
};

// ── Percentage → label ─────────────────────────────────────────────────────────
function pctLabel(score: number, max: number) {
  const p = max > 0 ? score / max : 0;
  if (p === 1) return "Perfect";
  if (p >= 0.75) return "Good";
  if (p >= 0.5)  return "Average";
  if (p > 0)     return "Weak";
  return "None";
}

function barColor(score: number, max: number) {
  const p = max > 0 ? score / max : 0;
  if (p === 1)   return "bg-emerald-500";
  if (p >= 0.6)  return "bg-amber-400";
  if (p > 0)     return "bg-orange-500";
  return "bg-red-500";
}
function textColor(score: number, max: number) {
  const p = max > 0 ? score / max : 0;
  if (p === 1)   return "text-emerald-400";
  if (p >= 0.5)  return "text-amber-400";
  if (p > 0)     return "text-orange-400";
  return "text-red-400";
}

// ── GunaBar ───────────────────────────────────────────────────────────────────
function GunaBar({ guna, delay }: { guna: GunaScore; delay: number }) {
  const [open, setOpen] = useState(false);
  const pct = guna.max > 0 ? guna.score / guna.max : 0;
  return (
    <motion.div
      initial={{ opacity: 0, x: -6 }} animate={{ opacity: 1, x: 0 }} transition={{ delay }}
      className="cosmic-card rounded-lg p-3"
    >
      <div className="flex items-center gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-0.5">
            <span className="text-sm font-medium text-foreground">{guna.name}</span>
            <div className="flex items-center gap-2">
              <span className={`text-[10px] ${textColor(guna.score, guna.max)}`}>{pctLabel(guna.score, guna.max)}</span>
              <span className={`text-xs font-bold ${textColor(guna.score, guna.max)}`}>{guna.score}/{guna.max}</span>
            </div>
          </div>
          <p className="text-[10px] text-muted-foreground mb-1.5">{guna.desc}</p>
          <div className="h-1.5 bg-muted/30 rounded-full overflow-hidden">
            <motion.div
              className={`h-full ${barColor(guna.score, guna.max)} rounded-full`}
              initial={{ width: 0 }} animate={{ width: `${pct * 100}%` }}
              transition={{ delay: delay + 0.1, duration: 0.5, ease: "easeOut" }}
            />
          </div>
          {guna.name === "Varna" && guna.alt_score !== undefined && guna.alt_score !== guna.score && (
            <p className="text-[9px] text-amber-400/80 mt-1">
              Rashi-based: {guna.alt_score}/{guna.max} · Nakshatra-based: {guna.score}/{guna.max}
            </p>
          )}
        </div>
        {guna.interpretation && (
          <button onClick={() => setOpen(!open)} className="shrink-0 text-muted-foreground hover:text-primary transition-colors">
            {open ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>
        )}
      </div>
      <AnimatePresence>
        {open && guna.interpretation && (
          <motion.p
            initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            className="text-[11px] text-muted-foreground leading-relaxed mt-2 pt-2 border-t border-border/20 overflow-hidden"
          >
            {guna.interpretation}
          </motion.p>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ── DoshaCard ──────────────────────────────────────────────────────────────────
function DoshaCard({ dosha, delay }: { dosha: DoshaSummary; delay: number }) {
  const [open, setOpen] = useState(false);
  const active = dosha.present;
  const high = active && dosha.severity === "High";
  const containerCls = high ? "bg-red-500/10 border-red-500/25" : active ? "bg-amber-500/10 border-amber-500/25" : "bg-emerald-500/10 border-emerald-500/25";
  const badgeCls = high ? "bg-red-500/20 text-red-400" : active ? "bg-amber-500/20 text-amber-400" : "bg-emerald-500/20 text-emerald-400";
  const Icon = high ? XCircle : active ? AlertTriangle : CheckCircle2;
  const iconCls = high ? "text-red-400" : active ? "text-amber-400" : "text-emerald-400";
  return (
    <motion.div
      initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} transition={{ delay }}
      className={`rounded-lg border p-3 space-y-1.5 ${containerCls}`}
    >
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <Icon className={`h-3.5 w-3.5 shrink-0 ${iconCls}`} />
          <span className="text-sm font-medium text-foreground">{dosha.name}</span>
        </div>
        <div className="flex items-center gap-2">
          <span className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${badgeCls}`}>
            {active ? dosha.severity : "None"}
          </span>
          <button onClick={() => setOpen(!open)} className="text-muted-foreground hover:text-foreground transition-colors">
            {open ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>
        </div>
      </div>
      <AnimatePresence>
        {open && (
          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} exit={{ opacity: 0, height: 0 }} className="space-y-1 overflow-hidden">
            <p className="text-[11px] text-muted-foreground leading-relaxed">{dosha.note}</p>
            {dosha.cancellation && <p className="text-[10px] text-emerald-400/90 leading-relaxed">✓ {dosha.cancellation}</p>}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

// ── LifeAreaBar ────────────────────────────────────────────────────────────────
function LifeAreaBar({ area, delay }: { area: LifeAreaScore; delay: number }) {
  const lc = area.label === "Excellent" ? "text-emerald-400" : area.label === "Good" ? "text-amber-400" : area.label === "Average" ? "text-orange-400" : "text-red-400";
  const bc = area.label === "Excellent" ? "bg-emerald-500" : area.label === "Good" ? "bg-amber-400" : area.label === "Average" ? "bg-orange-500" : "bg-red-500";
  return (
    <motion.div initial={{ opacity: 0, x: -4 }} animate={{ opacity: 1, x: 0 }} transition={{ delay }} className="flex items-center gap-3">
      <span className="text-base w-6 text-center shrink-0">{area.icon}</span>
      <div className="flex-1 min-w-0">
        <div className="flex justify-between mb-0.5">
          <span className="text-[11px] font-medium text-foreground">{area.area}</span>
          <span className={`text-[10px] font-medium ${lc}`}>{area.label}</span>
        </div>
        <div className="h-1.5 bg-muted/30 rounded-full overflow-hidden">
          <motion.div className={`h-full ${bc} rounded-full`} initial={{ width: 0 }} animate={{ width: `${area.score}%` }} transition={{ delay: delay + 0.1, duration: 0.5, ease: "easeOut" }} />
        </div>
      </div>
      <span className="text-[10px] text-muted-foreground w-7 text-right shrink-0">{area.score}%</span>
    </motion.div>
  );
}

// ── ProfileRow ─────────────────────────────────────────────────────────────────
function ProfileRow({ label, valA, valB, highlight }: { label: string; valA: string; valB: string; highlight?: boolean }) {
  return (
    <div className={`grid grid-cols-3 gap-1 py-1.5 border-b border-border/10 last:border-0 ${highlight ? "text-primary" : ""}`}>
      <span className="text-[10px] text-muted-foreground">{label}</span>
      <span className="text-[10px] text-foreground font-medium text-center">{valA}</span>
      <span className="text-[10px] text-foreground font-medium text-center">{valB}</span>
    </div>
  );
}

// ── Print/PDF ─────────────────────────────────────────────────────────────────
type PersonState = { name: string; dob: string; time: string; place: string };

function generatePDF(result: CompatibilityResponse, nameA: string, nameB: string, pA: PersonState, pB: PersonState) {
  const pct = result.percentage;
  const verdictColor = pct >= 72 ? "#10b981" : pct >= 55 ? "#f59e0b" : pct >= 36 ? "#f97316" : "#ef4444";

  const gunaRows = result.gunas.map(g => {
    const p = g.max > 0 ? Math.round((g.score / g.max) * 100) : 0;
    const color = p === 100 ? "#10b981" : p >= 60 ? "#f59e0b" : p > 0 ? "#f97316" : "#ef4444";
    return `<tr>
      <td>${g.name}</td><td style="text-align:center">${g.max}</td>
      <td style="text-align:center;color:${color};font-weight:600">${g.score}</td>
      <td style="text-align:center">${g.desc.split("—")[0].trim()}</td>
    </tr>`;
  }).join("");

  const areaRows = result.life_areas.map(a => {
    const color = a.label === "Excellent" ? "#10b981" : a.label === "Good" ? "#f59e0b" : a.label === "Average" ? "#f97316" : "#ef4444";
    return `<tr><td>${a.icon} ${a.area}</td><td style="color:${color};font-weight:600">${a.label}</td><td>${a.score}%</td></tr>`;
  }).join("");

  const doshaRows = result.dosha_summary.map(d => {
    const color = d.present ? (d.severity === "High" ? "#ef4444" : "#f59e0b") : "#10b981";
    return `<tr><td>${d.name}</td><td style="color:${color};font-weight:600">${d.present ? d.severity : "None"}</td><td style="font-size:11px;max-width:300px">${d.note.substring(0, 120)}...</td></tr>`;
  }).join("");

  const strengths = result.strengths.map(s => `<li>✓ ${s}</li>`).join("");
  const challenges = result.challenges.map(c => `<li>▲ ${c}</li>`).join("");

  const html = `<!DOCTYPE html><html><head><meta charset="UTF-8">
<title>Kundali Milan Report — ${nameA} &amp; ${nameB}</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: Georgia, 'Times New Roman', serif; background: #fff; color: #1a1a2e; font-size: 13px; line-height: 1.5; }
  .page { width: 210mm; min-height: 297mm; margin: 0 auto; padding: 18mm 16mm; }
  h1 { font-size: 28px; color: #8b5cf6; text-align: center; letter-spacing: 1px; }
  h2 { font-size: 15px; color: #6d28d9; border-bottom: 2px solid #ede9fe; padding-bottom: 4px; margin: 18px 0 10px; letter-spacing: 0.5px; }
  h3 { font-size: 12px; color: #7c3aed; margin: 12px 0 6px; }
  .subtitle { text-align: center; color: #6b7280; font-size: 12px; margin-top: 4px; }
  .header-bar { background: linear-gradient(135deg, #1e1b4b, #312e81); color: white; padding: 20px 24px; border-radius: 12px; text-align: center; margin-bottom: 20px; }
  .header-bar h1 { color: #fbbf24; font-size: 24px; margin-bottom: 6px; }
  .header-bar .subtitle { color: #c4b5fd; font-size: 12px; }
  .score-row { display: flex; justify-content: center; align-items: center; gap: 24px; margin: 14px 0; }
  .score-circle { width: 90px; height: 90px; border-radius: 50%; border: 6px solid #fbbf24; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #1e1b4b; }
  .score-num { font-size: 22px; color: #fbbf24; font-weight: bold; }
  .score-sub { font-size: 9px; color: #a5b4fc; }
  .verdict-box { text-align: center; }
  .verdict { font-size: 18px; font-weight: bold; }
  .verdict-detail { font-size: 11px; color: #4b5563; max-width: 350px; margin: 6px auto 0; }
  table { width: 100%; border-collapse: collapse; font-size: 12px; }
  th { background: #ede9fe; color: #4c1d95; padding: 6px 8px; text-align: left; font-size: 11px; font-weight: 600; }
  td { padding: 5px 8px; border-bottom: 1px solid #f3f4f6; vertical-align: top; }
  tr:nth-child(even) td { background: #fafafa; }
  .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
  .card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 12px; }
  .card-title { font-size: 11px; font-weight: 600; color: #6d28d9; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }
  ul { padding-left: 0; list-style: none; }
  li { padding: 3px 0; font-size: 11px; border-bottom: 1px solid #f3f4f6; }
  li:last-child { border: none; }
  .profile-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 0; }
  .profile-grid .ph { background: #ede9fe; color: #4c1d95; padding: 6px 8px; font-size: 11px; font-weight: 600; }
  .profile-grid .pc { padding: 5px 8px; font-size: 11px; border-bottom: 1px solid #f3f4f6; }
  .mangal-row { display: flex; gap: 12px; justify-content: center; margin: 10px 0; }
  .badge { padding: 4px 10px; border-radius: 20px; font-size: 10px; font-weight: 600; }
  .badge-ok { background: #d1fae5; color: #065f46; }
  .badge-warn { background: #fee2e2; color: #991b1b; }
  .footer { margin-top: 20px; padding-top: 12px; border-top: 1px solid #e5e7eb; font-size: 10px; color: #9ca3af; text-align: center; }
  .rajju-card { border: 1px solid; border-radius: 6px; padding: 10px; margin-bottom: 8px; }
  .rajju-high { border-color: #fca5a5; background: #fff1f2; }
  .rajju-ok { border-color: #86efac; background: #f0fdf4; }
  @media print {
    body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    .page { padding: 12mm; }
  }
</style>
</head><body><div class="page">

<div class="header-bar">
  <h1>☽ Kundali Milan Report</h1>
  <div class="subtitle">Ashtakoot 36-Guna Analysis · Vedic Astrology</div>
  <div style="margin-top:10px;font-size:13px;color:#e9d5ff">
    <strong style="color:#fbbf24">${nameA}</strong>
    <span style="color:#c4b5fd"> &amp; </span>
    <strong style="color:#fbbf24">${nameB}</strong>
  </div>
</div>

<!-- Score -->
<div class="score-row">
  <div class="score-circle">
    <span class="score-num">${pct}%</span>
    <span class="score-sub">${result.total_score}/${result.max_score}</span>
  </div>
  <div class="verdict-box">
    <div class="verdict" style="color:${verdictColor}">${result.verdict}</div>
    <div class="verdict-detail">${result.verdict_detail}</div>
    <div class="mangal-row">
      <span class="badge ${result.mangal_dosha.person_a ? "badge-warn" : "badge-ok"}">${nameA}: Mangal ${result.mangal_dosha.person_a ? "Present" : "Absent"}</span>
      <span class="badge ${result.mangal_dosha.person_b ? "badge-warn" : "badge-ok"}">${nameB}: Mangal ${result.mangal_dosha.person_b ? "Present" : "Absent"}</span>
    </div>
  </div>
</div>

<!-- Birth Details -->
<h2>Birth Details</h2>
<div class="two-col">
  <div class="card">
    <div class="card-title">${nameA}</div>
    <table><tbody>
      <tr><td style="color:#6b7280">Date</td><td>${pA.dob}</td></tr>
      <tr><td style="color:#6b7280">Time</td><td>${pA.time || "—"}</td></tr>
      <tr><td style="color:#6b7280">Place</td><td>${pA.place || "—"}</td></tr>
    </tbody></table>
  </div>
  <div class="card">
    <div class="card-title">${nameB}</div>
    <table><tbody>
      <tr><td style="color:#6b7280">Date</td><td>${pB.dob}</td></tr>
      <tr><td style="color:#6b7280">Time</td><td>${pB.time || "—"}</td></tr>
      <tr><td style="color:#6b7280">Place</td><td>${pB.place || "—"}</td></tr>
    </tbody></table>
  </div>
</div>

<!-- Astro Profiles -->
<h2>Astrological Profiles</h2>
<div class="profile-grid">
  <div class="ph">Attribute</div><div class="ph" style="text-align:center">${nameA}</div><div class="ph" style="text-align:center">${nameB}</div>
  ${[
    ["Nakshatra", result.nakshatra_a, result.nakshatra_b],
    ["Rashi", result.rashi_a, result.rashi_b],
    ["Gana", result.gana_a, result.gana_b],
    ["Nadi", result.nadi_a.split(" ")[0], result.nadi_b.split(" ")[0]],
    ["Varna", result.varna_a, result.varna_b],
    ["Yoni", result.yoni_a, result.yoni_b],
  ].map(([l, a, b]) => `<div class="pc" style="color:#6b7280">${l}</div><div class="pc" style="text-align:center">${a}</div><div class="pc" style="text-align:center">${b}</div>`).join("")}
</div>

<!-- 8 Kutas -->
<h2>Ashtakoot — 8 Kuta Scores</h2>
<table><thead><tr><th>Kuta</th><th style="text-align:center">Max</th><th style="text-align:center">Score</th><th>Significance</th></tr></thead>
<tbody>${gunaRows}</tbody>
<tfoot><tr style="background:#ede9fe"><td><strong>Total</strong></td><td style="text-align:center"><strong>36</strong></td><td style="text-align:center;font-weight:bold;color:${verdictColor}">${result.total_score}</td><td><strong>${result.verdict}</strong></td></tr></tfoot>
</table>

<!-- Life Areas -->
<h2>Life Area Compatibility</h2>
<table><thead><tr><th>Area</th><th>Rating</th><th>Score</th></tr></thead>
<tbody>${areaRows}</tbody></table>

<!-- Rajju & Vedha -->
<h2>Rajju &amp; Vedha Dosha</h2>
<div class="two-col">
  <div class="rajju-card ${result.rajju_dosha?.present ? "rajju-high" : "rajju-ok"}">
    <div style="font-weight:600;font-size:12px;margin-bottom:4px">${result.rajju_dosha?.present ? "⚠ Rajju Dosha — " + result.rajju_dosha.severity : "✓ No Rajju Dosha"}</div>
    <div style="font-size:10px;color:#374151">${result.rajju_dosha?.note?.substring(0, 200) ?? ""}...</div>
  </div>
  <div class="rajju-card ${result.vedha_dosha?.present ? "rajju-high" : "rajju-ok"}">
    <div style="font-weight:600;font-size:12px;margin-bottom:4px">${result.vedha_dosha?.present ? "⚠ Vedha Dosha Present" : "✓ No Vedha Dosha"}</div>
    <div style="font-size:10px;color:#374151">${result.vedha_dosha?.note ?? ""}</div>
  </div>
</div>

<!-- Doshas -->
<h2>Dosha Analysis</h2>
<table><thead><tr><th>Dosha</th><th>Severity</th><th>Note</th></tr></thead>
<tbody>${doshaRows}</tbody></table>

<!-- Strengths & Challenges -->
<h2>Strengths &amp; Challenges</h2>
<div class="two-col">
  <div class="card">
    <div class="card-title" style="color:#065f46">✓ Strengths</div>
    <ul>${strengths}</ul>
  </div>
  <div class="card">
    <div class="card-title" style="color:#92400e">▲ Challenges</div>
    <ul>${challenges}</ul>
  </div>
</div>

<div class="footer">
  Generated by Brahm AI · Lahiri Ayanamsha · pyswisseph DE431 · ${new Date().toLocaleDateString("en-IN", { day: "2-digit", month: "long", year: "numeric" })}<br>
  Ashtakoot Milan is one factor in Vedic compatibility — consult a qualified Jyotishi for complete guidance.
</div>

</div></body></html>`;

  const win = window.open("", "_blank");
  if (!win) return;
  win.document.write(html);
  win.document.close();
  setTimeout(() => win.print(), 800);
}

// ── ResultView ─────────────────────────────────────────────────────────────────
function ResultView({ result, nameA, nameB, personA, personB, onEdit }: {
  result: CompatibilityResponse;
  nameA: string; nameB: string;
  personA: PersonState; personB: PersonState;
  onEdit: () => void;
}) {
  const pct = result.percentage;
  const verdictColor = pct >= 72 ? "text-emerald-400" : pct >= 55 ? "text-amber-400" : pct >= 36 ? "text-orange-400" : "text-red-400";
  const ringColor = pct >= 72 ? "hsl(142 70% 50%)" : pct >= 55 ? "hsl(42 90% 64%)" : pct >= 36 ? "hsl(25 85% 55%)" : "hsl(0 72% 60%)";

  const radarData = result.gunas.map(g => ({
    kuta: g.name,
    score: g.max > 0 ? Math.round((g.score / g.max) * 100) : 0,
  }));

  const activeDoshas   = result.dosha_summary.filter(d => d.present);
  const inactiveDoshas = result.dosha_summary.filter(d => !d.present);

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="w-full">
      {/* ── Topbar: names + actions ── */}
      <div className="flex items-center justify-between mb-5 flex-wrap gap-3">
        <div>
          <h2 className="font-display text-xl text-foreground">{nameA} <span className="text-primary">×</span> {nameB}</h2>
          <p className="text-[11px] text-muted-foreground">Kundali Milan Report</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onEdit}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-muted/40 hover:bg-muted/60 text-muted-foreground hover:text-foreground transition-colors"
          >
            <Edit2 className="h-3 w-3" /> Edit Details
          </button>
          <button
            onClick={() => generatePDF(result, nameA, nameB, personA, personB)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-primary/20 hover:bg-primary/30 text-primary transition-colors"
          >
            <FileDown className="h-3 w-3" /> Download PDF
          </button>
        </div>
      </div>

      {/* ── Two-column grid ── */}
      <div className="grid lg:grid-cols-5 gap-5">

        {/* ── LEFT: Score + Radar + Life Areas + Gunas ── */}
        <div className="lg:col-span-3 space-y-4">

          {/* Score circle */}
          <div className="cosmic-card rounded-2xl p-5 flex flex-col sm:flex-row items-center gap-5">
            <div className="relative w-28 h-28 shrink-0">
              <svg viewBox="0 0 100 100" className="w-full h-full -rotate-90">
                <circle cx="50" cy="50" r="42" fill="none" stroke="hsl(225 25% 18%)" strokeWidth="6" />
                <motion.circle
                  cx="50" cy="50" r="42" fill="none"
                  stroke={ringColor} strokeWidth="6" strokeLinecap="round"
                  strokeDasharray={264}
                  initial={{ strokeDashoffset: 264 }}
                  animate={{ strokeDashoffset: 264 - (264 * pct) / 100 }}
                  transition={{ duration: 1.4, ease: "easeOut" }}
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="font-display text-3xl text-primary">{pct}%</span>
                <span className="text-[9px] text-muted-foreground">{result.total_score}/36</span>
              </div>
            </div>
            <div className="flex-1 text-center sm:text-left">
              <p className={`text-lg font-bold ${verdictColor}`}>{result.verdict}</p>
              <p className="text-[11px] text-muted-foreground mt-1 leading-relaxed">{result.verdict_detail}</p>
              <div className="flex flex-wrap gap-2 mt-3 justify-center sm:justify-start">
                <span className={`text-[10px] px-2.5 py-1 rounded-full font-medium border ${result.mangal_dosha.person_a ? "bg-red-500/15 text-red-400 border-red-500/20" : "bg-emerald-500/15 text-emerald-400 border-emerald-500/20"}`}>
                  {nameA}: Mangal {result.mangal_dosha.person_a ? "Present" : "Absent"}
                </span>
                <span className={`text-[10px] px-2.5 py-1 rounded-full font-medium border ${result.mangal_dosha.person_b ? "bg-red-500/15 text-red-400 border-red-500/20" : "bg-emerald-500/15 text-emerald-400 border-emerald-500/20"}`}>
                  {nameB}: Mangal {result.mangal_dosha.person_b ? "Present" : "Absent"}
                </span>
              </div>
            </div>
          </div>

          {/* Radar chart */}
          <div className="cosmic-card rounded-xl p-4">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">8-Kuta Radar</h3>
            <ResponsiveContainer width="100%" height={220}>
              <RadarChart data={radarData} margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
                <PolarGrid stroke="hsl(225 25% 22%)" />
                <PolarAngleAxis
                  dataKey="kuta"
                  tick={{ fontSize: 10, fill: "hsl(225 15% 60%)" }}
                />
                <Radar
                  dataKey="score" name="Score"
                  stroke="hsl(42 90% 64%)" fill="hsl(42 90% 64%)" fillOpacity={0.22}
                  strokeWidth={2}
                />
                <Tooltip
                  contentStyle={{ background: "hsl(225 25% 10%)", border: "1px solid hsl(225 25% 25%)", borderRadius: 8, fontSize: 11 }}
                  formatter={(v: number) => [`${v}%`, "Score"]}
                />
              </RadarChart>
            </ResponsiveContainer>
          </div>

          {/* Life Areas */}
          {result.life_areas?.length > 0 && (
            <div className="cosmic-card rounded-xl p-4">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-4">Life Area Compatibility</h3>
              <div className="space-y-3">
                {result.life_areas.map((a, i) => <LifeAreaBar key={a.area} area={a} delay={0.04 * i} />)}
              </div>
            </div>
          )}

          {/* Guna bars */}
          <div>
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">Ashtakoot — 8 Kuta Analysis</h3>
            <p className="text-[10px] text-muted-foreground mb-2">Tap ↓ on any row for couple-specific interpretation.</p>
            <div className="space-y-2">
              {result.gunas.map((g, i) => <GunaBar key={g.name} guna={g} delay={0.04 * i} />)}
            </div>
          </div>
        </div>

        {/* ── RIGHT: Birth Details + Profiles + Rajju/Vedha + Doshas + Strengths ── */}
        <div className="lg:col-span-2 space-y-4">

          {/* Birth details */}
          <div className="cosmic-card rounded-xl p-4">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Birth Details</h3>
            {[{ label: nameA, p: personA }, { label: nameB, p: personB }].map(({ label, p }) => (
              <div key={label} className="mb-3 last:mb-0">
                <p className="text-[10px] text-primary font-semibold mb-1.5">{label}</p>
                <div className="space-y-1">
                  {p.dob && (
                    <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
                      <Calendar className="h-3 w-3 shrink-0" />
                      <span>{p.dob}</span>
                    </div>
                  )}
                  {p.time && (
                    <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
                      <Clock className="h-3 w-3 shrink-0" />
                      <span>{p.time}</span>
                    </div>
                  )}
                  {p.place && (
                    <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
                      <MapPin className="h-3 w-3 shrink-0" />
                      <span>{p.place}</span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Astro profiles */}
          <div className="cosmic-card rounded-xl p-4">
            <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-3">Astrological Profiles</h3>
            <div className="grid grid-cols-3 gap-1 mb-1.5">
              <span className="text-[9px] text-muted-foreground" />
              <span className="text-[9px] text-primary text-center font-medium">{nameA}</span>
              <span className="text-[9px] text-primary text-center font-medium">{nameB}</span>
            </div>
            <ProfileRow label="Nakshatra" valA={result.nakshatra_a} valB={result.nakshatra_b} />
            <ProfileRow label="Rashi" valA={result.rashi_a} valB={result.rashi_b} />
            <ProfileRow label="Gana" valA={result.gana_a} valB={result.gana_b}
              highlight={result.gana_a !== result.gana_b && (result.gana_a === "Rakshasa" || result.gana_b === "Rakshasa")} />
            <ProfileRow label="Nadi" valA={result.nadi_a.split(" ")[0]} valB={result.nadi_b.split(" ")[0]}
              highlight={result.nadi_a === result.nadi_b} />
            <ProfileRow label="Varna" valA={VARNA_LABEL[result.varna_a] ?? result.varna_a} valB={VARNA_LABEL[result.varna_b] ?? result.varna_b} />
            {result.yoni_a && <ProfileRow label="Yoni" valA={result.yoni_a} valB={result.yoni_b} />}
            <p className="text-[9px] text-muted-foreground/50 pt-1 leading-relaxed">
              * Varna = spiritual temperament by Moon nakshatra — not caste.
            </p>
          </div>

          {/* Rajju & Vedha */}
          {(result.rajju_dosha || result.vedha_dosha) && (
            <div className="cosmic-card rounded-xl p-4 space-y-3">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Rajju &amp; Vedha</h3>
              {result.rajju_dosha && (
                <div className={`rounded-lg border p-2.5 ${result.rajju_dosha.present ? "bg-red-500/10 border-red-500/25" : "bg-emerald-500/10 border-emerald-500/25"}`}>
                  <div className="flex items-center justify-between mb-1.5">
                    <div className="flex items-center gap-1.5">
                      {result.rajju_dosha.present ? <XCircle className="h-3 w-3 text-red-400" /> : <CheckCircle2 className="h-3 w-3 text-emerald-400" />}
                      <span className="text-xs font-medium text-foreground">Rajju Dosha</span>
                    </div>
                    <span className={`text-[9px] px-1.5 py-0.5 rounded-full ${result.rajju_dosha.present ? "bg-red-500/20 text-red-400" : "bg-emerald-500/20 text-emerald-400"}`}>
                      {result.rajju_dosha.present ? result.rajju_dosha.severity : "None"}
                    </span>
                  </div>
                  <div className="flex gap-3 mb-1 text-[10px] text-muted-foreground">
                    <span>A: <span className="text-foreground">{result.rajju_dosha.rajju_a}</span></span>
                    <span>B: <span className="text-foreground">{result.rajju_dosha.rajju_b}</span></span>
                  </div>
                  <p className="text-[10px] text-muted-foreground leading-relaxed line-clamp-3">{result.rajju_dosha.note}</p>
                </div>
              )}
              {result.vedha_dosha && (
                <div className={`rounded-lg border p-2.5 ${result.vedha_dosha.present ? "bg-amber-500/10 border-amber-500/25" : "bg-emerald-500/10 border-emerald-500/25"}`}>
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-1.5">
                      {result.vedha_dosha.present ? <AlertTriangle className="h-3 w-3 text-amber-400" /> : <CheckCircle2 className="h-3 w-3 text-emerald-400" />}
                      <span className="text-xs font-medium text-foreground">Vedha Dosha</span>
                    </div>
                    <span className={`text-[9px] px-1.5 py-0.5 rounded-full ${result.vedha_dosha.present ? "bg-amber-500/20 text-amber-400" : "bg-emerald-500/20 text-emerald-400"}`}>
                      {result.vedha_dosha.present ? "Present" : "None"}
                    </span>
                  </div>
                  <p className="text-[10px] text-muted-foreground leading-relaxed">{result.vedha_dosha.note}</p>
                </div>
              )}
            </div>
          )}

          {/* Doshas */}
          {result.dosha_summary.length > 0 && (
            <div>
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2">Dosha Analysis</h3>
              <div className="space-y-2">
                {activeDoshas.map((d, i) => <DoshaCard key={d.name} dosha={d} delay={0.04 * i} />)}
                {inactiveDoshas.map((d, i) => <DoshaCard key={d.name} dosha={d} delay={0.04 * (activeDoshas.length + i)} />)}
              </div>
            </div>
          )}

          {/* Strengths & Challenges */}
          {(result.strengths?.length > 0 || result.challenges?.length > 0) && (
            <div className="space-y-3">
              {result.strengths?.length > 0 && (
                <div className="cosmic-card rounded-xl p-4">
                  <div className="flex items-center gap-2 mb-3">
                    <TrendingUp className="h-3.5 w-3.5 text-emerald-400" />
                    <h3 className="text-xs font-semibold text-emerald-400 uppercase tracking-wide">Strengths</h3>
                  </div>
                  <ul className="space-y-2">
                    {result.strengths.map((s, i) => (
                      <motion.li key={i} initial={{ opacity: 0, x: -4 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.04 * i }} className="flex items-start gap-2">
                        <span className="text-emerald-400 shrink-0 text-xs mt-0.5">✓</span>
                        <span className="text-[11px] text-muted-foreground leading-relaxed">{s}</span>
                      </motion.li>
                    ))}
                  </ul>
                </div>
              )}
              {result.challenges?.length > 0 && (
                <div className="cosmic-card rounded-xl p-4">
                  <div className="flex items-center gap-2 mb-3">
                    <TrendingDown className="h-3.5 w-3.5 text-amber-400" />
                    <h3 className="text-xs font-semibold text-amber-400 uppercase tracking-wide">Challenges</h3>
                  </div>
                  <ul className="space-y-2">
                    {result.challenges.map((c, i) => (
                      <motion.li key={i} initial={{ opacity: 0, x: -4 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.04 * i }} className="flex items-start gap-2">
                        <span className="text-amber-400 shrink-0 text-xs mt-0.5">▲</span>
                        <span className="text-[11px] text-muted-foreground leading-relaxed">{c}</span>
                      </motion.li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          <p className="text-[9px] text-muted-foreground/40 leading-relaxed pb-2">
            Lahiri Ayanamsha · pyswisseph DE431 · Ashtakoot is one of many factors — consult a qualified Jyotishi.
          </p>
        </div>
      </div>
    </motion.div>
  );
}

// ── PersonForm ─────────────────────────────────────────────────────────────────
function PersonForm({ label, person, selectedCity, onChange, onCitySelect }: {
  label: string;
  person: PersonState;
  selectedCity: City | null;
  onChange: (p: PersonState) => void;
  onCitySelect: (city: City) => void;
}) {
  const [suggestions, setSuggestions] = useState<City[]>([]);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const h = (e: MouseEvent) => { if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) setSuggestions([]); };
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);

  const handleCityInput = async (v: string) => {
    onChange({ ...person, place: v });
    const results = await searchCities(v);
    setSuggestions(results);
  };

  const handleSelect = (city: City) => { onChange({ ...person, place: city.name }); onCitySelect(city); setSuggestions([]); };

  return (
    <div className="cosmic-card rounded-xl p-5 space-y-3">
      <div className="flex items-center gap-2 mb-1">
        <Star className="h-4 w-4 text-primary" />
        <span className="font-display text-sm text-foreground">{label}</span>
      </div>
      <div>
        <Label className="text-xs text-muted-foreground">Name</Label>
        <Input value={person.name} onChange={e => onChange({ ...person, name: e.target.value })} placeholder="Full name" className="bg-muted/20 border-border/30 mt-1" />
      </div>
      <div>
        <Label className="text-xs text-muted-foreground">Date of Birth</Label>
        <Input type="date" value={person.dob} onChange={e => onChange({ ...person, dob: e.target.value })} className="bg-muted/20 border-border/30 mt-1" />
      </div>
      <div>
        <Label className="text-xs text-muted-foreground">Time of Birth</Label>
        <Input type="time" value={person.time} onChange={e => onChange({ ...person, time: e.target.value })} className="bg-muted/20 border-border/30 mt-1" />
      </div>
      <div ref={wrapperRef} className="relative">
        <Label className="text-xs text-muted-foreground">Birth Place</Label>
        <div className="relative mt-1">
          <Input value={person.place} onChange={e => handleCityInput(e.target.value)} placeholder="Search city..." autoComplete="off"
            className={`bg-muted/20 border-border/30 ${selectedCity ? "border-primary/50" : ""}`} />
          {selectedCity && (
            <span className="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] text-primary/70">
              ✓ {selectedCity.lat.toFixed(1)}, {selectedCity.lon.toFixed(1)}
            </span>
          )}
        </div>
        {suggestions.length > 0 && (
          <div className="absolute z-50 w-full mt-1 rounded-lg border border-border/30 bg-background shadow-lg overflow-hidden">
            {suggestions.map(city => (
              <button key={city.name} onMouseDown={() => handleSelect(city)}
                className="w-full text-left px-3 py-2 text-xs hover:bg-muted/40 transition-colors border-b border-border/10 last:border-0">
                <span className="font-medium">{city.name}</span>
                <span className="text-muted-foreground ml-2">{city.lat.toFixed(1)}°, {city.lon.toFixed(1)}°</span>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function CompatibilityPage() {
  const [personA, setPersonA] = useState<PersonState>({ name: "", dob: "", time: "", place: "" });
  const [personB, setPersonB] = useState<PersonState>({ name: "", dob: "", time: "", place: "" });
  const [cityA, setCityA] = useState<City | null>(null);
  const [cityB, setCityB] = useState<City | null>(null);
  const [varnaSystem, setVarnaSystem] = useState<"both" | "nakshatra" | "rashi">("both");
  const [result, setResult] = useState<CompatibilityResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const compatibility = useCompatibility();

  useEffect(() => { getCities().catch(() => {}); }, []); // preload cache

  const handleGenerate = async () => {
    if (!personA.dob || !personB.dob) return;
    setError(null);
    const DEF = { lat: 28.6139, lon: 77.209, tz: 5.5 };
    const cA = cityA ?? DEF; const cB = cityB ?? DEF;
    try {
      const res = await compatibility.mutateAsync({
        person_a: { name: personA.name, date: personA.dob, time: personA.time || "12:00", lat: cA.lat, lon: cA.lon, tz: cA.tz },
        person_b: { name: personB.name, date: personB.dob, time: personB.time || "12:00", lat: cB.lat, lon: cB.lon, tz: cB.tz },
        varna_system: varnaSystem,
      });
      setResult(res);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Calculation failed. Check birth details.");
    }
  };

  const nameA = personA.name || "Person A";
  const nameB = personB.name || "Person B";

  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">Kundali Milan</h1>
        <p className="text-sm text-muted-foreground">
          Ashtakoot 36-Guna matching · Life-area analysis · Radar visualization · PDF report
        </p>
      </motion.div>

      {result ? (
        <ResultView
          result={result} nameA={nameA} nameB={nameB}
          personA={personA} personB={personB}
          onEdit={() => setResult(null)}
        />
      ) : (
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="max-w-2xl">
          <div className="grid md:grid-cols-2 gap-5">
            <PersonForm label="Person A (Groom / Partner 1)" person={personA} selectedCity={cityA}
              onChange={p => { setPersonA(p); if (!p.place) setCityA(null); }} onCitySelect={setCityA} />
            <PersonForm label="Person B (Bride / Partner 2)" person={personB} selectedCity={cityB}
              onChange={p => { setPersonB(p); if (!p.place) setCityB(null); }} onCitySelect={setCityB} />
          </div>

          <div className="mt-4 cosmic-card rounded-xl p-3 flex flex-col gap-2">
            <p className="text-[11px] text-muted-foreground font-medium uppercase tracking-wide">Varna System</p>
            <div className="flex gap-2 flex-wrap">
              {(["both", "nakshatra", "rashi"] as const).map(sys => (
                <button key={sys} onClick={() => setVarnaSystem(sys)}
                  className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${varnaSystem === sys ? "bg-primary text-primary-foreground" : "bg-muted/30 text-muted-foreground hover:text-foreground"}`}>
                  {sys === "both" ? "Both" : sys === "nakshatra" ? "Nakshatra (Classical)" : "Rashi (Drik Ganita)"}
                </button>
              ))}
            </div>
            <p className="text-[10px] text-muted-foreground">
              {varnaSystem === "nakshatra" && "Parashari — Varna by birth Nakshatra"}
              {varnaSystem === "rashi" && "Modern Drik Ganita — Varna by Moon sign element"}
              {varnaSystem === "both" && "Shows Nakshatra score; Rashi alternative shown if different"}
            </p>
          </div>

          {error && <p className="mt-3 text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{error}</p>}

          <button
            onClick={handleGenerate}
            disabled={!personA.dob || !personB.dob || compatibility.isPending}
            className="mt-5 w-full py-3 rounded-xl font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-30"
          >
            {compatibility.isPending
              ? <><Loader2 className="h-4 w-4 animate-spin" /> Calculating Kundali Milan…</>
              : <><Heart className="h-4 w-4" /> Calculate Compatibility</>}
          </button>

          <p className="mt-3 text-[10px] text-muted-foreground text-center">
            Lahiri Ayanamsha · Moon Nakshatra · pyswisseph · Rajju &amp; Vedha included · PDF export
          </p>
        </motion.div>
      )}
      <PageBot pageContext="compatibility" pageData={result ?? {}} />
    </div>
  );
}
