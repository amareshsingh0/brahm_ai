/**
 * PrashnaPage — Horary (Prashna) Kundali
 * Casts a chart for the exact moment the question is asked.
 */
import { useState } from "react";
import { motion } from "framer-motion";
import { HelpCircle, Loader2, MapPin, RefreshCw } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import { searchCities, type City } from "@/lib/cities";
import { useKundliStore } from "@/store/kundliStore";
import { KundliChart } from "@/components/charts/KundliChart";
import PageBot from "@/components/PageBot";
import type { KundaliResponse } from "@/types/api";

const QUESTION_TYPES = [
  { value: "general",      label: "General" },
  { value: "wealth",       label: "Wealth & Finance" },
  { value: "career",       label: "Career & Work" },
  { value: "relationship", label: "Relationship & Marriage" },
  { value: "health",       label: "Health" },
  { value: "property",     label: "Property & Home" },
  { value: "travel",       label: "Travel & Foreign" },
  { value: "education",    label: "Education" },
  { value: "children",     label: "Children" },
  { value: "spiritual",    label: "Spiritual & Dharma" },
];

interface PrashnaResult extends KundaliResponse {
  prashna_question: string;
  prashna_type: string;
  prashna_verdict: "YES" | "NO" | "MIXED";
  prashna_factors: string[];
  hora_lord: string;
  prashna_datetime: string;
}

const GRAHA_SYMBOL: Record<string, string> = {
  Surya:"☉︎",Chandra:"☽︎",Mangal:"♂︎",Budh:"☿︎",Guru:"♃︎",
  Shukra:"♀︎",Shani:"♄︎",Rahu:"☊︎",Ketu:"☋︎",
};
const GRAHA_EN: Record<string, string> = {
  Surya:"Sun",Chandra:"Moon",Mangal:"Mars",Budh:"Mercury",
  Guru:"Jupiter",Shukra:"Venus",Shani:"Saturn",Rahu:"Rahu",Ketu:"Ketu",
};

function verdictStyle(v: string) {
  if (v === "YES")   return "text-emerald-400 bg-emerald-500/15 border-emerald-500/30";
  if (v === "NO")    return "text-red-400 bg-red-500/15 border-red-500/30";
  return "text-amber-400 bg-amber-500/15 border-amber-500/30";
}

export default function PrashnaPage() {
  const birthDetails = useKundliStore(s => s.birthDetails);

  const [question,     setQuestion]     = useState("");
  const [qType,        setQType]        = useState("general");
  const [lat,          setLat]          = useState(birthDetails?.lat?.toString() ?? "");
  const [lon,          setLon]          = useState(birthDetails?.lon?.toString() ?? "");
  const [tz,           setTz]           = useState(birthDetails?.tz?.toString() ?? "5.5");
  const [cityQuery,    setCityQuery]    = useState(birthDetails?.place ?? "");
  const [suggestions,  setSuggestions]  = useState<City[]>([]);
  const [loading,      setLoading]      = useState(false);
  const [result,       setResult]       = useState<PrashnaResult | null>(null);
  const [error,        setError]        = useState<string | null>(null);

  const handleCitySearch = async (q: string) => {
    setCityQuery(q);
    if (q.length >= 2) {
      const hits = await searchCities(q);
      setSuggestions(hits.slice(0, 6));
    } else {
      setSuggestions([]);
    }
  };

  const selectCity = (c: City) => {
    setCityQuery(c.name);
    setLat(c.lat.toString());
    setLon(c.lon.toString());
    setTz(c.tz.toString());
    setSuggestions([]);
  };

  const askNow = async () => {
    if (!lat || !lon) { setError("Please select a location."); return; }
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const data = await api.post<PrashnaResult>("/api/prashna", {
        lat: parseFloat(lat), lon: parseFloat(lon), tz: parseFloat(tz),
        question, question_type: qType,
      });
      setResult(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to cast Prashna chart.");
    } finally {
      setLoading(false);
    }
  };

  const planets = result
    ? Object.entries(result.grahas).map(([name, g]) => ({
        name,
        house: g.house,
        retro: g.retro,
        degree: g.degree,
      }))
    : [];

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-4xl mx-auto">

      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center gap-3">
          <HelpCircle className="w-5 h-5 text-star-gold" />
          <div>
            <h1 className="font-display text-2xl text-foreground text-glow-gold">Prashna Kundali</h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              Horary astrology — chart cast at the moment of your question
            </p>
          </div>
        </div>
      </motion.div>

      {/* Input Form */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1, transition: { delay: 0.1 } }}
        className="cosmic-card rounded-xl p-5 space-y-4">

        {/* Location */}
        <div className="relative">
          <Label className="text-xs text-muted-foreground mb-1 block">Your Current Location</Label>
          <div className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
              <Input
                className="pl-9 h-9 text-xs"
                placeholder="City name…"
                value={cityQuery}
                onChange={e => handleCitySearch(e.target.value)}
              />
              {suggestions.length > 0 && (
                <div className="absolute z-50 top-full mt-1 left-0 right-0 bg-background border border-border rounded-lg shadow-xl overflow-hidden">
                  {suggestions.map(c => (
                    <button key={`${c.lat}${c.lon}`} className="w-full text-left px-3 py-2 text-xs hover:bg-muted/40 transition-colors"
                      onClick={() => selectCity(c)}>
                      {c.name}{c.state ? `, ${c.state}` : ""}{c.country !== "IN" ? ` — ${c.country}` : ""}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <Input className="w-20 h-9 text-xs" placeholder="Lat" value={lat} onChange={e => setLat(e.target.value)} />
            <Input className="w-20 h-9 text-xs" placeholder="Lon" value={lon} onChange={e => setLon(e.target.value)} />
            <Input className="w-16 h-9 text-xs" placeholder="TZ" value={tz} onChange={e => setTz(e.target.value)} />
          </div>
        </div>

        {/* Question type */}
        <div>
          <Label className="text-xs text-muted-foreground mb-1 block">Question Domain</Label>
          <div className="flex flex-wrap gap-1.5">
            {QUESTION_TYPES.map(qt => (
              <button key={qt.value}
                onClick={() => setQType(qt.value)}
                className={`text-[11px] px-2.5 py-1 rounded-full border transition-colors ${
                  qType === qt.value
                    ? "bg-primary/20 border-primary/40 text-primary"
                    : "border-border/30 text-muted-foreground hover:text-foreground"
                }`}
              >
                {qt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Question text */}
        <div>
          <Label className="text-xs text-muted-foreground mb-1 block">Your Question (optional)</Label>
          <Input
            className="h-9 text-xs"
            placeholder="What would you like to know?"
            value={question}
            onChange={e => setQuestion(e.target.value)}
          />
        </div>

        <button
          onClick={askNow}
          disabled={loading || !lat || !lon}
          className="w-full h-9 rounded-lg bg-primary/80 hover:bg-primary text-primary-foreground text-sm font-medium transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
        >
          {loading ? <><Loader2 className="w-4 h-4 animate-spin" /> Casting Prashna Chart…</> : <><RefreshCw className="w-4 h-4" /> Ask Now — Cast Chart for This Moment</>}
        </button>

        {error && <p className="text-xs text-destructive">{error}</p>}
      </motion.div>

      {/* Results */}
      {result && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-5">

          {/* Verdict banner */}
          <div className={`rounded-xl p-5 border text-center ${verdictStyle(result.prashna_verdict)}`}>
            <p className="text-[11px] uppercase tracking-widest mb-1 opacity-70">Prashna Verdict</p>
            <p className="text-4xl font-bold">{result.prashna_verdict}</p>
            <p className="text-xs mt-2 opacity-80">{result.prashna_datetime} · Hora Lord: <strong>{GRAHA_EN[result.hora_lord] ?? result.hora_lord}</strong> {GRAHA_SYMBOL[result.hora_lord]}</p>
            {result.prashna_question && (
              <p className="text-xs mt-1 opacity-70 italic">"{result.prashna_question}"</p>
            )}
          </div>

          {/* Factors */}
          {result.prashna_factors.length > 0 && (
            <div className="cosmic-card rounded-xl p-4 space-y-2">
              <p className="text-xs font-semibold text-foreground mb-2">Chart Indicators</p>
              {result.prashna_factors.map((f, i) => (
                <p key={i} className="text-xs text-muted-foreground leading-relaxed pl-3 border-l-2 border-primary/30">{f}</p>
              ))}
            </div>
          )}

          {/* Chart */}
          <div className="cosmic-card rounded-xl p-4">
            <p className="text-xs font-semibold text-foreground mb-3">Prashna Chart — {result.prashna_datetime}</p>
            <KundliChart
              planets={planets}
              lagnaRashi={result.lagna.rashi}
              chartStyle="north"
            />
          </div>

          {/* Chart summary */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: "Lagna", val: `${result.lagna.rashi} ${result.lagna.degree}°` },
              { label: "Moon", val: `${result.grahas.Chandra?.rashi ?? "—"} H${result.grahas.Chandra?.house ?? "—"}` },
              { label: "Hora Lord", val: `${GRAHA_EN[result.hora_lord] ?? result.hora_lord} ${GRAHA_SYMBOL[result.hora_lord] ?? ""}` },
              { label: "Question", val: QUESTION_TYPES.find(q => q.value === result.prashna_type)?.label ?? result.prashna_type },
            ].map(item => (
              <div key={item.label} className="cosmic-card rounded-lg p-3 text-center">
                <p className="text-[10px] text-muted-foreground uppercase tracking-wide">{item.label}</p>
                <p className="text-sm font-semibold text-star-gold mt-1">{item.val}</p>
              </div>
            ))}
          </div>
        </motion.div>
      )}

      <PageBot
        pageContext="prashna"
        pageData={result ? { verdict: result.prashna_verdict, factors: result.prashna_factors, hora_lord: result.hora_lord } : {}}
        suggestions={[
          "How do I interpret the Prashna verdict?",
          "What does the hora lord indicate?",
          "Which house should I look at for relationship queries?",
          "What remedies improve a NO verdict?",
        ]}
      />
    </div>
  );
}
