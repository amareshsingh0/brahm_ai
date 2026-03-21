import { motion } from "framer-motion";
import { rashiData } from "@/store/kundliStore";
import { useState } from "react";
import { useHoroscope } from "@/hooks/useHoroscope";
import PageBot from '@/components/PageBot';

export default function HoroscopePage() {
  const [selectedRashi, setSelectedRashi] = useState("Aries");
  const { data: horoscope } = useHoroscope(selectedRashi);

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">Daily Horoscope</h1>
        <p className="text-sm text-muted-foreground">Your cosmic guidance for today</p>
      </motion.div>

      {/* Rashi selector */}
      <div className="flex flex-wrap gap-2">
        {rashiData.map((r) => (
          <button
            key={r.name}
            onClick={() => setSelectedRashi(r.name)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs transition-all ${
              selectedRashi === r.name
                ? "bg-primary/20 text-primary glow-border"
                : "text-muted-foreground hover:text-foreground bg-muted/20"
            }`}
            aria-label={`${r.name} horoscope`}
          >
            <span>{r.symbol}</span>
            {r.name}
          </button>
        ))}
      </div>

      {/* Horoscope display */}
      <motion.div
        key={selectedRashi}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-xl"
      >
        <div className="cosmic-card rounded-2xl p-5 sm:p-8 space-y-5 sm:space-y-6">
          <div className="text-center">
            <span className="text-6xl block mb-3">
              {rashiData.find((r) => r.name === selectedRashi)?.symbol}
            </span>
            <h2 className="font-display text-2xl text-foreground">{selectedRashi}</h2>
            <p className="text-xs text-primary mt-1">
              {rashiData.find((r) => r.name === selectedRashi)?.sanskritName} · {new Date().toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric" })}
            </p>
          </div>

          <div className="space-y-4">
            <div className="bg-muted/20 rounded-lg p-4">
              <p className="text-xs text-muted-foreground uppercase tracking-wider mb-2">Today's Prediction</p>
              <p className="text-sm text-foreground leading-relaxed">{horoscope?.prediction ?? "Loading..."}</p>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="bg-muted/20 rounded-lg p-3">
                <p className="text-xs text-muted-foreground mb-1">Lucky</p>
                <p className="text-sm text-primary font-medium">
                  {horoscope ? `${horoscope.lucky_color} · ${horoscope.lucky_number}` : "—"}
                </p>
              </div>
              <div className="bg-muted/20 rounded-lg p-3">
                <p className="text-xs text-muted-foreground mb-1">Nakshatra Lord</p>
                <p className="text-sm text-foreground">{rashiData.find((r) => r.name === selectedRashi)?.ruler ?? "—"}</p>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
      <PageBot pageContext="horoscope" pageData={{}} />
    </div>
  );
}
