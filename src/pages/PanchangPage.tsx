import { useState, useEffect } from "react";
import { useTranslation } from 'react-i18next';
import { motion, AnimatePresence } from "framer-motion";
import PageBot from '@/components/PageBot';
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Loader2, MapPin, RefreshCw, Sun, Moon, ChevronDown } from "lucide-react";
import { usePanchang } from "@/hooks/usePanchang";
import { useRahuKaalNotification } from "@/hooks/useRahuKaalNotification";
import { getCities, searchCities, type City } from "@/lib/cities";
import MuhurtaPage from "./MuhurtaPage";

// ── Plain-language explanations ───────────────────────────────────────────────

const ANGA_EXPLAIN: Record<string, { what: string; why: string; doWhat: string }> = {
  Tithi: {
    what: "Tithi is the lunar day — calculated from the angle between the Sun and Moon. Every 12° of difference = 1 tithi. There are 30 tithis in a lunar month.",
    why: "Each tithi has a ruling deity and energy. It determines which activities are naturally supported — fasting, worship, travel, business, or rest.",
    doWhat: "Pratipada/Pratipad: good for starting new work. Panchami/Dashami: auspicious for learning. Ekadashi: fast day, Vishnu worship. Purnima: full moon — powerful for charity. Amavasya: ancestor rites.",
  },
  Nakshatra: {
    what: "The 27 lunar mansions (star clusters) through which the Moon passes. The Moon stays in each nakshatra for about 1 day.",
    why: "The Moon's nakshatra affects your emotional state, intuition, and the type of energy available for activities. Each has a ruling deity and planet.",
    doWhat: "Ashwini, Pushya, Hasta, Rohini, Mrigashirsha: highly auspicious for new starts. Moola, Jyeshtha, Ashlesha, Magha: avoid major beginnings. Check your birth nakshatra for personal alignment.",
  },
  Yoga: {
    what: "Yoga is calculated by adding the longitudes of the Sun and Moon, then dividing by 13°20'. There are 27 yogas, each with a specific quality.",
    why: "Yoga reflects the combined energy of the day — whether the universe supports success, obstacles, progress, or caution.",
    doWhat: "Auspicious yogas (Siddhi, Shubha, Amrita, Brahma, Indra): begin important work. Inauspicious yogas (Vishkambha, Atiganda, Shula, Ganda, Vyaghata): prefer spiritual practices, avoid big decisions.",
  },
  Karana: {
    what: "Karana is half of a tithi — it changes approximately every 6 hours. There are 11 karanas, with Vishti (Bhadra) being the most inauspicious.",
    why: "Karana determines the quality of short time windows during the day. Important for choosing the right hour for specific tasks.",
    doWhat: "Bhadra active: avoid starting journeys, new work, signing contracts, or sacred rituals. All other karanas: generally supportive. Bava, Balava, Kaulava: especially auspicious.",
  },
  "Vara (Day)": {
    what: "Vara is the weekday per the Hindu calendar, each ruled by a planet. Ravivara=Sun, Somavara=Moon, Mangalavara=Mars, Budhavara=Mercury, Guruvara=Jupiter, Shukravara=Venus, Shanivara=Saturn.",
    why: "The ruling planet of the day influences which activities are naturally supported and which deities are best worshipped.",
    doWhat: "Sunday: Surya puja, avoid oil use. Monday: Shiva puja, fasting. Tuesday: Hanuman/Durga puja. Wednesday: Ganesha/Vishnu. Thursday: Guru puja, start learning. Friday: Lakshmi puja, beauty. Saturday: Shani puja, avoid non-veg.",
  },
};

const TIMING_EXPLAIN: Record<string, { what: string; doWhat: string; avoid?: string }> = {
  "Brahma Muhurta": {
    what: "The sacred 'creator's hour' — 96 minutes before sunrise (roughly 4–5:30 AM). Brahma = the creator, Muhurta = auspicious time window.",
    doWhat: "Meditation, yoga, pranayama, studying scriptures, chanting mantras. The mind is clearest and cosmic energy is at its purest at this hour.",
    avoid: "Sleeping through it (considered a missed spiritual opportunity). Heavy food the night before disturbs this period.",
  },
  Sunrise: {
    what: "Udaya — the moment the upper limb of the Sun crosses the horizon. All 5 Panchang elements (Tithi, Nakshatra, Yoga, Karana, Vara) are determined at this moment per the Udaya Tithi rule.",
    doWhat: "Offer Surya Arghya (water to the Sun). Begin your day's important activities after sunrise — this is when the day's energy officially starts.",
  },
  "Abhijit Muhurta": {
    what: "The most powerful muhurta of the day — a 48-minute window centered exactly at solar noon. 'Abhijit' means 'victorious'. One of the 30 muhurtas in a day.",
    doWhat: "Start the most important task of your day here. Business meetings, signing agreements, beginning long journeys, important decisions. Even inauspicious days have this window.",
    avoid: "Not effective on Wednesdays (Mercury's day weakens Abhijit). Avoid for north-facing activities.",
  },
  "Rahu Kaal": {
    what: "An inauspicious 90-minute period each day, ruled by Rahu (the shadow planet / north lunar node). Each weekday has a fixed 1/8th slot of daytime assigned to Rahu.",
    doWhat: "Use this time for spiritual practice, reading, meditation, or routine work that doesn't need an auspicious start.",
    avoid: "Starting new work, travel, medical procedures, business deals, signing contracts, marriage negotiations. Rahu represents obstacles and sudden reversals.",
  },
  Yamagandam: {
    what: "Inauspicious period governed by Yama — the deity of death and dharma. Like Rahu Kaal, it's a fixed segment of each weekday.",
    doWhat: "Ancestor worship (Pitra Tarpan) is actually enhanced during this period. Routine tasks, spiritual practices.",
    avoid: "All auspicious activities: weddings, new business, travel, moving houses, medical operations.",
  },
  "Gulika Kaal": {
    what: "Period governed by Gulika (Manda) — the son of Saturn. Considered inauspicious like Rahu Kaal but slightly less severe.",
    doWhat: "Tantra practices, Shani worship, studying difficult subjects. Administrative and discipline-related work.",
    avoid: "New beginnings, auspicious ceremonies, travel starts, signing important documents.",
  },
  Sunset: {
    what: "The moment the lower limb of the Sun touches the horizon. Marks the transition from daytime to nighttime energy.",
    doWhat: "Evening prayer (Sandhya Vandana), lighting a lamp (Diya), offering incense. A spiritually sensitive time of day.",
    avoid: "Starting new work, travel, or meals right at sunset in traditional practice.",
  },
  "Pradosh Kaal": {
    what: "The twilight period — 144 minutes starting from sunset. 'Pradosh' means 'at the beginning of the night'. Especially sacred on Trayodashi (13th tithi).",
    doWhat: "Shiva worship and Abhishek. Lighting oil/ghee lamps. Evening prayers. The Pradosh Vrat is observed on this window on Shukla/Krishna Trayodashi.",
    avoid: "Conflict, arguments, heavy meals. This is a Sattvic (pure) time meant for devotion.",
  },
  "Nishita Kaal": {
    what: "The midnight period — the 8th muhurta of the night, centered at astronomical midnight. Sacred for Kali, Shiva (Ardha-narishvara), and considered the birth time of Lord Krishna (Janmashtami).",
    doWhat: "Advanced Tantric practices, Kali/Shiva worship, deep meditation. Janmashtami celebration centers on this moment.",
    avoid: "Eating, worldly activities. This is the most Tamasic time — only advanced practitioners use this for sadhana.",
  },
};

const CHOG_EXPLAIN: Record<string, { what: string; good: string; avoid: string }> = {
  Amrit: {
    what: "Amrit means 'nectar of immortality'. The most auspicious Choghadiya period — ruled by the Moon.",
    good: "All activities: new work, travel, business, marriage, medical treatment. Start anything important here.",
    avoid: "Nothing — this is universally favorable.",
  },
  Shubh: {
    what: "Shubh means 'auspicious'. Ruled by Jupiter (Guru). Second-best Choghadiya period.",
    good: "Religious ceremonies, weddings, new learning, creative work, socializing, buying valuables.",
    avoid: "Generally none. Slightly less suited for purely material transactions.",
  },
  Labh: {
    what: "Labh means 'profit or gain'. Ruled by Mercury (Budha). Strong for material success.",
    good: "Business deals, trade, financial investments, buying property, opening shops, sales.",
    avoid: "Spiritual rituals are better in other periods.",
  },
  Char: {
    what: "Char means 'moving or dynamic'. Ruled by Venus (Shukra). Good for anything involving movement.",
    good: "Travel, changing homes/offices, starting journeys, outdoor activities, dynamic work.",
    avoid: "Signing long-term static commitments.",
  },
  Rog: {
    what: "Rog means 'disease or illness'. Ruled by Mars (Mangal). Generally inauspicious.",
    good: "Medical treatment, surgery (Mars governs surgeons). Administrative/government work.",
    avoid: "New business, travel, weddings, buying property, signing contracts.",
  },
  Kaal: {
    what: "Kaal means 'death or time'. Ruled by Saturn (Shani). Most inauspicious Choghadiya.",
    good: "Iron/metal work, collecting dues, cemetery-related work per some traditions.",
    avoid: "All auspicious activities: new starts, travel, celebrations, business deals.",
  },
  Udveg: {
    what: "Udveg means 'anxiety or disturbance'. Ruled by the Sun. Inauspicious for most activities.",
    good: "Government work, legal matters, dealing with authority figures (Sun rules power).",
    avoid: "New ventures, travel, celebrations, financial deals.",
  },
};

// ── Sub-components ─────────────────────────────────────────────────────────────

function ExpandIcon({ open }: { open: boolean }) {
  return (
    <ChevronDown className={`h-3.5 w-3.5 text-muted-foreground transition-transform duration-200 ${open ? "rotate-180" : ""}`} />
  );
}

function AngaCard({
  label, hindi, icon, value, sub, quality, expanded, onToggle,
}: {
  label: string; hindi: string; icon: string; value: string; sub: string;
  quality: "benefic" | "malefic" | "neutral"; expanded: boolean; onToggle: () => void;
}) {
  const qCls = {
    benefic: "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20",
    malefic: "bg-red-500/10 text-red-400 border border-red-500/20",
    neutral: "bg-muted/20 text-muted-foreground border border-border/20",
  }[quality];

  const exp = ANGA_EXPLAIN[label];

  return (
    <Card
      className="glass border-border/30 h-fit cursor-pointer hover:border-primary/30 transition-colors"
      onClick={onToggle}
    >
      <CardContent className="pt-4 pb-4 space-y-0">
        <div className="flex items-start gap-3">
          <span className="text-2xl mt-0.5">{icon}</span>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <p className="text-xs text-muted-foreground uppercase tracking-wider">{label}</p>
              <span className="text-xs text-muted-foreground">({hindi})</span>
            </div>
            <p className="font-semibold text-sm">{value}</p>
            <p className="text-xs text-muted-foreground mt-0.5 leading-relaxed">{sub}</p>
          </div>
          <div className="flex flex-col items-end gap-1.5 shrink-0">
            <Badge className={`text-xs ${qCls}`}>{quality}</Badge>
            <ExpandIcon open={expanded} />
          </div>
        </div>

        <AnimatePresence initial={false}>
          {expanded && exp && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="overflow-hidden"
            >
              <div className="mt-3 space-y-2 border-t border-border/20 pt-3">
                <div className="bg-muted/10 rounded-lg p-2.5 text-xs text-muted-foreground leading-relaxed">
                  <span className="text-foreground/70 font-medium">What is it? </span>
                  {exp.what}
                </div>
                <div className="bg-primary/5 border border-primary/10 rounded-lg p-2.5 text-xs text-muted-foreground leading-relaxed">
                  <span className="text-primary/70 font-medium">Why does it matter? </span>
                  {exp.why}
                </div>
                <div className="bg-emerald-500/5 border border-emerald-500/10 rounded-lg p-2.5 text-xs text-muted-foreground leading-relaxed">
                  <span className="text-emerald-400/80 font-medium">What to do? </span>
                  {exp.doWhat}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </CardContent>
    </Card>
  );
}

function TimingRow({
  icon, label, value, type, expanded, onToggle,
}: {
  icon: string; label: string; value: string;
  type: "benefic" | "malefic" | "neutral" | "special";
  expanded: boolean; onToggle: () => void;
}) {
  const bg   = type === "benefic" ? "bg-emerald-500/5 border-emerald-500/20"
             : type === "malefic" ? "bg-red-500/5 border-red-500/20"
             : type === "special" ? "bg-primary/5 border-primary/20"
             :                      "bg-muted/10 border-border/20";
  const clr  = type === "benefic" ? "text-emerald-400"
             : type === "malefic" ? "text-red-400"
             : type === "special" ? "text-primary"
             :                      "text-muted-foreground";

  const key  = Object.keys(TIMING_EXPLAIN).find(k => label.startsWith(k));
  const exp  = key ? TIMING_EXPLAIN[key] : null;

  return (
    <div
      className={`rounded-xl border ${bg} cursor-pointer hover:opacity-90 transition-opacity`}
      onClick={onToggle}
    >
      <div className="flex items-center justify-between p-3.5">
        <div className="flex items-center gap-2">
          <span>{icon}</span>
          <span className="text-sm">{label}</span>
        </div>
        <div className="flex items-center gap-2">
          <span className={`font-mono text-sm font-medium ${clr}`}>{value}</span>
          {exp && <ExpandIcon open={expanded} />}
        </div>
      </div>

      <AnimatePresence initial={false}>
        {expanded && exp && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="px-3.5 pb-3.5 space-y-1.5 border-t border-white/5 pt-2.5">
              <p className="text-xs text-muted-foreground leading-relaxed">
                <span className="text-foreground/60 font-medium">What: </span>{exp.what}
              </p>
              <p className="text-xs text-emerald-400/80 leading-relaxed">
                <span className="font-medium">✓ Do: </span>{exp.doWhat}
              </p>
              {exp.avoid && (
                <p className="text-xs text-red-400/80 leading-relaxed">
                  <span className="font-medium">✕ Avoid: </span>{exp.avoid}
                </p>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function ChogCard({ p, expanded, onToggle }: {
  p: { name: string; hindi: string; quality: string; auspicious: boolean; start: string; end: string };
  expanded: boolean; onToggle: () => void;
}) {
  const CHOG_QUALITY: Record<string, string> = {
    Amrit: "bg-emerald-500/15 border-emerald-500/30 text-emerald-300",
    Shubh: "bg-teal-500/15 border-teal-500/30 text-teal-300",
    Labh:  "bg-sky-500/15 border-sky-500/30 text-sky-300",
    Char:  "bg-blue-500/10 border-blue-500/20 text-blue-300",
    Rog:   "bg-red-500/10 border-red-500/20 text-red-400",
    Kaal:  "bg-rose-500/10 border-rose-500/20 text-rose-400",
    Udveg: "bg-orange-500/10 border-orange-500/20 text-orange-400",
  };
  const cls = CHOG_QUALITY[p.name] ?? "bg-muted/10 border-border/20 text-muted-foreground";
  const exp = CHOG_EXPLAIN[p.name];

  return (
    <div className={`rounded-xl border ${cls} cursor-pointer hover:opacity-90 transition-opacity`} onClick={onToggle}>
      <div className="p-3">
        <div className="flex items-center justify-between mb-1">
          <span className="font-semibold text-sm">{p.name}</span>
          <div className="flex items-center gap-1.5">
            {p.auspicious
              ? <span className="text-xs text-emerald-400">✓ Good</span>
              : <span className="text-xs text-red-400">✕ Avoid</span>}
            {exp && <ExpandIcon open={expanded} />}
          </div>
        </div>
        <p className="text-xs opacity-70">{p.hindi}</p>
        <p className="font-mono text-xs mt-1.5 opacity-80">{p.start} – {p.end}</p>
      </div>

      <AnimatePresence initial={false}>
        {expanded && exp && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <div className="px-3 pb-3 space-y-1.5 border-t border-white/10 pt-2">
              <p className="text-xs opacity-80 leading-relaxed">{exp.what}</p>
              <p className="text-xs text-emerald-400/80 leading-relaxed"><span className="font-medium">✓ </span>{exp.good}</p>
              <p className="text-xs text-red-400/80 leading-relaxed"><span className="font-medium">✕ </span>{exp.avoid}</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function PanchangPage() {
  const { t } = useTranslation();
  const [cityInput, setCityInput] = useState("New Delhi");
  const [suggestions, setSuggestions] = useState<City[]>([]);
  const [selectedCity, setSelectedCity] = useState<City>({ name: "New Delhi", lat: 28.6139, lon: 77.209, tz: 5.5 });
  const [now, setNow] = useState(new Date());
  const [chogTab, setChogTab] = useState<"day" | "night">("day");
  const [expandedKey, setExpandedKey] = useState<string | null>(null);
  const [notifPerm, setNotifPerm] = useState<NotificationPermission | "unsupported">(
    "Notification" in window ? Notification.permission : "unsupported"
  );

  const toggle = (k: string) => setExpandedKey(v => v === k ? null : k);

  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(t);
  }, []);
  useEffect(() => { getCities().catch(() => {}); }, []); // preload cache

  const localDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;

  const { data, isLoading, isError, refetch, isFetching } = usePanchang({
    date: localDate, lat: selectedCity.lat, lon: selectedCity.lon, tz: selectedCity.tz,
  });

  // Schedule Rahu Kaal browser notifications
  useRahuKaalNotification(data?.rahukaal?.start, data?.rahukaal?.end);

  useEffect(() => {
    const t = setInterval(() => refetch(), 5 * 60 * 1000);
    return () => clearInterval(t);
  }, [refetch]);

  const handleCityInput = async (v: string) => {
    setCityInput(v);
    const results = await searchCities(v);
    setSuggestions(results);
  };

  const todayLabel = now.toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "long", year: "numeric" });
  const timeLabel  = now.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit", second: "2-digit" });

  const paksha      = data?.tithi.paksha === "Shukla" ? "🌕 Shukla Paksha" : "🌑 Krishna Paksha";
  const tithiType   = data?.tithi.tithi_type;
  const tithiBadge  = tithiType === "vridhi" ? { label: "Vridhi — tithi spans 2 sunrises", cls: "bg-amber-500/10 text-amber-400 border-amber-500/30" }
                    : tithiType === "ksheya" ? { label: "Ksheya — tithi skipped (< 1 sunrise)", cls: "bg-rose-500/10 text-rose-400 border-rose-500/30" }
                    : null;

  const fmtSlot = (s: { start: string; end: string } | null | undefined) => s ? `${s.start} – ${s.end}` : "—";

  const timings = data ? [
    { icon: "🌄", label: "Brahma Muhurta",  value: fmtSlot(data.brahma_muhurta), type: "benefic"  as const },
    { icon: "🌅", label: "Sunrise",          value: data.sunrise,                 type: "benefic"  as const },
    { icon: "✨", label: "Abhijit Muhurta",  value: fmtSlot(data.abhijit_muhurta),type: "special"  as const },
    { icon: "⚠️", label: "Rahu Kaal",        value: fmtSlot(data.rahukaal),       type: "malefic"  as const },
    { icon: "💀", label: "Yamagandam",       value: fmtSlot(data.yamagandam),     type: "malefic"  as const },
    { icon: "🌀", label: "Gulika Kaal",      value: fmtSlot(data.gulika_kaal),    type: "malefic"  as const },
    { icon: "🌇", label: "Sunset",           value: data.sunset,                  type: "neutral"  as const },
    { icon: "🌆", label: "Pradosh Kaal",     value: fmtSlot(data.pradosh_kaal),   type: "special"  as const },
    ...(data.nishita_kaal ? [{
      icon: "🌑",
      label: `Nishita Kaal${data.nishita_kaal.midpoint ? ` (mid: ${data.nishita_kaal.midpoint})` : ""}`,
      value: `${data.nishita_kaal.start} – ${data.nishita_kaal.end}`,
      type: "special" as const,
    }] : []),
  ] : [];

  return (
    <Tabs defaultValue="panchang" className="w-full">
      <TabsList className="glass flex-wrap h-auto gap-1 p-1 mb-4">
        <TabsTrigger value="panchang" className="text-xs sm:text-sm">📅 {t('panchang.tab_panchang')}</TabsTrigger>
        <TabsTrigger value="muhurta" className="text-xs sm:text-sm">⭐ {t('panchang.tab_muhurta')}</TabsTrigger>
      </TabsList>

      <TabsContent value="panchang">
    <div className="space-y-4 pb-2">
      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className="flex items-start justify-between gap-3 flex-wrap">
        <div>
          <h1 className="font-display text-2xl sm:text-3xl text-primary text-glow-gold">📅 {t('panchang.title')}</h1>
          <p className="text-muted-foreground mt-0.5 text-sm">{todayLabel}</p>
          <p className="text-xs text-primary/60 font-mono">{timeLabel} · {selectedCity.name}</p>
        </div>
        <div className="flex items-center gap-3 mt-1">
          {data?.panchaka && (
            <Badge className="bg-rose-500/10 text-rose-400 border border-rose-500/30 text-xs">⚠ Panchaka Active</Badge>
          )}
          <button onClick={() => refetch()} disabled={isFetching}
            className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-primary transition-colors">
            <RefreshCw className={`h-3.5 w-3.5 ${isFetching ? "animate-spin" : ""}`} />
            {isFetching ? t('common.loading') : t('common.refresh')}
          </button>
        </div>
      </motion.div>

      {/* City selector */}
      <div className="relative max-w-xs">
        <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
        <Input value={cityInput} onChange={e => handleCityInput(e.target.value)}
          placeholder={t('onboarding.place_placeholder')} className="pl-8 bg-muted/20 border-border/30 text-sm h-9" autoComplete="off" />
        {suggestions.length > 0 && (
          <div className="absolute z-50 w-full mt-1 cosmic-card rounded-xl border border-border/40 overflow-hidden shadow-lg">
            {suggestions.map(city => (
              <button key={city.name} type="button"
                onClick={() => { setSelectedCity(city); setCityInput(city.name); setSuggestions([]); }}
                className="w-full text-left px-3 py-2 text-sm hover:bg-primary/10 hover:text-primary transition-colors border-b border-border/20 last:border-0">
                {city.name}
              </button>
            ))}
          </div>
        )}
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-6 w-6 animate-spin text-primary mr-2" />
          <span className="text-sm text-muted-foreground">{t('panchang.loading')}</span>
        </div>
      )}
      {isError && (
        <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 text-center text-sm text-muted-foreground">
          {t('panchang.error')}
        </div>
      )}

      {data && (
        <>
          {/* Paksha banner */}
          <div className="text-center py-2 text-sm text-primary/70 flex items-center justify-center gap-2 flex-wrap">
            <span>{paksha} · {data.tithi.name}</span>
            {tithiBadge && (
              <Badge className={`text-xs border ${tithiBadge.cls}`} title={tithiBadge.label}>
                {tithiType === "vridhi" ? "Vridhi" : "Ksheya"}
              </Badge>
            )}
            {tithiBadge && (
              <span className="text-xs text-muted-foreground/60">{tithiBadge.label}</span>
            )}
          </div>

          {/* Hint */}
          <p className="text-xs text-muted-foreground/50 text-center">
            Tap any card or row to understand what it means and what to do
          </p>

          {/* ── Panch Anga ── */}
          <div>
            <h2 className="font-display text-xs text-muted-foreground uppercase tracking-wider mb-3">Panch Anga — Five Elements of the Day</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 sm:gap-3">
              {[
                { label: "Tithi",       hindi: "तिथि",  icon: "🌙",
                  value: `${data.tithi.paksha} ${data.tithi.name}`,
                  sub: `Ends: ${data.tithi.end_time}${tithiType && tithiType !== "normal" ? ` · ${tithiType}` : ""}`,
                  quality: "neutral"  as const },
                { label: "Nakshatra",   hindi: "नक्षत्र", icon: "⭐",
                  value: data.nakshatra.name,
                  sub: `${data.nakshatra.hindi} · Pada ${data.nakshatra.pada} · Lord: ${data.nakshatra.lord}${data.nakshatra.end_time ? ` · Ends: ${data.nakshatra.end_time}` : ""}`,
                  quality: "benefic"  as const },
                { label: "Yoga",        hindi: "योग",  icon: "🔮",
                  value: data.yoga.name,
                  sub: `${data.yoga.hindi}${data.yoga.end_time ? ` · Ends: ${data.yoga.end_time}` : ""}`,
                  quality: data.yoga.is_auspicious ? "benefic" as const : "malefic" as const },
                { label: "Karana",      hindi: "करण",  icon: "📐",
                  value: data.karana.name,
                  sub: `${data.karana.hindi}${data.karana.is_bhadra ? " · ⚠ Bhadra — avoid new starts" : ""}`,
                  quality: data.karana.is_bhadra ? "malefic" as const : "neutral" as const },
                { label: "Vara (Day)",  hindi: "वार",  icon: "📅",
                  value: data.vara.name,
                  sub: `${data.vara.hindi} · Lord: ${data.vara.lord}`,
                  quality: "neutral"  as const },
              ].map(item => (
                <motion.div key={item.label} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
                  <AngaCard {...item} expanded={expandedKey === item.label} onToggle={() => toggle(item.label)} />
                </motion.div>
              ))}
            </div>
          </div>

          {/* ── Daily Timings ── */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h2 className="font-display text-xs text-muted-foreground uppercase tracking-wider">Daily Timings · {selectedCity.name}</h2>
              {"Notification" in window && notifPerm !== "granted" && (
                <button
                  onClick={async () => {
                    const p = await Notification.requestPermission();
                    setNotifPerm(p);
                  }}
                  className="text-[10px] px-2 py-1 rounded border border-primary/30 text-primary hover:bg-primary/10 transition flex items-center gap-1">
                  🔔 {t('panchang.enable_notifications')}
                </button>
              )}
              {notifPerm === "granted" && (
                <span className="text-[10px] text-emerald-400 flex items-center gap-1">🔔 {t('panchang.notifications_on')}</span>
              )}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {timings.map(t => (
                <TimingRow key={t.label} {...t}
                  expanded={expandedKey === `t_${t.label}`}
                  onToggle={() => toggle(`t_${t.label}`)} />
              ))}
            </div>
          </div>

          {/* ── Choghadiya ── */}
          {data.choghadiya && (
            <div>
              <div className="flex items-center justify-between mb-3">
                <h2 className="font-display text-xs text-muted-foreground uppercase tracking-wider">
                  Choghadiya — 8 Time Periods
                </h2>
                <div className="flex rounded-lg overflow-hidden border border-border/30 text-xs">
                  <button onClick={() => setChogTab("day")}
                    className={`flex items-center gap-1 px-3 py-1 transition-colors ${chogTab === "day" ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground"}`}>
                    <Sun className="h-3 w-3" /> {t('panchang.day_choghadiya')}
                  </button>
                  <button onClick={() => setChogTab("night")}
                    className={`flex items-center gap-1 px-3 py-1 transition-colors ${chogTab === "night" ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground"}`}>
                    <Moon className="h-3 w-3" /> {t('panchang.night_choghadiya')}
                  </button>
                </div>
              </div>
              <p className="text-xs text-muted-foreground/50 mb-3">Tap a period to see what activities are good or bad in that window</p>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 sm:gap-3">
                {(chogTab === "day" ? data.choghadiya.day : data.choghadiya.night).map((p, i) => (
                  <motion.div key={i} initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: i * 0.04 }}>
                    <ChogCard p={p}
                      expanded={expandedKey === `chog_${chogTab}_${i}`}
                      onToggle={() => toggle(`chog_${chogTab}_${i}`)} />
                  </motion.div>
                ))}
              </div>
            </div>
          )}

          {/* ── Summary guidance ── */}
          <div className="rounded-xl border border-border/30 bg-muted/10 p-4 space-y-2.5">
            <p className="text-sm font-medium">✨ Today's Summary</p>
            <div className={`rounded-lg p-3 text-xs leading-relaxed ${data.yoga.is_auspicious ? "bg-emerald-500/10 border border-emerald-500/20" : "bg-amber-500/10 border border-amber-500/20"}`}>
              <span className="font-medium">{data.yoga.name} Yoga</span>
              {data.yoga.is_auspicious ? " — Auspicious energy today. Good for important work and new beginnings." : " — Inauspicious yoga. Prefer spiritual practices over major decisions today."}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
              <div className="bg-emerald-500/5 border border-emerald-500/20 rounded-lg p-2.5 text-muted-foreground">
                <span className="text-emerald-400 font-medium">Best window: </span>
                Abhijit {fmtSlot(data.abhijit_muhurta)} — use for your most important task today.
              </div>
              <div className="bg-red-500/5 border border-red-500/20 rounded-lg p-2.5 text-muted-foreground">
                <span className="text-red-400 font-medium">Avoid starting new work: </span>
                Rahu Kaal {fmtSlot(data.rahukaal)}{data.yamagandam ? `, Yamagandam ${fmtSlot(data.yamagandam)}` : ""}.
              </div>
              {data.brahma_muhurta && (
                <div className="bg-primary/5 border border-primary/20 rounded-lg p-2.5 text-muted-foreground">
                  <span className="text-primary font-medium">Brahma Muhurta: </span>
                  {fmtSlot(data.brahma_muhurta)} — ideal for meditation and spiritual practice.
                </div>
              )}
              {data.panchaka && (
                <div className="bg-rose-500/5 border border-rose-500/20 rounded-lg p-2.5 text-muted-foreground">
                  <span className="text-rose-400 font-medium">⚠ Panchaka active — </span>
                  Avoid rooftop construction, cremation rites, and storing firewood today. Moon is in the last 5 nakshatras (Dhanishtha–Revati).
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
      </TabsContent>

      <TabsContent value="muhurta">
        <MuhurtaPage />
      </TabsContent>
      <PageBot pageContext="panchang" pageData={data ?? {}} />
    </Tabs>
  );
}
