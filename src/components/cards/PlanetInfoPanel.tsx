import { motion, AnimatePresence } from "framer-motion";
import type { PlanetData } from "@/store/kundliStore";
import { X } from "lucide-react";

interface PlanetInfoPanelProps {
  planet: PlanetData | null;
  onClose: () => void;
}

const planetDescriptions: Record<string, string> = {
  Sun: "Represents the soul, authority, and vitality. A strong Sun gives leadership qualities and confidence.",
  Moon: "Governs mind, emotions, and intuition. The Moon's placement reveals your emotional nature.",
  Mars: "Planet of energy, courage, and action. Mars drives ambition and determines physical vitality.",
  Mercury: "Rules intellect, communication, and commerce. Mercury shapes how you think and express.",
  Jupiter: "The great benefic — wisdom, expansion, and fortune. Jupiter brings growth and spiritual knowledge.",
  Venus: "Planet of love, beauty, and luxury. Venus governs relationships and artistic expression.",
  Saturn: "Taskmaster planet — discipline, karma, and hard work. Saturn teaches through challenges.",
  Rahu: "Shadow planet of ambition and obsession. Rahu amplifies desires and drives worldly pursuits.",
  Ketu: "Shadow planet of spirituality and detachment. Ketu brings past-life wisdom and liberation.",
};

export function PlanetInfoPanel({ planet, onClose }: PlanetInfoPanelProps) {
  return (
    <AnimatePresence>
      {planet && (
        <motion.div
          initial={{ opacity: 0, x: 30 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 30 }}
          className="cosmic-card rounded-xl p-6 space-y-4"
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="text-3xl" style={{ color: planet.color }}>{planet.symbol}</span>
              <h3 className="font-display text-xl text-foreground">{planet.name}</h3>
            </div>
            <button onClick={onClose} className="text-muted-foreground hover:text-foreground transition-colors">
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-muted/30 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">Rashi</span>
              <span className="text-foreground font-medium">{planet.rashi}</span>
            </div>
            <div className="bg-muted/30 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">House</span>
              <span className="text-foreground font-medium">{planet.house}</span>
            </div>
            <div className="bg-muted/30 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">Degree</span>
              <span className="text-foreground font-medium">{planet.degree}</span>
            </div>
            <div className="bg-muted/30 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">Nakshatra</span>
              <span className="text-foreground font-medium">{planet.nakshatra}</span>
            </div>
          </div>

          <div className="border-t border-border/30 pt-4">
            <h4 className="font-display text-sm text-primary mb-2">Interpretation</h4>
            <p className="text-sm text-muted-foreground leading-relaxed">
              {planetDescriptions[planet.name] || "Celestial body with unique astrological significance."}
            </p>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
