import { motion } from "framer-motion";
import { remediesData, samplePlanets } from "@/store/kundliStore";
import { useState } from "react";
import { Gem, Music, Calendar, Palette, Gift, Moon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";

const ENGLISH_TO_RASHI_KEY: Record<string, string> = {
  Aries: "mesha", Taurus: "vrishabha", Gemini: "mithuna", Cancer: "karka",
  Leo: "simha", Virgo: "kanya", Libra: "tula", Scorpio: "vrischika",
  Sagittarius: "dhanu", Capricorn: "makara", Aquarius: "kumbha", Pisces: "meena",
};

export default function RemediesPage() {
  const { t, i18n } = useTranslation();
  useRegisterPageBot('remedies', {});
  const [selectedPlanet, setSelectedPlanet] = useState("Sun");
  const remedy = remediesData.find((r) => r.planet === selectedPlanet)!;
  const planet = samplePlanets.find((p) => p.name === selectedPlanet);
  const mantraText = i18n.language === "en" ? remedy.mantra : (remedy.mantraDevanagari ?? remedy.mantra);

  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">{t("remedies.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("remedies.subtitle")}</p>
      </motion.div>

      {/* Planet selector */}
      <div className="flex flex-wrap gap-2">
        {remediesData.map((r) => {
          const p = samplePlanets.find((sp) => sp.name === r.planet);
          return (
            <button
              key={r.planet}
              onClick={() => setSelectedPlanet(r.planet)}
              className={`flex items-center gap-2 px-4 py-2 rounded-full text-sm transition-all ${
                selectedPlanet === r.planet
                  ? "bg-primary/20 text-primary glow-border"
                  : "text-muted-foreground hover:text-foreground bg-muted/20"
              }`}
              aria-label={`View remedies for ${r.planet}`}
            >
              <span style={{ color: p?.color }}>{p?.symbol}</span>
              {t(`planet.${r.planet}`, { defaultValue: r.planet })}
            </button>
          );
        })}
      </div>

      {/* Remedy detail */}
      <motion.div
        key={selectedPlanet}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-2xl space-y-4"
      >
        <div className="cosmic-card rounded-xl p-6 flex items-center gap-4">
          <span className="text-5xl" style={{ color: planet?.color }}>{planet?.symbol}</span>
          <div>
            <h2 className="font-display text-2xl text-foreground">{t(`planet.${selectedPlanet}`, { defaultValue: selectedPlanet })}</h2>
            <p className="text-sm text-muted-foreground">
              {planet?.sanskritName}
              {planet?.rashi && ` · ${t(`data.rashi.${ENGLISH_TO_RASHI_KEY[planet.rashi] ?? planet.rashi.toLowerCase()}.name`, { defaultValue: planet.rashi })}`}
              {planet?.house && ` · ${t("remedies.house_n", { n: planet.house })}`}
            </p>
          </div>
        </div>

        <div className="grid md:grid-cols-2 gap-4">
          <RemedyCard icon={<Music className="h-5 w-5 text-primary" />} title={t("remedies.mantra")} content={mantraText} sub={t("remedies.chant_desc")} />
          <RemedyCard icon={<Gem className="h-5 w-5 text-nebula" />} title={t("remedies.gemstone")} content={remedy.gemstoneKey ? t(remedy.gemstoneKey, { defaultValue: remedy.gemstone }) : remedy.gemstone} sub={t("remedies.wear_desc")} />
          <RemedyCard icon={<Calendar className="h-5 w-5 text-gold" />} title={t("remedies.fasting_day")} content={remedy.fastingKey ? t(remedy.fastingKey, { defaultValue: remedy.fasting }) : remedy.fasting} sub={t("remedies.fast_desc")} />
          <RemedyCard icon={<Palette className="h-5 w-5 text-foreground" />} title={t("remedies.color")} content={remedy.colorKey ? t(remedy.colorKey, { defaultValue: remedy.color }) : remedy.color} sub={t("remedies.color_desc")} />
          <RemedyCard icon={<Gift className="h-5 w-5 text-primary" />} title={t("remedies.donations")} content={remedy.donationKey ? t(remedy.donationKey, { defaultValue: remedy.donation }) : remedy.donation} sub={t("remedies.donate_desc", { day: remedy.dayKey ? t(remedy.dayKey, { defaultValue: remedy.day }) : remedy.day })} />
          <RemedyCard icon={<Moon className="h-5 w-5 text-muted-foreground" />} title={t("remedies.sacred_day")} content={remedy.dayKey ? t(remedy.dayKey, { defaultValue: remedy.day }) : remedy.day} sub={t("remedies.day_desc")} />
        </div>
      </motion.div>
    </div>
  );
}

function RemedyCard({ icon, title, content, sub }: { icon: React.ReactNode; title: string; content: string; sub: string }) {
  return (
    <div className="cosmic-card rounded-xl p-5 space-y-2">
      <div className="flex items-center gap-2">
        {icon}
        <span className="text-xs text-muted-foreground uppercase tracking-wider">{title}</span>
      </div>
      <p className="font-display text-sm text-foreground">{content}</p>
      <p className="text-xs text-muted-foreground">{sub}</p>
    </div>
  );
}
