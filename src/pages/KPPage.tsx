/**
 * KPPage — Krishnamurti Paddhati (KP) System
 * Shows planet & cusp sub-lords with significators
 */
import { useState } from "react";
import { motion } from "framer-motion";
import { Star, MapPin, Loader2, Info } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";
import { searchCities, type City } from "@/lib/cities";
import { useKundliStore } from "@/store/kundliStore";
import PageBot from "@/components/PageBot";

interface KPPlanet {
  longitude: number;
  degree: number;
  rashi: string;
  nakshatra: string;
  retro: boolean;
  star_lord: string;
  sub_lord: string;
  sub_sub_lord: string;
  sub_span_start: number;
  sub_span_end: number;
}
interface KPCusp {
  house: number;
  longitude: number;
  degree: number;
  rashi: string;
  nakshatra: string;
  star_lord: string;
  sub_lord: string;
  sub_sub_lord: string;
}
interface KPResult {
  name: string;
  ayanamsha: number;
  lagna: { rashi: string; longitude: number; star_lord: string; sub_lord: string; sub_sub_lord: string };
  planets: Record<string, KPPlanet>;
  cusps: KPCusp[];
  significators: Record<string, number[]>;
}

const GRAHA_ORDER = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];
const GRAHA_EN: Record<string, string> = {
  Surya:"Sun", Chandra:"Moon", Mangal:"Mars", Budh:"Mercury",
  Guru:"Jupiter", Shukra:"Venus", Shani:"Saturn", Rahu:"Rahu", Ketu:"Ketu",
};
const GRAHA_COLOR: Record<string, string> = {
  Surya:"#f59e0b", Chandra:"#94a3b8", Mangal:"#ef4444",
  Budh:"#22c55e", Guru:"#eab308", Shukra:"#a855f7",
  Shani:"#64748b", Rahu:"#6366f1", Ketu:"#f97316",
};

export default function KPPage() {
  const { birthDetails } = useKundliStore();

  const [date, setDate]     = useState(birthDetails?.dob ?? "");
  const [time, setTime]     = useState(birthDetails?.tob ?? "12:00");
  const [cityQ, setCityQ]   = useState(birthDetails?.place ?? "");
  const [cities, setCities] = useState<City[]>([]);
  const [lat, setLat]       = useState<number>(birthDetails?.lat ?? 28.6139);
  const [lon, setLon]       = useState<number>(birthDetails?.lon ?? 77.209);
  const [tz, setTz]         = useState<number>(birthDetails?.tz ?? 5.5);
  const [result, setResult] = useState<KPResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]   = useState("");
  const [activeSection, setActiveSection] = useState<"planets" | "cusps">("planets");

  async function handleCitySearch(q: string) {
    setCityQ(q);
    if (q.length < 2) { setCities([]); return; }
    const res = await searchCities(q);
    setCities(res.slice(0, 6));
  }
  function selectCity(c: City) {
    setCityQ(c.name);
    setLat(c.lat);
    setLon(c.lon);
    setTz(c.tz);
    setCities([]);
  }

  async function generate() {
    if (!date || !time) { setError("Please enter date and time"); return; }
    setError("");
    setLoading(true);
    try {
      const [hh, mm] = time.split(":").map(Number);
      const res = await api.post("/api/kp", { date, time, lat, lon, tz, name: "KP Chart" });
      setResult(res.data as KPResult);
    } catch (e: any) {
      setError(e.response?.data?.detail ?? "Error generating KP chart");
    } finally {
      setLoading(false);
    }
  }

  function SubLordBadge({ lord, size = "sm" }: { lord: string; size?: "xs" | "sm" }) {
    const color = GRAHA_COLOR[lord] ?? "#888";
    return (
      <span className={`inline-block rounded px-1.5 py-0.5 font-medium border ${size === "xs" ? "text-[10px]" : "text-xs"}`}
        style={{ color, borderColor: `${color}40`, background: `${color}12` }}>
        {lord}
      </span>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-20">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
          <Star className="h-6 w-6 text-primary" />
          KP System
        </h1>
        <p className="text-muted-foreground text-sm mt-1">Krishnamurti Paddhati — Sub-Lord Analysis (Placidus cusps, KP Ayanamsha)</p>
      </div>

      {/* Info card */}
      <div className="rounded-xl border border-primary/20 bg-primary/5 p-4 flex gap-3">
        <Info className="h-4 w-4 text-primary shrink-0 mt-0.5" />
        <p className="text-xs text-muted-foreground">
          KP system divides each nakshatra (13°20') into 9 sub-periods proportional to Vimshottari dasha years.
          The <strong className="text-foreground">Sub-Lord</strong> of a planet/cusp reveals the actual event signified.
          The <strong className="text-foreground">Star Lord</strong> indicates the source (which planet's house significations).
        </p>
      </div>

      {/* Input form */}
      <div className="rounded-xl border border-border/30 bg-card p-5 space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <Label className="text-xs text-muted-foreground mb-1 block">Date of Birth</Label>
            <Input type="date" value={date} onChange={e => setDate(e.target.value)} className="h-9 text-sm" />
          </div>
          <div>
            <Label className="text-xs text-muted-foreground mb-1 block">Time of Birth</Label>
            <Input type="time" value={time} onChange={e => setTime(e.target.value)} className="h-9 text-sm" />
          </div>
        </div>
        <div className="relative">
          <Label className="text-xs text-muted-foreground mb-1 block">Birth Place</Label>
          <div className="relative">
            <MapPin className="absolute left-2.5 top-2 h-4 w-4 text-muted-foreground" />
            <Input
              className="pl-8 h-9 text-sm"
              placeholder="Search city..."
              value={cityQ}
              onChange={e => handleCitySearch(e.target.value)}
            />
          </div>
          {cities.length > 0 && (
            <div className="absolute z-20 left-0 right-0 top-full mt-1 rounded-lg border border-border bg-card shadow-lg overflow-hidden">
              {cities.map((c, i) => (
                <button key={i} onClick={() => selectCity(c)}
                  className="w-full text-left px-3 py-2 text-sm hover:bg-muted/50 border-b border-border/20 last:border-0">
                  {c.name} <span className="text-xs text-muted-foreground ml-1">{c.state ?? ""}</span>
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="flex gap-2 text-xs text-muted-foreground">
          <span>Lat: {lat.toFixed(2)}</span>
          <span>Lon: {lon.toFixed(2)}</span>
          <span>TZ: +{tz}</span>
        </div>
        {error && <p className="text-xs text-destructive">{error}</p>}
        <button
          onClick={generate}
          disabled={loading}
          className="w-full h-10 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition disabled:opacity-50 flex items-center justify-center gap-2">
          {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> Calculating...</> : "Generate KP Chart"}
        </button>
      </div>

      {/* Results */}
      {result && (
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="space-y-4">
          {/* Lagna summary */}
          <div className="rounded-xl border border-primary/30 bg-primary/5 p-4">
            <p className="text-xs text-muted-foreground uppercase mb-2 font-medium tracking-wide">KP Lagna</p>
            <div className="flex flex-wrap gap-3 items-center">
              <span className="text-lg font-bold text-foreground">{result.lagna.rashi}</span>
              <span className="text-xs text-muted-foreground">{result.lagna.longitude.toFixed(2)}°</span>
              <div className="flex items-center gap-1.5 text-xs">
                <span className="text-muted-foreground">Star:</span>
                <SubLordBadge lord={result.lagna.star_lord} />
                <span className="text-muted-foreground">Sub:</span>
                <SubLordBadge lord={result.lagna.sub_lord} />
                <span className="text-muted-foreground">Sub-sub:</span>
                <SubLordBadge lord={result.lagna.sub_sub_lord} size="xs" />
              </div>
              <span className="text-xs text-muted-foreground ml-auto">Ayanamsha: {result.ayanamsha.toFixed(4)}°</span>
            </div>
          </div>

          {/* Section toggle */}
          <div className="flex rounded-lg overflow-hidden border border-border/30 w-fit">
            {(["planets","cusps"] as const).map(s => (
              <button key={s} onClick={() => setActiveSection(s)}
                className={`px-4 py-1.5 text-sm capitalize transition ${activeSection === s ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted/30"}`}>
                {s === "planets" ? "Planet Sub-Lords" : "Cusp Sub-Lords"}
              </button>
            ))}
          </div>

          {/* Planet sub-lords table */}
          {activeSection === "planets" && (
            <motion.div key="planets" initial={{ opacity: 0 }} animate={{ opacity: 1 }}
              className="rounded-xl border border-border/30 bg-card overflow-hidden">
              <div className="overflow-x-auto">
              <table className="w-full text-xs min-w-[600px]">
                <thead>
                  <tr className="border-b border-border/30 bg-muted/20">
                    {["Planet","Rashi","Nak°","Star Lord","Sub Lord","Sub-Sub","Sig. Houses","R"].map(h => (
                      <th key={h} className="text-left px-3 py-2 text-muted-foreground font-medium">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {GRAHA_ORDER.map((gn, i) => {
                    const p = result.planets[gn];
                    if (!p) return null;
                    const sigs = result.significators[gn] ?? [];
                    return (
                      <tr key={gn} className={`border-b border-border/10 ${i % 2 === 0 ? "bg-muted/5" : ""} hover:bg-muted/10`}>
                        <td className="px-3 py-2 font-medium" style={{ color: GRAHA_COLOR[gn] }}>
                          {gn} <span className="text-muted-foreground text-[10px]">({GRAHA_EN[gn]})</span>
                        </td>
                        <td className="px-3 py-2 text-foreground">{p.rashi}</td>
                        <td className="px-3 py-2 text-muted-foreground">{p.degree.toFixed(2)}°</td>
                        <td className="px-3 py-2"><SubLordBadge lord={p.star_lord} /></td>
                        <td className="px-3 py-2"><SubLordBadge lord={p.sub_lord} /></td>
                        <td className="px-3 py-2"><SubLordBadge lord={p.sub_sub_lord} size="xs" /></td>
                        <td className="px-3 py-2 text-muted-foreground">
                          {sigs.map(h => <span key={h} className="inline-block mr-1 px-1 bg-muted/30 rounded text-[10px]">H{h}</span>)}
                        </td>
                        <td className="px-3 py-2 text-center">{p.retro ? <span className="text-amber-400">℞</span> : ""}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              </div>
            </motion.div>
          )}

          {/* Cusp sub-lords table */}
          {activeSection === "cusps" && (
            <motion.div key="cusps" initial={{ opacity: 0 }} animate={{ opacity: 1 }}
              className="rounded-xl border border-border/30 bg-card overflow-hidden">
              <div className="overflow-x-auto">
              <table className="w-full text-xs min-w-[480px]">
                <thead>
                  <tr className="border-b border-border/30 bg-muted/20">
                    {["House","Rashi","Degree","Star Lord","Sub Lord","Sub-Sub Lord"].map(h => (
                      <th key={h} className="text-left px-3 py-2 text-muted-foreground font-medium">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {result.cusps.map((c, i) => (
                    <tr key={i} className={`border-b border-border/10 ${i % 2 === 0 ? "bg-muted/5" : ""} hover:bg-muted/10`}>
                      <td className="px-3 py-2 font-semibold text-foreground">H{c.house}</td>
                      <td className="px-3 py-2 text-foreground">{c.rashi}</td>
                      <td className="px-3 py-2 text-muted-foreground">{c.degree.toFixed(2)}°</td>
                      <td className="px-3 py-2"><SubLordBadge lord={c.star_lord} /></td>
                      <td className="px-3 py-2"><SubLordBadge lord={c.sub_lord} /></td>
                      <td className="px-3 py-2"><SubLordBadge lord={c.sub_sub_lord} size="xs" /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              </div>
            </motion.div>
          )}

          {/* KP Interpretation guide */}
          <div className="rounded-xl border border-border/30 bg-muted/10 p-4">
            <p className="text-xs font-medium text-foreground mb-2">How to Read KP Sub-Lords</p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-muted-foreground">
              <div><span className="text-foreground font-medium">Star Lord</span> — Indicates the source nakshatra lord. Shows what area of life is activated.</div>
              <div><span className="text-foreground font-medium">Sub Lord</span> — The key significator. Determines if the matter promised by the cusp/planet will fructify.</div>
              <div><span className="text-foreground font-medium">Significators</span> — Houses signified by each planet (occupation + ownership). Used to time events.</div>
            </div>
          </div>
        </motion.div>
      )}

      <PageBot
        pageContext="kp"
        pageData={{ kundali_raw: result ? JSON.stringify(result) : undefined }}
        suggestions={["Explain my 7th cusp sub-lord for marriage", "What does my 10th house sub-lord indicate for career?", "Which planets signify my 2nd house for wealth?"]}
      />
    </div>
  );
}
