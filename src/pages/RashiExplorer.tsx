import { motion } from "framer-motion";
import { RashiCard } from "@/components/cards/RashiCard";
import { rashiData } from "@/store/kundliStore";
import { useTranslation } from "react-i18next";

export default function RashiExplorer() {
  const { t } = useTranslation();
  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">{t("rashi_explorer.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("rashi_explorer.subtitle")}</p>
      </motion.div>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {rashiData.map((rashi, i) => (
          <RashiCard key={rashi.name} {...rashi} index={i} />
        ))}
      </div>
    </div>
  );
}
