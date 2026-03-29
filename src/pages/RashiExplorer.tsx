import { motion } from "framer-motion";
import { RashiCard } from "@/components/cards/RashiCard";
import { rashiData, useKundliStore } from "@/store/kundliStore";
import { useTranslation } from "react-i18next";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";

export default function RashiExplorer() {
  const { t } = useTranslation();
  const kundaliData = useKundliStore((s) => s.kundaliData);
  useRegisterPageBot('rashi', {});

  // Extract user's Moon/Lagna/Sun rashi from saved kundali (English names)
  const lagnaRashi  = kundaliData?.lagna?.rashi_en  ?? kundaliData?.lagna?.rashi  ?? null;
  const moonRashi   = (kundaliData?.grahas?.["Moon"]?.rashi_en  ?? kundaliData?.grahas?.["Moon"]?.rashi)  ?? null;
  const sunRashi    = (kundaliData?.grahas?.["Sun"]?.rashi_en   ?? kundaliData?.grahas?.["Sun"]?.rashi)   ?? null;

  const getUserTag = (name: string) => {
    if (lagnaRashi && name === lagnaRashi) return "lagna" as const;
    if (moonRashi  && name === moonRashi)  return "moon"  as const;
    if (sunRashi   && name === sunRashi)   return "sun"   as const;
    return undefined;
  };

  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">{t("rashi_explorer.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("rashi_explorer.subtitle")}</p>
        {kundaliData && (
          <div className="flex flex-wrap gap-2 mt-3 text-xs">
            {lagnaRashi && <span className="bg-amber-100 text-amber-800 border border-amber-300 px-2 py-0.5 rounded-full">Lagna: {lagnaRashi}</span>}
            {moonRashi  && <span className="bg-blue-100  text-blue-800  border border-blue-300  px-2 py-0.5 rounded-full">Janma Rashi: {moonRashi}</span>}
            {sunRashi   && <span className="bg-orange-100 text-orange-800 border border-orange-300 px-2 py-0.5 rounded-full">Sun Sign: {sunRashi}</span>}
          </div>
        )}
      </motion.div>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {rashiData.map((rashi, i) => (
          <RashiCard
            key={rashi.name}
            {...rashi}
            index={i}
            userTag={getUserTag(rashi.name)}
          />
        ))}
      </div>
    </div>
  );
}
