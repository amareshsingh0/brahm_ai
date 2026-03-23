import { motion } from "framer-motion";
import { useTranslation } from "react-i18next";

interface RashiCardProps {
  name: string;
  symbol: string;
  element: string;
  ruler: string;
  traits: string;
  index: number;
  sanskritName?: string;
}

const elementColors: Record<string, string> = {
  Fire: "from-red-900/30 to-orange-900/20 border-red-500/20",
  Earth: "from-green-900/30 to-emerald-900/20 border-green-500/20",
  Air: "from-cyan-900/30 to-sky-900/20 border-cyan-500/20",
  Water: "from-blue-900/30 to-indigo-900/20 border-blue-500/20",
};

export function RashiCard({ name, symbol, element, ruler, traits, index, sanskritName }: RashiCardProps) {
  const { t } = useTranslation();
  // Build i18n key from sanskritName (lowercase, used as rashi data key)
  const key = sanskritName?.toLowerCase().replace(/\s+/g, "_") ?? "";
  const translatedName = key ? t(`data.rashi.${key}.name`, { defaultValue: name }) : name;
  const translatedElement = key ? t(`data.rashi.${key}.element`, { defaultValue: element }) : element;
  const translatedTraits = key ? t(`data.rashi.${key}.traits`, { defaultValue: traits }) : traits;
  const translatedRuler = t(`planet.${ruler}`, { defaultValue: ruler });
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.4 }}
      whileHover={{ scale: 1.04, y: -4 }}
      className={`cosmic-card rounded-xl p-5 cursor-pointer bg-gradient-to-br ${elementColors[element] || ""} transition-shadow hover:shadow-[var(--shadow-glow)]`}
      role="article"
      aria-label={`${name} zodiac sign, ${element} element, ruled by ${ruler}`}
    >
      <div className="text-4xl mb-3 zodiac-glow">{symbol}</div>
      <h3 className="font-display text-lg text-foreground mb-0.5">{translatedName}</h3>
      {sanskritName && <p className="text-xs text-primary/60 mb-1">{sanskritName}</p>}
      <div className="flex items-center gap-2 mb-3">
        <span className="text-xs text-primary/80 bg-primary/10 px-2 py-0.5 rounded-full">{translatedElement}</span>
        <span className="text-xs text-muted-foreground">♦ {translatedRuler}</span>
      </div>
      <p className="text-xs text-muted-foreground leading-relaxed">{translatedTraits}</p>
    </motion.div>
  );
}
