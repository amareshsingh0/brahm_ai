/**
 * VarshpalPage — Annual Chart (Solar Return / Varshphal)
 * Computes the chart for the exact moment the Sun returns to its natal longitude.
 */
import { useState } from "react";
import { motion } from "framer-motion";
import { Sun, Loader2, MapPin, CalendarDays, ChevronDown, ChevronUp } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import { searchCities, type City } from "@/lib/cities";
import { useKundliStore } from "@/store/kundliStore";
import { KundliChart } from "@/components/charts/KundliChart";
import PageBot from "@/components/PageBot";
import type { KundaliResponse } from "@/types/api";
import { useTranslation } from "react-i18next";

interface VarshpalResult extends KundaliResponse {
  varshphal_year: number;
  solar_return_datetime: string;
  natal_sun_longitude: number;
  year_themes: string[];
}

const GRAHA_SYMBOL: Record<string, string> = {
  Surya:"☉︎",Chandra:"☽︎",Mangal:"♂︎",Budh:"☿︎",Guru:"♃︎",
  Shukra:"♀︎",Shani:"♄︎",Rahu:"☊︎",Ketu:"☋︎",
};
const GRAHA_COLOR: Record<string, string> = {
  Surya:"#D97706",Chandra:"#4F46E5",Mangal:"#DC2626",Budh:"#16A34A",
  Guru:"#B45309",Shukra:"#9333EA",Shani:"#334155",Rahu:"#0369A1",Ketu:"#C2410C",
};
const ORDER = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];

export default function VarshpalPage() {
  const { t } = useTranslation();
  const birthDetails = useKundliStore(s => s.birthDetails);
  const year = new Date().getFullYear();

  // Birth data
  const [bDate,  setBDate]  = useState(birthDetails?.dob    ?? "");
  const [bTime,  setBTime]  = useState(birthDetails?.time   ?? "");
  const [bLat,   setBLat]   = useState(birthDetails?.lat?.toString()  ?? "");
  const [bLon,   setBLon]   = useState(birthDetails?.lon?.toString()  ?? "");
  const [bTz,    setBTz]    = useState(birthDetails?.tz?.toString()   ?? "5.5");
  const [bCity,  setBCity]  = useState(birthDetails?.place  ?? "");

  // Target year
  const [targetYear, setTargetYear] = useState(year);

  // Current location (optional)
  const [useCurrent, setUseCurrent] = useState(false);
  const [cLat,   setCLat]   = useState("");
  const [cLon,   setCLon]   = useState("");
  const [cTz,    setCTz]    = useState("5.5");
  const [cCity,  setCCity]  = useState("");

  const [suggestions,  setSuggestions]  = useState<City[]>([]);
  const [sugField,     setSugField]     = useState<"birth" | "current">("birth");
  const [loading,      setLoading]      = useState(false);
  const [result,       setResult]       = useState<VarshpalResult | null>(null);
  const [error,        setError]        = useState<string | null>(null);
  const [showGrahas,   setShowGrahas]   = useState(false);

  const handleCitySearch = async (q: string, field: "birth" | "current") => {
    setSugField(field);
    if (field === "birth") setBCity(q); else setCCity(q);
    if (q.length >= 2) {
      const { searchCities: sc } = await import("@/lib/cities");
      const hits = await sc(q);
      setSuggestions(hits.slice(0, 6));
    } else {
      setSuggestions([]);
    }
  };

  const selectCity = (c: City) => {
    if (sugField === "birth") {
      setBCity(c.name); setBLat(c.lat.toString()); setBLon(c.lon.toString()); setBTz(c.tz.toString());
    } else {
      setCCity(c.name); setCLat(c.lat.toString()); setCLon(c.lon.toString()); setCTz(c.tz.toString());
    }
    setSuggestions([]);
  };

  const calculate = async () => {
    if (!bDate || !bTime || !bLat || !bLon) { setError(t("varshphal.error_fields")); return; }
    setLoading(true); setError(null); setResult(null);
    try {
      const body: Record<string, unknown> = {
        birth_date: bDate, birth_time: bTime,
        birth_lat: parseFloat(bLat), birth_lon: parseFloat(bLon), birth_tz: parseFloat(bTz),
        target_year: targetYear,
      };
      if (useCurrent && cLat && cLon) {
        body.current_lat = parseFloat(cLat);
        body.current_lon = parseFloat(cLon);
        body.current_tz  = parseFloat(cTz);
      }
      const data = await api.post<VarshpalResult>("/api/varshphal", body);
      setResult(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : t("varshphal.error_failed"));
    } finally {
      setLoading(false);
    }
  };

  const planets = result
    ? Object.entries(result.grahas).map(([name, g]) => ({ name, house: g.house, retro: g.retro, degree: g.degree }))
    : [];

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-4xl mx-auto">

      {/* Header */}
      <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center gap-3">
          <Sun className="w-5 h-5 text-star-gold" />
          <div>
            <h1 className="font-display text-2xl text-foreground text-glow-gold">{t("varshphal.title")}</h1>
            <p className="text-sm text-muted-foreground mt-0.5">{t("varshphal.subtitle")}</p>
          </div>
        </div>
      </motion.div>

      {/* Form */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1, transition: { delay: 0.1 } }}
        className="cosmic-card rounded-xl p-5 space-y-4">

        <p className="text-xs font-semibold text-foreground">{t("varshphal.birth_details")}</p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <Label className="text-xs text-muted-foreground mb-1 block">{t("varshphal.date_of_birth")}</Label>
            <Input type="date" className="h-9 text-xs" value={bDate} onChange={e => setBDate(e.target.value)} />
          </div>
          <div>
            <Label className="text-xs text-muted-foreground mb-1 block">{t("varshphal.time_of_birth")}</Label>
            <Input type="time" className="h-9 text-xs" value={bTime} onChange={e => setBTime(e.target.value)} />
          </div>
        </div>

        {/* Birth location */}
        <div className="relative">
          <Label className="text-xs text-muted-foreground mb-1 block">{t("varshphal.birth_city")}</Label>
          <div className="flex gap-2">
            <div className="relative flex-1">
              <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
              <Input className="pl-9 h-9 text-xs" placeholder="City…" value={bCity}
                onChange={e => handleCitySearch(e.target.value, "birth")} />
              {suggestions.length > 0 && sugField === "birth" && (
                <div className="absolute z-50 top-full mt-1 left-0 right-0 bg-background border border-border rounded-lg shadow-xl overflow-hidden">
                  {suggestions.map(c => (
                    <button key={`${c.lat}${c.lon}`} className="w-full text-left px-3 py-2 text-xs hover:bg-muted/40"
                      onClick={() => selectCity(c)}>
                      {c.name}{c.state ? `, ${c.state}` : ""}
                    </button>
                  ))}
                </div>
              )}
            </div>
            <Input className="w-20 h-9 text-xs" placeholder="Lat" value={bLat} onChange={e => setBLat(e.target.value)} />
            <Input className="w-20 h-9 text-xs" placeholder="Lon" value={bLon} onChange={e => setBLon(e.target.value)} />
            <Input className="w-16 h-9 text-xs" placeholder="TZ" value={bTz} onChange={e => setBTz(e.target.value)} />
          </div>
        </div>

        {/* Year selector */}
        <div>
          <Label className="text-xs text-muted-foreground mb-1 block">{t("varshphal.target_year")}</Label>
          <div className="flex gap-2 items-center">
            <button onClick={() => setTargetYear(y => y - 1)}
              className="w-8 h-9 rounded border border-border/40 text-muted-foreground hover:text-foreground text-lg transition-colors">−</button>
            <span className="text-sm font-bold text-foreground w-14 text-center">{targetYear}</span>
            <button onClick={() => setTargetYear(y => y + 1)}
              className="w-8 h-9 rounded border border-border/40 text-muted-foreground hover:text-foreground text-lg transition-colors">+</button>
            <CalendarDays className="w-4 h-4 text-muted-foreground ml-2" />
            <span className="text-xs text-muted-foreground">{t("varshphal.solar_return_age", { age: targetYear - parseInt(bDate.slice(0, 4)) || "—" })}</span>
          </div>
        </div>

        {/* Optional current location */}
        <div>
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" checked={useCurrent} onChange={e => setUseCurrent(e.target.checked)} className="accent-primary" />
            <span className="text-xs text-muted-foreground">{t("varshphal.use_current_location", { year: targetYear })}</span>
          </label>
          {useCurrent && (
            <div className="relative mt-2">
              <div className="flex flex-col sm:flex-row gap-2">
                <div className="relative flex-1">
                  <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
                  <Input className="pl-9 h-9 text-xs" placeholder="Current city…" value={cCity}
                    onChange={e => handleCitySearch(e.target.value, "current")} />
                  {suggestions.length > 0 && sugField === "current" && (
                    <div className="absolute z-50 top-full mt-1 left-0 right-0 bg-background border border-border rounded-lg shadow-xl overflow-hidden">
                      {suggestions.map(c => (
                        <button key={`${c.lat}${c.lon}`} className="w-full text-left px-3 py-2 text-xs hover:bg-muted/40"
                          onClick={() => selectCity(c)}>
                          {c.name}{c.state ? `, ${c.state}` : ""}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                <Input className="w-20 h-9 text-xs" placeholder="Lat" value={cLat} onChange={e => setCLat(e.target.value)} />
                <Input className="w-20 h-9 text-xs" placeholder="Lon" value={cLon} onChange={e => setCLon(e.target.value)} />
                <Input className="w-16 h-9 text-xs" placeholder="TZ" value={cTz} onChange={e => setCTz(e.target.value)} />
              </div>
            </div>
          )}
        </div>

        <button onClick={calculate} disabled={loading || !bDate || !bTime || !bLat || !bLon}
          className="w-full h-9 rounded-lg bg-primary/80 hover:bg-primary text-primary-foreground text-sm font-medium transition-colors flex items-center justify-center gap-2 disabled:opacity-50">
          {loading
            ? <><Loader2 className="w-4 h-4 animate-spin" /> {t("varshphal.computing")}</>
            : <><Sun className="w-4 h-4" /> {t("varshphal.calculate_btn", { year: targetYear })}</>}
        </button>

        {error && <p className="text-xs text-destructive">{error}</p>}
      </motion.div>

      {/* Results */}
      {result && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="space-y-5">

          {/* Header card */}
          <div className="cosmic-card rounded-xl p-5 border border-star-gold/20">
            <div className="flex items-center gap-2 mb-1">
              <Sun className="w-4 h-4 text-star-gold" />
              <p className="text-sm font-bold text-star-gold">{t("varshphal.title")} {result.varshphal_year}</p>
            </div>
            <p className="text-xs text-muted-foreground">{t("varshphal.solar_return")}: <span className="text-foreground font-medium">{result.solar_return_datetime}</span></p>
            <p className="text-xs text-muted-foreground mt-0.5">{t("varshphal.natal_sun")}: <span className="text-foreground">{result.natal_sun_longitude.toFixed(2)}°</span> · {t("varshphal.varshphal_lagna")}: <span className="text-foreground">{result.lagna.rashi}</span></p>
          </div>

          {/* Year themes */}
          {result.year_themes.length > 0 && (
            <div className="cosmic-card rounded-xl p-4 space-y-2 border border-primary/20">
              <p className="text-xs font-semibold text-foreground mb-2">{t("varshphal.year_themes", { year: result.varshphal_year })}</p>
              {result.year_themes.map((t, i) => (
                <div key={i} className="flex gap-2 text-xs text-muted-foreground leading-relaxed">
                  <span className="text-star-gold shrink-0">✦</span>
                  <span>{t}</span>
                </div>
              ))}
            </div>
          )}

          {/* Planet positions grid */}
          <div>
            <button onClick={() => setShowGrahas(g => !g)}
              className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground mb-2 transition-colors">
              {showGrahas ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
              {showGrahas ? t("varshphal.hide_grahas") : t("varshphal.show_grahas")}
            </button>
            {showGrahas && (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2">
                {ORDER.map(p => {
                  const g = result.grahas[p];
                  if (!g) return null;
                  return (
                    <div key={p} className="cosmic-card rounded-lg p-2.5 text-center">
                      <span className="text-xl block mb-0.5" style={{ color: GRAHA_COLOR[p] }}>{GRAHA_SYMBOL[p]}</span>
                      <p className="text-[10px] text-muted-foreground">{p}</p>
                      <p className="text-xs text-star-gold font-semibold">{g.rashi}</p>
                      <p className="text-[10px] text-muted-foreground">{g.degree}° H{g.house}</p>
                      {g.retro && <p className="text-[9px] text-amber-400">℞</p>}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Chart */}
          <div className="cosmic-card rounded-xl p-4">
            <p className="text-xs font-semibold text-foreground mb-3">Varshphal Chart — {result.varshphal_year}</p>
            <KundliChart planets={planets} lagnaRashi={result.lagna.rashi} chartStyle="north" />
          </div>

          {/* Yogas */}
          {result.yogas.filter(y => y.present !== false).length > 0 && (
            <div className="cosmic-card rounded-xl p-4 space-y-2">
              <p className="text-xs font-semibold text-foreground mb-2">Active Yogas this Year</p>
              {result.yogas.filter(y => y.present !== false).map(y => (
                <div key={y.name} className="flex items-start gap-2 text-xs">
                  <span className="text-emerald-400 font-bold shrink-0">✓</span>
                  <div>
                    <span className="text-foreground font-medium">{y.name}</span>
                    <span className="text-muted-foreground ml-2">{y.desc}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </motion.div>
      )}

      <PageBot
        pageContext="varshphal"
        pageData={result ? { year: result.varshphal_year, themes: result.year_themes, lagna: result.lagna.rashi } : {}}
        suggestions={[
          "What do the year themes mean for me?",
          "How does the Varshphal lagna differ from my natal lagna?",
          "Which planets are most important in this year's chart?",
          "What remedies should I do based on this year's chart?",
        ]}
      />
    </div>
  );
}
