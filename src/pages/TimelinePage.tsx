import { motion } from "framer-motion";
import { DashaTimeline } from "@/components/charts/DashaTimeline";

export default function TimelinePage() {
  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">Dasha Timeline</h1>
        <p className="text-sm text-muted-foreground">Your life's planetary periods based on Vimshottari Dasha system</p>
      </motion.div>

      <div className="max-w-2xl">
        <DashaTimeline />
      </div>
    </div>
  );
}
