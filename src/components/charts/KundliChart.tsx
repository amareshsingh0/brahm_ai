import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";
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
const RASHI_TO_KEY: Record<string, string> = {
  Mesha:"mesha", Vrishabha:"vrishabha", Mithuna:"mithuna", Karka:"karka",
  Simha:"simha", Kanya:"kanya", Tula:"tula", Vrischika:"vrischika",
  Dhanu:"dhanu", Makara:"makara", Kumbha:"kumbha", Meena:"meena",
};
const RASHI_NUM: Record<string, number> = {};
RASHI_LIST.forEach((r, i) => { RASHI_NUM[r] = i; });

const GRAHA_SYMBOLS: Record<string, string> = {
  Sun:"☉︎\uFE0E", Moon:"☽︎\uFE0E", Mars:"♂︎\uFE0E", Mercury:"☿︎\uFE0E",
  Jupiter:"♃︎\uFE0E", Venus:"♀︎\uFE0E", Saturn:"♄︎\uFE0E", Rahu:"☊︎\uFE0E", Ketu:"☋︎\uFE0E",
};
const PLANET_COLORS: Record<string, string> = {
  Sun:"#D97706", Moon:"#4F46E5", Mars:"#DC2626", Mercury:"#16A34A",
  Jupiter:"#B45309", Venus:"#9333EA", Saturn:"#334155", Rahu:"#0369A1", Ketu:"#C2410C",
};

// Light theme tokens
const BG        = "hsl(220 20% 97%)";   // chart canvas
const BG_CENTER = "hsl(220 15% 93%)";   // center / inner box
const GOLD      = "hsl(38 80% 32%)";    // darker gold — readable on light
const GOLD_DIM  = "hsl(38 80% 32% / 0.5)";
const GOLD_FAINT= "hsl(38 80% 32% / 0.25)";
const LINE      = "hsl(38 80% 32% / 0.22)";
const NUM_TEXT  = "hsl(215 20% 38%)";   // house numbers
const LABEL_SUB = "hsl(215 20% 50%)";   // sub-labels

const CELL = 110;
const O = 5;

// ── Planet renderer ────────────────────────────────────────────────────────────
// Symbols stay compact/close (original feel).
// Row spacing is increased so labels never collide with the symbol on the next row.
function renderPlanets(
  planetsHere: PlanetData[],
  cx: number, cy: number,
  onPlanetClick?: (p: PlanetData) => void,
  selectedPlanet?: PlanetData | null,
  tFn?: (key: string, opts?: object) => string,
) {
  const n = planetsHere.length;
  if (n === 0) return null;

  // Adaptive font sizes — reduce only for many planets
  const symSize = n <= 4 ? 16 : n <= 6 ? 13 : 11;
  const lblSize = n <= 4 ? 9  : n <= 6 ? 8  : 7;

  // rowH must satisfy: rowH > symSize*0.3 + lblSize*1.3 + 3 + buffer
  // so that label of row N doesn't touch symbol of row N+1.
  // rowH = symSize + lblSize + 9 guarantees ~8px clearance (derived algebraically).
  const rowH = symSize + lblSize + 9;

  // Column layout
  // n≤2  → single column (stack vertically, stays compact at cx)
  // n≤6  → 2 columns at cx ± colOff
  // n>6  → 3 columns at cx-colOff, cx, cx+colOff
  const cols   = n <= 2 ? 1 : n <= 6 ? 2 : 3;
  const colOff = cols === 2 ? 15 : 14;   // tight original-style spacing
  const rows   = Math.ceil(n / cols);

  const totalH = (rows - 1) * rowH;
  const startY = cy - totalH / 2;   // vertically centered in available space

  return planetsHere.map((p, i) => {
    const col = i % cols;
    const row = Math.floor(i / cols);

    // Horizontal position
    const px =
      cols === 1 ? cx :
      cols === 2 ? (col === 0 ? cx - colOff : cx + colOff) :
      cx + (col - 1) * colOff;   // col 0 → cx-14, col 1 → cx, col 2 → cx+14

    const py = startY + row * rowH;
    const isSelected = selectedPlanet?.name === p.name;
    const color = PLANET_COLORS[p.name] ?? "#64748b";

    return (
      <motion.g key={p.name} onClick={() => onPlanetClick?.(p)}
        className="cursor-pointer"
        initial={{ opacity: 0 }} animate={{ opacity: 1 }}
        transition={{ delay: 0.2 + i * 0.04 }}>
        {isSelected && (
          <circle cx={px} cy={py - 4} r={symSize * 0.6} fill={`${color}28`} />
        )}
        <text x={px} y={py} textAnchor="middle" fontSize={symSize}
          fontWeight="600"
          fontFamily="'Segoe UI Symbol', 'Apple Symbols', 'Noto Sans Symbols', serif"
          fill={isSelected ? "#d97706" : color}
          style={{ filter: `drop-shadow(0 0 3px ${isSelected ? "#d9770660" : color + "55"})` }}>
          {GRAHA_SYMBOLS[p.name] ?? p.symbol ?? "?"}
        </text>
        <text x={px} y={py + lblSize + 3} textAnchor="middle" fontSize={lblSize}
          fontWeight="500"
          fill={isSelected ? GOLD : LABEL_SUB}>
          {tFn ? tFn(`planet.${p.name}`, { defaultValue: p.sanskritName ?? p.name }) : (p.sanskritName ?? p.name)}
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

function NorthIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet, tFn }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
  tFn?: (key: string, opts?: object) => string;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const byHouse: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    if (!byHouse[p.house]) byHouse[p.house] = [];
    byHouse[p.house].push(p);
  }
  const houseRashi = (id: number) => RASHI_LIST[(lagnaIdx + id - 1) % 12];
  const rashiLabel = (rn: string) => tFn ? tFn(`data.rashi.${RASHI_TO_KEY[rn] ?? rn.toLowerCase()}.name`, { defaultValue: rn }) : rn;
  const lagnaLabel = tFn ? tFn("chart.lagna", { defaultValue: "Lagna" }) : "Lagna";
  const lagLabel = tFn ? tFn("chart.lag", { defaultValue: "Lag" }) : "Lag";
  const W = 4 * CELL + O * 2;

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-full mx-auto">
      <rect x={O} y={O} width={4*CELL} height={4*CELL}
        fill={BG} stroke={GOLD_DIM} strokeWidth="1.5" rx="4" />
      <rect x={O+CELL} y={O+CELL} width={2*CELL} height={2*CELL}
        fill={BG_CENTER} stroke={GOLD_FAINT} strokeWidth="0.5" />
      <line x1={O+CELL} y1={O+CELL} x2={O+3*CELL} y2={O+3*CELL} stroke={GOLD_FAINT} strokeWidth="0.6" />
      <line x1={O+3*CELL} y1={O+CELL} x2={O+CELL} y2={O+3*CELL} stroke={GOLD_FAINT} strokeWidth="0.6" />
      <text x={O+2*CELL} y={O+2*CELL-12} textAnchor="middle"
        fill={GOLD_DIM} fontSize="11" fontFamily="serif" letterSpacing="1">
        {rashiLabel(lagnaRashi)}
      </text>
      <text x={O+2*CELL} y={O+2*CELL+5} textAnchor="middle"
        fill={LABEL_SUB} fontSize="9" fontFamily="serif">{lagnaLabel}</text>

      {NI_GRID.map(cell => {
        const x = O + cell.col * CELL;
        const y = O + cell.row * CELL;
        const rn = houseRashi(cell.id);
        const planetsHere = byHouse[cell.id] ?? [];
        const isL = cell.id === 1;
        return (
          <g key={cell.id}>
            <rect x={x} y={y} width={CELL} height={CELL}
              fill={isL ? "hsl(38 80% 32% / 0.07)" : "transparent"}
              stroke={LINE} strokeWidth="0.8" />
            <text x={x+6} y={y+14} fontSize="10" fill={NUM_TEXT} fontFamily="sans-serif">{cell.id}</text>
            <text x={x+CELL-5} y={y+14} textAnchor="end" fontSize="9.5"
              fill={GOLD} fontFamily="serif">
              {rashiLabel(rn)}
            </text>
            {isL && <text x={x+CELL/2} y={y+26} textAnchor="middle" fontSize="8.5"
              fill={LABEL_SUB}>{lagLabel}</text>}
            {renderPlanets(planetsHere, x+CELL/2, y+CELL/2+10, onPlanetClick, selectedPlanet, tFn)}
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

function SouthIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet, tFn }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
  tFn?: (key: string, opts?: object) => string;
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
  const rashiLabel = (rn: string) => tFn ? tFn(`data.rashi.${RASHI_TO_KEY[rn] ?? rn.toLowerCase()}.name`, { defaultValue: rn }) : rn;
  const W = 4 * CELL + O * 2;

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-full mx-auto">
      <rect x={O} y={O} width={4*CELL} height={4*CELL}
        fill={BG} stroke={GOLD_DIM} strokeWidth="1.5" rx="4" />
      <rect x={O+CELL} y={O+CELL} width={2*CELL} height={2*CELL}
        fill={BG_CENTER} stroke={GOLD_FAINT} strokeWidth="0.5" />
      <text x={O+2*CELL} y={O+2*CELL-5} textAnchor="middle"
        fill={GOLD_DIM} fontSize="9.5" fontFamily="serif">Brahm AI</text>
      <text x={O+2*CELL} y={O+2*CELL+9} textAnchor="middle"
        fill={LABEL_SUB} fontSize="8.5" fontFamily="serif">Kundali</text>

      {SI_CELLS.map(({ rashi, row, col }) => {
        const x = O + col * CELL;
        const y = O + row * CELL;
        const rn = RASHI_LIST[rashi];
        const planetsHere = byRashi[rashi] ?? [];
        const isL = rashi === lagnaIdx;
        return (
          <g key={rashi}>
            <rect x={x} y={y} width={CELL} height={CELL}
              fill={isL ? "hsl(38 80% 32% / 0.08)" : "transparent"}
              stroke={LINE} strokeWidth="0.8" />
            {isL && <polygon points={`${x+2},${y+2} ${x+18},${y+2} ${x+2},${y+18}`}
              fill={GOLD} />}
            <text x={x+6} y={y+15} fontSize="9.5" fill={NUM_TEXT}>{rashi+1}</text>
            <text x={x+CELL/2} y={y+16} textAnchor="middle"
              fontSize="10" fill={GOLD} fontFamily="serif" fontWeight="600">
              {rashiLabel(rn)}
            </text>
            {renderPlanets(planetsHere, x+CELL/2, y+CELL/2+12, onPlanetClick, selectedPlanet, tFn)}
          </g>
        );
      })}
    </svg>
  );
}

// ── East Indian (Diamond) Chart ───────────────────────────────────────────────
function EastIndianChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet, tFn }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
  tFn?: (key: string, opts?: object) => string;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const byHouse: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    if (!byHouse[p.house]) byHouse[p.house] = [];
    byHouse[p.house].push(p);
  }
  const houseRashi = (id: number) => RASHI_LIST[(lagnaIdx + id - 1) % 12];
  const rashiLabel = (rn: string) => tFn ? tFn(`data.rashi.${RASHI_TO_KEY[rn] ?? rn.toLowerCase()}.name`, { defaultValue: rn }) : rn;
  const lagnaLabel = tFn ? tFn("chart.lagna", { defaultValue: "Lagna" }) : "Lagna";
  const W = 450; const C = W / 2;

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
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-full mx-auto">
      <rect x={0} y={0} width={W} height={W} fill={BG} rx="4" />
      <polygon points={`${C},8 ${W-8},${C} ${C},${W-8} 8,${C}`}
        fill="none" stroke={GOLD_DIM} strokeWidth="1.5" />
      <polygon points={`${C},${C-80} ${C+80},${C} ${C},${C+80} ${C-80},${C}`}
        fill={BG_CENTER} stroke={GOLD_FAINT} strokeWidth="0.8" />
      <line x1={C} y1={8} x2={C} y2={W-8} stroke={LINE} strokeWidth="0.7" />
      <line x1={8} y1={C} x2={W-8} y2={C} stroke={LINE} strokeWidth="0.7" />
      <line x1={8} y1={8} x2={W-8} y2={W-8} stroke={GOLD_FAINT} strokeWidth="0.6" />
      <line x1={W-8} y1={8} x2={8} y2={W-8} stroke={GOLD_FAINT} strokeWidth="0.6" />

      {EAST_HOUSES.map(({ id, cx: hcx, cy: hcy }) => {
        const rn = houseRashi(id);
        const planetsHere = byHouse[id] ?? [];
        const isL = id === 1;
        return (
          <g key={id}>
            <text x={hcx} y={hcy - 14} textAnchor="middle" fontSize="9.5" fill={NUM_TEXT}>{id}</text>
            <text x={hcx} y={hcy - 2} textAnchor="middle" fontSize="10"
              fill={isL ? GOLD : GOLD_DIM} fontFamily="serif">
              {rashiLabel(rn)}{isL ? " ▲" : ""}
            </text>
            {renderPlanets(planetsHere, hcx, hcy + 14, onPlanetClick, selectedPlanet, tFn)}
          </g>
        );
      })}

      <text x={C} y={C - 6} textAnchor="middle" fill={GOLD_DIM} fontSize="9" fontFamily="serif">East</text>
      <text x={C} y={C + 8} textAnchor="middle" fill={LABEL_SUB} fontSize="8.5" fontFamily="serif">{rashiLabel(lagnaRashi)} {lagnaLabel}</text>
    </svg>
  );
}

// ── Western Circular Chart ────────────────────────────────────────────────────
function WesternChart({ planets, lagnaRashi, onPlanetClick, selectedPlanet, tFn }: {
  planets: PlanetData[]; lagnaRashi: string;
  onPlanetClick?: (p: PlanetData) => void; selectedPlanet?: PlanetData | null;
  tFn?: (key: string, opts?: object) => string;
}) {
  const lagnaIdx = RASHI_NUM[lagnaRashi] ?? 0;
  const byHouse: Record<number, PlanetData[]> = {};
  for (const p of planets) {
    if (!byHouse[p.house]) byHouse[p.house] = [];
    byHouse[p.house].push(p);
  }
  const houseRashi = (id: number) => RASHI_LIST[(lagnaIdx + id - 1) % 12];
  const rashiLabel = (rn: string) => tFn ? tFn(`data.rashi.${RASHI_TO_KEY[rn] ?? rn.toLowerCase()}.name`, { defaultValue: rn }) : rn;
  const lagnaLabel = tFn ? tFn("chart.lagna", { defaultValue: "Lagna" }) : "Lagna";
  const W = 450; const C = W / 2;
  const R1 = 210; const R2 = 165; const R3 = 65;

  const RASHI_SYMBOLS: Record<string, string> = {
    Mesha:"♈︎",Vrishabha:"♉︎",Mithuna:"♊︎",Karka:"♋︎",
    Simha:"♌︎",Kanya:"♍︎",Tula:"♎︎",Vrischika:"♏︎",
    Dhanu:"♐︎",Makara:"♑︎",Kumbha:"♒︎",Meena:"♓︎",
  };

  return (
    <svg viewBox={`0 0 ${W} ${W}`} className="w-full max-w-full mx-auto">
      <rect x={0} y={0} width={W} height={W} fill={BG} rx="4" />
      <circle cx={C} cy={C} r={R1} fill="none" stroke={GOLD_DIM} strokeWidth="1.5" />
      <circle cx={C} cy={C} r={R2} fill="none" stroke={LINE} strokeWidth="0.8" />
      <circle cx={C} cy={C} r={R3} fill={BG_CENTER} stroke={GOLD_FAINT} strokeWidth="0.5" />

      {Array.from({ length: 12 }, (_, i) => {
        const angle = Math.PI - (i * Math.PI / 6);
        return (
          <line key={i}
            x1={C + R3 * Math.cos(angle)} y1={C - R3 * Math.sin(angle)}
            x2={C + R1 * Math.cos(angle)} y2={C - R1 * Math.sin(angle)}
            stroke={LINE} strokeWidth="0.7" />
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
            <text x={C + rMid * Math.cos(mid)} y={C - rMid * Math.sin(mid) + 4}
              textAnchor="middle" fontSize={isL ? "15" : "13"}
              fill={isL ? GOLD : GOLD_DIM}>
              {RASHI_SYMBOLS[rn] ?? ""}
            </text>
            <text x={C + rH * Math.cos(mid)} y={C - rH * Math.sin(mid) + 4}
              textAnchor="middle" fontSize="9" fill={NUM_TEXT}>{houseId}</text>
            <text x={C + rH * Math.cos(mid)} y={C - rH * Math.sin(mid) + 15}
              textAnchor="middle" fontSize="7.5" fill={LABEL_SUB} fontFamily="serif">
              {rashiLabel(rn)}
            </text>
            {(() => {
              const n = planetsHere.length;
              if (n === 0) return null;
              // Adaptive sizes
              const symSz = n <= 3 ? 14 : n <= 5 ? 12 : 10;
              const lblSz = n <= 3 ? 7.5 : n <= 5 ? 7 : 6.5;
              const rowH  = symSz + lblSz + 6;  // vertical gap between rows (radial)
              // tangential columns, radial rows
              const cols = n <= 2 ? n : n <= 4 ? 2 : n <= 6 ? 3 : 3;
              const rows = Math.ceil(n / cols);
              const tOff = n <= 2 ? 18 : n <= 4 ? 16 : 14;  // tangential px per col slot
              const colCenter = (cols - 1) / 2;
              const rowCenter = (rows - 1) / 2;
              const perpAngle = mid + Math.PI / 2;

              return planetsHere.map((p, pi) => {
                const col = pi % cols;
                const row = Math.floor(pi / cols);
                // tangential displacement
                const tang = (col - colCenter) * tOff;
                // radial displacement (positive = toward center)
                const rOff = (row - rowCenter) * rowH;
                const r = rP - rOff;
                const ppX = C + r * Math.cos(mid) + tang * Math.cos(perpAngle);
                const ppY = C - r * Math.sin(mid) - tang * Math.sin(perpAngle);
                const color = PLANET_COLORS[p.name] ?? "#64748b";
                const isSel = selectedPlanet?.name === p.name;
                const lbl = tFn ? tFn(`planet.${p.name}`, { defaultValue: p.sanskritName ?? p.name }) : (p.sanskritName ?? p.name);
                return (
                  <motion.g key={p.name} onClick={() => onPlanetClick?.(p)}
                    className="cursor-pointer"
                    initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                    transition={{ delay: 0.25 + pi * 0.04 }}>
                    {isSel && <circle cx={ppX} cy={ppY} r={symSz * 0.65} fill={`${color}28`} />}
                    <text x={ppX} y={ppY + symSz * 0.38} textAnchor="middle" fontSize={symSz}
                      fontWeight="600"
                      fontFamily="'Segoe UI Symbol', 'Apple Symbols', 'Noto Sans Symbols', serif"
                      fill={isSel ? "#d97706" : color}
                      style={{ filter: `drop-shadow(0 0 3px ${isSel ? "#d9770660" : color + "55"})` }}>
                      {GRAHA_SYMBOLS[p.name] ?? p.symbol ?? "?"}
                    </text>
                    <text x={ppX} y={ppY + symSz * 0.38 + lblSz + 2} textAnchor="middle" fontSize={lblSz}
                      fontWeight="500" fill={isSel ? GOLD : LABEL_SUB}>
                      {lbl}
                    </text>
                  </motion.g>
                );
              });
            })()}
          </g>
        );
      })}

      <text x={C} y={C - 8} textAnchor="middle" fill={GOLD_DIM} fontSize="10" fontFamily="serif">{rashiLabel(lagnaRashi)}</text>
      <text x={C} y={C + 6} textAnchor="middle" fill={LABEL_SUB} fontSize="8.5" fontFamily="serif">{lagnaLabel}</text>
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

  const { t } = useTranslation();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const tFn = (key: string, opts?: object) => t(key, opts as any);
  const STYLES: { key: ChartStyle; label: string }[] = [
    { key: "north", label: t("chart.north", { defaultValue: "North" }) },
    { key: "south", label: t("chart.south", { defaultValue: "South" }) },
    { key: "east",  label: t("chart.east",  { defaultValue: "East" }) },
    { key: "west",  label: t("chart.west",  { defaultValue: "West" }) },
  ];

  return (
    <div>
      {showStyleToggle && (
        <div className="flex gap-1.5 mb-3 flex-wrap">
          {STYLES.map(s => (
            <button key={s.key} onClick={() => setSettings({ chartStyle: s.key })}
              className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                style === s.key
                  ? "bg-primary/15 text-primary border border-primary/35"
                  : "text-muted-foreground hover:text-foreground border border-border"
              }`}>
              {s.label}
            </button>
          ))}
        </div>
      )}
      <motion.div key={style} initial={{ opacity: 0, scale: 0.97 }} animate={{ opacity: 1, scale: 1 }} transition={{ duration: 0.3 }}>
        {style === "north" ? (
          <NorthIndianChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} tFn={tFn} />
        ) : style === "south" ? (
          <SouthIndianChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} tFn={tFn} />
        ) : style === "east" ? (
          <EastIndianChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} tFn={tFn} />
        ) : (
          <WesternChart planets={displayPlanets} lagnaRashi={lagna} onPlanetClick={onPlanetClick} selectedPlanet={selectedPlanet} tFn={tFn} />
        )}
      </motion.div>
    </div>
  );
}
