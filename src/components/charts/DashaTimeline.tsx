import { motion } from "framer-motion";
import { dashaData, useKundliStore } from "@/store/kundliStore";

const natureColors = {
  positive: "bg-emerald-500/70",
  neutral: "bg-amber-500/70",
  challenging: "bg-red-500/70",
};

const natureBorders = {
  positive: "border-emerald-500/30",
  neutral: "border-amber-500/30",
  challenging: "border-red-500/30",
};

export function DashaTimeline() {
  const kundaliData = useKundliStore((s) => s.kundaliData);
  const currentYear = new Date().getFullYear();

  // Use real dasha data from API if available
  const displayData = kundaliData?.dashas
    ? kundaliData.dashas.map((d) => ({
        planet: d.lord,
        start: parseInt(d.start.split("-")[0]),
        end: parseInt(d.end.split("-")[0]),
        nature: "neutral" as const,
      }))
    : dashaData;

  const totalYears = displayData[displayData.length - 1].end - displayData[0].start;

  return (
    <div className="space-y-4">
      {displayData.map((dasha, i) => {
        const duration = dasha.end - dasha.start;
        const widthPercent = (duration / totalYears) * 100;
        const isCurrent = currentYear >= dasha.start && currentYear < dasha.end;

        return (
          <motion.div
            key={dasha.planet}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: i * 0.1 }}
            className={`cosmic-card rounded-lg p-4 border ${natureBorders[dasha.nature]} ${isCurrent ? "glow-border" : ""}`}
          >
            <div className="flex items-center justify-between mb-2">
              <span className="font-display text-sm text-foreground">
                {dasha.planet} Dasha
                {isCurrent && <span className="ml-2 text-xs text-primary animate-pulse-glow">● Active</span>}
              </span>
              <span className="text-xs text-muted-foreground">
                {dasha.start} — {dasha.end}
              </span>
            </div>
            <div className="w-full h-2 bg-muted/30 rounded-full overflow-hidden">
              <motion.div
                className={`h-full rounded-full ${natureColors[dasha.nature]}`}
                initial={{ width: 0 }}
                animate={{ width: `${widthPercent}%` }}
                transition={{ delay: 0.3 + i * 0.1, duration: 0.6 }}
              />
            </div>
            <div className="flex justify-between mt-1">
              <span className="text-xs text-muted-foreground">{duration} years</span>
              <span className={`text-xs capitalize ${
                dasha.nature === "positive" ? "text-emerald-400" :
                dasha.nature === "neutral" ? "text-amber-400" : "text-red-400"
              }`}>
                {dasha.nature}
              </span>
            </div>
          </motion.div>
        );
      })}

      <div className="flex items-center gap-4 pt-2 text-xs text-muted-foreground">
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-emerald-500/70" /> Positive</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-amber-500/70" /> Neutral</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-red-500/70" /> Challenging</span>
      </div>
    </div>
  );
}
