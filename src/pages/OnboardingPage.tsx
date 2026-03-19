import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { useKundliStore } from "@/store/kundliStore";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Moon, Sparkles, ArrowRight, Loader2, CheckCircle2, Star } from "lucide-react";
import cosmicBg from "@/assets/cosmic-bg.jpg";
import { useKundali } from "@/hooks/useKundali";
import { getCities, type City } from "@/lib/cities";
import type { KundaliResponse } from "@/types/api";

export default function OnboardingPage() {
  const navigate = useNavigate();
  const setBirthDetails = useKundliStore((s) => s.setBirthDetails);
  const kundaliMutation = useKundali();
  const [form, setForm] = useState({ name: "", dateOfBirth: "", timeOfBirth: "", birthPlace: "" });
  const [step, setStep] = useState(0);
  const [cities, setCities] = useState<City[]>([]);
  const [citySuggestions, setCitySuggestions] = useState<City[]>([]);
  const [selectedCity, setSelectedCity] = useState<City | null>(null);
  const [result, setResult] = useState<KundaliResponse | null>(null);
  const cityInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => { getCities().then(setCities).catch(() => {}); }, []);

  const handleCityInput = (value: string) => {
    setForm({ ...form, birthPlace: value });
    setSelectedCity(null);
    if (value.length < 2) { setCitySuggestions([]); return; }
    const q = value.toLowerCase();
    setCitySuggestions(cities.filter((c) => c.name.toLowerCase().includes(q)).slice(0, 6));
  };

  const handleCitySelect = (city: City) => {
    setForm({ ...form, birthPlace: city.name });
    setSelectedCity(city);
    setCitySuggestions([]);
  };

  const steps = [
    { label: "Your Name", field: "name" as const, type: "text", placeholder: "Enter your name", icon: "✨" },
    { label: "Date of Birth", field: "dateOfBirth" as const, type: "date", placeholder: "", icon: "📅" },
    { label: "Time of Birth", field: "timeOfBirth" as const, type: "time", placeholder: "", icon: "🕐" },
    { label: "Birth Place", field: "birthPlace" as const, type: "text", placeholder: "Search city...", icon: "📍" },
  ];

  const currentStep = steps[step];
  const canProceed = form[currentStep.field].trim() !== "";

  const handleNext = () => {
    if (step < steps.length - 1) {
      setStep(step + 1);
    } else {
      setBirthDetails(form);
      const city = selectedCity ?? { lat: 28.6139, lon: 77.209, tz: 5.5 };
      kundaliMutation.mutate(
        { name: form.name, date: form.dateOfBirth, time: form.timeOfBirth, lat: city.lat, lon: city.lon, tz: city.tz, place: form.birthPlace },
        { onSuccess: (data) => setResult(data) }
      );
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && canProceed) handleNext();
  };

  // ── Loading screen ────────────────────────────────────────────────────
  if (kundaliMutation.isPending) {
    return (
      <div className="min-h-[calc(100vh-3.5rem)] flex items-center justify-center p-6">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-center space-y-4">
          <motion.div animate={{ rotate: 360 }} transition={{ duration: 2, repeat: Infinity, ease: "linear" }}>
            <Moon className="h-14 w-14 text-primary zodiac-glow mx-auto" />
          </motion.div>
          <h2 className="font-display text-xl text-foreground">Calculating Your Kundali...</h2>
          <p className="text-sm text-muted-foreground">Reading planetary positions at birth</p>
          <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
            <span>Consulting pyswisseph + Lahiri ayanamsha</span>
          </div>
        </motion.div>
      </div>
    );
  }

  // ── Result screen ─────────────────────────────────────────────────────
  if (result) {
    return (
      <div className="min-h-[calc(100vh-3.5rem)] flex items-center justify-center p-6 relative overflow-hidden">
        <img src={cosmicBg} alt="" className="absolute inset-0 w-full h-full object-cover opacity-20" />
        <div className="absolute inset-0 bg-gradient-to-b from-background via-background/90 to-background" />

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="relative z-10 w-full max-w-md space-y-6">
          {/* Success header */}
          <div className="text-center space-y-2">
            <motion.div initial={{ scale: 0 }} animate={{ scale: 1 }} transition={{ type: "spring", stiffness: 200 }}>
              <CheckCircle2 className="h-14 w-14 text-emerald-400 mx-auto" />
            </motion.div>
            <h1 className="font-display text-2xl text-foreground text-glow-gold">Kundali Ready!</h1>
            <p className="text-sm text-muted-foreground">{result.name} · {result.place}</p>
          </div>

          {/* Key details card */}
          <div className="cosmic-card rounded-2xl p-6 space-y-4">
            <h2 className="font-display text-sm text-primary uppercase tracking-wider flex items-center gap-2">
              <Star className="h-3.5 w-3.5" /> Key Positions
            </h2>
            <div className="grid grid-cols-2 gap-3">
              <StatBox label="Lagna (Ascendant)" value={result.lagna.rashi} sub={result.lagna.nakshatra} />
              {result.grahas["Chandra"] && <StatBox label="Moon (Chandra)" value={result.grahas["Chandra"].rashi} sub={result.grahas["Chandra"].nakshatra} />}
              {result.grahas["Surya"] && <StatBox label="Sun (Surya)" value={result.grahas["Surya"].rashi} sub={result.grahas["Surya"].status} />}
              {result.grahas["Guru"] && <StatBox label="Jupiter (Guru)" value={result.grahas["Guru"].rashi} sub={`H${result.grahas["Guru"].house}`} />}
            </div>

            {/* Current Dasha */}
            {result.dashas[0] && (
              <div className="bg-primary/5 border border-primary/20 rounded-xl p-3">
                <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-1">Current Mahadasha</p>
                <p className="text-sm font-display text-primary">{result.dashas[0].lord} Dasha</p>
                <p className="text-[10px] text-muted-foreground">{result.dashas[0].start} → {result.dashas[0].end}</p>
              </div>
            )}

            {/* Yogas */}
            {result.yogas.length > 0 && (
              <div>
                <p className="text-[10px] text-muted-foreground uppercase tracking-wider mb-2">Active Yogas</p>
                <div className="flex flex-wrap gap-1.5">
                  {result.yogas.slice(0, 4).map((y) => (
                    <span key={y.name} className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      {y.name}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* All planet positions */}
          <div className="cosmic-card rounded-2xl p-5 space-y-2">
            <h2 className="font-display text-sm text-primary uppercase tracking-wider mb-3">All Planet Positions</h2>
            {Object.entries(result.grahas).map(([name, g]) => (
              <div key={name} className="flex items-center justify-between text-xs bg-muted/20 rounded-lg px-3 py-2">
                <span className="text-muted-foreground w-20">{name}</span>
                <span className="text-foreground font-medium">{g.rashi}</span>
                <span className="text-muted-foreground">H{g.house}</span>
                <span className="text-muted-foreground/70">{g.nakshatra}</span>
                {g.retro && <span className="text-amber-400 text-[9px]">℞</span>}
                {g.status !== "Normal" && <span className="text-emerald-400 text-[9px]">{g.status.slice(0, 4)}</span>}
              </div>
            ))}
          </div>

          {/* Navigation buttons */}
          <div className="flex gap-3">
            <button
              onClick={() => navigate("/kundli")}
              className="flex-1 py-3 rounded-xl text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition-colors flex items-center justify-center gap-2"
            >
              <Sparkles className="h-4 w-4" /> View Full Chart
            </button>
            <button
              onClick={() => navigate("/")}
              className="flex-1 py-3 rounded-xl text-sm text-muted-foreground hover:text-foreground transition-colors bg-muted/20"
            >
              Go to Dashboard
            </button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ── Error fallback ────────────────────────────────────────────────────
  if (kundaliMutation.isError) {
    return (
      <div className="min-h-[calc(100vh-3.5rem)] flex items-center justify-center p-6">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="cosmic-card rounded-2xl p-8 max-w-sm text-center space-y-4">
          <span className="text-4xl block">⚠️</span>
          <h2 className="font-display text-lg text-foreground">Calculation Error</h2>
          <p className="text-sm text-muted-foreground">Could not reach the Kundali API. Your details are saved.</p>
          <div className="flex gap-3">
            <button onClick={() => kundaliMutation.reset()} className="flex-1 py-2.5 rounded-xl text-sm bg-muted/20 text-muted-foreground hover:text-foreground">
              Try Again
            </button>
            <button onClick={() => navigate("/")} className="flex-1 py-2.5 rounded-xl text-sm bg-primary text-primary-foreground">
              Continue
            </button>
          </div>
        </motion.div>
      </div>
    );
  }

  // ── Main form ─────────────────────────────────────────────────────────
  return (
    <div className="min-h-[calc(100vh-3.5rem)] flex items-center justify-center p-6 relative overflow-hidden">
      <img src={cosmicBg} alt="" className="absolute inset-0 w-full h-full object-cover opacity-20" />
      <div className="absolute inset-0 bg-gradient-to-b from-background via-background/90 to-background" />

      <motion.div initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} className="relative z-10 w-full max-w-md space-y-8">
        {/* Header */}
        <div className="text-center space-y-3">
          <motion.div animate={{ rotate: 360 }} transition={{ duration: 20, repeat: Infinity, ease: "linear" }} className="inline-block">
            <Moon className="h-12 w-12 text-primary zodiac-glow mx-auto" />
          </motion.div>
          <h1 className="font-display text-3xl text-foreground text-glow-gold">Discover Your Stars</h1>
          <p className="text-sm text-muted-foreground">Enter your birth details to generate your Vedic birth chart</p>
        </div>

        {/* Progress */}
        <div className="flex gap-2">
          {steps.map((_, i) => (
            <div key={i} className="flex-1 h-1 rounded-full overflow-hidden bg-muted/30">
              <motion.div className="h-full bg-primary" initial={{ width: 0 }} animate={{ width: i <= step ? "100%" : "0%" }} transition={{ duration: 0.3 }} />
            </div>
          ))}
        </div>

        {/* Form Step */}
        <motion.div key={step} initial={{ opacity: 0, x: 30 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -30 }} className="cosmic-card rounded-2xl p-8 space-y-6">
          <div className="text-center">
            <span className="text-4xl mb-3 block">{currentStep.icon}</span>
            <Label className="font-display text-lg text-foreground">{currentStep.label}</Label>
          </div>

          {step === 3 ? (
            <div className="relative">
              <Input
                ref={cityInputRef}
                type="text"
                placeholder="Search city..."
                value={form.birthPlace}
                onChange={(e) => handleCityInput(e.target.value)}
                onKeyDown={handleKeyDown}
                className="text-center text-lg bg-muted/20 border-border/30 h-14 focus:border-primary/50 focus:ring-primary/20"
                autoFocus
                aria-label="Birth Place"
                autoComplete="off"
              />
              {selectedCity && (
                <p className="text-[10px] text-primary text-center mt-1">
                  {selectedCity.lat.toFixed(2)}°N {selectedCity.lon.toFixed(2)}°E · TZ {selectedCity.tz}
                </p>
              )}
              <AnimatePresence>
                {citySuggestions.length > 0 && (
                  <motion.div initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} className="absolute z-50 w-full mt-1 cosmic-card rounded-xl border border-border/40 overflow-hidden">
                    {citySuggestions.map((city) => (
                      <button key={city.name} type="button" onClick={() => handleCitySelect(city)} className="w-full text-left px-4 py-2.5 text-sm hover:bg-primary/10 hover:text-primary transition-colors border-b border-border/20 last:border-0">
                        {city.name}
                      </button>
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          ) : (
            <Input
              type={currentStep.type}
              placeholder={currentStep.placeholder}
              value={form[currentStep.field]}
              onChange={(e) => setForm({ ...form, [currentStep.field]: e.target.value })}
              onKeyDown={handleKeyDown}
              className="text-center text-lg bg-muted/20 border-border/30 h-14 focus:border-primary/50 focus:ring-primary/20"
              autoFocus
              aria-label={currentStep.label}
            />
          )}

          <div className="flex gap-3">
            {step > 0 && (
              <button onClick={() => setStep(step - 1)} className="flex-1 py-3 rounded-xl text-sm text-muted-foreground hover:text-foreground transition-colors bg-muted/20">
                Back
              </button>
            )}
            <button
              onClick={handleNext}
              disabled={!canProceed}
              className="flex-1 py-3 rounded-xl text-sm font-medium bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-30 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {step === steps.length - 1 ? <><Sparkles className="h-4 w-4" /> Generate Kundli</> : <>Continue <ArrowRight className="h-4 w-4" /></>}
            </button>
          </div>
        </motion.div>

        {/* Skip */}
        <button onClick={() => navigate("/")} className="block mx-auto text-xs text-muted-foreground/50 hover:text-muted-foreground transition-colors">
          Skip for now — explore with sample data
        </button>
      </motion.div>
    </div>
  );
}

function StatBox({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="bg-muted/20 rounded-xl p-3 text-center">
      <p className="text-[9px] text-muted-foreground uppercase tracking-wider mb-1">{label}</p>
      <p className="text-sm font-display text-foreground">{value}</p>
      {sub && <p className="text-[10px] text-primary/70 mt-0.5">{sub}</p>}
    </div>
  );
}
