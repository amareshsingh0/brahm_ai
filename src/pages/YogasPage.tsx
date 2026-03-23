import { motion } from "framer-motion";
import { yogasData, useKundliStore } from "@/store/kundliStore";
import { useTranslation } from "react-i18next";

const effectStyles = {
  benefic: { badge: "bg-emerald-500/20 text-emerald-400", border: "border-emerald-500/20" },
  malefic: { badge: "bg-red-500/20 text-red-400", border: "border-red-500/20" },
  mixed: { badge: "bg-amber-500/20 text-amber-400", border: "border-amber-500/20" },
};

export default function YogasPage() {
  const { t } = useTranslation();
  const kundaliData = useKundliStore((s) => s.kundaliData);

  // Use real yogas if kundali is calculated, else fallback to sample
  const presentYogas = kundaliData?.yogas
    ? kundaliData.yogas.map((y) => ({
        name: y.name, sanskritName: y.name, planets: [y.desc],
        description: y.desc, effect: "benefic" as const, present: true,
      }))
    : yogasData.filter((y) => y.present);
  const absentYogas = kundaliData?.yogas ? [] : yogasData.filter((y) => !y.present);

  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">{t("yogas.title")}</h1>
        <p className="text-sm text-muted-foreground">{t("yogas.subtitle")}</p>
      </motion.div>

      {/* Present yogas */}
      <div>
        <h2 className="font-display text-sm text-primary mb-3 uppercase tracking-wider">{t("yogas.present_heading")}</h2>
        <div className="grid md:grid-cols-2 gap-4">
          {presentYogas.map((yoga, i) => (
            <YogaCard key={yoga.name} yoga={yoga} index={i} />
          ))}
        </div>
      </div>

      {/* Absent yogas */}
      <div>
        <h2 className="font-display text-sm text-muted-foreground mb-3 uppercase tracking-wider">{t("yogas.absent_heading")}</h2>
        <div className="grid md:grid-cols-2 gap-4">
          {absentYogas.map((yoga, i) => (
            <YogaCard key={yoga.name} yoga={yoga} index={i} dimmed />
          ))}
        </div>
      </div>
    </div>
  );
}

function YogaCard({ yoga, index, dimmed = false }: { yoga: typeof yogasData[0]; index: number; dimmed?: boolean }) {
  const { t } = useTranslation();
  const styles = effectStyles[yoga.effect];
  const yogaKey = yoga.name.toLowerCase().replace(/ /g, "_");
  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.08 }}
      className={`cosmic-card rounded-xl p-5 border ${styles.border} ${dimmed ? "opacity-50" : ""}`}
    >
      <div className="flex items-start justify-between mb-3">
        <div>
          <h3 className="font-display text-lg text-foreground">{yoga.name}</h3>
          <p className="text-xs text-primary/60">{yoga.sanskritName}</p>
        </div>
        <span className={`text-xs px-2 py-0.5 rounded-full ${styles.badge}`}>
          {t(`yogas.effect_${yoga.effect}`, { defaultValue: yoga.effect })}
        </span>
      </div>
      <p className="text-xs text-muted-foreground leading-relaxed mb-3">{t(`data.yoga.${yogaKey}.desc`, { defaultValue: yoga.description })}</p>
      <div className="flex gap-1.5">
        {yoga.planets.map((p) => (
          <span key={p} className="text-xs px-2 py-0.5 rounded-full bg-muted/30 text-muted-foreground">
            {t(`planet.${p}`, { defaultValue: p })}
          </span>
        ))}
      </div>
    </motion.div>
  );
}
