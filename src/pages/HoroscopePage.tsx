import { motion } from "framer-motion";
import { rashiData } from "@/store/kundliStore";
import { useState, useEffect } from "react";
import { useHoroscope } from "@/hooks/useHoroscope";
import { useKundliStore } from "@/store/kundliStore";
import { useLivePlanets } from "@/hooks/useLivePlanets";
import PageBot from '@/components/PageBot';
import { useTranslation } from "react-i18next";
import { Sparkles } from "lucide-react";

// English rashi name → rashi data lookup
const RASHI_BY_SANSKRIT: Record<string, string> = {
  Mesha: "Aries", Vrishabha: "Taurus", Mithuna: "Gemini", Karka: "Cancer",
  Simha: "Leo", Kanya: "Virgo", Tula: "Libra", Vrischika: "Scorpio",
  Dhanu: "Sagittarius", Makara: "Capricorn", Kumbha: "Aquarius", Meena: "Pisces",
};

export default function HoroscopePage() {
  const { t, i18n } = useTranslation();
  const kundali = useKundliStore((s) => s.kundaliData);
  const snapshot = useLivePlanets();

  // Derive user's natal Moon rashi (Janma Rashi) from kundali
  const userMoonRashi: string | null = (() => {
    if (!kundali) return null;
    const moonRashiSanskrit = kundali.grahas["Chandra"]?.rashi;
    return moonRashiSanskrit ? (RASHI_BY_SANSKRIT[moonRashiSanskrit] ?? null) : null;
  })();

  const [selectedRashi, setSelectedRashi] = useState(userMoonRashi ?? "Aries");
  const [autoSelected, setAutoSelected] = useState(!!userMoonRashi);

  // Once kundali loads (async), update the selected rashi
  useEffect(() => {
    if (userMoonRashi && autoSelected) {
      setSelectedRashi(userMoonRashi);
    }
  }, [userMoonRashi]);

  const { data: horoscope, isLoading } = useHoroscope(selectedRashi);

  const currentRashiMeta = rashiData.find((r) => r.name === selectedRashi);
  const todayMoonRashi = snapshot?.grahas["Chandra"]?.rashi ?? null; // live Moon rashi (Sanskrit)
  const todayMoonEnglish = todayMoonRashi ? (RASHI_BY_SANSKRIT[todayMoonRashi] ?? todayMoonRashi) : null;

  return (
    <div className="p-4 sm:p-6 space-y-4 sm:space-y-6">
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
        <h1 className="font-display text-2xl text-foreground text-glow-gold mb-1">{t('horoscope.title')}</h1>
        <p className="text-sm text-muted-foreground">{t('horoscope.subtitle')}</p>
      </motion.div>

      {/* Today's Moon position pill */}
      {todayMoonEnglish && (
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <span className="text-base">☽</span>
          <span>Today's Moon is in <span className="text-primary font-medium">{todayMoonEnglish}</span></span>
        </div>
      )}

      {/* Auto-select notice */}
      {autoSelected && userMoonRashi && (
        <div className="flex items-center gap-2 text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 w-fit">
          <Sparkles className="h-3 w-3" />
          Showing your Janma Rashi ({userMoonRashi}) from your Kundali
        </div>
      )}

      {/* Rashi selector */}
      <div className="flex flex-wrap gap-2">
        {rashiData.map((r) => (
          <button
            key={r.name}
            onClick={() => { setSelectedRashi(r.name); setAutoSelected(false); }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs transition-all ${
              selectedRashi === r.name
                ? "bg-primary/20 text-primary glow-border"
                : "text-muted-foreground hover:text-foreground bg-muted/20"
            }`}
            aria-label={`${r.name} horoscope`}
          >
            <span>{r.symbol}</span>
            {t(`data.rashi.${r.sanskritName.toLowerCase()}.name`, { defaultValue: r.name })}
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
              {currentRashiMeta?.symbol}
            </span>
            <h2 className="font-display text-2xl text-foreground">
              {t(`data.rashi.${currentRashiMeta?.sanskritName?.toLowerCase()}.name`, { defaultValue: selectedRashi })}
            </h2>
            <p className="text-xs text-primary mt-1">
              {currentRashiMeta?.sanskritName} · {new Date().toLocaleDateString(i18n.language, { month: "long", day: "numeric", year: "numeric" })}
            </p>
          </div>

          <div className="space-y-4">
            <div className="bg-muted/20 rounded-lg p-4">
              <p className="text-xs text-muted-foreground uppercase tracking-wider mb-2">{t('horoscope.todays_prediction')}</p>
              {isLoading ? (
                <div className="space-y-2">
                  <div className="h-3 bg-muted/40 rounded animate-pulse w-full" />
                  <div className="h-3 bg-muted/40 rounded animate-pulse w-4/5" />
                  <div className="h-3 bg-muted/40 rounded animate-pulse w-3/5" />
                </div>
              ) : (
                <p className="text-sm text-foreground leading-relaxed">
                  {horoscope?.prediction ?? t('horoscope.loading')}
                </p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="bg-muted/20 rounded-lg p-3">
                <p className="text-xs text-muted-foreground mb-1">{t('horoscope.lucky')}</p>
                <p className="text-sm text-primary font-medium">
                  {horoscope ? `${horoscope.lucky_color} · ${horoscope.lucky_number}` : "—"}
                </p>
              </div>
              <div className="bg-muted/20 rounded-lg p-3">
                <p className="text-xs text-muted-foreground mb-1">Sign Ruler</p>
                <p className="text-sm text-foreground">
                  {horoscope?.sign_ruler ?? currentRashiMeta?.ruler ?? "—"}
                </p>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
      <PageBot pageContext="horoscope" pageData={{}} />
    </div>
  );
}
