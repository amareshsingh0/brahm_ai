/**
 * MarriagePage — Vivah (Marriage) Analysis
 * Classical Jyotish: Vimshottari Dasha scoring + 7th house + Navamsa
 */
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Heart, Loader2, MapPin, CalendarDays, Clock, Star,
  AlertTriangle, CheckCircle2, XCircle, ChevronDown, ChevronUp, RefreshCw,
} from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import { searchCities, type City } from "@/lib/cities";
import { useKundliStore } from "@/store/kundliStore";
import PageBot from "@/components/PageBot";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";

// ── Types ──────────────────────────────────────────────────────────────────────
interface TimingWindow {
  period: string;
  start: string;
  end: string;
  score: number;
  probability: "Very High" | "High" | "Moderate" | "Low";
}
interface DelayFactor {
  factor: string;
  effect: string;
  severity: "High" | "Moderate" | "Low";
}
interface MarriageYoga {
  name: string;
  present: boolean;
  description?: string;
}
interface SpouseProfile {
  "7th_house_sign": string;
  "7th_house_sign_en": string;
  "7th_lord": string;
  "7th_lord_house": number | string;
  "7th_lord_sign": string;
  planets_in_7th: string[];
  traits: string[];
  profession_hint: string;
  planet_notes: string[];
  direction: string;
  karaka_strength: string;
  navamsa_karaka: string;
}
interface MarriageTiming {
  current_period: TimingWindow | null;
  favorable_windows: TimingWindow[];
  next_strong_window: string;
  estimated_age_range: string;
  overall_probability: string;
  overall_probability_pct: number;
}
interface MarriageResult {
  status: string;
  marriage_timing: MarriageTiming;
  delay_factors: DelayFactor[];
  marriage_yogas: MarriageYoga[];
  spouse_profile: SpouseProfile;
  chart_summary: Record<string, string>;
  ai_analysis: string;
}

// ── Helpers ────────────────────────────────────────────────────────────────────
const PROB_CONFIG: Record<string, { color: string; bg: string; ring: string }> = {
  "Very High": { color: "text-emerald-700", bg: "bg-emerald-50 border-emerald-200", ring: "#10b981" },
  "High":      { color: "text-green-700",   bg: "bg-green-50 border-green-200",     ring: "#16a34a" },
  "Moderate":  { color: "text-amber-700",   bg: "bg-amber-50 border-amber-200",     ring: "#b45309" },
  "Low":       { color: "text-red-700",     bg: "bg-red-50 border-red-200",         ring: "#dc2626" },
};
const SEV_CONFIG: Record<string, string> = {
  High:     "bg-red-50 border-red-200 text-red-700",
  Moderate: "bg-amber-50 border-amber-200 text-amber-700",
  Low:      "bg-slate-50 border-slate-200 text-slate-600",
};

function ProbRing({ pct, probability }: { pct: number; probability: string }) {
  const cfg = PROB_CONFIG[probability] ?? PROB_CONFIG["Moderate"];
  const r = 52;
  const circ = 2 * Math.PI * r;
  const dash = (pct / 100) * circ;
  return (
    <div className="relative flex items-center justify-center" style={{ width: 140, height: 140 }}>
      <svg width={140} height={140} className="-rotate-90">
        <circle cx={70} cy={70} r={r} fill="none" stroke="currentColor" className="text-muted/20" strokeWidth={10} />
        <circle
          cx={70} cy={70} r={r} fill="none"
          stroke={cfg.ring} strokeWidth={10} strokeLinecap="round"
          strokeDasharray={`${dash} ${circ - dash}`}
          style={{ transition: "stroke-dasharray 1s ease" }}
        />
      </svg>
      <div className="absolute flex flex-col items-center">
        <span className={`text-2xl font-bold ${cfg.color}`}>{pct}%</span>
        <span className="text-xs text-muted-foreground">score</span>
      </div>
    </div>
  );
}

// ── Main component ─────────────────────────────────────────────────────────────
export default function MarriagePage() {
  const birthDetails = useKundliStore(s => s.birthDetails);
  const [result,      setResult]      = useState<MarriageResult | null>(null);
  const [loading,     setLoading]     = useState(false);
  const [error,       setError]       = useState<string | null>(null);
  const [showAi,      setShowAi]      = useState(false);

  // Birth form state
  const [name,   setName]   = useState(birthDetails?.name   ?? "");
  const [bDate,  setBDate]  = useState(birthDetails?.dob    ?? "");
  const [bTime,  setBTime]  = useState(birthDetails?.time   ?? "");
  const [lat,    setLat]    = useState(birthDetails?.lat?.toString() ?? "");
  const [lon,    setLon]    = useState(birthDetails?.lon?.toString() ?? "");
  const [tz,     setTz]     = useState(birthDetails?.tz?.toString()  ?? "5.5");
  const [city,   setCity]   = useState(birthDetails?.place  ?? "");
  const [gender, setGender] = useState("male");
  const [suggestions, setSuggestions] = useState<City[]>([]);

  useRegisterPageBot('marriage', result ? { result } : {});

  const handleCitySearch = async (q: string) => {
    setCity(q);
    if (q.length >= 2) {
      const hits = await searchCities(q);
      setSuggestions(hits.slice(0, 6));
    } else {
      setSuggestions([]);
    }
  };
  const selectCity = (c: City) => {
    setCity(c.name); setLat(c.lat.toString()); setLon(c.lon.toString()); setTz(c.tz.toString());
    setSuggestions([]);
  };

  const analyze = async () => {
    if (!bDate || !bTime) { setError("Please enter date and time of birth"); return; }
    if (!lat || !lon) { setError("Please select a city to get coordinates"); return; }
    setLoading(true); setError(null);
    try {
      const res = await api.post<MarriageResult>("/api/marriage", {
        name, date: bDate, time: bTime, place: city,
        lat: parseFloat(lat), lon: parseFloat(lon), tz: parseFloat(tz),
        gender,
      });
      setResult(res);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Analysis failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto p-4 sm:p-6 space-y-6">
      <PageBot storageKey="marriage" pageData={result ? JSON.stringify(result) : ""} />

      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="p-2 rounded-xl bg-pink-50 border border-pink-200">
          <Heart className="w-5 h-5 text-pink-600" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-foreground">Marriage Analysis</h1>
          <p className="text-sm text-muted-foreground">Vivah Jyotish — Dasha + 7th House + Navamsa</p>
        </div>
        {result && (
          <button
            onClick={() => { setResult(null); setError(null); }}
            className="ml-auto flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
          >
            <RefreshCw className="w-3.5 h-3.5" /> Recalculate
          </button>
        )}
      </div>

      {/* Input form */}
      {!result && (
        <div className="rounded-2xl border border-border bg-card p-5 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Name */}
            <div>
              <Label className="text-xs text-muted-foreground mb-1">Name (optional)</Label>
              <Input value={name} onChange={e => setName(e.target.value)} placeholder="Your name" className="bg-background" />
            </div>
            {/* Gender */}
            <div>
              <Label className="text-xs text-muted-foreground mb-1">Gender</Label>
              <div className="flex gap-2 mt-1">
                {["male","female","other"].map(g => (
                  <button
                    key={g}
                    onClick={() => setGender(g)}
                    className={`px-3 py-1.5 rounded-lg text-sm border transition-colors ${
                      gender === g ? "bg-amber-600 text-white border-amber-600" : "border-border text-muted-foreground hover:border-amber-400"
                    }`}
                  >
                    {g.charAt(0).toUpperCase() + g.slice(1)}
                  </button>
                ))}
              </div>
            </div>
            {/* Date */}
            <div>
              <Label className="text-xs text-muted-foreground mb-1"><CalendarDays className="w-3 h-3 inline mr-1" />Date of Birth</Label>
              <Input type="date" value={bDate} onChange={e => setBDate(e.target.value)} className="bg-background" />
            </div>
            {/* Time */}
            <div>
              <Label className="text-xs text-muted-foreground mb-1"><Clock className="w-3 h-3 inline mr-1" />Time of Birth</Label>
              <Input type="time" value={bTime} onChange={e => setBTime(e.target.value)} className="bg-background" />
            </div>
            {/* City */}
            <div className="sm:col-span-2 relative">
              <Label className="text-xs text-muted-foreground mb-1"><MapPin className="w-3 h-3 inline mr-1" />Birth Place</Label>
              <Input value={city} onChange={e => handleCitySearch(e.target.value)} placeholder="Search city..." className="bg-background" />
              {suggestions.length > 0 && (
                <div className="absolute z-10 w-full mt-1 bg-card border border-border rounded-xl shadow-lg overflow-hidden">
                  {suggestions.map((c, i) => (
                    <button key={i} onClick={() => selectCity(c)} className="w-full text-left px-3 py-2 text-sm hover:bg-muted transition-colors">
                      {c.label || c.name}{c.country ? `, ${c.country}` : ""}
                    </button>
                  ))}
                </div>
              )}
              {lat && <p className="text-xs text-muted-foreground mt-1">{lat}, {lon} (tz {tz})</p>}
            </div>
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <button
            onClick={analyze} disabled={loading}
            className="w-full py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-medium flex items-center justify-center gap-2 transition-colors disabled:opacity-60"
          >
            {loading ? <><Loader2 className="w-4 h-4 animate-spin" />Analyzing...</> : <><Heart className="w-4 h-4" />Analyze Marriage</>}
          </button>
        </div>
      )}

      {/* Results */}
      <AnimatePresence>
        {result && (
          <motion.div key="results" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-5">

            {/* 1. Probability card */}
            <div className="rounded-2xl border border-border bg-card p-5">
              <div className="flex flex-col sm:flex-row items-center gap-5">
                <ProbRing
                  pct={result.marriage_timing.overall_probability_pct}
                  probability={result.marriage_timing.overall_probability}
                />
                <div className="flex-1 space-y-3 w-full">
                  <div>
                    <span className={`inline-block px-3 py-1 rounded-full text-sm font-semibold border ${
                      (PROB_CONFIG[result.marriage_timing.overall_probability] ?? PROB_CONFIG["Moderate"]).bg
                    } ${(PROB_CONFIG[result.marriage_timing.overall_probability] ?? PROB_CONFIG["Moderate"]).color}`}>
                      {result.marriage_timing.overall_probability} Probability
                    </span>
                  </div>
                  {result.marriage_timing.current_period && (
                    <div className="text-sm space-y-0.5">
                      <p className="text-muted-foreground text-xs">Current Period</p>
                      <p className="font-semibold text-foreground">{result.marriage_timing.current_period.period}</p>
                      <p className="text-xs text-muted-foreground">
                        {result.marriage_timing.current_period.start} → {result.marriage_timing.current_period.end}
                      </p>
                    </div>
                  )}
                  <div className="flex flex-wrap gap-4 text-sm">
                    {result.marriage_timing.estimated_age_range && (
                      <div>
                        <p className="text-xs text-muted-foreground">Est. Age Range</p>
                        <p className="font-semibold text-foreground">{result.marriage_timing.estimated_age_range} yrs</p>
                      </div>
                    )}
                    {result.marriage_timing.next_strong_window && (
                      <div>
                        <p className="text-xs text-muted-foreground">Next Strong Window</p>
                        <p className="font-semibold text-foreground text-xs">{result.marriage_timing.next_strong_window}</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* 2. Favorable windows */}
            {result.marriage_timing.favorable_windows.length > 0 && (
              <div className="space-y-2">
                <h2 className="text-sm font-semibold text-foreground">Favorable Timing Windows</h2>
                <div className="flex gap-3 overflow-x-auto pb-1">
                  {result.marriage_timing.favorable_windows.map((w, i) => {
                    const cfg = PROB_CONFIG[w.probability] ?? PROB_CONFIG["Moderate"];
                    return (
                      <div key={i} className="shrink-0 w-44 rounded-xl border border-border bg-card p-3 space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span className={`text-lg font-bold ${cfg.color}`}>{w.score}/10</span>
                          <span className={`text-xs px-2 py-0.5 rounded-full border ${cfg.bg} ${cfg.color}`}>{w.probability}</span>
                        </div>
                        <p className="text-xs font-medium text-foreground leading-tight">{w.period}</p>
                        <p className="text-xs text-muted-foreground">{w.start.slice(0,7)} → {w.end.slice(0,7)}</p>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}

            {/* 3. Spouse profile */}
            {result.spouse_profile && (
              <div className="rounded-2xl border border-border bg-card p-5 space-y-4">
                <h2 className="text-sm font-semibold text-foreground">Spouse Profile</h2>
                <div className="flex flex-wrap gap-4">
                  <div>
                    <p className="text-xs text-muted-foreground">7th House Sign</p>
                    <p className="text-xl font-bold text-amber-700">{result.spouse_profile["7th_house_sign_en"] || result.spouse_profile["7th_house_sign"]}</p>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground">7th Lord</p>
                    <p className="font-semibold text-foreground">{result.spouse_profile["7th_lord"]}</p>
                    <p className="text-xs text-muted-foreground">House {result.spouse_profile["7th_lord_house"]}</p>
                  </div>
                  {result.spouse_profile.direction && (
                    <div>
                      <p className="text-xs text-muted-foreground">Direction</p>
                      <p className="font-semibold text-foreground">{result.spouse_profile.direction}</p>
                    </div>
                  )}
                </div>

                {/* Traits */}
                {result.spouse_profile.traits.length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {result.spouse_profile.traits.map((t, i) => (
                      <span key={i} className="px-3 py-1 rounded-full bg-amber-50 border border-amber-200 text-amber-700 text-xs">{t}</span>
                    ))}
                  </div>
                )}

                {result.spouse_profile.profession_hint && (
                  <div>
                    <p className="text-xs text-muted-foreground">Profession Hint</p>
                    <p className="text-sm text-foreground">{result.spouse_profile.profession_hint}</p>
                  </div>
                )}

                {result.spouse_profile.karaka_strength && (
                  <div className="text-xs rounded-lg bg-blue-50 border border-blue-200 text-blue-700 px-3 py-2">
                    {result.spouse_profile.karaka_strength}
                  </div>
                )}

                {result.spouse_profile.navamsa_karaka && (
                  <div>
                    <p className="text-xs text-muted-foreground">Navamsa D9 Insight</p>
                    <p className="text-sm text-foreground">{result.spouse_profile.navamsa_karaka}</p>
                  </div>
                )}
              </div>
            )}

            {/* 4. Marriage yogas */}
            {result.marriage_yogas.length > 0 && (
              <div className="rounded-2xl border border-border bg-card divide-y divide-border">
                <div className="px-5 py-3">
                  <h2 className="text-sm font-semibold text-foreground">Marriage Yogas</h2>
                </div>
                {result.marriage_yogas.map((y, i) => (
                  <div key={i} className="flex gap-3 px-5 py-3">
                    <div className="mt-0.5 shrink-0">
                      {y.present
                        ? <CheckCircle2 className="w-4 h-4 text-green-600" />
                        : <XCircle className="w-4 h-4 text-muted-foreground/40" />
                      }
                    </div>
                    <div>
                      <p className={`text-sm font-medium ${y.present ? "text-foreground" : "text-muted-foreground"}`}>{y.name}</p>
                      {y.description && y.present && (
                        <p className="text-xs text-muted-foreground mt-0.5">{y.description}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* 5. Delay factors */}
            {result.delay_factors.length > 0 && (
              <div className="space-y-2">
                <h2 className="text-sm font-semibold text-foreground">Delay / Challenge Factors</h2>
                {result.delay_factors.map((d, i) => (
                  <div key={i} className={`rounded-xl border p-3 flex gap-3 ${SEV_CONFIG[d.severity] ?? SEV_CONFIG["Low"]}`}>
                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                    <div>
                      <p className="text-sm font-medium">{d.factor}</p>
                      {d.effect && <p className="text-xs mt-0.5 opacity-80">{d.effect}</p>}
                    </div>
                    <span className="ml-auto text-xs font-medium opacity-70 whitespace-nowrap">{d.severity}</span>
                  </div>
                ))}
              </div>
            )}

            {/* 6. AI analysis */}
            {result.ai_analysis && (
              <div className="rounded-2xl border border-border bg-card overflow-hidden">
                <button
                  className="w-full flex items-center justify-between px-5 py-3 text-sm font-semibold text-foreground"
                  onClick={() => setShowAi(v => !v)}
                >
                  <span className="flex items-center gap-2"><Star className="w-4 h-4 text-amber-500" />AI Analysis</span>
                  {showAi ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                </button>
                <AnimatePresence>
                  {showAi && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }} transition={{ duration: 0.2 }}
                      className="overflow-hidden"
                    >
                      <div className="px-5 pb-5 text-sm text-foreground/90 leading-relaxed whitespace-pre-wrap border-t border-border pt-4">
                        {result.ai_analysis}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            )}

          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
