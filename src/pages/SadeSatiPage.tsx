/**
 * SadeSatiPage — Sade Sati & Shani Transit Calculator
 *
 * Fetches current Saturn position from /api/gochar.
 * Allows Moon Rashi selection (auto-filled from kundali store).
 * Computes Sade Sati phase, Ashtama Shani, and Kantaka Shani.
 */

import { useState, useEffect, useMemo } from "react";
import { motion } from "framer-motion";
import {
  RefreshCw, ChevronDown, ChevronUp, Star,
  AlertTriangle, CheckCircle2, Info,
} from "lucide-react";
import { api } from "@/lib/api";
import { useKundliStore } from "@/store/kundliStore";
import PageBot from "@/components/PageBot";
import { useTranslation } from "react-i18next";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";

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
  const { t } = useTranslation();
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
          title: t("sade_sati.active", { phase: t(`sade_sati.phase_${analysis.sadeSatiPhase?.toLowerCase()}`, { defaultValue: analysis.sadeSatiPhase }) }),
          subtitle: t("sade_sati.sade_sati_desc", {
            shaniRashi,
            position: analysis.sadeSatiPhase === "Rising" ? "12th" : analysis.sadeSatiPhase === "Peak" ? "1st (same)" : "2nd",
            moonRashi: selectedMoon,
          }),
        };
      case "ashtama":
        return {
          bg: "bg-orange-500/10 border-orange-500/30",
          text: "text-orange-400",
          icon: <AlertTriangle className="w-5 h-5 text-orange-400" />,
          title: t("sade_sati.ashtama_active"),
          subtitle: t("sade_sati.ashtama_desc", { moonRashi: selectedMoon }),
        };
      case "kantaka":
        return {
          bg: "bg-amber-500/10 border-amber-500/30",
          text: "text-amber-400",
          icon: <AlertTriangle className="w-5 h-5 text-amber-400" />,
          title: t("sade_sati.kantaka_active", { h: analysis.shaniFromLagna }),
          subtitle: t("sade_sati.kantaka_desc", { shaniRashi, h: analysis.shaniFromLagna, lagnaRashi }),
        };
      case "clear":
        return {
          bg: "bg-emerald-500/10 border-emerald-500/30",
          text: "text-emerald-400",
          icon: <CheckCircle2 className="w-5 h-5 text-emerald-400" />,
          title: t("sade_sati.clear"),
          subtitle: t("sade_sati.clear_desc", { n: analysis.shaniFromMoon, moonRashi: selectedMoon }),
        };
    }
  })();

  const pageData = useMemo(() => ({
    moon_rashi: selectedMoon,
    saturn_rashi: shaniRashi,
    sade_sati_status: analysis?.status,
    sade_sati_phase: analysis?.sadeSatiPhase,
    shani_from_moon: analysis?.shaniFromMoon,
  }), [selectedMoon, shaniRashi, analysis]);
  useRegisterPageBot('sade_sati', pageData);

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-3xl mx-auto">

      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-display text-2xl text-foreground text-glow-gold">{t("sade_sati.title")}</h1>
            <p className="text-sm text-muted-foreground mt-1">{t("sade_sati.subtitle")}</p>
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
            <p className="text-xs text-muted-foreground uppercase tracking-wider">{t("sade_sati.saturn_today")}</p>
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
              <p className="text-xs text-muted-foreground mt-1">{t("sade_sati.not_available")}</p>
            )}
          </div>
        </div>
        <div className="text-xs text-muted-foreground/60">
          {t("sade_sati.saturn_period")}
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
          <p className="text-sm font-medium text-foreground">{t("sade_sati.select_moon")}</p>
          {storeMoonRashi && (
            <span className="text-xs text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
              {t("sade_sati.from_kundali")}
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
              <div>{t(`data.rashi.${r.toLowerCase()}.name`, { defaultValue: r })}</div>
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
          <h3 className="text-sm font-semibold text-foreground">{t("data.sade_sati.lifecycle_heading")}</h3>

          <div className="space-y-3">
            {(
              [
                {
                  label: t("data.sade_sati.phase1_label"),
                  rashi: phaseInfo.risingRashi,
                  active: analysis.sadeSatiPhase === "Rising",
                  color: "text-amber-400",
                  activeBg: "bg-amber-500/10 border-amber-500/25",
                  desc: t("data.sade_sati.phase1_desc"),
                },
                {
                  label: t("data.sade_sati.phase2_label"),
                  rashi: phaseInfo.peakRashi,
                  active: analysis.sadeSatiPhase === "Peak",
                  color: "text-red-400",
                  activeBg: "bg-red-500/10 border-red-500/25",
                  desc: t("data.sade_sati.phase2_desc"),
                },
                {
                  label: t("data.sade_sati.phase3_label"),
                  rashi: phaseInfo.settingRashi,
                  active: analysis.sadeSatiPhase === "Setting",
                  color: "text-emerald-400",
                  activeBg: "bg-emerald-500/10 border-emerald-500/25",
                  desc: t("data.sade_sati.phase3_desc"),
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
                        {t("sade_sati.now")}
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-star-gold">
                    {RASHI_SYMBOLS[phase.rashi]} {t("sade_sati.saturn_in_rashi", { rashi: t(`data.rashi.${phase.rashi.toLowerCase()}.name`, { defaultValue: phase.rashi }) })}
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
          <h3 className="text-sm font-semibold text-orange-400">{t("data.sade_sati.ashtama_heading")}</h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            {t("data.sade_sati.ashtama_desc")}
          </p>
          <p className="text-xs text-muted-foreground leading-relaxed">
            {t("data.sade_sati.ashtama_advice")}
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
            {t("data.sade_sati.kantaka_heading")} — H{analysis.shaniFromLagna} from Lagna ({lagnaRashi})
          </h3>
          <p className="text-xs text-muted-foreground leading-relaxed">
            {t("data.sade_sati.kantaka_desc")}
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
              <p className="text-xs font-medium text-foreground">{t("data.sade_sati.next_sade_sati_title")}</p>
              <p className="text-xs text-muted-foreground leading-relaxed">
                {t("sade_sati.next_begins_when", {
                  triggerRashi: `${RASHI_SYMBOLS[nextTrigger]} ${t(`data.rashi.${nextTrigger.toLowerCase()}.name`, { defaultValue: nextTrigger })}`,
                  moonRashi: `${RASHI_SYMBOLS[selectedMoon]} ${t(`data.rashi.${selectedMoon.toLowerCase()}.name`, { defaultValue: selectedMoon })}`,
                  currentRashi: shaniRashi ? `${RASHI_SYMBOLS[shaniRashi]} ${t(`data.rashi.${shaniRashi.toLowerCase()}.name`, { defaultValue: shaniRashi })}` : "—",
                })}
              </p>
              <p className="text-xs text-muted-foreground/70 leading-relaxed">
                {t("sade_sati.next_cycle_span", {
                  r1: t(`data.rashi.${nextTrigger.toLowerCase()}.name`, { defaultValue: nextTrigger }),
                  r2: t(`data.rashi.${selectedMoon.toLowerCase()}.name`, { defaultValue: selectedMoon }),
                  r3: t(`data.rashi.${RASHIS[(RASHI_IDX[selectedMoon] + 1) % 12].toLowerCase()}.name`, { defaultValue: RASHIS[(RASHI_IDX[selectedMoon] + 1) % 12] }),
                })}
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
              {t("data.sade_sati.shani_summary_heading")}
            </p>
          </div>
          <div className="divide-y divide-border/15">
            {[
              { label: t("data.sade_sati.moon_rashi_label"), value: `${RASHI_SYMBOLS[selectedMoon]} ${selectedMoon}` },
              { label: t("data.sade_sati.saturn_current_label"), value: shaniRashi ? `${RASHI_SYMBOLS[shaniRashi]} ${shaniRashi}` : "—" },
              { label: t("data.sade_sati.saturn_from_moon_label"), value: `${analysis.shaniFromMoon}th house` },
              ...(lagnaRashi && analysis.shaniFromLagna
                ? [{ label: t("data.sade_sati.saturn_from_lagna_label"), value: `${analysis.shaniFromLagna}th house` }]
                : []),
              {
                label: t("data.sade_sati.sade_sati_status_label"),
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
            <h3 className="text-sm font-semibold text-foreground">{t("data.sade_sati.remedies_heading")}</h3>
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
              t("data.sade_sati.remedy1"),
              t("data.sade_sati.remedy2"),
              t("data.sade_sati.remedy3"),
              t("data.sade_sati.remedy4"),
              t("data.sade_sati.remedy5"),
              t("data.sade_sati.remedy6"),
              t("data.sade_sati.remedy7"),
              t("data.sade_sati.remedy8"),
              t("data.sade_sati.remedy9"),
              t("data.sade_sati.remedy10"),
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
        <h3 className="text-sm font-semibold text-foreground">{t("data.sade_sati.about_heading")}</h3>
        <p className="text-xs text-muted-foreground leading-relaxed">
          {t("data.sade_sati.about_p1")}
        </p>
        <p className="text-xs text-muted-foreground leading-relaxed">
          {t("data.sade_sati.about_p2")}
        </p>
        <p className="text-xs text-muted-foreground leading-relaxed">
          {t("data.sade_sati.about_p3")}
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
