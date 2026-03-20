import { useState } from "react";
import { motion } from "framer-motion";
import { samplePlanets, type PlanetData, useKundliStore, type ChartStyle } from "@/store/kundliStore";

interface KundliChartProps {
  onPlanetClick?: (planet: PlanetData) => void;
  selectedPlanet?: PlanetData | null;
  planets?: PlanetData[];
  lagnaRashi?: string;
  chartStyle?: ChartStyle;
  showStyleToggle?: boolean;
}

// ── Constants ─────────────────────────────────────────────────────────────────
const RASHI_LIST = [
  "Mesha","Vrishabha","Mithuna","Karka",
  "Simha","Kanya","Tula","Vrischika",
  "Dhanu","Makara","Kumbha","Meena",
];
const RASHI_ABBR: Record<string, string> = {
  Mesha:"Mes", Vrishabha:"Vri", Mithuna:"Mit", Karka:"Kar",
  Simha:"Sim", Kanya:"Kan", Tula:"Tul", Vrischika:"Vrc",
  Dhanu:"Dha", Makara:"Mak", Kumbha:"Kum", Meena:"Mee",
};
const RASHI_NUM: Record<string, number> = {};
RASHI_LIST.forEach((r, i) => { RASHI_NUM[r] = i; });

const GRAHA_SYMBOLS: Record<string, string> = {
  Sun:"☉", Moon:"☽", Mars:"♂", Mercury:"☿",
  Jupiter:"♃", Venus:"♀", Saturn:"♄", Rahu:"☊", Ketu:"☋",
};
const PLANET_COLORS: Record<string, string> = {
  Sun:"#f59e0b", Moon:"#94a3b8", Mars:"#ef4444", Mercury:"#22c55e",
  Jupiter:"#eab308", Venus:"#a855f7", Saturn:"#64748b", Rahu:"#6366f1", Ketu:"#f97316",
};

const CELL = 110; // slightly bigger cells
const O = 5;

// ── Planet renderer ───────────────────────────────────────────────────────────
function renderPlanets(
  planetsHere: PlanetData[],
  cx: number, cy: number,
  onPlanetClick?: (p: PlanetData) => void,
  selectedPlanet?: PlanetData | null,
) {
  return planetsHere.map((p, i) => {
    const cols = planetsHere.length <= 2 ? 1 : 2;
    const colOff = planetsHere.length === 1 ? 0 : (i % 2 === 0 ? -14 : 14);
    const rowOff = Math.floor(i / 2) * 22;
    const px = cx + (cols === 1 ? 0 : colOff);
    const py = cy + rowOff;
    const isSelected = selectedPlanet?.name === p.name;
    const color = PLANET_COLORS[p.name] ?? "#94a3b8";
    return (
      <motion.g key={p.name} onClick={() => onPlanetClick?.(p)}
        className="cursor-pointer"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }}
        transition={{ delay: 0.25 + i * 0.04 }}>
        {isSelected && (
          <circle cx={px} cy={py - 4} r="9" fill={`${color}22`} />
        )}
        <text x={px} y={py} textAnchor="middle" fontSize="15"
          fill={isSelected ? "#fbbf24" : color}
          style={{ filter: isSelected ? "drop-shadow(0 0 6px #fbbf2499)" : "none" }}>
          {GRAHA_SYMBOLS[p.name] ?? p.symbol ?? "?"}
        </text>
        <text x={px} y={py + 11} textAnchor="middle" fontSize="8.5"
          fill={isSelected ? "hsl(42 90% 64% / 0.9)" : "hsl(225 15% 60%)"}>
          {p.sanskritName?.slice(0, 3) ?? p.name.slice(0, 3)}
        </text>
      </motion.g>
    );
  });
}

// ── North Indian Chart ────────────────────────────────────────────────────────
interface GridCell { id: number; row: number; col: number }
const NI_GRID: GridCell[] = [
  { id:3,row:0,col:0 }, { id:2,row:0,col:1 },
  { id:1,row:0,col:2 }, { id:12,row:0,col:3 },
  { id:11,row:1,col:3 }, { id:10,row:2,col:3 },
  { id:9,row:3,col:3 }, { id:8,row:3,col:2 },
  { id:7,row:3,col:1 }, { id:6,row:3,col:0 },
  { id:5,row:2,col:0 }, { id:4,row:1,col:0 },
];

function NorthIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const byHouse: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    if (!byHouse[p.house]) byHouse[p.house] = [];
    byHouse[p.house].push(p);
  }
  const houseRashi = (id: number) => RASHI_LIST[(lagnaIdx + id - 1) % 12];
  const W = 4 * CELL + O * 2;

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-[440px] mx-auto">
      <rect x={O} y={O} width={4*CELL} height={4*CELL}
        fill="hsl(225 25% 7%)" stroke="hsl(42 90% 64% / 0.35)" strokeWidth="1.5" rx="4" />
      <rect x={O+CELL} y={O+CELL} width={2*CELL} height={2*CELL}
        fill="hsl(225 25% 5%)" stroke="hsl(42 90% 64% / 0.12)" strokeWidth="0.5" />
      <line x1={O+CELL} y1={O+CELL} x2={O+3*CELL} y2={O+3*CELL} stroke="hsl(42 90% 64% / 0.08)" strokeWidth="0.6" />
      <line x1={O+3*CELL} y1={O+CELL} x2={O+CELL} y2={O+3*CELL} stroke="hsl(42 90% 64% / 0.08)" strokeWidth="0.6" />
      <text x={O+2*CELL} y={O+2*CELL-12} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.65)" fontSize="11" fontFamily="serif" letterSpacing="1">
        {lagnaRashi}
      </text>
      <text x={O+2*CELL} y={O+2*CELL+5} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.4)" fontSize="9" fontFamily="serif">Lagna</text>

      {NI_GRID.map(cell => {
        const x = O + cell.col * CELL;
        const y = O + cell.row * CELL;
        const rn = houseRashi(cell.id);
        const planetsHere = byHouse[cell.id] ?? [];
        const isL = cell.id === 1;
        return (
          <g key={cell.id}>
            <rect x={x} y={y} width={CELL} height={CELL}
              fill={isL ? "hsl(42 90% 64% / 0.06)" : "transparent"}
              stroke="hsl(42 90% 64% / 0.22)" strokeWidth="0.8" />
            <text x={x+6} y={y+14} fontSize="10" fill="hsl(225 15% 45%)" fontFamily="sans-serif">{cell.id}</text>
            <text x={x+CELL-5} y={y+14} textAnchor="end" fontSize="9.5"
              fill="hsl(42 90% 64% / 0.6)" fontFamily="serif">
              {RASHI_ABBR[rn] ?? rn.slice(0,3)}
            </text>
            {isL && <text x={x+CELL/2} y={y+26} textAnchor="middle" fontSize="8.5"
              fill="hsl(42 90% 64% / 0.4)">Lag</text>}
            {renderPlanets(planetsHere, x+CELL/2, y+CELL/2+10, onPlanetClick, selectedPlanet)}
          </g>
        );
      })}
    </svg>
  );
}

// ── South Indian Chart ────────────────────────────────────────────────────────
const SI_CELLS: { rashi: number; row: number; col: number }[] = [
  {rashi:11,row:0,col:0},{rashi:0,row:0,col:1},{rashi:1,row:0,col:2},{rashi:2,row:0,col:3},
  {rashi:10,row:1,col:0},{rashi:3,row:1,col:3},
  {rashi:9,row:2,col:0},{rashi:4,row:2,col:3},
  {rashi:8,row:3,col:0},{rashi:7,row:3,col:1},{rashi:6,row:3,col:2},{rashi:5,row:3,col:3},
];

function SouthIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const byRashi: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    const ri = RASHI_NUM[p.rashi];
    if (ri !== undefined) {
      if (!byRashi[ri]) byRashi[ri] = [];
      byRashi[ri].push(p);
    }
  }
  const W = 4 * CELL + O * 2;

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-[440px] mx-auto">
      <rect x={O} y={O} width={4*CELL} height={4*CELL}
        fill="hsl(225 25% 7%)" stroke="hsl(42 90% 64% / 0.35)" strokeWidth="1.5" rx="4" />
      <rect x={O+CELL} y={O+CELL} width={2*CELL} height={2*CELL}
        fill="hsl(225 25% 5%)" stroke="hsl(42 90% 64% / 0.12)" strokeWidth="0.5" />
      <text x={O+2*CELL} y={O+2*CELL-5} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.45)" fontSize="9.5" fontFamily="serif">Brahm AI</text>
      <text x={O+2*CELL} y={O+2*CELL+9} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.28)" fontSize="8.5" fontFamily="serif">Kundali</text>

      {SI_CELLS.map(({ rashi, row, col }) => {
        const x = O + col * CELL;
        const y = O + row * CELL;
        const rn = RASHI_LIST[rashi];
        const planetsHere = byRashi[rashi] ?? [];
        const isL = rashi === lagnaIdx;
        return (
          <g key={rashi}>
            <rect x={x} y={y} width={CELL} height={CELL}
              fill={isL ? "hsl(42 90% 64% / 0.07)" : "transparent"}
              stroke="hsl(42 90% 64% / 0.22)" strokeWidth="0.8" />
            {isL && <polygon points={`${x+2},${y+2} ${x+18},${y+2} ${x+2},${y+18}`}
              fill="hsl(42 90% 64% / 0.7)" />}
            <text x={x+6} y={y+15} fontSize="9.5" fill="hsl(225 15% 45%)">{rashi+1}</text>
            <text x={x+CELL/2} y={y+16} textAnchor="middle"
              fontSize="10" fill="hsl(42 90% 64% / 0.75)" fontFamily="serif" fontWeight="bold">
              {RASHI_ABBR[rn] ?? rn}
            </text>
            {renderPlanets(planetsHere, x+CELL/2, y+CELL/2+12, onPlanetClick, selectedPlanet)}
          </g>
        );
      })}
    </svg>
  );
}

// ── East Indian (Diamond) Chart ───────────────────────────────────────────────
function EastIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const byHouse: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    if (!byHouse[p.house]) byHouse[p.house] = [];
    byHouse[p.house].push(p);
  }
  const houseRashi = (id: number) => RASHI_LIST[(lagnaIdx + id - 1) % 12];
  const W = 450; const C = W / 2;

  // East Indian: 3×3 grid with 4 corner triangles + 4 side triangles + center
  // Classic layout: H1=top, clockwise
  const houses: { id: number; cx: number; cy: number }[] = [
    { id:1,  cx: C,       cy: 55 },
    { id:2,  cx: C+130,   cy: 90 },
    { id:3,  cx: C+195,   cy: C },
    { id:4,  cx: C+130,   cy: C+135 },
    { id:5,  cx: C,       cy: C+195 },
    { id:6,  cx: C-130,   cy: C+135 },
    { id:7,  cx: C,       cy: C+340 },
    { id:8,  cx: C-130,   cy: C+135 },
    { id:9,  cx: C-195,   cy: C },
    { id:10, cx: C-130,   cy: 90 },
    { id:11, cx: C-65,    cy: 55 },
    { id:12, cx: C+65,    cy: 55 },
  ];

  // Simplified East Indian with proper diamond grid
  const EAST_HOUSES: { id: number; cx: number; cy: number }[] = [
    { id:1,  cx: C,       cy: 52 },
    { id:2,  cx: C+110,   cy: 100 },
    { id:3,  cx: C+190,   cy: C },
    { id:4,  cx: C+110,   cy: C+120 },
    { id:5,  cx: C+55,    cy: C+195 },
    { id:6,  cx: C,       cy: C+128 },
    { id:7,  cx: C,       cy: C+330 },
    { id:8,  cx: C-55,    cy: C+195 },
    { id:9,  cx: C-110,   cy: C+120 },
    { id:10, cx: C-190,   cy: C },
    { id:11, cx: C-110,   cy: 100 },
    { id:12, cx: C,       cy: C-128 },
  ];

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-[440px] mx-auto">
      <rect x={0} y={0} width={W} height={W} fill="hsl(225 25% 7%)" rx="4" />
      {/* Outer diamond */}
      <polygon points={`${C},8 ${W-8},${C} ${C},${W-8} 8,${C}`}
        fill="none" stroke="hsl(42 90% 64% / 0.35)" strokeWidth="1.5" />
      {/* Inner diamond */}
      <polygon points={`${C},${C-80} ${C+80},${C} ${C},${C+80} ${C-80},${C}`}
        fill="hsl(225 25% 5%)" stroke="hsl(42 90% 64% / 0.15)" strokeWidth="0.8" />
      {/* Cross lines */}
      <line x1={C} y1={8} x2={C} y2={W-8} stroke="hsl(42 90% 64% / 0.18)" strokeWidth="0.7" />
      <line x1={8} y1={C} x2={W-8} y2={C} stroke="hsl(42 90% 64% / 0.18)" strokeWidth="0.7" />
      {/* Diagonal lines */}
      <line x1={8} y1={8} x2={W-8} y2={W-8} stroke="hsl(42 90% 64% / 0.12)" strokeWidth="0.6" />
      <line x1={W-8} y1={8} x2={8} y2={W-8} stroke="hsl(42 90% 64% / 0.12)" strokeWidth="0.6" />

      {EAST_HOUSES.map(({ id, cx: hcx, cy: hcy }) => {
        const rn = houseRashi(id);
        const planetsHere = byHouse[id] ?? [];
        const isL = id === 1;
        return (
          <g key={id}>
            <text x={hcx} y={hcy - 14} textAnchor="middle" fontSize="9.5" fill="hsl(225 15% 45%)">{id}</text>
            <text x={hcx} y={hcy - 2} textAnchor="middle" fontSize="10"
              fill={isL ? "hsl(42 90% 64% / 0.85)" : "hsl(42 90% 64% / 0.55)"} fontFamily="serif">
              {RASHI_ABBR[rn]}{isL ? " ▲" : ""}
            </text>
            {renderPlanets(planetsHere, hcx, hcy + 14, onPlanetClick, selectedPlanet)}
          </g>
        );
      })}

      <text x={C} y={C - 6} textAnchor="middle" fill="hsl(42 90% 64% / 0.5)" fontSize="9" fontFamily="serif">East</text>
      <text x={C} y={C + 8} textAnchor="middle" fill="hsl(42 90% 64% / 0.3)" fontSize="8.5" fontFamily="serif">{lagnaRashi}</text>
    </svg>
  );
}

// ── Western Circular Chart ────────────────────────────────────────────────────
function WesternChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const byHouse: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    if (!byHouse[p.house]) byHouse[p.house] = [];
    byHouse[p.house].push(p);
  }
  const houseRashi = (id: number) => RASHI_LIST[(lagnaIdx + id - 1) % 12];
  const W = 450; const C = W / 2;
  const R1 = 210; const R2 = 165; const R3 = 65;

  const RASHI_SYMBOLS: Record<string, string> = {
    Mesha:"♈",Vrishabha:"♉",Mithuna:"♊",Karka:"♋",
    Simha:"♌",Kanya:"♍",Tula:"♎",Vrischika:"♏",
    Dhanu:"♐",Makara:"♑",Kumbha:"♒",Meena:"♓",
  };

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-[440px] mx-auto">
      <rect x={0} y={0} width={W} height={W} fill="hsl(225 25% 7%)" rx="4" />
      <circle cx={C} cy={C} r={R1} fill="none" stroke="hsl(42 90% 64% / 0.35)" strokeWidth="1.5" />
      <circle cx={C} cy={C} r={R2} fill="none" stroke="hsl(42 90% 64% / 0.22)" strokeWidth="0.8" />
      <circle cx={C} cy={C} r={R3} fill="hsl(225 25% 5%)" stroke="hsl(42 90% 64% / 0.15)" strokeWidth="0.5" />

      {Array.from({ length: 12 }, (_, i) => {
        const angle = Math.PI - (i * Math.PI / 6);
        return (
          <line key={i}
            x1={C + R3 * Math.cos(angle)} y1={C - R3 * Math.sin(angle)}
            x2={C + R1 * Math.cos(angle)} y2={C - R1 * Math.sin(angle)}
            stroke="hsl(42 90% 64% / 0.18)" strokeWidth="0.7" />
        );
      })}

      {Array.from({ length: 12 }, (_, i) => {
        const houseId = i + 1;
        const mid = Math.PI - ((i + 0.5) * Math.PI / 6);
        const rn = houseRashi(houseId);
        const isL = houseId === 1;
        const rMid = (R1 + R2) / 2;
        const rH = (R2 + R3) / 2 - 10;
        const rP = (R2 + R3) / 2 + 14;
        const planetsHere = byHouse[houseId] ?? [];

        return (
          <g key={houseId}>
            {/* Rashi symbol in outer ring */}
            <text x={C + rMid * Math.cos(mid)} y={C - rMid * Math.sin(mid) + 4}
              textAnchor="middle" fontSize={isL ? "15" : "13"}
              fill={isL ? "hsl(42 90% 64% / 0.95)" : "hsl(42 90% 64% / 0.65)"}>
              {RASHI_SYMBOLS[rn] ?? ""}
            </text>
            {/* House number */}
            <text x={C + rH * Math.cos(mid)} y={C - rH * Math.sin(mid) + 4}
              textAnchor="middle" fontSize="9" fill="hsl(225 15% 45%)">{houseId}</text>
            {/* Rashi abbr */}
            <text x={C + rH * Math.cos(mid)} y={C - rH * Math.sin(mid) + 15}
              textAnchor="middle" fontSize="7.5" fill="hsl(42 90% 64% / 0.4)" fontFamily="serif">
              {RASHI_ABBR[rn]}
            </text>
            {/* Planets */}
            {planetsHere.map((p, pi) => {
              const off = (pi - (planetsHere.length - 1) / 2) * 16;
              const ppX = C + rP * Math.cos(mid) + off * Math.cos(mid + Math.PI / 2);
              const ppY = C - rP * Math.sin(mid) - off * Math.sin(mid + Math.PI / 2);
              const color = PLANET_COLORS[p.name] ?? "#94a3b8";
              const isSel = selectedPlanet?.name === p.name;
              return (
                <motion.g key={p.name} onClick={() => onPlanetClick?.(p)}
                  className="cursor-pointer"
                  initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                  transition={{ delay: 0.25 + pi * 0.04 }}>
                  <text x={ppX} y={ppY + 4} textAnchor="middle" fontSize="13"
                    fill={isSel ? "#fbbf24" : color}
                    style={{ filter: isSel ? "drop-shadow(0 0 5px #fbbf2488)" : "none" }}>
                    {GRAHA_SYMBOLS[p.name] ?? p.symbol ?? "?"}
                  </text>
                </motion.g>
              );
            })}
          </g>
        );
      })}

      <text x={C} y={C - 8} textAnchor="middle" fill="hsl(42 90% 64% / 0.55)" fontSize="10" fontFamily="serif">{lagnaRashi}</text>
      <text x={C} y={C + 6} textAnchor="middle" fill="hsl(42 90% 64% / 0.35)" fontSize="8.5" fontFamily="serif">Lagna</text>
    </svg>
  );
}

// ── Main Export ───────────────────────────────────────────────────────────────
export function KundliChart({
  onPlanetClick, selectedPlanet, planets: planetsProp,
  lagnaRashi, chartStyle: styleProp, showStyleToggle = true,
}: KundliChartProps) {
  const displayPlanets = planetsProp ?? samplePlanets;
  const lagna = lagnaRashi ?? "Tula";
  const storeStyle = useKundliStore(s => s.kundaliSettings.chartStyle);
  const setSettings = useKundliStore(s => s.setKundaliSettings);
  const style = styleProp ?? storeStyle;

  const STYLES: { key: ChartStyle; label: string }[] = [
    { key: "north", label: "North" },
    { key: "south", label: "South" },
    { key: "east",  label: "East" },
    { key: "west",  label: "West" },
  ];

  return (
    <div>
      {showStyleToggle && (
        <div className="flex gap-1.5 mb-3 flex-wrap">
          {STYLES.map(s => (
            <button key={s.key} onClick={() => setSettings({ chartStyle: s.key })}
              className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                style === s.key
                  ? "bg-primary/20 text-primary border border-primary/35"
                  : "text-muted-foreground hover:text-foreground border border-border/20"
              }`}>
              {s.label}
            </button>
          ))}
        </div>
      )}
      <motion.div key={style} initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.3 }}>
        {style === "north" ? (
          <NorthIndianChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} />
        ) : style === "south" ? (
          <SouthIndianChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} />
        ) : style === "east" ? (
          <EastIndianChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} />
        ) : (
          <WesternChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} />
        )}
      </motion.div>
    </div>
  );
}
