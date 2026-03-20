/**
 * GocharPage — Current Planetary Transits
 *
 * Section 1: Current sky — all 9 grahas + their rashi today
 * Section 2: Personal analysis — transits over natal chart (if kundali available)
 * Section 3: Opportunities + Cautions from current sky
 */

import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { TrendingUp, AlertTriangle, RefreshCw, User } from "lucide-react";
import { api } from "@/lib/api";
import { useKundliStore } from "@/store/kundliStore";
import PageBot from "@/components/PageBot";

const GRAHA_SYMBOL: Record<string, string> = {
  Surya: "☉", Chandra: "☽", Mangal: "♂", Budh: "☿",
  Guru: "♃",  Shukra: "♀",  Shani: "♄", Rahu: "☊", Ketu: "☋",
};
const GRAHA_EN: Record<string, string> = {
  Surya: "Sun", Chandra: "Moon", Mangal: "Mars", Budh: "Mercury",
  Guru: "Jupiter", Shukra: "Venus", Shani: "Saturn", Rahu: "Rahu", Ketu: "Ketu",
};
const GRAHA_COLOR: Record<string, string> = {
  Surya:   "#FFB347", Chandra: "#C0D8FF", Mangal: "#FF6B6B",
  Budh:    "#7FD17F", Guru:    "#FFD700",  Shukra: "#D89FFF",
  Shani:   "#778899", Rahu:    "#6A8FD0",  Ketu:   "#C87941",
};
const GRAHA_DESC: Record<string, string> = {
  Surya:   "Vitality & Soul",    Chandra: "Mind & Emotions",
  Mangal:  "Energy & Action",    Budh:    "Intellect & Speech",
  Guru:    "Wisdom & Fortune",   Shukra:  "Love & Beauty",
  Shani:   "Karma & Discipline", Rahu:    "Ambition & Desire",
  Ketu:    "Spirituality",
};
const ORDER = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];

interface CurrentPositions {
  [planet: string]: { rashi: string; degree: number };
}
interface AnalysisResult {
  current_positions: Record<string, string>;
  opportunities: string[];
  cautions: string[];
  sade_sati: boolean;
  summary: string;
}

export default function GocharPage() {
  const kundaliData  = useKundliStore((s) => s.kundaliData);
  const birthDetails = useKundliStore((s) => s.birthDetails);

  const [positions, setPositions] = useState<CurrentPositions | null>(null);
  const [analysis,  setAnalysis]  = useState<AnalysisResult | null>(null);
  const [loading,   setLoading]   = useState(true);
  const [loadingAn, setLoadingAn] = useState(false);
  const [error,     setError]     = useState<string | null>(null);
  const [lastFetch, setLastFetch] = useState<Date | null>(null);

  const fetchPositions = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get<{ status: string; positions: CurrentPositions }>("/api/gochar");
      if (res.status === "ok") {
        setPositions(res.positions);
        setLastFetch(new Date());
      }
    } catch {
      setError("Could not load planet positions. Please check your connection.");
    } finally {
      setLoading(false);
    }
  };

  const fetchAnalysis = async () => {
    if (!kundaliData) return;
    const lagnaRashi = kundaliData.lagna?.rashi;
    const moonRashi  = kundaliData.grahas?.Chandra?.rashi;
    if (!lagnaRashi || !moonRashi) return;

    setLoadingAn(true);
    try {
      const res = await api.post<{ status: string } & AnalysisResult>(
        "/api/gochar/analyze",
        { lagna_rashi: lagnaRashi, moon_rashi: moonRashi, name: birthDetails?.name ?? "" }
      );
      if (res.status === "ok") setAnalysis(res);
    } catch {
      // silently fail
    } finally {
      setLoadingAn(false);
    }
  };

  useEffect(() => { fetchPositions(); }, []);
  useEffect(() => { if (positions) fetchAnalysis(); }, [positions, kundaliData]);

  const pageData = analysis
    ? {
        gochar_summary: analysis.summary,
        opportunities:  analysis.opportunities,
        cautions:       analysis.cautions,
        sade_sati:      analysis.sade_sati,
        lagna_rashi:    kundaliData?.lagna?.rashi,
        moon_rashi:     kundaliData?.grahas?.Chandra?.rashi,
      }
    : {};

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-5xl mx-auto">

      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl text-foreground text-glow-gold">Gochar</h1>
            <p className="text-sm text-muted-foreground mt-1">
              Current planetary transits — where each planet is in the sky today
            </p>
          </div>
          <button
            onClick={fetchPositions}
            className="p-2 rounded-lg hover:bg-muted/40 text-muted-foreground hover:text-foreground transition-colors"
            title="Refresh"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
        {lastFetch && (
          <p className="text-xs text-muted-foreground/60 mt-1">
            Updated: {lastFetch.toLocaleTimeString()}
          </p>
        )}
      </motion.div>

      {/* Section 1: Current Positions */}
      {loading ? (
        <div className="grid grid-cols-3 sm:grid-cols-5 gap-3">
          {ORDER.map((p) => (
            <div key={p} className="cosmic-card rounded-xl p-4 animate-pulse h-24" />
          ))}
        </div>
      ) : error ? (
        <div className="cosmic-card rounded-xl p-6 text-center text-destructive text-sm">{error}</div>
      ) : positions ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="grid grid-cols-3 sm:grid-cols-5 gap-3"
        >
          {ORDER.map((planet) => {
            const pos = positions[planet];
            if (!pos) return null;
            return (
              <div
                key={planet}
                className="cosmic-card rounded-xl p-3 sm:p-4 flex flex-col items-center gap-1 hover:border-primary/30 transition-colors"
              >
                <span className="text-2xl" style={{ color: GRAHA_COLOR[planet] }}>
                  {GRAHA_SYMBOL[planet]}
                </span>
                <span className="text-xs font-medium text-foreground">{GRAHA_EN[planet]}</span>
                <span className="text-xs text-star-gold font-semibold">{pos.rashi}</span>
                <span className="text-[10px] text-muted-foreground">{pos.degree.toFixed(1)}°</span>
                <span className="text-[9px] text-muted-foreground/60 text-center leading-tight hidden sm:block">
                  {GRAHA_DESC[planet]}
                </span>
              </div>
            );
          })}
        </motion.div>
      ) : null}

      {/* Section 2: Personal Analysis */}
      {!kundaliData ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="cosmic-card rounded-xl p-6 flex items-center gap-4 border border-dashed border-muted"
        >
          <User className="w-8 h-8 text-muted-foreground/40 shrink-0" />
          <div>
            <p className="text-sm text-foreground font-medium">Personal Transit Analysis</p>
            <p className="text-xs text-muted-foreground mt-1">
              Generate your birth chart to see how today's transits affect you personally —
              which planet is moving through which house in your natal chart.
            </p>
          </div>
        </motion.div>
      ) : loadingAn ? (
        <div className="cosmic-card rounded-xl p-6 space-y-3 animate-pulse">
          <div className="h-4 bg-muted/30 rounded w-1/3" />
          <div className="h-3 bg-muted/20 rounded w-full" />
          <div className="h-3 bg-muted/20 rounded w-4/5" />
        </div>
      ) : analysis ? (
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">

          <div className="flex items-center gap-2">
            <User className="w-4 h-4 text-star-gold" />
            <h2 className="text-sm font-semibold text-foreground">
              {birthDetails?.name ? `${birthDetails.name}'s Transit Analysis` : "Your Transit Analysis"}
            </h2>
            {analysis.sade_sati && (
              <span className="text-[10px] bg-destructive/20 text-destructive px-2 py-0.5 rounded-full font-medium">
                Sade Sati Active
              </span>
            )}
          </div>

          {/* Personal positions table */}
          <div className="cosmic-card rounded-xl overflow-hidden">
            <div className="px-4 py-2 border-b border-border/30 bg-muted/10">
              <p className="text-xs text-muted-foreground">Transit positions relative to your natal chart</p>
            </div>
            <div className="divide-y divide-border/20">
              {Object.entries(analysis.current_positions).map(([planet, pos]) => (
                <div key={planet} className="flex items-center px-4 py-2.5 gap-3">
                  <span className="text-base w-6 text-center" style={{ color: GRAHA_COLOR[planet] }}>
                    {GRAHA_SYMBOL[planet]}
                  </span>
                  <span className="text-xs text-foreground w-20">{GRAHA_EN[planet] ?? planet}</span>
                  <span className="text-xs text-star-gold font-medium">{pos}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Opportunities */}
          {analysis.opportunities.length > 0 && (
            <div className="cosmic-card rounded-xl p-4 space-y-2 border border-green-500/20">
              <div className="flex items-center gap-2 mb-1">
                <TrendingUp className="w-4 h-4 text-green-400" />
                <h3 className="text-sm font-semibold text-green-400">Opportunities</h3>
              </div>
              {analysis.opportunities.map((o, i) => (
                <p key={i} className="text-xs text-muted-foreground leading-relaxed pl-1 border-l-2 border-green-500/30">
                  {o}
                </p>
              ))}
            </div>
          )}

          {/* Cautions */}
          {analysis.cautions.length > 0 && (
            <div className="cosmic-card rounded-xl p-4 space-y-2 border border-amber-500/20">
              <div className="flex items-center gap-2 mb-1">
                <AlertTriangle className="w-4 h-4 text-amber-400" />
                <h3 className="text-sm font-semibold text-amber-400">Cautions</h3>
              </div>
              {analysis.cautions.map((c, i) => (
                <p key={i} className="text-xs text-muted-foreground leading-relaxed pl-1 border-l-2 border-amber-500/30">
                  {c}
                </p>
              ))}
            </div>
          )}
        </motion.div>
      ) : null}

      <PageBot
        pageContext="gochar"
        pageData={pageData}
        suggestions={[
          "How are today's transits affecting my career?",
          "What does Jupiter's current position mean for me?",
          "What are the remedies for Sade Sati?",
          "When will the planetary support improve?",
        ]}
      />
    </div>
  );
}
