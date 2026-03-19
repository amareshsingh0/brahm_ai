import { motion } from "framer-motion";
import { samplePlanets, type PlanetData } from "@/store/kundliStore";

interface KundliChartProps {
  onPlanetClick?: (planet: PlanetData) => void;
  selectedPlanet?: PlanetData | null;
  planets?: PlanetData[];
}

// North Indian Kundli chart - diamond layout
const housePositions = [
  { id: 1, points: "200,10 350,160 200,160", label: { x: 200, y: 90 } },
  { id: 2, points: "200,10 50,160 200,160", label: { x: 140, y: 90 } },
  { id: 3, points: "50,160 10,200 50,340", label: { x: 40, y: 250 } },
  { id: 4, points: "50,160 200,310 50,340", label: { x: 100, y: 270 } },
  { id: 5, points: "200,310 50,340 200,390", label: { x: 140, y: 350 } },
  { id: 6, points: "200,310 200,390 350,340", label: { x: 260, y: 350 } },
  { id: 7, points: "200,310 350,160 350,340", label: { x: 300, y: 270 } },
  { id: 8, points: "350,160 390,200 350,340", label: { x: 360, y: 250 } },
  { id: 9, points: "350,340 390,200 390,340", label: { x: 375, y: 290 } },
  { id: 10, points: "200,160 200,310 350,160", label: { x: 270, y: 210 } },
  { id: 11, points: "200,160 50,160 200,310", label: { x: 130, y: 210 } },
  { id: 12, points: "50,160 10,200 10,160", label: { x: 25, y: 175 } },
];

// Simplified house layout for the diamond
const houses: { id: number; x: number; y: number; w: number; h: number }[] = [
  { id: 1, x: 150, y: 0, w: 100, h: 100 },     // top center
  { id: 2, x: 50, y: 0, w: 100, h: 100 },       // top left
  { id: 3, x: 0, y: 100, w: 100, h: 100 },      // left top
  { id: 4, x: 0, y: 200, w: 100, h: 100 },      // left bottom
  { id: 5, x: 50, y: 300, w: 100, h: 100 },     // bottom left
  { id: 6, x: 150, y: 300, w: 100, h: 100 },    // bottom center
  { id: 7, x: 250, y: 300, w: 100, h: 100 },    // bottom right
  { id: 8, x: 300, y: 200, w: 100, h: 100 },    // right bottom
  { id: 9, x: 300, y: 100, w: 100, h: 100 },    // right top
  { id: 10, x: 250, y: 0, w: 100, h: 100 },     // top right
  { id: 11, x: 150, y: 100, w: 100, h: 100 },   // center top
  { id: 12, x: 150, y: 200, w: 100, h: 100 },   // center bottom
];

export function KundliChart({ onPlanetClick, selectedPlanet, planets: planetsProp }: KundliChartProps) {
  const displayPlanets = planetsProp ?? samplePlanets;
  const getPlanetsInHouse = (houseNum: number) =>
    displayPlanets.filter((p) => p.house === houseNum);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.8, ease: "easeOut" }}
      className="relative"
    >
      <svg viewBox="0 0 400 400" className="w-full max-w-md mx-auto">
        {/* Outer diamond */}
        <motion.rect
          x="10" y="10" width="380" height="380"
          fill="none"
          stroke="hsl(42 90% 64% / 0.3)"
          strokeWidth="1.5"
          rx="4"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 1.5 }}
        />

        {/* Inner diamond */}
        <motion.polygon
          points="200,10 390,200 200,390 10,200"
          fill="none"
          stroke="hsl(42 90% 64% / 0.4)"
          strokeWidth="1"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 1.5, delay: 0.3 }}
        />

        {/* Cross lines */}
        <line x1="200" y1="10" x2="200" y2="390" stroke="hsl(42 90% 64% / 0.2)" strokeWidth="0.5" />
        <line x1="10" y1="200" x2="390" y2="200" stroke="hsl(42 90% 64% / 0.2)" strokeWidth="0.5" />

        {/* Diagonal lines forming houses */}
        <line x1="10" y1="10" x2="200" y2="200" stroke="hsl(42 90% 64% / 0.15)" strokeWidth="0.5" />
        <line x1="390" y1="10" x2="200" y2="200" stroke="hsl(42 90% 64% / 0.15)" strokeWidth="0.5" />
        <line x1="10" y1="390" x2="200" y2="200" stroke="hsl(42 90% 64% / 0.15)" strokeWidth="0.5" />
        <line x1="390" y1="390" x2="200" y2="200" stroke="hsl(42 90% 64% / 0.15)" strokeWidth="0.5" />

        {/* House numbers */}
        {houses.map((house) => (
          <text
            key={house.id}
            x={house.x + house.w / 2}
            y={house.y + 20}
            textAnchor="middle"
            className="fill-muted-foreground/40 text-[10px]"
          >
            {house.id}
          </text>
        ))}

        {/* Planets in houses */}
        {houses.map((house) => {
          const planets = getPlanetsInHouse(house.id);
          return planets.map((planet, i) => (
            <motion.g
              key={planet.name}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.5 + i * 0.1 }}
              onClick={() => onPlanetClick?.(planet)}
              className="cursor-pointer"
            >
              <text
                x={house.x + house.w / 2 + (i % 2 === 0 ? -15 : 15)}
                y={house.y + 45 + Math.floor(i / 2) * 22}
                textAnchor="middle"
                className="text-sm font-bold transition-all duration-200"
                fill={selectedPlanet?.name === planet.name ? "hsl(42 90% 64%)" : planet.color}
                style={{
                  filter: selectedPlanet?.name === planet.name ? "drop-shadow(0 0 6px hsl(42 90% 64% / 0.6))" : "none"
                }}
              >
                {planet.symbol}
              </text>
              <text
                x={house.x + house.w / 2 + (i % 2 === 0 ? -15 : 15)}
                y={house.y + 57 + Math.floor(i / 2) * 22}
                textAnchor="middle"
                className="fill-muted-foreground text-[7px]"
              >
                {planet.name.slice(0, 2)}
              </text>
            </motion.g>
          ));
        })}

        {/* Center label */}
        <text x="200" y="195" textAnchor="middle" className="fill-primary/50 text-[9px] font-display uppercase tracking-widest">
          Janam
        </text>
        <text x="200" y="210" textAnchor="middle" className="fill-primary/30 text-[8px]">
          Kundli
        </text>
      </svg>
    </motion.div>
  );
}
