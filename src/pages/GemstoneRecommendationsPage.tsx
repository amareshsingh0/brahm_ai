import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Link } from "react-router-dom";
import { Gem, Info, AlertTriangle, Star, ChevronDown, ChevronUp } from "lucide-react";
import { useKundliStore } from "@/store/kundliStore";
import { useTranslation } from "react-i18next";

// ── Data tables ───────────────────────────────────────────────────────────────

interface StoneEntry {
  stone: string;
  color: string;
  planet: string;
  purpose: string;
}

const LAGNA_STONE: Record<string, StoneEntry> = {
  Mesha:     { stone: "Red Coral (Moonga)",              color: "Red",         planet: "Mangal", purpose: "Courage, Energy, Health" },
  Vrishabha: { stone: "Diamond (Heera) or White Sapphire", color: "White/Clear", planet: "Shukra", purpose: "Love, Wealth, Beauty" },
  Mithuna:   { stone: "Emerald (Panna)",                  color: "Green",       planet: "Budh",   purpose: "Intellect, Communication, Business" },
  Karka:     { stone: "Pearl (Moti)",                     color: "White",       planet: "Chandra", purpose: "Emotions, Home, Mother" },
  Simha:     { stone: "Ruby (Manik)",                     color: "Red",         planet: "Surya",  purpose: "Authority, Career, Health" },
  Kanya:     { stone: "Emerald (Panna)",                  color: "Green",       planet: "Budh",   purpose: "Analysis, Health, Service" },
  Tula:      { stone: "Diamond or White Sapphire",        color: "White/Clear", planet: "Shukra", purpose: "Relationships, Harmony, Wealth" },
  Vrischika: { stone: "Red Coral (Moonga)",               color: "Red",         planet: "Mangal", purpose: "Power, Research, Transformation" },
  Dhanu:     { stone: "Yellow Sapphire (Pukhraj)",        color: "Yellow",      planet: "Guru",   purpose: "Fortune, Wisdom, Expansion" },
  Makara:    { stone: "Blue Sapphire (Neelam)",           color: "Blue",        planet: "Shani",  purpose: "Discipline, Career, Karma — wear with extreme caution after testing" },
  Kumbha:    { stone: "Blue Sapphire (Neelam)",           color: "Blue",        planet: "Shani",  purpose: "Humanitarian work, Innovation — wear with extreme caution" },
  Meena:     { stone: "Yellow Sapphire (Pukhraj)",        color: "Yellow",      planet: "Guru",   purpose: "Spirituality, Fortune, Compassion" },
};

const RASHI_LORD: Record<string, string> = {
  Mesha: "Mangal", Vrishabha: "Shukra", Mithuna: "Budh", Karka: "Chandra",
  Simha: "Surya",  Kanya: "Budh",       Tula: "Shukra",  Vrischika: "Mangal",
  Dhanu: "Guru",   Makara: "Shani",     Kumbha: "Shani", Meena: "Guru",
};

interface WearEntry {
  metal: string;
  finger: string;
  day: string;
  mantra: string;
}

const STONE_WEAR: Record<string, WearEntry> = {
  Mangal:  { metal: "Gold or Copper",    finger: "Ring finger",   day: "Tuesday",   mantra: "ॐ क्रां क्रीं क्रौं सः भौमाय नमः" },
  Shukra:  { metal: "Gold or Silver",    finger: "Middle finger", day: "Friday",    mantra: "ॐ द्रां द्रीं द्रौं सः शुक्राय नमः" },
  Budh:    { metal: "Gold",              finger: "Little finger", day: "Wednesday", mantra: "ॐ ब्रां ब्रीं ब्रौं सः बुधाय नमः" },
  Chandra: { metal: "Silver",            finger: "Little finger", day: "Monday",    mantra: "ॐ श्रां श्रीं श्रौं सः चंद्रमसे नमः" },
  Surya:   { metal: "Gold",              finger: "Ring finger",   day: "Sunday",    mantra: "ॐ ह्रां ह्रीं ह्रौं सः सूर्याय नमः" },
  Guru:    { metal: "Gold",              finger: "Index finger",  day: "Thursday",  mantra: "ॐ ग्रां ग्रीं ग्रौं सः गुरवे नमः" },
  Shani:   { metal: "Iron or Silver",    finger: "Middle finger", day: "Saturday",  mantra: "ॐ प्रां प्रीं प्रौं सः शनैश्चराय नमः" },
};

// House lords for benefic section — 4th, 9th, 10th house lords derived from lagna
// We derive 4th/9th/10th lord rashis by counting from lagna
const RASHI_ORDER = [
  "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
  "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena",
];

function rashiAt(lagnaRashi: string, houseOffset: number): string {
  const idx = RASHI_ORDER.indexOf(lagnaRashi);
  if (idx === -1) return "";
  return RASHI_ORDER[(idx + houseOffset) % 12];
}

// Gem color → Tailwind accent
function gemColorClass(color: string): string {
  const c = color.toLowerCase();
  if (c.includes("red"))    return "from-red-500/20 to-red-600/10 border-red-500/30";
  if (c.includes("green"))  return "from-emerald-500/20 to-emerald-600/10 border-emerald-500/30";
  if (c.includes("yellow")) return "from-amber-400/20 to-amber-500/10 border-amber-400/30";
  if (c.includes("blue"))   return "from-blue-500/20 to-blue-600/10 border-blue-500/30";
  if (c.includes("white") || c.includes("clear")) return "from-slate-300/20 to-slate-400/10 border-slate-400/30";
  return "from-purple-500/20 to-purple-600/10 border-purple-500/30";
}

function gemIconColor(color: string): string {
  const c = color.toLowerCase();
  if (c.includes("red"))    return "text-red-400";
  if (c.includes("green"))  return "text-emerald-400";
  if (c.includes("yellow")) return "text-amber-400";
  if (c.includes("blue"))   return "text-blue-400";
  if (c.includes("white") || c.includes("clear")) return "text-slate-300";
  return "text-purple-400";
}

// ── StoneCard ─────────────────────────────────────────────────────────────────
function StoneCard({
  label,
  entry,
  badge,
  size = "normal",
  remedyFor,
  delay = 0,
}: {
  label: string;
  entry: StoneEntry;
  badge?: string;
  size?: "large" | "normal";
  remedyFor?: string;
  delay?: number;
}) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const wear = STONE_WEAR[entry.planet];
  const isCaution = entry.planet === "Shani";

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay }}
      className={`relative rounded-xl border bg-gradient-to-br ${gemColorClass(entry.color)} p-${size === "large" ? "5" : "4"} space-y-2`}
    >
      {badge && (
        <span className="absolute top-3 right-3 text-xs px-2 py-0.5 rounded-full bg-primary/20 text-primary font-medium">
          {badge}
        </span>
      )}
      {isCaution && (
        <span className="absolute top-3 right-3 text-xs px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-400 font-medium flex items-center gap-1">
          <AlertTriangle className="h-3 w-3" /> {t("gemstones.caution")}
        </span>
      )}

      <div className="flex items-center gap-2.5">
        <Gem className={`shrink-0 ${size === "large" ? "h-6 w-6" : "h-5 w-5"} ${gemIconColor(entry.color)}`} />
        <div>
          <p className={`font-display ${size === "large" ? "text-lg" : "text-base"} text-foreground leading-tight`}>
            {entry.stone}
          </p>
          <p className="text-xs text-muted-foreground">{label}</p>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 text-xs">
        <span className="px-2 py-0.5 rounded-full bg-muted/30 text-muted-foreground">
          {t("gemstones.planet_label")}: <span className="text-foreground font-medium">{entry.planet}</span>
        </span>
        <span className="px-2 py-0.5 rounded-full bg-muted/30 text-muted-foreground">
          {t("gemstones.color_label")}: <span className="text-foreground font-medium">{entry.color}</span>
        </span>
      </div>

      <p className="text-xs text-muted-foreground leading-relaxed">{entry.purpose}</p>

      {remedyFor && (
        <p className="text-xs text-amber-400/90 leading-relaxed">
          {t("gemstones.remedy_for", { planet: remedyFor })}
        </p>
      )}

      {wear && (
        <>
          <button
            onClick={() => setOpen(!open)}
            className="flex items-center gap-1.5 text-xs text-primary/80 hover:text-primary transition-colors pt-0.5"
          >
            <Info className="h-3 w-3" />
            {t("gemstones.how_to_wear")}
            {open ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
          </button>
          <AnimatePresence>
            {open && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: "auto" }}
                exit={{ opacity: 0, height: 0 }}
                className="overflow-hidden"
              >
                <div className="pt-2 border-t border-border/20 grid grid-cols-2 gap-x-4 gap-y-1.5 text-xs">
                  <div>
                    <span className="text-muted-foreground">{t("gemstones.metal")}:</span>{" "}
                    <span className="text-foreground font-medium">{wear.metal}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">{t("gemstones.finger")}:</span>{" "}
                    <span className="text-foreground font-medium">{wear.finger}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">{t("gemstones.day")}:</span>{" "}
                    <span className="text-foreground font-medium">{wear.day}</span>
                  </div>
                  <div className="col-span-2">
                    <span className="text-muted-foreground">{t("gemstones.mantra")}:</span>{" "}
                    <span className="text-foreground font-medium select-all">{wear.mantra}</span>
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </>
      )}
    </motion.div>
  );
}

// ── General guide (no kundali) ────────────────────────────────────────────────
function GeneralGuide() {
  const { t } = useTranslation();
  return (
    <div className="space-y-4">
      <div className="cosmic-card rounded-xl p-4 flex items-start gap-3">
        <Info className="h-4 w-4 text-primary shrink-0 mt-0.5" />
        <div>
          <p className="text-sm font-medium text-foreground mb-1">{t("gemstones.no_kundali")}</p>
          <p className="text-xs text-muted-foreground leading-relaxed">
            {t("gemstones.no_kundali_desc")}{" "}
            <Link to="/kundli" className="text-primary hover:underline">
              {t("gemstones.go_to_kundali")}
            </Link>
          </p>
        </div>
      </div>

      <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide pt-2">
        {t("gemstones.general_title")}
      </h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {Object.entries(LAGNA_STONE).map(([rashi, entry], i) => (
          <StoneCard
            key={rashi}
            label={rashi}
            entry={entry}
            delay={0.03 * i}
          />
        ))}
      </div>
    </div>
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function GemstoneRecommendationsPage() {
  const { t } = useTranslation();
  const kundaliData = useKundliStore((s) => s.kundaliData);

  if (!kundaliData) {
    return (
      <div className="p-4 sm:p-6 space-y-6">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <div className="flex items-center gap-2.5 mb-1">
            <Gem className="h-6 w-6 text-primary" />
            <h1 className="font-display text-2xl text-foreground text-glow-gold">{t("gemstones.title")}</h1>
          </div>
          <p className="text-sm text-muted-foreground">{t("gemstones.subtitle")}</p>
        </motion.div>
        <GeneralGuide />
      </div>
    );
  }

  // ── Extract data from store ──────────────────────────────────────────────
  const lagnaRashi   = kundaliData.lagna?.rashi ?? "";
  const moonRashi    = kundaliData.grahas?.["Chandra"]?.rashi ?? "";
  const grahas       = kundaliData.grahas ?? {};

  const primaryEntry  = LAGNA_STONE[lagnaRashi];
  const secondaryEntry = LAGNA_STONE[moonRashi];

  // Benefic house lords: 4th, 9th, 10th from lagna
  const beneficHouses = [
    { label: t("gemstones.house_4th"), house: 3 },
    { label: t("gemstones.house_9th"), house: 8 },
    { label: t("gemstones.house_10th"), house: 9 },
  ];

  const beneficEntries = beneficHouses
    .map(({ label, house }) => {
      const rashi = rashiAt(lagnaRashi, house);
      const entry = LAGNA_STONE[rashi];
      if (!entry) return null;
      // Skip if same as primary
      if (entry.planet === primaryEntry?.planet) return null;
      return { label, rashi, entry };
    })
    .filter(Boolean) as { label: string; rashi: string; entry: StoneEntry }[];

  // Deduplicate benefic by planet
  const seenPlanets = new Set<string>(primaryEntry ? [primaryEntry.planet] : []);
  if (secondaryEntry) seenPlanets.add(secondaryEntry.planet);
  const uniqueBenefic = beneficEntries.filter((b) => {
    if (seenPlanets.has(b.entry.planet)) return false;
    seenPlanets.add(b.entry.planet);
    return true;
  });

  // Debilitated planets needing remedy
  const PLANET_LORD_RASHI: Record<string, string> = {
    Surya: "Simha", Chandra: "Karka", Mangal: "Mesha", Budh: "Mithuna",
    Guru: "Dhanu", Shukra: "Vrishabha", Shani: "Makara",
    Rahu: "", Ketu: "",
  };
  const debilitatedPlanets = Object.entries(grahas)
    .filter(([, g]) => g?.status?.includes("Neecha"))
    .map(([planet]) => {
      // Stone for the debilitated planet itself (strengthen it)
      const lordRashi = PLANET_LORD_RASHI[planet];
      const entry = LAGNA_STONE[lordRashi ?? ""] ?? null;
      return { planet, entry };
    })
    .filter((d) => d.entry !== null) as { planet: string; entry: StoneEntry }[];

  // Exalted planets (fortify strength)
  const exaltedPlanets = Object.entries(grahas)
    .filter(([, g]) => g?.status?.includes("Uchcha"))
    .map(([planet]) => {
      const lordRashi = PLANET_LORD_RASHI[planet];
      const entry = LAGNA_STONE[lordRashi ?? ""] ?? null;
      return { planet, entry };
    })
    .filter((d) => d.entry !== null) as { planet: string; entry: StoneEntry }[];

  const cautionRules = [
    "Never wear Blue Sapphire (Neelam) without a 3-day trial on the body first — observe dreams, mood, and events.",
    "Do not combine Red Coral (Moonga) with Emerald (Panna) — Mars and Mercury are natural enemies.",
    "Do not wear Pearl and Blue Sapphire together without expert guidance.",
    "Yellow Sapphire and Diamond are generally compatible but consult a Jyotishi for your specific chart.",
    "Wear gemstones only in the prescribed metal, finger, and day for maximum effect.",
    "Minimum weight: Ruby 3 ratti, Pearl 5 ratti, Emerald 3 ratti, Yellow Sapphire 3 ratti, Blue Sapphire 2 ratti.",
  ];

  return (
    <div className="p-4 sm:p-6 space-y-8">

      {/* Header */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <div className="flex items-center gap-2.5 mb-1">
          <Gem className="h-6 w-6 text-primary" />
          <h1 className="font-display text-2xl text-foreground text-glow-gold">{t("gemstones.title")}</h1>
        </div>
        <p className="text-sm text-muted-foreground">
          {t("gemstones.subtitle")} —{" "}
          <span className="text-foreground font-medium">{lagnaRashi} lagna</span> ·{" "}
          <span className="text-foreground font-medium">{moonRashi} moon</span>
        </p>
      </motion.div>

      {/* ── Primary stone ── */}
      {primaryEntry && (
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <Star className="h-4 w-4 text-amber-400" />
            <h2 className="text-sm font-semibold text-foreground uppercase tracking-wide">
              {t("gemstones.primary_gem")} — {lagnaRashi}
            </h2>
          </div>
          <StoneCard
            label={`${lagnaRashi} lagna stone · ruled by ${RASHI_LORD[lagnaRashi] ?? ""}`}
            entry={primaryEntry}
            badge="Primary"
            size="large"
            delay={0}
          />
        </section>
      )}

      {/* ── Secondary stone (moon) ── */}
      {secondaryEntry && secondaryEntry.planet !== primaryEntry?.planet && (
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-base">☽︎</span>
            <h2 className="text-sm font-semibold text-foreground uppercase tracking-wide">
              {t("gemstones.secondary_gem")} — {moonRashi}
            </h2>
          </div>
          <StoneCard
            label={`${moonRashi} moon stone · ruled by ${RASHI_LORD[moonRashi] ?? ""}`}
            entry={secondaryEntry}
            badge="Secondary"
            delay={0.06}
          />
        </section>
      )}

      {/* ── Benefic/supportive stones ── */}
      {uniqueBenefic.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-base">✦</span>
            <h2 className="text-sm font-semibold text-foreground uppercase tracking-wide">
              {t("gemstones.benefic_gems")}
            </h2>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {uniqueBenefic.map((b, i) => (
              <StoneCard
                key={b.rashi}
                label={b.label}
                entry={b.entry}
                delay={0.06 * (i + 1)}
              />
            ))}
          </div>
        </section>
      )}

      {/* ── Exalted planets ── */}
      {exaltedPlanets.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="text-base text-emerald-400">↑</span>
            <h2 className="text-sm font-semibold text-emerald-400 uppercase tracking-wide">
              {t("gemstones.exalted_title")}
            </h2>
          </div>
          <p className="text-xs text-muted-foreground -mt-1">
            {t("gemstones.exalted_desc")}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {exaltedPlanets.map((e, i) => (
              <StoneCard
                key={e.planet}
                label={`${e.planet} — Exalted (Uchcha)`}
                entry={e.entry}
                badge="Exalted"
                delay={0.05 * (i + 1)}
              />
            ))}
          </div>
        </section>
      )}

      {/* ── Debilitated planets needing remedy ── */}
      {debilitatedPlanets.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-400" />
            <h2 className="text-sm font-semibold text-amber-400 uppercase tracking-wide">
              {t("gemstones.debilitated_title")}
            </h2>
          </div>
          <p className="text-xs text-muted-foreground -mt-1">
            {t("gemstones.debilitated_desc")}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {debilitatedPlanets.map((d, i) => (
              <StoneCard
                key={d.planet}
                label={`Remedy for ${d.planet} weakness`}
                entry={d.entry}
                remedyFor={d.planet}
                delay={0.05 * (i + 1)}
              />
            ))}
          </div>
        </section>
      )}

      {/* ── Caution section ── */}
      <section className="space-y-3">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-4 w-4 text-red-400" />
          <h2 className="text-sm font-semibold text-red-400 uppercase tracking-wide">
            Caution &amp; Important Notes
          </h2>
        </div>
        <div className="cosmic-card rounded-xl p-4 space-y-2">
          {cautionRules.map((rule, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, x: -4 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.04 * i }}
              className="flex items-start gap-2.5"
            >
              <AlertTriangle className="h-3 w-3 text-amber-400 shrink-0 mt-0.5" />
              <p className="text-xs text-muted-foreground leading-relaxed">{rule}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Disclaimer */}
      <p className="text-xs text-muted-foreground/60 leading-relaxed pb-2">
        Ratna Shastra recommendations are based on classical Vedic astrology texts (Brihat Parashara Hora Shastra).
        Always consult a qualified Jyotishi before wearing gemstones — individual chart combinations vary significantly.
      </p>
    </div>
  );
}
