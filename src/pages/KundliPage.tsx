import { useState } from "react";
import { motion } from "framer-motion";
import { KundliChart } from "@/components/charts/KundliChart";
import { PlanetInfoPanel } from "@/components/cards/PlanetInfoPanel";
import { samplePlanets, yogasData, useKundliStore, type PlanetData } from "@/store/kundliStore";
import { Link } from "react-router-dom";
import { Zap } from "lucide-react";
import PageBot from '@/components/PageBot';

const GRAHA_SYMBOLS: Record<string, string> = {
  Surya: "☉", Chandra: "☽", Mangal: "♂", Budh: "☿",
  Guru: "♃", Shukra: "♀", Shani: "♄", Rahu: "☊", Ketu: "☋",
};
const GRAHA_EN: Record<string, string> = {
  Surya: "Sun", Chandra: "Moon", Mangal: "Mars", Budh: "Mercury",
  Guru: "Jupiter", Shukra: "Venus", Shani: "Saturn", Rahu: "Rahu", Ketu: "Ketu",
};
const GRAHA_COLOR: Record<string, string> = {
  Surya: "hsl(42 90% 64%)", Chandra: "hsl(220 20% 90%)", Mangal: "hsl(0 70% 55%)",
  Budh: "hsl(140 50% 55%)", Guru: "hsl(42 70% 60%)", Shukra: "hsl(292 84% 61%)",
  Shani: "hsl(220 30% 45%)", Rahu: "hsl(225 25% 50%)", Ketu: "hsl(30 60% 45%)",
};

export default function KundliPage() {
  const kundaliData = useKundliStore((s) => s.kundaliData);
  const [selectedPlanet, setSelectedPlanet] = useState<PlanetData | null>(null);
  const [activeTab, setActiveTab] = useState("Janam");

  // Map API kundaliData to PlanetData[] for display, fallback to samplePlanets
  const planets: PlanetData[] = kundaliData
    ? Object.entries(kundaliData.grahas).map(([gname, g]) => ({
        name: GRAHA_EN[gname] ?? gname,
        symbol: GRAHA_SYMBOLS[gname] ?? "?",
        rashi: g.rashi,
        house: g.house,
        degree: `${g.degree.toFixed(1)}°`,
        nakshatra: g.nakshatra,
        color: GRAHA_COLOR[gname] ?? "hsl(0 0% 60%)",
        sanskritName: gname,
      }))
    : samplePlanets;

  const activeYogas = kundaliData
    ? kundaliData.yogas.slice(0, 3).map((y) => ({ name: y.name, planets: [y.desc], present: true }))
    : yogasData.filter((y) => y.present).slice(0, 3);

  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">My Kundli</h1>
        <p className="text-sm text-muted-foreground">Click on any planet to see its interpretation</p>
      </motion.div>

      <div className="grid lg:grid-cols-[1fr_360px] gap-6">
        {/* Chart */}
        <div className="cosmic-card rounded-xl p-6">
          <div className="flex gap-2 mb-4 border-b border-border/30 pb-3">
            {["Janam", "Navamsa", "Transit"].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`text-xs px-3 py-1.5 rounded-full transition-colors ${
                  activeTab === tab ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
                aria-label={`${tab} chart view`}
              >
                {tab}
              </button>
            ))}
          </div>
          <KundliChart onPlanetClick={setSelectedPlanet} selectedPlanet={selectedPlanet} planets={planets} />
        </div>

        {/* Right panel */}
        <div className="space-y-4">
          {selectedPlanet ? (
            <PlanetInfoPanel planet={selectedPlanet} onClose={() => setSelectedPlanet(null)} />
          ) : (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="cosmic-card rounded-xl p-6">
              <h3 className="font-display text-sm text-foreground mb-4">Planet Positions</h3>
              <div className="space-y-2">
                {planets.map((p) => (
                  <button
                    key={p.name}
                    onClick={() => setSelectedPlanet(p)}
                    className="w-full flex items-center gap-3 p-2 rounded-lg hover:bg-muted/30 transition-colors text-left"
                    aria-label={`${p.name} in ${p.rashi}, house ${p.house}, ${p.degree}`}
                  >
                    <span className="text-lg" style={{ color: p.color }}>{p.symbol}</span>
                    <div className="flex-1">
                      <span className="text-sm text-foreground">{p.name}</span>
                      <span className="text-[10px] text-muted-foreground/60 ml-1">({p.sanskritName})</span>
                      <span className="text-xs text-muted-foreground ml-2">{p.rashi} · H{p.house}</span>
                    </div>
                    <span className="text-xs text-muted-foreground">{p.degree}</span>
                  </button>
                ))}
              </div>
            </motion.div>
          )}

          {/* Mini yogas */}
          <div className="cosmic-card rounded-xl p-5">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Zap className="h-4 w-4 text-primary" />
                <h3 className="font-display text-sm text-foreground">Active Yogas</h3>
              </div>
              <Link to="/yogas" className="text-[10px] text-primary hover:underline">View All →</Link>
            </div>
            <div className="space-y-2">
              {activeYogas.map((y) => (
                <div key={y.name} className="bg-muted/20 rounded-lg p-2.5">
                  <p className="text-xs text-foreground font-medium">{y.name}</p>
                  <p className="text-[10px] text-muted-foreground">{y.planets.join(" + ")}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
      <PageBot pageContext="kundali" pageData={kundaliData ?? {}} />
    </div>
  );
}
