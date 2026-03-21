/**
 * SadeSatiPage — Sade Sati & Shani Transit Calculator
 *
 * Fetches current Saturn position from /api/gochar.
 * Allows Moon Rashi selection (auto-filled from kundali store).
 * Computes Sade Sati phase, Ashtama Shani, and Kantaka Shani.
 */

import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import {
  RefreshCw, ChevronDown, ChevronUp, Star,
  AlertTriangle, CheckCircle2, Info,
} from "lucide-react";
import { api } from "@/lib/api";
import { useKundliStore } from "@/store/kundliStore";
import PageBot from "@/components/PageBot";

// ── Constants ────────────────────────────────────────────────────────────────

const RASHIS = [
  "Mesha", "Vrishabha", "Mithuna", "Karka",
  "Simha", "Kanya", "Tula", "Vrischika",
  "Dhanu", "Makara", "Kumbha", "Meena",
];

const RASHI_IDX: Record<string, number> = {
  Mesha: 0, Vrishabha: 1, Mithuna: 2, Karka: 3,
  Simha: 4, Kanya: 5, Tula: 6, Vrischika: 7,
  Dhanu: 8, Makara: 9, Kumbha: 10, Meena: 11,
};

const RASHI_SYMBOLS: Record<string, string> = {
  Mesha: "♈︎", Vrishabha: "♉︎", Mithuna: "♊︎", Karka: "♋︎",
  Simha: "♌︎", Kanya: "♍︎", Tula: "♎︎", Vrischika: "♏︎",
  Dhanu: "♐︎", Makara: "♑︎", Kumbha: "♒︎", Meena: "♓︎",
};

// Saturn spends ~2.5 years per rashi (30 months)
const SATURN_TRANSIT_MONTHS = 30;

// Kantaka Shani houses from lagna: 1, 4, 7, 10
const KANTAKA_HOUSES = [1, 4, 7, 10];

// ── Computation helpers ──────────────────────────────────────────────────────

type SadeSatiPhase = "Rising" | "Peak" | "Setting" | null;
type ShaniStatus = "sade_sati" | "ashtama" | "kantaka" | "clear";

interface ShaniAnalysis {
  status: ShaniStatus;
  sadeSatiPhase: SadeSatiPhase;
  shaniFromMoon: number;     // house number from Moon rashi (1-based)
  shaniFromLagna: number;    // house number from Lagna (1-based, if lagna known)
  moonRashi: string;
  shaniRashi: string;
}

function computeShaniAnalysis(
  moonRashi: string,
  shaniRashi: string,
  lagnaRashi?: string
): ShaniAnalysis {
  const moonIdx  = RASHI_IDX[moonRashi]  ?? 0;
  const shaniIdx = RASHI_IDX[shaniRashi] ?? 0;

  // shaniFromMoon: 1 = same rashi as moon, 2 = next, etc.
  const shaniFromMoon = ((shaniIdx - moonIdx + 12) % 12) + 1;

  let status: ShaniStatus = "clear";
  let sadeSatiPhase: SadeSatiPhase = null;

  if (shaniFromMoon === 12) {
    status = "sade_sati";
    sadeSatiPhase = "Rising";
  } else if (shaniFromMoon === 1) {
    status = "sade_sati";
    sadeSatiPhase = "Peak";
  } else if (shaniFromMoon === 2) {
    status = "sade_sati";
    sadeSatiPhase = "Setting";
  } else if (shaniFromMoon === 8) {
    status = "ashtama";
  }

  // Kantaka: check from lagna
  let shaniFromLagna = 0;
  if (lagnaRashi) {
    const lagnaIdx = RASHI_IDX[lagnaRashi] ?? 0;
    shaniFromLagna = ((shaniIdx - lagnaIdx + 12) % 12) + 1;
    if (status === "clear" && KANTAKA_HOUSES.includes(shaniFromLagna)) {
      status = "kantaka";
    }
  }

  return { status, sadeSatiPhase, shaniFromMoon, shaniFromLagna, moonRashi, shaniRashi };
}

/**
 * Given current Saturn rashi and Moon rashi, find when the current Sade Sati
 * started and when it ends, based on fixed 2.5yr/rashi transit.
 * We don't have exact transit dates from the API, so we describe the phases textually.
 */
function getSadeSatiPhaseInfo(phase: SadeSatiPhase, shaniRashi: string, moonRashi: string) {
  const moonIdx  = RASHI_IDX[moonRashi];
  const risingRashi  = RASHIS[(moonIdx - 1 + 12) % 12]; // 12th from moon
  const peakRashi    = moonRashi;                          // same as moon
  const settingRashi = RASHIS[(moonIdx + 1) % 12];         // 2nd from moon

  return {
    risingRashi,
    peakRashi,
    settingRashi,
    currentPhaseRashi: shaniRashi,
  };
}

/**
 * Find the next Sade Sati triggering rashi — the 12th rashi from moon.
 * If not currently in Sade Sati, tells which rashi Saturn needs to enter.
 */
function getNextSadeSatiRashi(moonRashi: string): string {
  const moonIdx = RASHI_IDX[moonRashi];
  return RASHIS[(moonIdx - 1 + 12) % 12];
}

// ── Main component ───────────────────────────────────────────────────────────

interface GocharPositions {
  [planet: string]: { rashi: string; degree: number };
}

export default function SadeSatiPage() {
  const kundaliData = useKundliStore((s) => s.kundaliData);

  // Auto-fill Moon rashi from store
  const storeMoonRashi = kundaliData?.grahas?.Chandra?.rashi ?? "";
  const lagnaRashi     = kundaliData?.lagna?.rashi ?? "";

  const [selectedMoon, setSelectedMoon] = useState<string>(storeMoonRashi);
  const [positions,    setPositions]    = useState<GocharPositions | null>(null);
  const [loading,      setLoading]      = useState(true);
  const [error,        setError]        = useState<string | null>(null);
  const [remediesOpen, setRemediesOpen] = useState(false);

  // Keep moon rashi in sync with store if it loads later
  useEffect(() => {
    if (storeMoonRashi && !selectedMoon) setSelectedMoon(storeMoonRashi);
  }, [storeMoonRashi]);

  const fetchPositions = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get<{ status: string; positions: GocharPositions }>("/api/gochar");
      if (res.status === "ok") setPositions(res.positions);
      else setError("Could not load Saturn position.");
    } catch {
      setError("Could not reach server. Showing offline mode.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchPositions(); }, []);

  const shaniRashi = positions?.Shani?.rashi ?? "";
  const shaniDeg   = positions?.Shani?.degree ?? 0;

  const analysis =
    selectedMoon && shaniRashi
      ? computeShaniAnalysis(selectedMoon, shaniRashi, lagnaRashi || undefined)
      : null;

  const phaseInfo = analysis?.sadeSatiPhase
    ? getSadeSatiPhaseInfo(analysis.sadeSatiPhase, shaniRashi, selectedMoon)
    : null;

  const nextTrigger = selectedMoon && !analysis?.sadeSatiPhase
    ? getNextSadeSatiRashi(selectedMoon)
    : null;

  // Status banner config
  const bannerConfig = (() => {
    if (!analysis) return null;
    switch (analysis.status) {
      case "sade_sati":
        return {
          bg: "bg-red-500/10 border-red-500/30",
          text: "text-red-400",
          icon: <AlertTriangle className="w-5 h-5 text-red-400" />,
          title: `Sade Sati Active — ${analysis.sadeSatiPhase} Phase`,
          subtitle: `Saturn (♄) is in ${shaniRashi}, which is the ${
            analysis.sadeSatiPhase === "Rising" ? "12th"
            : analysis.sadeSatiPhase === "Peak"  ? "1st (same)"
            : "2nd"
          } sign from your Moon rashi (${selectedMoon}).`,
        };
      case "ashtama":
        return {
          bg: "bg-orange-500/10 border-orange-500/30",
          text: "text-orange-400",
          icon: <AlertTriangle className="w-5 h-5 text-orange-400" />,
          title: "Ashtama Shani Active",
          subtitle: `Saturn is in the 8th house from Moon (${selectedMoon}). Unexpected obstacles, health caution.`,
        };
      case "kantaka":
        return {
          bg: "bg-amber-500/10 border-amber-500/30",
          text: "text-amber-400",
          icon: <AlertTriangle className="w-5 h-5 text-amber-400" />,
          title: `Kantaka Shani — H${analysis.shaniFromLagna} from Lagna`,
          subtitle: `Saturn in ${shaniRashi} is in H${analysis.shaniFromLagna} from your lagna (${lagnaRashi}). Career and relationship friction possible.`,
        };
      case "clear":
        return {
          bg: "bg-emerald-500/10 border-emerald-500/30",
          text: "text-emerald-400",
          icon: <CheckCircle2 className="w-5 h-5 text-emerald-400" />,
          title: "No Sade Sati — Saturn Transit Clear",
          subtitle: `Saturn is in the ${analysis.shaniFromMoon}th position from your Moon rashi (${selectedMoon}). No major Shani affliction at present.`,
        };
    }
  })();

  const pageData = {
    moon_rashi: selectedMoon,
    saturn_rashi: shaniRashi,
    sade_sati_status: analysis?.status,
    sade_sati_phase: analysis?.sadeSatiPhase,
    shani_from_moon: analysis?.shaniFromMoon,
  };

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-3xl mx-auto">

      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl text-foreground text-glow-gold">Sade Sati</h1>
            <p className="text-sm text-muted-foreground mt-1">
              Saturn's 7.5-year transit cycle — know your current phase
            </p>
          </div>
          <button
            onClick={fetchPositions}
            disabled={loading}
            className="p-2 rounded-lg hover:bg-muted/40 text-muted-foreground hover:text-foreground transition-colors disabled:opacity-40"
            title="Refresh Saturn position"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
          </button>
        </div>
      </motion.div>

      {/* Saturn position card */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.05 }}
        className="cosmic-card rounded-xl p-4 flex items-center justify-between gap-4 flex-wrap"
      >
        <div className="flex items-center gap-3">
          <span className="text-2xl" style={{ color: "#64748b" }}>♄︎</span>
          <div>
            <p className="text-xs text-muted-foreground uppercase tracking-wider">Saturn Today</p>
            {loading ? (
              <div className="h-5 w-28 bg-muted/30 rounded animate-pulse mt-1" />
            ) : error ? (
              <p className="text-xs text-destructive mt-1">{error}</p>
            ) : shaniRashi ? (
              <p className="text-sm font-semibold text-foreground">
                {RASHI_SYMBOLS[shaniRashi]} {shaniRashi}
                <span className="text-xs text-muted-foreground font-normal ml-2">
                  {shaniDeg.toFixed(2)}°
                </span>
              </p>
            ) : (
              <p className="text-xs text-muted-foreground mt-1">Not available</p>
            )}
          </div>
        </div>
        <div className="text-xs text-muted-foreground/60">
          Spends ~2.5 yrs per rashi · 7.5 yrs total per cycle
        </div>
      </motion.div>

      {/* Moon Rashi Selector */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="cosmic-card rounded-xl p-4 space-y-3"
      >
        <div className="flex items-center gap-2">
          <span className="text-base" style={{ color: "#4F46E5" }}>☽︎</span>
          <p className="text-sm font-medium text-foreground">Your Moon Rashi (Janma Rashi)</p>
          {storeMoonRashi && (
            <span className="text-xs text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
              From kundali
            </span>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-2">
          {RASHIS.map((r) => (
            <button
              key={r}
              onClick={() => setSelectedMoon(r)}
              className={`rounded-lg px-2 py-2 text-xs font-medium transition-all border ${
                selectedMoon === r
                  ? "bg-primary/20 border-primary/50 text-primary"
                  : "bg-muted/10 border-border/20 text-muted-foreground hover:border-primary/30 hover:text-foreground"
              }`}
            >
              <div className="text-base leading-none mb-0.5">{RASHI_SYMBOLS[r]}</div>
              <div>{r}</div>
            </button>
          ))}
        </div>
      </motion.div>

      {/* Status Banner */}
      {bannerConfig && analysis && (
        <motion.div
          key={analysis.status}
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.12 }}
          className={`rounded-xl border p-4 space-y-1 ${bannerConfig.bg}`}
        >
          <div className="flex items-center gap-2">
            {bannerConfig.icon}
            <h2 className={`text-sm font-bold ${bannerConfig.text}`}>{bannerConfig.title}</h2>
          </div>
          <p className="text-xs text-muted-foreground leading-relaxed pl-7">
            {bannerConfig.subtitle}
          </p>
        </motion.div>
      )}

      {/* Sade Sati Phase Details */}
      {analysis?.sadeSatiPhase && phaseInfo && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="cosmic-card rounded-xl p-5 space-y-4 border border-red-500/15"
        >
          <h3 className="text-sm font-semibold text-foreground">Sade Sati Lifecycle</h3>

          <div className="space-y-3">
            {(
              [
                {
                  label: "Phase 1 — Rising",
                  rashi: phaseInfo.risingRashi,
                  active: analysis.sadeSatiPhase === "Rising",
                  color: "text-amber-400",
                  activeBg: "bg-amber-500/10 border-amber-500/25",
                  desc: "First 2.5 years — new challenges begin. Change of circumstances, restlessness, and unexpected shifts in surroundings or living situation. The native starts to feel Shani's presence.",
                },
                {
                  label: "Phase 2 — Peak",
                  rashi: phaseInfo.peakRashi,
                  active: analysis.sadeSatiPhase === "Peak",
                  color: "text-red-400",
                  activeBg: "bg-red-500/10 border-red-500/25",
                  desc: "Middle 2.5 years — most intense phase. Tests of dharma and karma are at their strongest. Emotional pressures, health tests, and major life changes are common. This is Shani's direct gaze on the mind.",
                },
                {
                  label: "Phase 3 — Setting",
                  rashi: phaseInfo.settingRashi,
                  active: analysis.sadeSatiPhase === "Setting",
                  color: "text-emerald-400",
                  activeBg: "bg-emerald-500/10 border-emerald-500/25",
                  desc: "Final 2.5 years — gradual relief. Karma begins to resolve. Results of past efforts materialise. Discipline adopted during Sade Sati starts yielding rewards.",
                },
              ] as const
            ).map((phase, i) => (
              <div
                key={i}
                className={`rounded-lg border p-3 transition-all ${
                  phase.active ? `${phase.activeBg} border-opacity-100` : "border-border/20 opacity-60"
                }`}
              >
                <div className="flex items-center justify-between gap-2 flex-wrap">
                  <div className="flex items-center gap-2">
                    <span className={`text-xs font-semibold ${phase.active ? phase.color : "text-muted-foreground"}`}>
                      {phase.label}
                    </span>
                    {phase.active && (
                      <span className={`text-xs px-2 py-0.5 rounded-full border ${phase.activeBg} ${phase.color} font-medium`}>
                        Now
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-star-gold">
                    {RASHI_SYMBOLS[phase.rashi]} Saturn in {phase.rashi}
                  </span>
                </div>
                {phase.active && (
                  <p className="text-xs text-muted-foreground leading-relaxed mt-2">
                    {phase.desc}
                  </p>
                )}
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {/* Ashtama / Kantaka detail */}
      {analysis && analysis.status === "ashtama" && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="cosmic-card rounded-xl p-5 space-y-2 border border-orange-500/20"
        >
          <h3 className="text-sm font-semibold text-orange-400">Ashtama Shani</h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Saturn is transiting the 8th sign from your Moon rashi. The 8th house signifies sudden events, hidden challenges, chronic health issues, and transformation. This is considered one of the more difficult Shani transits, though less prolonged than Sade Sati (Saturn passes through in ~2.5 years).
          </p>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Recommended: Avoid rash decisions, prioritise health, and perform Shani puja on Saturdays.
          </p>
        </motion.div>
      )}

      {analysis && analysis.status === "kantaka" && lagnaRashi && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="cosmic-card rounded-xl p-5 space-y-2 border border-amber-500/20"
        >
          <h3 className="text-sm font-semibold text-amber-400">
            Kantaka Shani — H{analysis.shaniFromLagna} from Lagna ({lagnaRashi})
          </h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            Saturn in a Kantaka position (1st, 4th, 7th, or 10th from lagna) creates friction in life's pillars — self, home, partnerships, and career. Effects are moderate and transit-dependent (2.5 years). Discipline and remedial measures can significantly reduce the impact.
          </p>
        </motion.div>
      )}

      {/* Upcoming Sade Sati (when not active) */}
      {analysis && analysis.status !== "sade_sati" && nextTrigger && selectedMoon && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.18 }}
          className="cosmic-card rounded-xl p-4 space-y-2 border border-border/20"
        >
          <div className="flex items-start gap-2">
            <Info className="w-4 h-4 text-muted-foreground/60 shrink-0 mt-0.5" />
            <div className="space-y-1">
              <p className="text-xs font-medium text-foreground">Next Sade Sati</p>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Your next Sade Sati begins when Saturn enters{" "}
                <span className="text-star-gold font-medium">
                  {RASHI_SYMBOLS[nextTrigger]} {nextTrigger}
                </span>{" "}
                (the 12th sign from your Moon rashi {RASHI_SYMBOLS[selectedMoon]} {selectedMoon}).{" "}
                Saturn is currently in {shaniRashi ? `${RASHI_SYMBOLS[shaniRashi]} ${shaniRashi}` : "—"}.
              </p>
              <p className="text-xs text-muted-foreground/70 leading-relaxed">
                Once it begins, the full 7.5-year cycle spans {nextTrigger} → {selectedMoon} → {RASHIS[(RASHI_IDX[selectedMoon] + 1) % 12]}.
              </p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Shani Sthana summary table */}
      {analysis && selectedMoon && shaniRashi && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="cosmic-card rounded-xl overflow-hidden border border-border/20"
        >
          <div className="px-4 py-2.5 bg-muted/10 border-b border-border/20">
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
              Shani Position Summary
            </p>
          </div>
          <div className="divide-y divide-border/15">
            {[
              { label: "Moon Rashi (Janma)", value: `${RASHI_SYMBOLS[selectedMoon]} ${selectedMoon}` },
              { label: "Saturn Current Rashi", value: shaniRashi ? `${RASHI_SYMBOLS[shaniRashi]} ${shaniRashi}` : "—" },
              { label: "Saturn from Moon", value: `${analysis.shaniFromMoon}th house` },
              ...(lagnaRashi && analysis.shaniFromLagna
                ? [{ label: "Saturn from Lagna", value: `${analysis.shaniFromLagna}th house` }]
                : []),
              {
                label: "Sade Sati Status",
                value:
                  analysis.sadeSatiPhase
                    ? `Active — ${analysis.sadeSatiPhase} Phase`
                    : analysis.status === "ashtama"
                    ? "Ashtama Shani"
                    : analysis.status === "kantaka"
                    ? `Kantaka Shani (H${analysis.shaniFromLagna})`
                    : "Clear",
              },
            ].map((row) => (
              <div key={row.label} className="flex items-center justify-between px-4 py-2.5 gap-4">
                <span className="text-xs text-muted-foreground">{row.label}</span>
                <span className="text-xs text-foreground font-medium text-right">{row.value}</span>
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {/* Remedies */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.22 }}
        className="cosmic-card rounded-xl p-5 border border-border/20"
      >
        <button
          onClick={() => setRemediesOpen((v) => !v)}
          className="flex items-center justify-between w-full"
        >
          <div className="flex items-center gap-2">
            <Star className="w-4 h-4 text-star-gold" />
            <h3 className="text-sm font-semibold text-foreground">Shani Remedies</h3>
          </div>
          {remediesOpen ? (
            <ChevronUp className="w-4 h-4 text-muted-foreground" />
          ) : (
            <ChevronDown className="w-4 h-4 text-muted-foreground" />
          )}
        </button>

        {remediesOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            className="mt-4 space-y-2"
          >
            {[
              "Perform Shani puja every Saturday — light sesame oil lamp before Shani idol",
              "Recite Hanuman Chalisa daily — Hanumanji is considered a protector against Shani's difficult transits",
              "Observe Saturday fast — eat only once, avoid meat, alcohol, and oil-heavy food",
              "Donate black sesame seeds, black cloth, mustard oil, or iron to the needy on Saturdays",
              "Wear an iron ring on the middle finger of the right hand (from Shanivar puja)",
              "Recite Shani mantra: ॐ शं शनैश्चराय नमः — 108 times on Saturdays",
              "Blue Sapphire (Neelam) can be worn only after careful horoscope analysis by a qualified Jyotishi — never self-prescribe",
              "Visit Shani Shingnapur, Thirunallar, or Kukke Subramanya during Sade Sati for remedial pujas",
              "Be disciplined, patient, and honest — Shani rewards sincerity and punishes shortcuts",
              "Recite Shri Shani Chalisa and Shani Stotra during the Sade Sati period",
            ].map((r, i) => (
              <div key={i} className="flex items-start gap-2 text-xs text-muted-foreground leading-relaxed">
                <Star className="w-3 h-3 text-star-gold shrink-0 mt-0.5" />
                <span>{r}</span>
              </div>
            ))}
          </motion.div>
        )}
      </motion.div>

      {/* About Sade Sati info section */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.25 }}
        className="cosmic-card rounded-xl p-5 space-y-3 border border-border/15"
      >
        <h3 className="text-sm font-semibold text-foreground">About Sade Sati</h3>
        <p className="text-xs text-muted-foreground leading-relaxed">
          Sade Sati (साढ़े साती) means "seven and a half" in Hindi — referring to the 7.5 years Saturn spends transiting through three consecutive rashis: the 12th from your natal Moon, the Moon's own rashi, and the 2nd from it. Since Saturn spends approximately 2.5 years in each rashi, the entire cycle lasts 7.5 years.
        </p>
        <p className="text-xs text-muted-foreground leading-relaxed">
          Sade Sati is not always negative. Saturn is the planet of discipline, karma, and long-term reward. Many people experience profound personal growth, career restructuring, and spiritual deepening during this period. The effects depend heavily on Saturn's placement and strength in the natal chart.
        </p>
        <p className="text-xs text-muted-foreground leading-relaxed">
          A person experiences Sade Sati approximately two to three times in a lifetime. It recurs every ~29.5 years (one full Saturn orbit).
        </p>
      </motion.div>

      <PageBot
        pageContext="sade_sati"
        pageData={pageData}
        suggestions={[
          "How will Sade Sati affect my career?",
          "What are the best remedies for Sade Sati?",
          "Is Sade Sati always negative?",
          "When does my current Sade Sati end?",
        ]}
      />
    </div>
  );
}
