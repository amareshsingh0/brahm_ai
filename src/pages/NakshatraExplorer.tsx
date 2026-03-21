import { motion } from "framer-motion";
import { nakshatraData } from "@/store/kundliStore";
import { useState } from "react";
import { X } from "lucide-react";

export default function NakshatraExplorer() {
  const [selected, setSelected] = useState<typeof nakshatraData[0] | null>(null);

  return (
    <div className="p-6 space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">Nakshatra Explorer</h1>
        <p className="text-sm text-muted-foreground">The 27 lunar mansions of Vedic astrology</p>
      </motion.div>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-3">
        {nakshatraData.map((n, i) => (
          <motion.button
            key={n.name}
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.03 }}
            whileHover={{ scale: 1.03, y: -3 }}
            onClick={() => setSelected(n)}
            className="cosmic-card rounded-xl p-4 text-left hover:glow-border transition-shadow"
            aria-label={`${n.name} nakshatra, ruled by ${n.ruler}`}
          >
            <span className="text-2xl mb-2 block">{n.symbol}</span>
            <p className="font-display text-sm text-foreground">{n.name}</p>
            <p className="text-xs text-primary/70">#{n.number} · {n.ruler}</p>
            <p className="text-xs text-muted-foreground mt-1">{n.nature}</p>
          </motion.button>
        ))}
      </div>

      {/* Detail panel */}
      {selected && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="cosmic-card rounded-xl p-6 max-w-lg mx-auto"
        >
          <div className="flex justify-between items-start mb-4">
            <div className="flex items-center gap-3">
              <span className="text-4xl">{selected.symbol}</span>
              <div>
                <h3 className="font-display text-xl text-foreground">{selected.name}</h3>
                <p className="text-xs text-primary">Nakshatra #{selected.number}</p>
              </div>
            </div>
            <button onClick={() => setSelected(null)} className="text-muted-foreground hover:text-foreground">
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-muted/20 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">Deity</span>
              <span className="text-foreground">{selected.deity}</span>
            </div>
            <div className="bg-muted/20 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">Ruler</span>
              <span className="text-foreground">{selected.ruler}</span>
            </div>
            <div className="bg-muted/20 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">Nature</span>
              <span className="text-foreground">{selected.nature}</span>
            </div>
            <div className="bg-muted/20 rounded-lg p-3">
              <span className="text-muted-foreground text-xs block">Rashi</span>
              <span className="text-foreground">{selected.pada}</span>
            </div>
          </div>
          <div className="mt-4 bg-muted/20 rounded-lg p-3">
            <span className="text-muted-foreground text-xs block mb-1">Key Traits</span>
            <span className="text-sm text-foreground">{selected.traits}</span>
          </div>
        </motion.div>
      )}
    </div>
  );
}
