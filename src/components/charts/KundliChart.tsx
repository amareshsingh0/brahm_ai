import { useState } from "react";
import { motion } from "framer-motion";
import { samplePlanets, type PlanetData } from "@/store/kundliStore";

interface KundliChartProps {
  onPlanetClick?: (planet: PlanetData) => void;
  selectedPlanet?: PlanetData | null;
  planets?: PlanetData[];
  lagnaRashi?: string;
  chartStyle?: "north" | "south";
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

const CELL = 100; // each grid cell = 100px, viewBox = 410×410

// ── North Indian chart grid layout ────────────────────────────────────────────
// 4×4 grid where center 2×2 is empty. 12 border cells = 12 houses.
// Layout: top-row=[H3,H2,H1,H12], right-col=[H11,H10], bot-row=[H9,H8,H7,H6], left-col=[H5,H4]
interface GridCell { id: number; row: number; col: number }
const NI_GRID: GridCell[] = [
  { id: 3,  row: 0, col: 0 }, { id: 2,  row: 0, col: 1 },
  { id: 1,  row: 0, col: 2 }, { id: 12, row: 0, col: 3 },
  { id: 11, row: 1, col: 3 }, { id: 10, row: 2, col: 3 },
  { id: 9,  row: 3, col: 3 }, { id: 8,  row: 3, col: 2 },
  { id: 7,  row: 3, col: 1 }, { id: 6,  row: 3, col: 0 },
  { id: 5,  row: 2, col: 0 }, { id: 4,  row: 1, col: 0 },
];

// ── South Indian chart rashi positions ────────────────────────────────────────
// Fixed rashis, lagna marked with triangle. 4×4 grid, center 2×2 empty.
const SI_CELLS: { rashi: number; row: number; col: number }[] = [
  { rashi: 11, row: 0, col: 0 }, { rashi:  0, row: 0, col: 1 },
  { rashi:  1, row: 0, col: 2 }, { rashi:  2, row: 0, col: 3 },
  { rashi: 10, row: 1, col: 0 }, { rashi:  3, row: 1, col: 3 },
  { rashi:  9, row: 2, col: 0 }, { rashi:  4, row: 2, col: 3 },
  { rashi:  8, row: 3, col: 0 }, { rashi:  7, row: 3, col: 1 },
  { rashi:  6, row: 3, col: 2 }, { rashi:  5, row: 3, col: 3 },
];

const O = 5; // padding offset

// ── Planet renderer helper ────────────────────────────────────────────────────
function renderPlanets(
  planetsHere: PlanetData[],
  cx: number, cy: number, cellW: number, cellH: number,
  onPlanetClick?: (p: PlanetData) => void,
  selectedPlanet?: PlanetData | null,
) {
  return planetsHere.map((p, i) => {
    const colOff = planetsHere.length === 1 ? 0 : i % 2 === 0 ? -16 : 16;
    const rowOff = Math.floor(i / 2) * 20;
    const px = cx + colOff;
    const py = cy + rowOff;
    const isSelected = selectedPlanet?.name === p.name;
    const color = PLANET_COLORS[p.name] ?? "#94a3b8";
    return (
      <motion.g key={p.name} onClick={() => onPlanetClick?.(p)}
        className="cursor-pointer"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }}
        transition={{ delay: 0.3 + i * 0.05 }}>
        <text x={px} y={py} textAnchor="middle" fontSize="13"
          fill={isSelected ? "#fbbf24" : color}
          style={{ filter: isSelected ? "drop-shadow(0 0 5px #fbbf2488)" : "none" }}>
          {GRAHA_SYMBOLS[p.name] ?? p.symbol ?? "?"}
        </text>
        <text x={px} y={py + 10} textAnchor="middle" fontSize="6.5"
          fill="hsl(225 15% 55%)">
          {p.sanskritName?.slice(0, 3) ?? p.name.slice(0, 2)}
        </text>
      </motion.g>
    );
  });
}

// ── North Indian Chart ────────────────────────────────────────────────────────
function NorthIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet }: {
  planets: PlanetData[];
  lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void;
  selectedPlanet?: PlanetData | null;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const planetsByHouse: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    if (!planetsByHouse[p.house]) planetsByHouse[p.house] = [];
    planetsByHouse[p.house].push(p);
  }
  const houseRashi = (houseId: number) => RASHI_LIST[(lagnaIdx + houseId - 1) % 12];
  const W = 4 * CELL + O * 2;

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-[380px] mx-auto">
      {/* Outer border */}
      <rect x={O} y={O} width={4*CELL} height={4*CELL}
        fill="hsl(225 25% 7%)" stroke="hsl(42 90% 64% / 0.35)" strokeWidth="1.5" rx="3" />
      {/* Center 2×2 empty area */}
      <rect x={O + CELL} y={O + CELL} width={2*CELL} height={2*CELL}
        fill="hsl(225 25% 5%)" stroke="hsl(42 90% 64% / 0.12)" strokeWidth="0.5" />
      {/* Center diagonal decoration */}
      <line x1={O+CELL} y1={O+CELL} x2={O+3*CELL} y2={O+3*CELL} stroke="hsl(42 90% 64% / 0.08)" strokeWidth="0.5" />
      <line x1={O+3*CELL} y1={O+CELL} x2={O+CELL} y2={O+3*CELL} stroke="hsl(42 90% 64% / 0.08)" strokeWidth="0.5" />
      {/* Center label */}
      <text x={O + 2*CELL} y={O + 2*CELL - 10} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.6)" fontSize="8.5" fontFamily="serif" letterSpacing="1">
        {lagnaRashi}
      </text>
      <text x={O + 2*CELL} y={O + 2*CELL + 5} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.4)" fontSize="7.5" fontFamily="serif">
        Lagna
      </text>

      {/* 12 house cells */}
      {NI_GRID.map((cell) => {
        const x = O + cell.col * CELL;
        const y = O + cell.row * CELL;
        const rashiName = houseRashi(cell.id);
        const planetsHere = planetsByHouse[cell.id] ?? [];
        const isLagna = cell.id === 1;

        // Planet center in cell
        const pCenterX = x + CELL / 2;
        const pCenterY = y + CELL / 2 + 8;

        return (
          <g key={cell.id}>
            <rect x={x} y={y} width={CELL} height={CELL}
              fill={isLagna ? "hsl(42 90% 64% / 0.06)" : "transparent"}
              stroke="hsl(42 90% 64% / 0.22)" strokeWidth="0.8" />
            {/* House number */}
            <text x={x+5} y={y+12} fontSize="8.5" fill="hsl(225 15% 40%)" fontFamily="sans-serif">{cell.id}</text>
            {/* Rashi abbr */}
            <text x={x+CELL-4} y={y+12} textAnchor="end" fontSize="8"
              fill="hsl(42 90% 64% / 0.55)" fontFamily="serif">
              {RASHI_ABBR[rashiName] ?? rashiName.slice(0,3)}
            </text>
            {isLagna && (
              <text x={x+CELL/2} y={y+23} textAnchor="middle" fontSize="7"
                fill="hsl(42 90% 64% / 0.4)">Lag</text>
            )}
            {renderPlanets(planetsHere, pCenterX, pCenterY, CELL, CELL, onPlanetClick, selectedPlanet)}
          </g>
        );
      })}
    </svg>
  );
}

// ── South Indian Chart ────────────────────────────────────────────────────────
function SouthIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet }: {
  planets: PlanetData[];
  lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void;
  selectedPlanet?: PlanetData | null;
}) {
  const lagnaRashiIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const planetsByRashi: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    const ri = RASHI_NUM[p.rashi];
    if (ri !== undefined) {
      if (!planetsByRashi[ri]) planetsByRashi[ri] = [];
      planetsByRashi[ri].push(p);
    }
  }
  const W = 4 * CELL + O * 2;

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-[380px] mx-auto">
      <rect x={O} y={O} width={4*CELL} height={4*CELL}
        fill="hsl(225 25% 7%)" stroke="hsl(42 90% 64% / 0.35)" strokeWidth="1.5" rx="3" />
      {/* Center */}
      <rect x={O+CELL} y={O+CELL} width={2*CELL} height={2*CELL}
        fill="hsl(225 25% 5%)" stroke="hsl(42 90% 64% / 0.12)" strokeWidth="0.5" />
      <text x={O+2*CELL} y={O+2*CELL-4} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.4)" fontSize="8" fontFamily="serif">Brahm AI</text>
      <text x={O+2*CELL} y={O+2*CELL+10} textAnchor="middle"
        fill="hsl(42 90% 64% / 0.25)" fontSize="7" fontFamily="serif">Janam Kundali</text>

      {SI_CELLS.map(({ rashi, row, col }) => {
        const x = O + col * CELL;
        const y = O + row * CELL;
        const rashiName = RASHI_LIST[rashi];
        const planetsHere = planetsByRashi[rashi] ?? [];
        const isLagna = rashi === lagnaRashiIdx;
        const pCenterX = x + CELL / 2;
        const pCenterY = y + CELL / 2 + 10;

        return (
          <g key={rashi}>
            <rect x={x} y={y} width={CELL} height={CELL}
              fill={isLagna ? "hsl(42 90% 64% / 0.07)" : "transparent"}
              stroke="hsl(42 90% 64% / 0.22)" strokeWidth="0.8" />
            {/* Lagna triangle marker */}
            {isLagna && (
              <polygon points={`${x+2},${y+2} ${x+16},${y+2} ${x+2},${y+16}`}
                fill="hsl(42 90% 64% / 0.65)" />
            )}
            {/* Rashi number (top-left) */}
            <text x={x+5} y={y+13} fontSize="8" fill="hsl(225 15% 40%)">{rashi+1}</text>
            {/* Rashi name */}
            <text x={x+CELL/2} y={y+14} textAnchor="middle"
              fontSize="8.5" fill="hsl(42 90% 64% / 0.7)" fontFamily="serif" fontWeight="bold">
              {RASHI_ABBR[rashiName] ?? rashiName}
            </text>
            {renderPlanets(planetsHere, pCenterX, pCenterY, CELL, CELL, onPlanetClick, selectedPlanet)}
          </g>
        );
      })}
    </svg>
  );
}

// ── Main Export ───────────────────────────────────────────────────────────────
export function KundliChart({ onPlanetClick, selectedPlanet, planets: planetsProp, lagnaRashi, chartStyle }: KundliChartProps) {
  const displayPlanets = planetsProp ?? samplePlanets;
  const lagna = lagnaRashi ?? "Tula";
  const [style, setStyle] = useState<"north" | "south">(chartStyle ?? "north");

  return (
    <div>
      <div className="flex gap-1 mb-3">
        {(["north", "south"] as const).map(s => (
          <button key={s} onClick={() => setStyle(s)}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
              style === s
                ? "bg-primary/20 text-primary border border-primary/35"
                : "text-muted-foreground hover:text-foreground border border-border/20"
            }`}>
            {s === "north" ? "North Indian" : "South Indian (HR)"}
          </button>
        ))}
      </div>
      <motion.div key={style} initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.35 }}>
        {style === "north" ? (
          <NorthIndianChart planets={displayPlanets} lagnaRashi={lagna}
            onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} />
        ) : (
          <SouthIndianChart planets={displayPlanets} lagnaRashi={lagna}
            onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} />
        )}
      </motion.div>
    </div>
  );
}
