/**
 * DoshaPage — Dosha Analysis
 * Computes Mangal Dosha, Kaal Sarp Dosha, Pitra Dosha, and Grahan Yoga
 * entirely from the kundali store — no backend calls needed.
 */

import { useState, useMemo } from "react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import {
  ShieldAlert, ShieldCheck, ChevronDown, ChevronUp,
  AlertTriangle, Star, Zap, Moon,
} from "lucide-react";
import { useKundliStore } from "@/store/kundliStore";
import PageBot from "@/components/PageBot";
import { useTranslation } from "react-i18next";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";

// ── Rashi index map ─────────────────────────────────────────────────────────
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

// ── Kaal Sarp type by Rahu rashi index ─────────────────────────────────────
const KAAL_SARP_TYPES: Record<number, { name: string; effect: string }> = {
  0:  { name: "Ananta",      effect: "Challenges in self-expression, obstacles from influential people." },
  1:  { name: "Kulika",      effect: "Financial hardships, health concerns, ancestral debts." },
  2:  { name: "Vasuki",      effect: "Problems with siblings, short journeys, communication troubles." },
  3:  { name: "Shankhapal",  effect: "Domestic discord, difficulties with mother, emotional turbulence." },
  4:  { name: "Padma",       effect: "Losses in speculation, problems with children, obstacles in creative work." },
  5:  { name: "Mahapadma",   effect: "Health issues, conflicts with enemies, service-related difficulties." },
  6:  { name: "Taksha",      effect: "Partnership troubles, legal disputes, obstacles in marriage." },
  7:  { name: "Karkotak",    effect: "Hidden enemies, sudden reversals, occult troubles." },
  8:  { name: "Shankhachur", effect: "Troubles with higher education, father, religious life." },
  9:  { name: "Ghatak",      effect: "Career obstacles, public reputation issues, challenges with authority." },
  10: { name: "Vishdhar",    effect: "Gains from unorthodox means, social isolation, unpredictable gains/losses." },
  11: { name: "Sheshnaag",   effect: "Foreign connections create challenges, expenditure, spiritual restlessness." },
};

// ── 9th house lord by rashi (Vedic) ─────────────────────────────────────────
const RASHI_LORDS: Record<string, string> = {
  Mesha: "Mangal", Vrishabha: "Shukra", Mithuna: "Budh", Karka: "Chandra",
  Simha: "Surya", Kanya: "Budh", Tula: "Shukra", Vrischika: "Mangal",
  Dhanu: "Guru", Makara: "Shani", Kumbha: "Shani", Meena: "Guru",
};

// Debilitation rashis for each graha
const DEBILITATION: Record<string, string> = {
  Surya: "Tula", Chandra: "Vrischika", Mangal: "Karka", Budh: "Meena",
  Guru: "Makara", Shukra: "Kanya", Shani: "Mesha",
};

// ── Computation helpers ──────────────────────────────────────────────────────
function isKaalSarp(grahas: Record<string, any>): boolean {
  const planets = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani"];
  const rahu = RASHI_IDX[grahas["Rahu"]?.rashi] ?? 0;

  // Ketu is always 6 rashis away from Rahu
  // Check all planets in arc rahu -> ketu clockwise (6 steps)
  const inArc = planets.every((p) => {
    const ri = RASHI_IDX[grahas[p]?.rashi] ?? 0;
    return ((ri - rahu + 12) % 12) < 6;
  });
  // Or reverse arc: ketu -> rahu clockwise
  const ketu = (rahu + 6) % 12;
  const inArcRev = planets.every((p) => {
    const ri = RASHI_IDX[grahas[p]?.rashi] ?? 0;
    return ((ri - ketu + 12) % 12) < 6;
  });
  return inArc || inArcRev;
}

function getKaalSarpDirection(grahas: Record<string, any>): "Forward" | "Reverse" {
  const planets = ["Surya", "Chandra", "Mangal", "Budh", "Guru", "Shukra", "Shani"];
  const rahu = RASHI_IDX[grahas["Rahu"]?.rashi] ?? 0;
  const inArc = planets.every((p) => {
    const ri = RASHI_IDX[grahas[p]?.rashi] ?? 0;
    return ((ri - rahu + 12) % 12) < 6;
  });
  return inArc ? "Forward" : "Reverse";
}

interface MangalDoshaResult {
  present: boolean;
  house: number;
  severity: "Strong" | "Moderate" | "Absent";
  cancelled: boolean;
  cancellationReasons: string[];
}

function computeMangalDosha(
  grahas: Record<string, any>,
  lagnaRashi: string
): MangalDoshaResult {
  const mangal = grahas["Mangal"];
  if (!mangal) return { present: false, house: 0, severity: "Absent", cancelled: false, cancellationReasons: [] };

  const house: number = mangal.house;
  const doshaHouses = [1, 4, 7, 8, 12];
  const present = doshaHouses.includes(house);

  if (!present) return { present: false, house, severity: "Absent", cancelled: false, cancellationReasons: [] };

  const severity: "Strong" | "Moderate" = [7, 8].includes(house) ? "Strong" : "Moderate";

  // Cancellation checks
  const cancellationReasons: string[] = [];
  const mangalRashi = mangal.rashi;

  // Mars in own sign
  if (mangalRashi === "Mesha" || mangalRashi === "Vrischika") {
    cancellationReasons.push("Mars is in its own sign — inherent strength reduces malefic effect");
  }
  // Mars exalted
  if (mangalRashi === "Makara") {
    cancellationReasons.push("Mars is exalted in Makara — exaltation cancels Dosha");
  }
  // Lagna is Mesha, Vrischika, Karka, or Kumbha
  if (["Mesha", "Vrischika", "Karka", "Kumbha"].includes(lagnaRashi)) {
    cancellationReasons.push(`${lagnaRashi} lagna naturally neutralises Mangal Dosha`);
  }
  // Venus in H1 or H2
  const shukra = grahas["Shukra"];
  if (shukra && (shukra.house === 1 || shukra.house === 2)) {
    cancellationReasons.push(`Venus in H${shukra.house} counters Mars energy`);
  }
  // Saturn in H1
  const shani = grahas["Shani"];
  if (shani && shani.house === 1) {
    cancellationReasons.push("Saturn in H1 provides discipline that offsets Mangal Dosha");
  }

  return {
    present: true,
    house,
    severity,
    cancelled: cancellationReasons.length > 0,
    cancellationReasons,
  };
}

interface PitraDoshaResult {
  present: boolean;
  causes: string[];
}

function computePitraDosha(grahas: Record<string, any>, lagnaRashi: string): PitraDoshaResult {
  const causes: string[] = [];

  const surya = grahas["Surya"];
  const rahu  = grahas["Rahu"];
  const ketu  = grahas["Ketu"];
  const shani = grahas["Shani"];

  // Sun afflicted by Saturn/Rahu/Ketu (same house or aspect)
  if (surya) {
    const suryaHouse = surya.house;
    const malefics = [rahu, ketu, shani].filter(Boolean);
    for (const m of malefics) {
      if (m.house === suryaHouse) {
        const mName =
          m === rahu ? "Rahu" : m === ketu ? "Ketu" : "Saturn";
        causes.push(`Sun conjunct ${mName} in H${suryaHouse} — affliction of Pitru karaka`);
      }
    }
    // Sun in H9 with any malefic
    if (suryaHouse === 9) {
      const maleficIn9 = malefics.filter((m) => m.house === 9);
      if (maleficIn9.length > 0) {
        causes.push("Sun in H9 (Pitru sthana) afflicted by malefic planet");
      }
    }
  }

  // 9th house lord in H6/H8/H12 or debilitated
  const rashiList = [
    "Mesha", "Vrishabha", "Mithuna", "Karka", "Simha", "Kanya",
    "Tula", "Vrischika", "Dhanu", "Makara", "Kumbha", "Meena",
  ];
  const lagnaIdx = RASHI_IDX[lagnaRashi] ?? 0;
  // 9th house rashi
  const ninthRashiIdx = (lagnaIdx + 8) % 12;
  const ninthRashi = rashiList[ninthRashiIdx];
  const ninthLordName = RASHI_LORDS[ninthRashi];
  const ninthLord = grahas[ninthLordName];
  if (ninthLord) {
    if ([6, 8, 12].includes(ninthLord.house)) {
      causes.push(`9th lord (${ninthLordName}) placed in H${ninthLord.house} (dusthana) — weakens ancestral blessings`);
    }
    // Check debilitation
    if (DEBILITATION[ninthLordName] && ninthLord.rashi === DEBILITATION[ninthLordName]) {
      causes.push(`9th lord (${ninthLordName}) is debilitated in ${ninthLord.rashi}`);
    }
  }

  return { present: causes.length > 0, causes };
}

interface GrahanYogaResult {
  present: boolean;
  planets: string[];
  description: string;
}

function computeGrahanYoga(grahas: Record<string, any>): GrahanYogaResult {
  const rahu = grahas["Rahu"];
  const ketu = grahas["Ketu"];
  const surya = grahas["Surya"];
  const chandra = grahas["Chandra"];
  const affectedPlanets: string[] = [];

  if (surya && rahu && surya.house === rahu.house) affectedPlanets.push("Sun + Rahu (Solar Eclipse Yoga)");
  if (surya && ketu && surya.house === ketu.house) affectedPlanets.push("Sun + Ketu (Solar Eclipse Yoga)");
  if (chandra && rahu && chandra.house === rahu.house) affectedPlanets.push("Moon + Rahu (Lunar Eclipse Yoga)");
  if (chandra && ketu && chandra.house === ketu.house) affectedPlanets.push("Moon + Ketu (Lunar Eclipse Yoga)");

  return {
    present: affectedPlanets.length > 0,
    planets: affectedPlanets,
    description:
      affectedPlanets.length > 0
        ? "A luminary (Sun or Moon) is conjunct a lunar node (Rahu/Ketu), creating an eclipse-like shadow that can obscure the significations of the affected luminary."
        : "",
  };
}

// ── Sub-components ───────────────────────────────────────────────────────────

function StatusBadge({ present, cancelled = false }: { present: boolean; cancelled?: boolean }) {
  if (!present) {
    return (
      <span className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full bg-emerald-500/15 text-emerald-400 border border-emerald-500/25 font-medium">
        <ShieldCheck className="w-3 h-3" /> Absent
      </span>
    );
  }
  if (cancelled) {
    return (
      <span className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full bg-amber-500/15 text-amber-400 border border-amber-500/25 font-medium">
        <ShieldAlert className="w-3 h-3" /> Present · Cancelled
      </span>
    );
  }
  return (
    <span className="inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full bg-red-500/15 text-red-400 border border-red-500/25 font-medium">
      <ShieldAlert className="w-3 h-3" /> Present
    </span>
  );
}

function SeverityBadge({ severity }: { severity: "Strong" | "Moderate" | "Absent" }) {
  const cls =
    severity === "Strong"
      ? "bg-red-500/15 text-red-400 border-red-500/25"
      : severity === "Moderate"
      ? "bg-amber-500/15 text-amber-400 border-amber-500/25"
      : "bg-muted/15 text-muted-foreground border-border/20";
  return (
    <span className={`inline-flex items-center text-xs px-2 py-0.5 rounded-full border font-medium ${cls}`}>
      {severity}
    </span>
  );
}

function RemediesPanel({ remedies }: { remedies: string[] }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  return (
    <div className="mt-4">
      <button
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-1.5 text-xs text-primary hover:text-primary/80 transition-colors font-medium"
      >
        {open ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        {t("dosha.remedies")}
      </button>
      {open && (
        <motion.ul
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          exit={{ opacity: 0, height: 0 }}
          className="mt-2 space-y-1.5 pl-1"
        >
          {remedies.map((r, i) => (
            <li key={i} className="flex items-start gap-2 text-xs text-muted-foreground leading-relaxed">
              <Star className="w-3 h-3 text-star-gold shrink-0 mt-0.5" />
              <span>{r}</span>
            </li>
          ))}
        </motion.ul>
      )}
    </div>
  );
}

const CARD_REMEDIES = {
  mangal: [
    "Chant Hanuman Chalisa every Tuesday",
    "Recite Mangal mantra: ॐ क्रां क्रीं क्रौं सः भौमाय नमः (108 times on Tuesdays)",
    "Donate red lentils (masoor dal) on Tuesdays",
    "Wear Red Coral (Moonga) after consulting a Jyotishi — only if Mars is a benefic lord for your lagna",
    "Avoid arguments and major decisions on Tuesdays",
    "Fast on Tuesdays and offer sindoor to Lord Hanuman",
  ],
  kaalSarp: [
    "Perform Nag Panchami puja with milk offering to Shiva Lingam",
    "Rahu-Ketu Shanti puja — best performed on Saturday (Rahu) or Tuesday (Ketu)",
    "Visit Trimbakeshwar (Nashik) or Ujjain Mahakal for Kaal Sarp Dosha nivaran puja",
    "Donate black sesame seeds and black blanket on Saturdays",
    "Recite Maha Mrityunjaya mantra 108 times daily",
    "Keep a silver snake idol at your place of worship",
  ],
  pitra: [
    "Perform Pitru Tarpan — offer water mixed with sesame seeds to ancestors daily in the morning",
    "Conduct Shraddha ceremony annually during Pitru Paksha (16-day lunar period)",
    "Donate food and clothes to Brahmins on Amavasya (new moon day)",
    "Offer water to a Peepal tree every Saturday",
    "Perform Pitra Puja (ancestral rites) at Gaya, Varanasi, or at a sacred river",
    "Recite Pitru Stotra or Gayatri Mantra 108 times daily",
  ],
  grahan: [
    "Recite Mahamrityunjaya mantra daily: ॐ त्र्यम्बकं यजामहे... (108 times)",
    "Perform Surya/Chandra Grahan remedies on actual eclipse days",
    "Avoid starting new ventures during eclipse periods",
    "Donate black sesame (for Rahu) or multicoloured cloth (for Ketu) on Saturdays/Tuesdays respectively",
    "Recite Aditya Hridayam for Sun-Rahu/Ketu conjunction",
  ],
};

// ── Main Page ────────────────────────────────────────────────────────────────
export default function DoshaPage() {
  const { t } = useTranslation();
  const kundaliData = useKundliStore((s) => s.kundaliData);
  useRegisterPageBot('dosha', kundaliData ? { grahas: kundaliData.grahas, doshas: kundaliData.doshas } : {});

  if (!kundaliData) {
    return (
      <div className="p-4 sm:p-6 max-w-2xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="cosmic-card rounded-2xl p-8 flex flex-col items-center gap-4 text-center border border-dashed border-muted"
        >
          <ShieldAlert className="w-12 h-12 text-muted-foreground/40" />
          <div>
            <h2 className="text-lg font-semibold text-foreground mb-1">{t("dosha.no_kundali")}</h2>
            <p className="text-sm text-muted-foreground">
              {t("dosha.no_kundali_desc")}
            </p>
          </div>
          <Link
            to="/kundli"
            className="mt-2 px-5 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
          >
            {t("dosha.generate_btn")}
          </Link>
        </motion.div>
      </div>
    );
  }

  const { grahas, lagna } = kundaliData;
  const lagnaRashi = lagna?.rashi ?? "";

  // Run computations
  const mangalResult  = computeMangalDosha(grahas, lagnaRashi);
  const kaalsarpActive = isKaalSarp(grahas);
  const rahuRashiIdx  = RASHI_IDX[grahas["Rahu"]?.rashi] ?? 0;
  const ksType        = KAAL_SARP_TYPES[rahuRashiIdx];
  const ksDirection   = kaalsarpActive ? getKaalSarpDirection(grahas) : null;
  const pitraResult   = computePitraDosha(grahas, lagnaRashi);
  const grahanResult  = computeGrahanYoga(grahas);

  const pageData = {
    mangal_dosha: mangalResult.present
      ? `Present in H${mangalResult.house}, severity: ${mangalResult.severity}${mangalResult.cancelled ? ", cancelled" : ""}`
      : "Absent",
    kaal_sarp: kaalsarpActive
      ? `Active — ${ksType?.name} Kaal Sarp (${ksDirection}), Rahu in ${grahas["Rahu"]?.rashi}`
      : "Absent",
    pitra_dosha: pitraResult.present
      ? `Present — ${pitraResult.causes.join("; ")}`
      : "Absent",
    grahan_yoga: grahanResult.present
      ? `Present — ${grahanResult.planets.join(", ")}`
      : "Absent",
  };

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-3xl mx-auto">
      {/* Page header */}
      <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold">{t("dosha.title")}</h1>
        <p className="text-sm text-muted-foreground mt-1">{t("dosha.subtitle")}</p>
      </motion.div>

      {/* ── Mangal Dosha ─────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.05 }}
        className={`cosmic-card rounded-xl p-5 border ${
          mangalResult.present && !mangalResult.cancelled
            ? "border-red-500/25"
            : mangalResult.cancelled
            ? "border-amber-500/20"
            : "border-border/20"
        }`}
      >
        <div className="flex items-start justify-between gap-3 flex-wrap">
          <div className="flex items-center gap-2">
            <span className="text-xl text-red-400">♂︎</span>
            <div>
              <h2 className="text-base font-semibold text-foreground">{t("dosha.mangal_dosha")}</h2>
              <p className="text-xs text-muted-foreground">{t("dosha.mangal_desc")}</p>
            </div>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            <StatusBadge present={mangalResult.present} cancelled={mangalResult.cancelled} />
            {mangalResult.present && <SeverityBadge severity={mangalResult.severity} />}
          </div>
        </div>

        <div className="mt-4 space-y-2">
          {mangalResult.present ? (
            <>
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <span className="text-red-400 font-medium">{t("dosha.mars_in_house", { n: mangalResult.house })}</span>
                {grahas["Mangal"]?.rashi && (
                  <span className="text-xs text-star-gold">
                    {RASHI_SYMBOLS[grahas["Mangal"].rashi]} {grahas["Mangal"].rashi}
                  </span>
                )}
              </div>
              <p className="text-xs text-muted-foreground leading-relaxed">
                {mangalResult.severity === "Strong"
                  ? t("dosha.mangal_strong_desc", { n: mangalResult.house })
                  : t("dosha.mangal_moderate_desc", { n: mangalResult.house })}
              </p>
              {mangalResult.cancelled && (
                <div className="mt-2 rounded-lg bg-amber-500/8 border border-amber-500/20 p-3 space-y-1">
                  <p className="text-xs font-medium text-amber-400 mb-1">{t("dosha.cancellation_factors")}</p>
                  {mangalResult.cancellationReasons.map((r, i) => (
                    <p key={i} className="text-xs text-amber-300/80 leading-relaxed pl-2 border-l border-amber-500/30">
                      {r}
                    </p>
                  ))}
                </div>
              )}
            </>
          ) : (
            <p className="text-xs text-muted-foreground">
              {t("dosha.no_mangal", { n: mangalResult.house })}
            </p>
          )}
        </div>

        <RemediesPanel remedies={CARD_REMEDIES.mangal} />
      </motion.div>

      {/* ── Kaal Sarp Dosha ──────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className={`cosmic-card rounded-xl p-5 border ${
          kaalsarpActive ? "border-red-500/25" : "border-border/20"
        }`}
      >
        <div className="flex items-start justify-between gap-3 flex-wrap">
          <div className="flex items-center gap-2">
            <span className="text-xl text-[#6366f1]">☊︎</span>
            <div>
              <h2 className="text-base font-semibold text-foreground">{t("dosha.kaal_sarp")}</h2>
              <p className="text-xs text-muted-foreground">{t("dosha.kaal_sarp_desc")}</p>
            </div>
          </div>
          <StatusBadge present={kaalsarpActive} />
        </div>

        <div className="mt-4 space-y-2">
          {kaalsarpActive ? (
            <>
              <div className="flex items-center gap-3 flex-wrap">
                <div className="flex items-center gap-1.5 text-sm">
                  <span className="text-muted-foreground text-xs">{t("dosha.type_label")}:</span>
                  <span className="text-red-400 font-semibold">{ksType?.name}</span>
                  <span className="text-xs text-muted-foreground">Kaal Sarp</span>
                </div>
                <span className="text-xs px-2 py-0.5 rounded-full bg-muted/20 text-muted-foreground border border-border/20">
                  {ksDirection} arc
                </span>
              </div>
              <div className="flex gap-4 text-xs text-muted-foreground">
                <span>
                  Rahu:{" "}
                  <span className="text-star-gold">
                    {RASHI_SYMBOLS[grahas["Rahu"]?.rashi]} {grahas["Rahu"]?.rashi}
                  </span>
                </span>
                <span>
                  Ketu:{" "}
                  <span className="text-star-gold">
                    {RASHI_SYMBOLS[grahas["Ketu"]?.rashi]} {grahas["Ketu"]?.rashi}
                  </span>
                </span>
              </div>
              {ksType && (
                <p className="text-xs text-muted-foreground leading-relaxed mt-1">{ksType.effect}</p>
              )}
            </>
          ) : (
            <p className="text-xs text-muted-foreground">{t("dosha.no_kaal_sarp")}</p>
          )}
        </div>

        <RemediesPanel remedies={CARD_REMEDIES.kaalSarp} />
      </motion.div>

      {/* ── Pitra Dosha ──────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
        className={`cosmic-card rounded-xl p-5 border ${
          pitraResult.present ? "border-amber-500/25" : "border-border/20"
        }`}
      >
        <div className="flex items-start justify-between gap-3 flex-wrap">
          <div className="flex items-center gap-2">
            <span className="text-xl" style={{ color: "#D97706" }}>☉︎</span>
            <div>
              <h2 className="text-base font-semibold text-foreground">{t("dosha.pitra_dosha")}</h2>
              <p className="text-xs text-muted-foreground">{t("dosha.pitra_desc")}</p>
            </div>
          </div>
          <StatusBadge present={pitraResult.present} />
        </div>

        <div className="mt-4 space-y-2">
          {pitraResult.present ? (
            <>
              <p className="text-xs text-muted-foreground leading-relaxed">{t("dosha.pitra_present_desc")}</p>
              <ul className="space-y-1.5 mt-1">
                {pitraResult.causes.map((c, i) => (
                  <li key={i} className="flex items-start gap-2 text-xs text-amber-300/90 leading-relaxed pl-2 border-l-2 border-amber-500/30">
                    <AlertTriangle className="w-3 h-3 text-amber-400 shrink-0 mt-0.5" />
                    <span>{c}</span>
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <p className="text-xs text-muted-foreground">{t("dosha.pitra_absent")}</p>
          )}
        </div>

        <RemediesPanel remedies={CARD_REMEDIES.pitra} />
      </motion.div>

      {/* ── Grahan Yoga ──────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className={`cosmic-card rounded-xl p-5 border ${
          grahanResult.present ? "border-purple-500/25" : "border-border/20"
        }`}
      >
        <div className="flex items-start justify-between gap-3 flex-wrap">
          <div className="flex items-center gap-2">
            <Moon className="w-5 h-5 text-purple-400" />
            <div>
              <h2 className="text-base font-semibold text-foreground">{t("dosha.grahan_yoga")}</h2>
              <p className="text-xs text-muted-foreground">{t("dosha.grahan_desc")}</p>
            </div>
          </div>
          <StatusBadge present={grahanResult.present} />
        </div>

        <div className="mt-4 space-y-2">
          {grahanResult.present ? (
            <>
              <ul className="space-y-1">
                {grahanResult.planets.map((p, i) => (
                  <li key={i} className="flex items-center gap-2 text-xs text-purple-300 font-medium">
                    <Zap className="w-3 h-3 text-purple-400 shrink-0" />
                    {p}
                  </li>
                ))}
              </ul>
              <p className="text-xs text-muted-foreground leading-relaxed mt-1">
                {grahanResult.description}
              </p>
            </>
          ) : (
            <p className="text-xs text-muted-foreground">
              No Sun–Moon conjunction with Rahu or Ketu found. Grahan Yoga is absent.
            </p>
          )}
        </div>

        <RemediesPanel remedies={CARD_REMEDIES.grahan} />
      </motion.div>

      {/* Disclaimer */}
      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3 }}
        className="text-xs text-muted-foreground/50 text-center pb-2"
      >
        Dosha analysis is indicative. Consult a qualified Jyotishi for personalised guidance.
      </motion.p>

      <PageBot
        pageContext="dosha"
        pageData={pageData}
        suggestions={[
          "How serious is my Mangal Dosha?",
          "What are the detailed remedies for Kaal Sarp?",
          "How does Pitra Dosha affect my life?",
          "Can Grahan Yoga be overcome?",
        ]}
      />
    </div>
  );
}
