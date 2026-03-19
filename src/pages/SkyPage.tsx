/**
 * SkyPage — Live planetary sky viewer
 *
 * Tab 1 · Live Sky     — zodiac wheel + per-second Jyotish table + visibility
 * Tab 2 · 24h Movement — full-day planet track, midnight reset
 * Tab 3 · Today for You — personalized transit forecast from natal chart
 */

import { useState, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Globe, Clock, User, Eye, EyeOff, RefreshCw, TrendingDown } from "lucide-react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  useLivePlanets, use24hTrack,
  RASHI_INDEX, PLANET_SPEED_PER_DAY, toDMS, angularDiff,
  type LivePlanetData, type LivePlanetsSnapshot,
} from "@/hooks/useLivePlanets";
import { useKundliStore } from "@/store/kundliStore";

// ── Planet meta ───────────────────────────────────────────────────────────────

const GRAHA_SYMBOL: Record<string, string> = {
  Surya: "☉", Chandra: "☽", Mangal: "♂", Budh: "☿",
  Guru: "♃",  Shukra: "♀",  Shani: "♄", Rahu: "☊", Ketu: "☋",
};
const GRAHA_COLOR: Record<string, string> = {
  Surya:   "#FFB347", Chandra: "#C0D8FF", Mangal: "#FF6B6B",
  Budh:    "#7FD17F", Guru:    "#FFD700",  Shukra: "#D89FFF",
  Shani:   "#778899", Rahu:    "#6A8FD0",  Ketu:   "#C87941",
};
const GRAHA_NAME_HI: Record<string, string> = {
  Surya: "सूर्य", Chandra: "चंद्र", Mangal: "मंगल", Budh: "बुध",
  Guru: "गुरु",   Shukra: "शुक्र",  Shani: "शनि",  Rahu: "राहु", Ketu: "केतु",
};

const RASHI_SHORT = ["♈","♉","♊","♋","♌","♍","♎","♏","♐","♑","♒","♓"];
const RASHI_NAMES = [
  "Mesha","Vrishabha","Mithuna","Karka",
  "Simha","Kanya","Tula","Vrischika",
  "Dhanu","Makara","Kumbha","Meena",
];

const ORDER = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatTime(date: Date): string {
  return date.toLocaleTimeString("en-IN", { hour12: false, hour: "2-digit", minute: "2-digit", second: "2-digit" });
}

function fmtCountdown(secs: number): string {
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  return `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}:${String(s).padStart(2,"0")}`;
}

// ── Zodiac Wheel SVG ──────────────────────────────────────────────────────────

function ZodiacWheel({ snapshot }: { snapshot: LivePlanetsSnapshot }) {
  const [hovered, setHovered] = useState<string | null>(null);
  const cx = 200, cy = 200, R = 180;
  const planetR = 130;

  return (
    <div className="relative w-full max-w-sm mx-auto aspect-square">
      <svg viewBox="0 0 400 400" className="w-full h-full drop-shadow-lg">
        {/* Outer ring: dark bg */}
        <circle cx={cx} cy={cy} r={R} fill="hsl(240 15% 8%)" stroke="hsl(42 90% 64% / 0.15)" strokeWidth="1" />

        {/* 12 Rashi segments */}
        {RASHI_NAMES.map((name, i) => {
          const startDeg = i * 30 - 90;
          const endDeg   = startDeg + 30;
          const startRad = (startDeg * Math.PI) / 180;
          const endRad   = (endDeg   * Math.PI) / 180;
          const x1 = cx + R * Math.cos(startRad);
          const y1 = cy + R * Math.sin(startRad);
          const x2 = cx + R * Math.cos(endRad);
          const y2 = cy + R * Math.sin(endRad);
          // Label angle
          const midRad  = ((startDeg + 15) * Math.PI) / 180;
          const labelR  = 158;
          const lx = cx + labelR * Math.cos(midRad);
          const ly = cy + labelR * Math.sin(midRad);

          return (
            <g key={name}>
              {/* Segment divider line */}
              <line
                x1={cx + 90 * Math.cos(startRad)} y1={cy + 90 * Math.sin(startRad)}
                x2={x1} y2={y1}
                stroke="hsl(42 90% 64% / 0.12)" strokeWidth="0.5"
              />
              {/* Segment arc fill (very subtle) */}
              <path
                d={`M ${cx + 90 * Math.cos(startRad)} ${cy + 90 * Math.sin(startRad)}
                    L ${x1} ${y1}
                    A ${R} ${R} 0 0 1 ${x2} ${y2}
                    L ${cx + 90 * Math.cos(endRad)} ${cy + 90 * Math.sin(endRad)}
                    A 90 90 0 0 0 ${cx + 90 * Math.cos(startRad)} ${cy + 90 * Math.sin(startRad)}`}
                fill={`hsl(${i * 30} 40% 12% / 0.4)`}
                stroke="none"
              />
              {/* Rashi symbol */}
              <text
                x={lx} y={ly}
                textAnchor="middle" dominantBaseline="central"
                fontSize="11" fill="hsl(42 90% 64% / 0.55)"
                fontFamily="serif"
              >
                {RASHI_SHORT[i]}
              </text>
            </g>
          );
        })}

        {/* Inner circle (ecliptic zone) */}
        <circle cx={cx} cy={cy} r={90}  fill="hsl(240 15% 5%)"  stroke="hsl(42 90% 64% / 0.08)" strokeWidth="0.5" />
        <circle cx={cx} cy={cy} r={45}  fill="hsl(240 20% 4%)"  stroke="hsl(42 90% 64% / 0.05)" strokeWidth="0.5" />

        {/* Lagna marker */}
        {snapshot.lagna && (() => {
          const rad = (snapshot.lagna.eclipticLon - 90) * Math.PI / 180;
          const lx  = cx + (R - 8) * Math.cos(rad);
          const ly  = cy + (R - 8) * Math.sin(rad);
          return (
            <g>
              <line
                x1={cx + 90 * Math.cos(rad)} y1={cy + 90 * Math.sin(rad)}
                x2={lx} y2={ly}
                stroke="hsl(42 90% 64% / 0.7)" strokeWidth="1.5" strokeDasharray="3,2"
              />
              <text x={lx} y={ly} textAnchor="middle" dominantBaseline="central"
                fontSize="8" fill="hsl(42 90% 64%)" fontWeight="bold">L</text>
            </g>
          );
        })()}

        {/* Planets */}
        {ORDER.map((name) => {
          const p    = snapshot.grahas[name];
          if (!p) return null;
          const rad  = (p.eclipticLon - 90) * Math.PI / 180;
          const px   = cx + planetR * Math.cos(rad);
          const py   = cy + planetR * Math.sin(rad);
          const glow = GRAHA_COLOR[name] ?? "#fff";
          const isHovered = hovered === name;

          return (
            <g key={name}
               onMouseEnter={() => setHovered(name)}
               onMouseLeave={() => setHovered(null)}
               style={{ cursor: "pointer" }}
            >
              {/* Glow halo */}
              {(p.visible || isHovered) && (
                <circle cx={px} cy={py} r={isHovered ? 16 : 10}
                  fill={`${glow}22`} stroke={`${glow}44`} strokeWidth="0.5" />
              )}
              {/* Planet symbol */}
              <text x={px} y={py}
                textAnchor="middle" dominantBaseline="central"
                fontSize={isHovered ? "16" : "13"}
                fill={p.combust ? `${glow}66` : glow}
                style={{ filter: p.visible ? `drop-shadow(0 0 4px ${glow})` : "none" }}
              >
                {GRAHA_SYMBOL[name]}
              </text>
              {/* Retro marker */}
              {p.retro && (
                <text x={px + 8} y={py - 6} fontSize="6" fill={glow} opacity={0.8}>℞</text>
              )}
              {/* Tooltip on hover */}
              {isHovered && (
                <g>
                  <rect x={px - 28} y={py + 14} width={56} height={22} rx={4}
                    fill="hsl(240 15% 8%)" stroke={`${glow}66`} strokeWidth="0.5" />
                  <text x={px} y={py + 22} textAnchor="middle" fontSize="7" fill={glow}>
                    {p.rashi} {p.dms}
                  </text>
                  <text x={px} y={py + 30} textAnchor="middle" fontSize="6" fill="hsl(0 0% 60%)">
                    {p.nakshatra}
                  </text>
                </g>
              )}
            </g>
          );
        })}

        {/* Center Om */}
        <text x={cx} y={cy} textAnchor="middle" dominantBaseline="central"
          fontSize="18" fill="hsl(42 90% 64% / 0.3)" fontFamily="serif">ॐ</text>
      </svg>

      {/* Live pulse indicator */}
      <div className="absolute top-2 right-2 flex items-center gap-1">
        <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
        <span className="text-[9px] text-green-400">LIVE</span>
      </div>
    </div>
  );
}

// ── Planet Table ──────────────────────────────────────────────────────────────

function PlanetTable({ snapshot }: { snapshot: LivePlanetsSnapshot }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead>
          <tr className="text-muted-foreground border-b border-border/20">
            <th className="text-left py-2 px-2 font-normal">Graha</th>
            <th className="text-left py-2 px-2 font-normal">Rashi</th>
            <th className="text-left py-2 px-2 font-normal hidden sm:table-cell">Degree</th>
            <th className="text-left py-2 px-2 font-normal hidden md:table-cell">Nakshatra</th>
            <th className="text-center py-2 px-2 font-normal">Sky</th>
          </tr>
        </thead>
        <tbody>
          {ORDER.map((name, i) => {
            const p = snapshot.grahas[name];
            if (!p) return null;
            const color = GRAHA_COLOR[name] ?? "#fff";
            return (
              <motion.tr
                key={name}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.04 }}
                className="border-b border-border/10 hover:bg-muted/10 transition-colors"
              >
                <td className="py-2 px-2">
                  <div className="flex items-center gap-2">
                    <span className="text-lg" style={{ color, filter: `drop-shadow(0 0 4px ${color})` }}>
                      {GRAHA_SYMBOL[name]}
                    </span>
                    <div>
                      <p className="font-medium text-foreground">{name}</p>
                      <p className="text-[10px] text-muted-foreground">{GRAHA_NAME_HI[name]}</p>
                    </div>
                    {p.retro && (
                      <span className="text-[9px] text-amber-400 px-1 py-0.5 rounded bg-amber-400/10 border border-amber-400/20">
                        ℞ retro
                      </span>
                    )}
                  </div>
                </td>
                <td className="py-2 px-2 text-foreground">{p.rashi}</td>
                <td className="py-2 px-2 text-foreground font-mono hidden sm:table-cell">{p.dms}</td>
                <td className="py-2 px-2 text-muted-foreground hidden md:table-cell">
                  {p.nakshatra} <span className="text-[10px]">Pada {p.pada}</span>
                </td>
                <td className="py-2 px-2 text-center">
                  {name === "Rahu" || name === "Ketu" ? (
                    <span className="text-[10px] text-muted-foreground">Shadow</span>
                  ) : p.combust ? (
                    <span title="Combust — too close to Sun">
                      <EyeOff className="h-3.5 w-3.5 text-muted-foreground/40 mx-auto" />
                    </span>
                  ) : p.visible ? (
                    <span title="Visible in sky">
                      <Eye className="h-3.5 w-3.5 text-green-400 mx-auto" />
                    </span>
                  ) : (
                    <span title="Below horizon / daytime">
                      <EyeOff className="h-3.5 w-3.5 text-muted-foreground/40 mx-auto" />
                    </span>
                  )}
                </td>
              </motion.tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ── 24h Movement Tab ──────────────────────────────────────────────────────────

function Track24h({ snapshot }: { snapshot: LivePlanetsSnapshot }) {
  const track = use24hTrack(snapshot);
  const now   = snapshot.localTime;
  const currentHour  = now.getHours() + now.getMinutes() / 60;

  return (
    <div className="space-y-6">
      {/* Midnight countdown */}
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs text-muted-foreground">Day resets at midnight</p>
          <p className="font-display text-2xl text-primary">
            {fmtCountdown(snapshot.secondsToMidnight)}
          </p>
        </div>
        <div className="text-right">
          <p className="text-xs text-muted-foreground">Day progress</p>
          <p className="font-display text-lg text-foreground">{Math.round(snapshot.dayProgress * 100)}%</p>
          <div className="w-24 h-1.5 bg-muted/30 rounded-full mt-1 overflow-hidden">
            <div
              className="h-full bg-primary rounded-full transition-all"
              style={{ width: `${snapshot.dayProgress * 100}%` }}
            />
          </div>
        </div>
      </div>

      {/* Moon track — most prominent (moves 13°/day) */}
      {track && (() => {
        const moonTrack = track["Chandra"];
        if (!moonTrack) return null;
        const startLon = moonTrack[0].lon;
        const endLon   = moonTrack[24].lon;
        const moonRange = ((endLon - startLon + 360) % 360);

        return (
          <div className="cosmic-card rounded-xl p-4 space-y-3">
            <div className="flex items-center gap-2">
              <span className="text-xl" style={{ color: GRAHA_COLOR.Chandra }}>☽</span>
              <div>
                <p className="text-sm font-medium text-foreground">Moon Track Today</p>
                <p className="text-xs text-muted-foreground">
                  Moves ~{moonRange.toFixed(2)}° across the sky today
                </p>
              </div>
              <div className="ml-auto text-right">
                <p className="text-xs text-muted-foreground">Now</p>
                <p className="font-mono text-sm text-foreground">
                  {snapshot.grahas["Chandra"]?.rashi} {toDMS(snapshot.grahas["Chandra"]?.degInRashi ?? 0)}
                </p>
              </div>
            </div>

            {/* 24h bar chart */}
            <div className="relative h-12 bg-muted/10 rounded-lg overflow-hidden">
              {/* Hour ticks */}
              {[0, 6, 12, 18, 24].map((h) => (
                <div key={h}
                  className="absolute top-0 bottom-0 border-l border-border/20 flex items-end pb-1"
                  style={{ left: `${(h / 24) * 100}%` }}
                >
                  <span className="text-[8px] text-muted-foreground/40 pl-0.5">{String(h).padStart(2,"0")}:00</span>
                </div>
              ))}
              {/* Moon position track */}
              <div className="absolute inset-y-0 w-full flex items-center">
                <div
                  className="h-1 rounded-full"
                  style={{
                    background: `linear-gradient(90deg, ${GRAHA_COLOR.Chandra}22, ${GRAHA_COLOR.Chandra}88)`,
                    width: "100%",
                  }}
                />
              </div>
              {/* Current time marker */}
              <div
                className="absolute top-0 bottom-0 w-px bg-primary"
                style={{ left: `${(currentHour / 24) * 100}%` }}
              >
                <div className="w-2 h-2 rounded-full bg-primary absolute -top-1 -translate-x-1/2" />
              </div>
              {/* Moon dot at current position */}
              <div
                className="absolute w-4 h-4 rounded-full flex items-center justify-center text-xs"
                style={{
                  left: `${(currentHour / 24) * 100}%`,
                  top: "50%",
                  transform: "translate(-50%, -50%)",
                  backgroundColor: GRAHA_COLOR.Chandra + "33",
                  color: GRAHA_COLOR.Chandra,
                  filter: `drop-shadow(0 0 4px ${GRAHA_COLOR.Chandra})`,
                  fontSize: "12px",
                }}
              >
                ☽
              </div>
            </div>

            {/* Hourly positions for Moon */}
            <div className="grid grid-cols-6 gap-1">
              {[0, 4, 8, 12, 16, 20].map((h) => {
                const pos = moonTrack[h];
                return (
                  <div key={h} className={`text-center rounded p-1 ${h === Math.floor(now.getHours() / 4) * 4 ? "bg-primary/10" : "bg-muted/10"}`}>
                    <p className="text-[9px] text-muted-foreground">{String(h).padStart(2,"0")}:00</p>
                    <p className="text-[10px] text-foreground font-mono">{pos.dms.split("°")[0]}°</p>
                    <p className="text-[9px] text-muted-foreground truncate">{pos.rashi.slice(0,4)}</p>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })()}

      {/* All planets 24h summary */}
      <div className="cosmic-card rounded-xl p-4 space-y-3">
        <p className="text-xs text-muted-foreground uppercase tracking-wider">All Planets — Today's Range</p>
        {ORDER.map((name) => {
          const p = snapshot.grahas[name];
          if (!p || !track) return null;
          const t    = track[name];
          if (!t) return null;
          const color = GRAHA_COLOR[name] ?? "#fff";
          const speed = PLANET_SPEED_PER_DAY[name] ?? 0;
          const moveToday = Math.abs(speed).toFixed(3);

          return (
            <div key={name} className="flex items-center gap-3">
              <span className="w-5 text-center text-base" style={{ color }}>
                {GRAHA_SYMBOL[name]}
              </span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between mb-0.5">
                  <span className="text-xs text-foreground">{name}</span>
                  <span className="text-[10px] text-muted-foreground font-mono">
                    {moveToday}°/day
                    {p.retro && <span className="text-amber-400 ml-1">℞</span>}
                  </span>
                </div>
                <div className="h-1 bg-muted/20 rounded-full overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all"
                    style={{
                      width: `${(snapshot.dayProgress) * 100}%`,
                      backgroundColor: color,
                      opacity: 0.7,
                    }}
                  />
                </div>
              </div>
              <div className="text-right min-w-0">
                <p className="text-[10px] font-mono text-foreground">{p.rashi.slice(0,4)} {toDMS(p.degInRashi)}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Today for You ─────────────────────────────────────────────────────────────

/**
 * Transit house = (transit_rashi_index - natal_moon_rashi_index + 12) % 12 + 1
 * Based on Chandra Rashi (Janma Rashi) as reference.
 */
function getTransitHouse(transitRashi: string, natalMoonRashi: string): number {
  const t = RASHI_INDEX[transitRashi] ?? 0;
  const n = RASHI_INDEX[natalMoonRashi] ?? 0;
  return ((t - n + 12) % 12) + 1;
}

const MOON_TRANSIT_EFFECTS: Record<number, { quality: "good" | "mixed" | "bad"; text: string; advice: string }> = {
  1:  { quality: "mixed", text: "Moon in 1st — introspection & self-focus.",        advice: "Good for meditation, self-care. Avoid initiating conflicts." },
  2:  { quality: "mixed", text: "Moon in 2nd — speech & finances.",                 advice: "Favorable for financial discussions. Watch harsh words." },
  3:  { quality: "good",  text: "Moon in 3rd — courage, siblings, short travel.",   advice: "Excellent for communication, writing, short journeys." },
  4:  { quality: "good",  text: "Moon in 4th — home, mother, emotions.",            advice: "Spend time at home. Connect with family. Nurture inner self." },
  5:  { quality: "good",  text: "Moon in 5th — creativity, children, intellect.",   advice: "Good for studies, creative work, romance." },
  6:  { quality: "bad",   text: "Moon in 6th — obstacles, health matters.",         advice: "Be careful with health. Avoid lending money. Stay patient." },
  7:  { quality: "mixed", text: "Moon in 7th — relationships, partnerships.",        advice: "Good for partnerships, marriage matters, social interaction." },
  8:  { quality: "bad",   text: "Moon in 8th — sudden changes, hidden matters.",    advice: "Avoid risky ventures. Good for research, spiritual practices." },
  9:  { quality: "good",  text: "Moon in 9th — dharma, fortune, teachers.",         advice: "Blessings from elders. Good for spiritual activities, long travel." },
  10: { quality: "good",  text: "Moon in 10th — career, public life, action.",      advice: "Excellent for career moves, public dealings, leadership." },
  11: { quality: "good",  text: "Moon in 11th — gains, friends, fulfilment.",       advice: "Expect gains, social connections. Good day for networking." },
  12: { quality: "bad",   text: "Moon in 12th — rest, expenses, foreign.",          advice: "Rest and retreat. Avoid spending. Spiritual practice is favored." },
};

const QUALITY_STYLE = {
  good:  { bg: "bg-green-500/10 border-green-500/20",   text: "text-green-400",  badge: "bg-green-500/20" },
  mixed: { bg: "bg-amber-500/10 border-amber-500/20",  text: "text-amber-400",  badge: "bg-amber-500/20" },
  bad:   { bg: "bg-red-500/10 border-red-500/20",      text: "text-red-400",    badge: "bg-red-500/20"   },
};

function TodayForYou({ snapshot }: { snapshot: LivePlanetsSnapshot }) {
  const kundali = useKundliStore((s) => s.kundaliData);

  if (!kundali) {
    return (
      <div className="cosmic-card rounded-xl p-8 text-center space-y-3">
        <span className="text-3xl block">🌟</span>
        <p className="font-display text-base text-foreground">Generate Your Kundali First</p>
        <p className="text-xs text-muted-foreground">
          Your personalized transit forecast requires your natal chart.
          Go to <strong>My Kundli</strong> → Onboarding to generate it.
        </p>
      </div>
    );
  }

  const natalMoonRashi   = kundali.grahas["Chandra"]?.rashi ?? "Mesha";
  const natalLagnaRashi  = kundali.lagna?.rashi ?? "Mesha";
  const today = new Date().toISOString().slice(0, 10);
  const currentDasha = kundali.dashas.find((d) =>
    d.is_current || (d.start <= today && d.end >= today)
  ) ?? kundali.dashas[0];
  const transitMoonRashi = snapshot.grahas["Chandra"]?.rashi ?? "Mesha";
  const transitSunRashi  = snapshot.grahas["Surya"]?.rashi ?? "Mesha";

  const moonHouse = getTransitHouse(transitMoonRashi, natalMoonRashi);
  const sunHouse  = getTransitHouse(transitSunRashi,  natalMoonRashi);
  const moonEffect = MOON_TRANSIT_EFFECTS[moonHouse];

  // Notable transits — Jupiter & Saturn relative to natal Moon
  const jupRashi  = snapshot.grahas["Guru"]?.rashi   ?? "Mesha";
  const satRashi  = snapshot.grahas["Shani"]?.rashi  ?? "Mesha";
  const jupHouse  = getTransitHouse(jupRashi, natalMoonRashi);
  const satHouse  = getTransitHouse(satRashi, natalMoonRashi);

  // Lucky planets today (those in upachaya 3,6,10,11 from lagna)
  const upachaya = [3, 6, 10, 11];
  const luckyGrahas = ORDER.filter((name) => {
    const rashi = snapshot.grahas[name]?.rashi;
    if (!rashi) return false;
    const house = getTransitHouse(rashi, natalLagnaRashi);
    return upachaya.includes(house);
  });

  // Day quality overall
  const goodHouses  = [3,4,5,9,10,11];
  const overallGood = goodHouses.includes(moonHouse);

  return (
    <div className="space-y-4">
      {/* Day quality banner */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className={`cosmic-card rounded-xl p-4 border ${overallGood ? "bg-green-500/5 border-green-500/20" : "bg-amber-500/5 border-amber-500/20"}`}
      >
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs text-muted-foreground">Today's cosmic energy for</p>
            <p className="font-display text-lg text-foreground">{kundali.name}</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              Janma Rashi: <span className="text-primary">{natalMoonRashi}</span> ·
              Lagna: <span className="text-primary">{natalLagnaRashi}</span>
            </p>
          </div>
          <div className="text-right">
            <span className={`text-2xl ${overallGood ? "text-green-400" : "text-amber-400"}`}>
              {overallGood ? "🌟" : "⚡"}
            </span>
            <p className="text-xs mt-1">
              <span className={overallGood ? "text-green-400" : "text-amber-400"}>
                {overallGood ? "Auspicious day" : "Moderate day"}
              </span>
            </p>
          </div>
        </div>
      </motion.div>

      {/* Current Dasha */}
      {currentDasha && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.05 }}
          className="cosmic-card rounded-xl p-4 border border-primary/20 bg-primary/5"
        >
          <p className="text-[10px] uppercase tracking-wider text-muted-foreground mb-1">Current Mahadasha</p>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-2xl" style={{ color: GRAHA_COLOR[currentDasha.lord] }}>
                {GRAHA_SYMBOL[currentDasha.lord] ?? "✦"}
              </span>
              <div>
                <p className="font-display text-base text-foreground">{currentDasha.lord} Dasha</p>
                <p className="text-xs text-muted-foreground">Ends {currentDasha.end?.split("T")[0]}</p>
              </div>
            </div>
            <p className="text-xs text-muted-foreground text-right">
              {currentDasha.years} year period<br />
              <span className="text-primary">Active now</span>
            </p>
          </div>
        </motion.div>
      )}

      {/* Moon Transit */}
      {moonEffect && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.1 }}
          className={`cosmic-card rounded-xl p-4 border ${QUALITY_STYLE[moonEffect.quality].bg}`}
        >
          <div className="flex items-start gap-3">
            <span className="text-2xl mt-0.5" style={{ color: GRAHA_COLOR.Chandra }}>☽</span>
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-1">
                <p className="text-xs font-medium text-foreground">Moon Transit — House {moonHouse}</p>
                <span className={`text-[9px] px-1.5 py-0.5 rounded-full font-medium ${QUALITY_STYLE[moonEffect.quality].badge} ${QUALITY_STYLE[moonEffect.quality].text}`}>
                  {moonEffect.quality === "good" ? "Favorable" : moonEffect.quality === "bad" ? "Caution" : "Mixed"}
                </span>
              </div>
              <p className="text-xs text-foreground mb-1">{moonEffect.text}</p>
              <p className="text-[11px] text-muted-foreground">{moonEffect.advice}</p>
              <p className="text-[10px] text-muted-foreground/60 mt-1">
                Moon in {transitMoonRashi} transiting your {moonHouse}{moonHouse === 1 ? "st" : moonHouse === 2 ? "nd" : moonHouse === 3 ? "rd" : "th"} from natal Moon ({natalMoonRashi})
              </p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Sun Transit */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.15 }}
        className="cosmic-card rounded-xl p-4"
      >
        <div className="flex items-start gap-3">
          <span className="text-2xl" style={{ color: GRAHA_COLOR.Surya }}>☉</span>
          <div>
            <p className="text-xs font-medium text-foreground mb-1">Sun Transit — House {sunHouse}</p>
            <p className="text-[11px] text-muted-foreground">
              Sun is in {transitSunRashi}, transiting your {sunHouse}{sunHouse===1?"st":sunHouse===2?"nd":sunHouse===3?"rd":"th"} house.
              {[1,4,7,10].includes(sunHouse) && " Kendra — strong influence on your actions."}
              {[5,9].includes(sunHouse) && " Trikona — blessings and fortune."}
              {[3,6,10,11].includes(sunHouse) && " Upachaya — growth through effort."}
            </p>
          </div>
        </div>
      </motion.div>

      {/* Jupiter & Saturn long-term transits */}
      <div className="grid grid-cols-2 gap-3">
        {[
          { name: "Guru", house: jupHouse, label: "Jupiter" },
          { name: "Shani", house: satHouse, label: "Saturn" },
        ].map(({ name, house, label }) => {
          const good = [2,5,7,9,11].includes(house);
          return (
            <motion.div
              key={name}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
              className={`cosmic-card rounded-xl p-3 border ${good ? "border-green-500/15" : "border-amber-500/15"}`}
            >
              <div className="flex items-center gap-2 mb-1">
                <span style={{ color: GRAHA_COLOR[name] }}>{GRAHA_SYMBOL[name]}</span>
                <p className="text-xs text-foreground">{label}</p>
              </div>
              <p className="text-[10px] text-muted-foreground">
                House {house} from your Moon
              </p>
              <p className={`text-[10px] mt-0.5 ${good ? "text-green-400" : "text-amber-400"}`}>
                {good ? "Supportive transit" : "Challenging, builds strength"}
              </p>
            </motion.div>
          );
        })}
      </div>

      {/* Lucky planets today */}
      {luckyGrahas.length > 0 && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.25 }}
          className="cosmic-card rounded-xl p-4"
        >
          <p className="text-[10px] uppercase tracking-wider text-muted-foreground mb-2">
            Favorable Planets Today (Upachaya from your Lagna)
          </p>
          <div className="flex flex-wrap gap-2">
            {luckyGrahas.map((name) => (
              <span
                key={name}
                className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs border"
                style={{ borderColor: GRAHA_COLOR[name] + "44", color: GRAHA_COLOR[name] }}
              >
                {GRAHA_SYMBOL[name]} {name}
              </span>
            ))}
          </div>
        </motion.div>
      )}

      {/* Yogas active in natal chart */}
      {kundali.yogas && kundali.yogas.length > 0 && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="cosmic-card rounded-xl p-4 space-y-2"
        >
          <p className="text-[10px] uppercase tracking-wider text-muted-foreground">
            Your Natal Yogas (always active)
          </p>
          {kundali.yogas.slice(0, 3).map((yoga) => (
            <div key={yoga.name} className="flex items-start gap-2">
              <span className="text-primary mt-0.5">✦</span>
              <div>
                <p className="text-xs font-medium text-foreground">{yoga.name}</p>
                <p className="text-[10px] text-muted-foreground">{yoga.desc}</p>
              </div>
            </div>
          ))}
        </motion.div>
      )}
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function SkyPage() {
  const snapshot = useLivePlanets();

  const visibleCount = useMemo(() => {
    if (!snapshot) return 0;
    return ORDER.filter((n) => snapshot.grahas[n]?.visible).length;
  }, [snapshot]);

  if (!snapshot) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-center space-y-3">
          <RefreshCw className="h-6 w-6 text-primary mx-auto animate-spin" />
          <p className="text-xs text-muted-foreground">Loading planetary positions…</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 sm:p-6 space-y-5 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="font-display text-2xl text-foreground text-glow-gold">Live Sky</h1>
          <p className="text-sm text-muted-foreground">
            Real-time sidereal positions · Updates every second
          </p>
        </div>
        <div className="text-right">
          <p className="font-mono text-xl text-primary">
            {formatTime(snapshot.localTime)}
          </p>
          <p className="text-[10px] text-muted-foreground">IST</p>
        </div>
      </div>

      {/* Quick stats */}
      <div className="grid grid-cols-3 gap-3">
        <div className="cosmic-card rounded-xl p-3 text-center">
          <div className="flex items-center justify-center gap-1 mb-1">
            <Eye className="h-3.5 w-3.5 text-green-400" />
            <span className="text-[10px] text-muted-foreground">Visible</span>
          </div>
          <p className="font-display text-xl text-green-400">{visibleCount}</p>
          <p className="text-[10px] text-muted-foreground">planets in sky</p>
        </div>
        <div className="cosmic-card rounded-xl p-3 text-center">
          <div className="flex items-center justify-center gap-1 mb-1">
            <TrendingDown className="h-3.5 w-3.5 text-amber-400" />
            <span className="text-[10px] text-muted-foreground">Retrograde</span>
          </div>
          <p className="font-display text-xl text-amber-400">
            {ORDER.filter((n) => snapshot.grahas[n]?.retro).length}
          </p>
          <p className="text-[10px] text-muted-foreground">grahas ℞</p>
        </div>
        <div className="cosmic-card rounded-xl p-3 text-center">
          <div className="flex items-center justify-center gap-1 mb-1">
            <Globe className="h-3.5 w-3.5 text-primary" />
            <span className="text-[10px] text-muted-foreground">Lagna</span>
          </div>
          <p className="font-display text-sm text-primary">{snapshot.lagna?.rashi ?? "—"}</p>
          <p className="text-[10px] text-muted-foreground font-mono">{snapshot.lagna?.dms ?? "—"}</p>
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="live">
        <TabsList className="w-full grid grid-cols-3">
          <TabsTrigger value="live" className="gap-1.5 text-xs">
            <Globe className="h-3.5 w-3.5" /> Live Sky
          </TabsTrigger>
          <TabsTrigger value="track" className="gap-1.5 text-xs">
            <Clock className="h-3.5 w-3.5" /> 24h Track
          </TabsTrigger>
          <TabsTrigger value="today" className="gap-1.5 text-xs">
            <User className="h-3.5 w-3.5" /> Today for You
          </TabsTrigger>
        </TabsList>

        {/* ── Tab 1: Live Sky ── */}
        <TabsContent value="live" className="space-y-5 mt-4">
          <div className="lg:grid lg:grid-cols-2 lg:gap-6 space-y-5 lg:space-y-0">
            {/* Zodiac Wheel */}
            <div className="cosmic-card rounded-2xl p-4 flex flex-col items-center gap-3">
              <p className="text-xs text-muted-foreground uppercase tracking-wider self-start">
                Sidereal Zodiac — Live
              </p>
              <ZodiacWheel snapshot={snapshot} />
              <p className="text-[10px] text-muted-foreground/50 text-center">
                Hover planets for details · L = Lagna
              </p>
            </div>

            {/* Planet table */}
            <div className="cosmic-card rounded-2xl p-4">
              <div className="flex items-center justify-between mb-3">
                <p className="text-xs text-muted-foreground uppercase tracking-wider">
                  Graha Positions
                </p>
                <div className="flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
                  <span className="text-[10px] text-green-400">Live · per second</span>
                </div>
              </div>
              <AnimatePresence mode="wait">
                <PlanetTable key={snapshot.localTime.getSeconds()} snapshot={snapshot} />
              </AnimatePresence>
            </div>
          </div>

          {/* Visibility legend */}
          <div className="flex flex-wrap gap-4 text-[10px] text-muted-foreground">
            <span className="flex items-center gap-1.5"><Eye className="h-3 w-3 text-green-400" /> Visible in sky</span>
            <span className="flex items-center gap-1.5"><EyeOff className="h-3 w-3 text-muted-foreground/40" /> Below horizon / combust</span>
            <span className="flex items-center gap-1.5"><span className="text-amber-400 text-xs">℞</span> Retrograde</span>
            <span className="flex items-center gap-1.5"><span className="text-[10px] opacity-60">●</span> Combust (near Sun)</span>
          </div>
        </TabsContent>

        {/* ── Tab 2: 24h Track ── */}
        <TabsContent value="track" className="mt-4">
          <Track24h snapshot={snapshot} />
        </TabsContent>

        {/* ── Tab 3: Today for You ── */}
        <TabsContent value="today" className="mt-4">
          <TodayForYou snapshot={snapshot} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
