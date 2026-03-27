/**
 * ProfileSetupModal — shown after first login (when no birth data saved).
 * User can fill birth details or skip.
 * On submit → POST /api/user → kundliStore updated → Supabase saved.
 */
import { useState, useEffect, useRef } from "react";
import { X, Star, Zap, Heart, Shield, ChevronDown } from "lucide-react";
import { useAuthStore } from "@/store/authStore";
import { useKundliStore } from "@/store/kundliStore";
import { apiFetch } from "@/lib/apiFetch";
import { searchCities } from "@/lib/cities";
import type { City } from "@/lib/cities";

const BENEFITS = [
  { icon: Star,   title: "Personalized Kundali",   desc: "Get your exact birth chart — Lagna, Dasha, Yogas instantly." },
  { icon: Zap,    title: "AI That Knows You",       desc: "Every chat answer is specific to YOUR planets, not generic." },
  { icon: Heart,  title: "Love & Career Timing",   desc: "Know exact periods for marriage, job change, foreign travel." },
  { icon: Shield, title: "Saved Across Devices",   desc: "Profile synced — never enter birth details again." },
];

const GENDERS = ["Male", "Female", "Other", "Prefer not to say"] as const;

interface Props {
  onClose: () => void;
}

export default function ProfileSetupModal({ onClose }: Props) {
  const { userId, name: authName, setProfileSetupSeen } = useAuthStore();
  const { setBirthDetails } = useKundliStore();

  const [name, setName]         = useState(authName || "");
  const [gender, setGender]     = useState("");
  const [dob, setDob]           = useState("");
  const [tob, setTob]           = useState("");
  const [place, setPlace]       = useState("");
  const [city, setCity]         = useState<City | null>(null);
  const [suggestions, setSuggestions] = useState<City[]>([]);
  const [saving, setSaving]     = useState(false);
  const [error, setError]       = useState("");
  const searchRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // City search
  useEffect(() => {
    if (place.length < 2 || city?.name === place) { setSuggestions([]); return; }
    if (searchRef.current) clearTimeout(searchRef.current);
    searchRef.current = setTimeout(async () => {
      const results = await searchCities(place).catch(() => []);
      setSuggestions(results.slice(0, 6));
    }, 300);
  }, [place, city]);

  const handleSkip = () => {
    setProfileSetupSeen();
    onClose();
  };

  const handleSubmit = async () => {
    if (!dob || !place || !city) {
      setError("Date of birth and place are required.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const body = {
        session_id: userId || "",
        name:       name.trim(),
        date:       dob,
        time:       tob,
        place:      city.name,
        lat:        city.lat,
        lon:        city.lon,
        tz:         city.tz,
        gender,
        language:   "english",
        plan:       "free",
      };
      const res = await apiFetch("/api/user", {
        method:  "POST",
        body:    JSON.stringify(body),
      });
      if (!res.ok) throw new Error("Save failed");

      // Update local store
      setBirthDetails({
        name:        name.trim(),
        dateOfBirth: dob,
        timeOfBirth: tob,
        birthPlace:  city.name,
        lat:         city.lat,
        lon:         city.lon,
        tz:          city.tz,
      });
      setProfileSetupSeen();
      onClose();
    } catch {
      setError("Could not save profile. Try again.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div className="relative bg-background border border-border rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">

        {/* Close */}
        <button
          onClick={handleSkip}
          className="absolute top-4 right-4 p-1.5 rounded-full hover:bg-muted text-muted-foreground"
        >
          <X className="w-4 h-4" />
        </button>

        <div className="p-6 md:p-8">
          {/* Header */}
          <div className="mb-6">
            <p className="text-xs text-amber-700 font-medium tracking-wide uppercase mb-1">One-time setup</p>
            <h2 className="text-xl font-bold text-foreground">Complete Your Birth Profile</h2>
            <p className="text-sm text-muted-foreground mt-1">
              Save once — unlock personalized Vedic insights across every feature.
            </p>
          </div>

          {/* Benefits strip */}
          <div className="grid grid-cols-2 gap-3 mb-6">
            {BENEFITS.map(({ icon: Icon, title, desc }) => (
              <div key={title} className="flex gap-2.5 p-3 rounded-xl bg-amber-50 border border-amber-100">
                <div className="mt-0.5 shrink-0">
                  <Icon className="w-4 h-4 text-amber-700" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-amber-900">{title}</p>
                  <p className="text-xs text-amber-700/80 mt-0.5 leading-relaxed">{desc}</p>
                </div>
              </div>
            ))}
          </div>

          {/* Form */}
          <div className="space-y-4">

            {/* Name */}
            <div>
              <label className="text-xs font-medium text-foreground mb-1 block">Full Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your name"
                className="w-full px-3 py-2.5 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500"
              />
            </div>

            {/* Gender */}
            <div>
              <label className="text-xs font-medium text-foreground mb-1 block">Gender</label>
              <div className="relative">
                <select
                  value={gender}
                  onChange={(e) => setGender(e.target.value)}
                  className="w-full px-3 py-2.5 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500 appearance-none"
                >
                  <option value="">Select gender</option>
                  {GENDERS.map((g) => <option key={g} value={g}>{g}</option>)}
                </select>
                <ChevronDown className="absolute right-3 top-3 w-4 h-4 text-muted-foreground pointer-events-none" />
              </div>
            </div>

            {/* DOB + TOB row */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs font-medium text-foreground mb-1 block">Date of Birth <span className="text-red-500">*</span></label>
                <input
                  type="date"
                  value={dob}
                  onChange={(e) => setDob(e.target.value)}
                  max={new Date().toISOString().split("T")[0]}
                  className="w-full px-3 py-2.5 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-foreground mb-1 block">Time of Birth</label>
                <input
                  type="time"
                  value={tob}
                  onChange={(e) => setTob(e.target.value)}
                  className="w-full px-3 py-2.5 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500"
                />
              </div>
            </div>

            {/* Place of Birth with autocomplete */}
            <div className="relative">
              <label className="text-xs font-medium text-foreground mb-1 block">Place of Birth <span className="text-red-500">*</span></label>
              <input
                type="text"
                value={place}
                onChange={(e) => { setPlace(e.target.value); setCity(null); }}
                placeholder="Search city..."
                className="w-full px-3 py-2.5 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500"
              />
              {city && (
                <p className="text-xs text-green-600 mt-1">
                  ✓ {city.name} — {city.lat.toFixed(2)}°N, {city.lon.toFixed(2)}°E, UTC+{city.tz}
                </p>
              )}
              {suggestions.length > 0 && !city && (
                <div className="absolute z-10 top-full left-0 right-0 mt-1 border border-border rounded-xl bg-background shadow-lg overflow-hidden">
                  {suggestions.map((c) => (
                    <button
                      key={`${c.name}-${c.lat}`}
                      className="w-full text-left px-4 py-2.5 text-sm hover:bg-muted flex items-center gap-2 border-b border-border last:border-0"
                      onClick={() => { setPlace(c.name); setCity(c); setSuggestions([]); }}
                    >
                      <span className="font-medium">{c.name}</span>
                      <span className="text-muted-foreground text-xs">{c.country}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>

            {error && <p className="text-xs text-red-500">{error}</p>}

            {/* Actions */}
            <div className="flex gap-3 pt-2">
              <button
                onClick={handleSkip}
                className="flex-1 px-4 py-2.5 text-sm text-muted-foreground border border-border rounded-lg hover:bg-muted transition-colors"
              >
                Skip for now
              </button>
              <button
                onClick={handleSubmit}
                disabled={saving}
                className="flex-1 px-4 py-2.5 text-sm font-medium text-white bg-orange-600 rounded-lg hover:bg-orange-700 disabled:opacity-60 transition-colors"
              >
                {saving ? "Saving..." : "Save Profile"}
              </button>
            </div>

            <p className="text-xs text-center text-muted-foreground">
              You can always update this later in Profile settings.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
