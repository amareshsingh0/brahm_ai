import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useTranslation } from "react-i18next";
import { ChevronDown, ChevronUp } from "lucide-react";

interface RashiCardProps {
  name: string;
  symbol: string;
  element: string;
  ruler: string;
  traits: string;
  index: number;
  sanskritName?: string;
  quality?: string;
  nature?: string;
  bodyPart?: string;
  luckyColor?: string;
  userTag?: "lagna" | "moon" | "sun";
}

const elementColors: Record<string, string> = {
  Fire:  "from-red-50  to-orange-50  border-red-200",
  Earth: "from-green-50 to-emerald-50 border-green-200",
  Air:   "from-sky-50   to-cyan-50    border-sky-200",
  Water: "from-blue-50  to-indigo-50  border-blue-200",
};

const userTagStyles: Record<string, { badge: string; ring: string }> = {
  lagna: { badge: "bg-amber-100 text-amber-800 border border-amber-300", ring: "ring-2 ring-amber-400" },
  moon:  { badge: "bg-blue-100  text-blue-800  border border-blue-300",  ring: "ring-2 ring-blue-400" },
  sun:   { badge: "bg-orange-100 text-orange-800 border border-orange-300", ring: "ring-2 ring-orange-400" },
};

const userTagLabels: Record<string, string> = {
  lagna: "Lagna",
  moon:  "Janma Rashi",
  sun:   "Sun Sign",
};

export function RashiCard({ name, symbol, element, ruler, traits, index, sanskritName, quality, nature, bodyPart, luckyColor, userTag }: RashiCardProps) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);

  const key = sanskritName?.toLowerCase().replace(/\s+/g, "_") ?? "";
  const translatedName = key ? t(`data.rashi.${key}.name`, { defaultValue: name }) : name;
  const translatedElement = key ? t(`data.rashi.${key}.element`, { defaultValue: element }) : element;
  const translatedTraits = key ? t(`data.rashi.${key}.traits`, { defaultValue: traits }) : traits;
  const translatedRuler = t(`planet.${ruler}`, { defaultValue: ruler });

  const tagStyle = userTag ? userTagStyles[userTag] : null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.4 }}
      whileHover={{ scale: 1.02, y: -2 }}
      className={`relative rounded-xl border p-5 bg-gradient-to-br ${elementColors[element] || "from-muted to-background border-border"} ${tagStyle?.ring ?? ""} transition-all cursor-pointer`}
      onClick={() => setExpanded((v) => !v)}
      role="article"
      aria-label={`${name} zodiac sign, ${element} element, ruled by ${ruler}`}
    >
      {/* User tag badge */}
      {tagStyle && (
        <span className={`absolute top-2 right-2 text-[10px] font-semibold px-2 py-0.5 rounded-full ${tagStyle.badge}`}>
          {userTagLabels[userTag!]}
        </span>
      )}

      <div className="text-4xl mb-3 zodiac-glow">{symbol}</div>
      <h3 className="font-display text-lg text-foreground mb-0.5">{translatedName}</h3>
      {sanskritName && <p className="text-xs text-amber-700 mb-1">{sanskritName}</p>}

      <div className="flex items-center gap-2 mb-3 flex-wrap">
        <span className="text-xs text-amber-800 bg-amber-100 px-2 py-0.5 rounded-full whitespace-nowrap">{translatedElement}</span>
        <span className="text-xs text-muted-foreground whitespace-nowrap">♦ {translatedRuler}</span>
      </div>

      <p className="text-xs text-muted-foreground leading-relaxed">{translatedTraits}</p>

      {/* Expand button */}
      <div className="flex justify-end mt-2">
        <button className="text-muted-foreground/60 hover:text-muted-foreground" aria-label="Toggle details">
          {expanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        </button>
      </div>

      {/* Expandable details */}
      <AnimatePresence>
        {expanded && (
          <motion.div
            key="details"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="border-t border-border/40 mt-2 pt-3 grid grid-cols-2 gap-y-2 gap-x-3 text-xs">
              {quality && (
                <div>
                  <p className="text-muted-foreground/60 uppercase tracking-wide text-[10px]">Quality</p>
                  <p className="text-foreground font-medium">{quality}</p>
                </div>
              )}
              {nature && (
                <div>
                  <p className="text-muted-foreground/60 uppercase tracking-wide text-[10px]">Nature</p>
                  <p className="text-foreground font-medium">{nature}</p>
                </div>
              )}
              {bodyPart && (
                <div>
                  <p className="text-muted-foreground/60 uppercase tracking-wide text-[10px]">Body Part</p>
                  <p className="text-foreground font-medium">{bodyPart}</p>
                </div>
              )}
              {luckyColor && (
                <div>
                  <p className="text-muted-foreground/60 uppercase tracking-wide text-[10px]">Lucky Color</p>
                  <p className="text-foreground font-medium">{luckyColor}</p>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
