import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Clock, Plus, Trash2, Loader2, Star, ChevronDown, ChevronUp, MapPin } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import { useKundliStore } from "@/store/kundliStore";
import { searchCities, getCities, type City } from "@/lib/cities";
import PageBot from "@/components/PageBot";

// ── Types ──────────────────────────────────────────────────────────────────────
interface LifeEvent {
  date: string;
  type: string;
  description: string;
}

interface Candidate {
  time: string;
  delta_minutes: number;
  lagna_rashi: string;
  lagna_changed: boolean;
  score: number;
  moon_nakshatra: string;
  sample_maha: string;
  sample_antar: string;
}

interface RectResult {
  candidates: Candidate[];
  approx_time: string;
  uncertainty_minutes: number;
  event_count: number;
}

// ── Event type options ─────────────────────────────────────────────────────────
const EVENT_TYPES = [
  { value: "marriage",       label: "Marriage / Relationship" },
  { value: "child",          label: "Child Birth" },
  { value: "career_start",   label: "Career Start / First Job" },
  { value: "career_change",  label: "Career Change / Promotion" },
  { value: "property",       label: "Property / Home Purchase" },
  { value: "health",         label: "Major Health Issue" },
  { value: "accident",       label: "Accident / Surgery" },
  { value: "foreign",        label: "Foreign Travel / Relocation" },
  { value: "loss",           label: "Death in Family / Major Loss" },
  { value: "success",        label: "Major Success / Award" },
  { value: "education",      label: "Education / Degree" },
  { value: "spiritual",      label: "Spiritual Awakening" },
  { value: "separation",     label: "Separation / Divorce" },
  { value: "financial_loss", label: "Financial Loss" },
  { value: "financial_gain", label: "Financial Gain / Windfall" },
];

const UNCERTAINTY_OPTIONS = [
  { value: 30,  label: "±30 minutes" },
  { value: 60,  label: "±1 hour" },
  { value: 90,  label: "±1.5 hours" },
  { value: 120, label: "±2 hours" },
  { value: 180, label: "±3 hours" },
];

function scoreColor(score: number) {
  if (score >= 70) return "text-emerald-500";
  if (score >= 50) return "text-amber-500";
  return "text-muted-foreground";
}
function scoreBg(score: number) {
  if (score >= 70) return "bg-emerald-500/10 border-emerald-500/25";
  if (score >= 50) return "bg-amber-500/10 border-amber-500/25";
  return "bg-muted/20 border-border/20";
}
function scoreBar(score: number) {
  if (score >= 70) return "bg-emerald-500";
  if (score >= 50) return "bg-amber-500";
  return "bg-muted-foreground/40";
}

export default function RectificationPage() {
  const birthDetails = useKundliStore(s => s.birthDetails);
  const settings = useKundliStore(s => s.kundaliSettings);

  // Form state — pre-fill from kundali store if available
  const [date, setDate] = useState(birthDetails?.dateOfBirth ?? "");
  const [approxTime, setApproxTime] = useState(birthDetails?.timeOfBirth ?? "");
  const [uncertainty, setUncertainty] = useState(60);
  const [events, setEvents] = useState<LifeEvent[]>([{ date: "", type: "marriage", description: "" }]);

  // Location
  const [place, setPlace] = useState(birthDetails?.birthPlace ?? "");
  const [lat, setLat] = useState<number>(birthDetails?.lat ?? 0);
  const [lon, setLon] = useState<number>(birthDetails?.lon ?? 0);
  const [tz, setTz] = useState<number>(birthDetails?.tz ?? 5.5);
  const [citySuggestions, setCitySuggestions] = useState<City[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // Results
  const [result, setResult] = useState<RectResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [expandedIdx, setExpandedIdx] = useState<number | null>(0);

  // ── City search ──────────────────────────────────────────────────────────────
  const handlePlaceInput = async (val: string) => {
    setPlace(val);
    if (val.length < 2) { setCitySuggestions([]); return; }
    const results = val.length < 4
      ? getCities().filter(c => c.name.toLowerCase().startsWith(val.toLowerCase())).slice(0, 6)
      : await searchCities(val);
    setCitySuggestions(results.slice(0, 6));
    setShowSuggestions(true);
  };

  const selectCity = (c: City) => {
    setPlace(c.name); setLat(c.lat); setLon(c.lon); setTz(c.tz);
    setShowSuggestions(false); setCitySuggestions([]);
  };

  // ── Events helpers ───────────────────────────────────────────────────────────
  const addEvent = () => setEvents(e => [...e, { date: "", type: "career_start", description: "" }]);
  const removeEvent = (i: number) => setEvents(e => e.filter((_, idx) => idx !== i));
  const updateEvent = (i: number, field: keyof LifeEvent, val: string) =>
    setEvents(e => e.map((ev, idx) => idx === i ? { ...ev, [field]: val } : ev));

  // ── Submit ───────────────────────────────────────────────────────────────────
  const handleSubmit = async () => {
    if (!date || !approxTime) { setError("Birth date and approximate time are required."); return; }
    if (!lat || !lon) { setError("Please select a birth place from the suggestions."); return; }
    const validEvents = events.filter(e => e.date && e.type);
    setLoading(true); setError(""); setResult(null);
    try {
      const data = await api.post<RectResult>("/api/rectification", {
        date, approx_time: approxTime, uncertainty_minutes: uncertainty,
        lat, lon, tz, ayanamsha: settings.ayanamsha,
        events: validEvents,
      });
      setResult(data);
      setExpandedIdx(0);
    } catch (e: any) {
      setError(e?.message ?? "Calculation failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-4 pb-10">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-primary/15 border border-primary/25 flex items-center justify-center">
          <Clock className="h-4.5 w-4.5 text-primary" />
        </div>
        <div>
          <h1 className="text-lg font-display text-foreground">Birth Time Rectification</h1>
          <p className="text-xs text-muted-foreground">Narrow down your exact birth time using life events & Vimshottari Dasha alignment</p>
        </div>
      </div>

      {/* Form */}
      <div className="cosmic-card rounded-xl p-4 space-y-4">
        <p className="text-xs font-medium text-foreground flex items-center gap-1.5">
          <Clock className="h-3.5 w-3.5 text-primary" /> Birth Details
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div>
            <Label className="text-xs text-muted-foreground mb-1">Date of Birth</Label>
            <Input type="date" value={date} onChange={e => setDate(e.target.value)}
              className="text-xs h-9" />
          </div>
          <div>
            <Label className="text-xs text-muted-foreground mb-1">Approximate Birth Time</Label>
            <Input type="time" value={approxTime} onChange={e => setApproxTime(e.target.value)}
              className="text-xs h-9" />
          </div>
        </div>

        {/* Place */}
        <div className="relative">
          <Label className="text-xs text-muted-foreground mb-1">Birth Place</Label>
          <div className="relative">
            <MapPin className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
            <Input value={place} onChange={e => handlePlaceInput(e.target.value)}
              onFocus={() => citySuggestions.length > 0 && setShowSuggestions(true)}
              placeholder="Search city…" className="pl-8 text-xs h-9" />
          </div>
          {showSuggestions && citySuggestions.length > 0 && (
            <div className="absolute z-50 w-full mt-1 bg-card border border-border rounded-lg shadow-lg overflow-hidden">
              {citySuggestions.map((c, i) => (
                <button key={i} onMouseDown={() => selectCity(c)}
                  className="w-full text-left px-3 py-2 text-xs hover:bg-muted/40 transition-colors flex justify-between">
                  <span className="text-foreground">{c.name}</span>
                  <span className="text-muted-foreground">{c.lat.toFixed(2)}°, {c.lon.toFixed(2)}°</span>
                </button>
              ))}
            </div>
          )}
          {lat !== 0 && <p className="text-[10px] text-muted-foreground/60 mt-0.5">
            {lat.toFixed(4)}°N, {lon.toFixed(4)}°E · TZ {tz >= 0 ? "+" : ""}{tz}
          </p>}
        </div>

        {/* Uncertainty */}
        <div>
          <Label className="text-xs text-muted-foreground mb-1">Time Uncertainty Range</Label>
          <div className="flex flex-wrap gap-2">
            {UNCERTAINTY_OPTIONS.map(o => (
              <button key={o.value} onClick={() => setUncertainty(o.value)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors ${
                  uncertainty === o.value
                    ? "bg-primary/20 text-primary border-primary/40"
                    : "text-muted-foreground border-border/40 hover:bg-muted/30"
                }`}>
                {o.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Life Events */}
      <div className="cosmic-card rounded-xl p-4 space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-xs font-medium text-foreground flex items-center gap-1.5">
            <Star className="h-3.5 w-3.5 text-primary" /> Known Life Events
            <span className="text-muted-foreground/60 font-normal">(add at least 2 for best accuracy)</span>
          </p>
          <button onClick={addEvent}
            className="flex items-center gap-1 text-xs text-primary hover:text-primary/80 transition-colors">
            <Plus className="h-3.5 w-3.5" /> Add Event
          </button>
        </div>

        <AnimatePresence>
          {events.map((ev, i) => (
            <motion.div key={i} initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
              className="grid grid-cols-1 sm:grid-cols-[1fr_1fr_auto] gap-2 p-3 bg-muted/10 rounded-lg border border-border/20">
              <div>
                <Label className="text-[10px] text-muted-foreground mb-1">Event Date</Label>
                <Input type="date" value={ev.date} onChange={e => updateEvent(i, "date", e.target.value)}
                  className="text-xs h-8" />
              </div>
              <div>
                <Label className="text-[10px] text-muted-foreground mb-1">Event Type</Label>
                <select value={ev.type} onChange={e => updateEvent(i, "type", e.target.value)}
                  className="w-full h-8 bg-background border border-border/40 rounded-md px-2 text-xs text-foreground focus:outline-none focus:border-primary/50">
                  {EVENT_TYPES.map(t => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>
              <div className="flex items-end">
                {events.length > 1 && (
                  <button onClick={() => removeEvent(i)}
                    className="h-8 w-8 flex items-center justify-center text-muted-foreground hover:text-destructive rounded-md transition-colors">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                )}
              </div>
            </motion.div>
          ))}
        </AnimatePresence>

        {error && <p className="text-xs text-destructive">{error}</p>}

        <button onClick={handleSubmit} disabled={loading}
          className="w-full py-2.5 rounded-xl btn-saffron text-sm font-semibold flex items-center justify-center gap-2 disabled:opacity-60">
          {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> Calculating…</> : <><Clock className="h-4 w-4" /> Rectify Birth Time</>}
        </button>
      </div>

      {/* Results */}
      <AnimatePresence>
        {result && (
          <motion.div key="result" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
            <div className="cosmic-card rounded-xl p-4 space-y-3">
              <div className="flex items-center justify-between">
                <p className="text-xs font-medium text-foreground flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-primary" /> Rectification Results
                </p>
                <span className="text-[10px] text-muted-foreground">
                  {result.candidates.length} candidates · {result.event_count} events analyzed
                </span>
              </div>

              <div className="text-[10px] text-muted-foreground/70 bg-muted/20 rounded-lg px-3 py-2">
                Score = how well the active Mahadasha/Antardasha lord matches the event type's natural karaka.
                Higher = stronger alignment. Best candidate shown first.
              </div>

              <div className="space-y-2">
                {result.candidates.map((c, i) => (
                  <div key={i} className={`rounded-xl border transition-all ${scoreBg(c.score)}`}>
                    <button className="w-full flex items-center gap-3 p-3 text-left"
                      onClick={() => setExpandedIdx(expandedIdx === i ? null : i)}>
                      {/* Rank */}
                      <div className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold shrink-0 ${
                        i === 0 ? "bg-amber-500/20 text-amber-400" : "bg-muted/30 text-muted-foreground"
                      }`}>
                        {i + 1}
                      </div>
                      {/* Time */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-base font-bold text-foreground">{c.time}</span>
                          {c.delta_minutes !== 0 && (
                            <span className="text-[10px] text-muted-foreground">
                              ({c.delta_minutes > 0 ? "+" : ""}{c.delta_minutes} min)
                            </span>
                          )}
                          {i === 0 && <span className="text-[10px] px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-400 font-medium">Best Match</span>}
                          {c.lagna_changed && <span className="text-[10px] px-1.5 py-0.5 rounded bg-primary/15 text-primary">Lagna: {c.lagna_rashi}</span>}
                        </div>
                        {/* Score bar */}
                        <div className="mt-1.5 flex items-center gap-2">
                          <div className="flex-1 h-1.5 rounded-full bg-muted/40">
                            <div className={`h-1.5 rounded-full transition-all ${scoreBar(c.score)}`}
                              style={{ width: `${c.score}%` }} />
                          </div>
                          <span className={`text-xs font-semibold w-10 text-right ${scoreColor(c.score)}`}>{c.score}%</span>
                        </div>
                      </div>
                      {expandedIdx === i ? <ChevronUp className="h-3.5 w-3.5 text-muted-foreground shrink-0" /> : <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />}
                    </button>

                    {/* Expanded detail */}
                    <AnimatePresence>
                      {expandedIdx === i && (
                        <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }}
                          exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                          <div className="px-3 pb-3 grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs border-t border-border/15 pt-3">
                            <div className="bg-muted/20 rounded-lg p-2">
                              <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Lagna Rashi</p>
                              <p className="font-medium text-foreground mt-0.5">{c.lagna_rashi}</p>
                            </div>
                            <div className="bg-muted/20 rounded-lg p-2">
                              <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Moon Nakshatra</p>
                              <p className="font-medium text-foreground mt-0.5">{c.moon_nakshatra}</p>
                            </div>
                            {c.sample_maha && (
                              <div className="bg-muted/20 rounded-lg p-2">
                                <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Mahadasha (Event 1)</p>
                                <p className="font-medium text-foreground mt-0.5">{c.sample_maha}</p>
                              </div>
                            )}
                            {c.sample_antar && (
                              <div className="bg-muted/20 rounded-lg p-2">
                                <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Antardasha (Event 1)</p>
                                <p className="font-medium text-foreground mt-0.5">{c.sample_antar}</p>
                              </div>
                            )}
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                ))}
              </div>

              <p className="text-[10px] text-muted-foreground/60 leading-relaxed">
                This is a computational aid — final rectification should be confirmed with a qualified Jyotishi.
                Accuracy improves with more life events and precise event dates.
              </p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <PageBot pageContext="rectification" pageData={{ result }} />
    </div>
  );
}
