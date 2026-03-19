import { useState, useEffect, useCallback, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  ChevronLeft, ChevronRight, MapPin, Loader2, CalendarDays,
  Sun, Moon, Star, List, Grid3X3, AlertCircle, Info, Clock,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { useCalendar } from "@/hooks/useCalendar";
import { useGrahan } from "@/hooks/useGrahan";
import { getCities, type City } from "@/lib/cities";
import type { CalendarDayData, FestivalEntry, Eclipse } from "@/types/api";

// ── Constants ────────────────────────────────────────────────────────────────
const WEEK_HEADERS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];
const TRADITIONS = [
  { key: "smarta",    label: "Smarta" },
  { key: "vaishnava", label: "Vaishnava" },
  { key: "shakta",    label: "Shakta" },
  { key: "shaiva",    label: "Shaiva" },
];

// ── Day cell color logic ─────────────────────────────────────────────────────
function dayBg(day: CalendarDayData, eclipse?: Eclipse) {
  // Eclipse gets highest priority — distinct dark-red tint
  if (eclipse)          return "bg-red-950/40 border-red-500/50 hover:bg-red-900/40";
  if (day.has_festival) return "bg-amber-500/10 border-amber-500/25 hover:bg-amber-500/20";
  if (day.is_purnima)   return "bg-blue-500/10 border-blue-500/25 hover:bg-blue-500/20";
  if (day.is_amavasya)  return "bg-slate-600/15 border-slate-500/25 hover:bg-slate-500/20";
  if (day.is_ekadashi)  return "bg-emerald-500/10 border-emerald-500/25 hover:bg-emerald-500/20";
  if (day.is_pradosh)   return "bg-purple-500/10 border-purple-500/25 hover:bg-purple-500/20";
  if (day.is_chaturthi) return "bg-orange-500/8 border-orange-500/20 hover:bg-orange-500/15";
  return "bg-card/30 border-border/20 hover:bg-primary/5";
}

function dayIcon(day: CalendarDayData, eclipse?: Eclipse) {
  if (eclipse)         return eclipse.type.includes("Solar") ? "☀️🌑" : "🌕🌑";
  if (day.is_purnima)  return "🌕";
  if (day.is_amavasya) return "🌑";
  if (day.is_ekadashi) return "✦";
  if (day.is_pradosh)  return "☽";
  return null;
}

function shortTithi(name: string) {
  if (name === "N/A") return "";
  if (name.length <= 6) return name;
  return name.slice(0, 5) + ".";
}

// ── Festival timing helpers ───────────────────────────────────────────────────

/** Build a "best day" explanation for a festival */
function festivalTimingNote(f: FestivalEntry): string | null {
  const lines: string[] = [];

  if (f.tithi_prev_day) {
    lines.push(`Tithi started previous day. Observed today per Udaya Tithi rule (tithi active at sunrise).`);
  }

  if (f.tithi_type === "vridhi") {
    lines.push(`Vriddhi tithi — spans two sunrises. First sunrise day is primary for observation.`);
  } else if (f.tithi_type === "ksheya") {
    lines.push(`Kshaya tithi — tithi skipped a sunrise. Combined with adjacent tithi for observance.`);
  }

  if (f.date_end && f.date_end !== f.date) {
    const d1 = new Date(f.date + "T00:00:00");
    const d2 = new Date(f.date_end + "T00:00:00");
    const span = Math.round((d2.getTime() - d1.getTime()) / 86400000);
    if (span === 1) {
      // Tithi crosses midnight into next day
      lines.push(
        `Tithi crosses midnight into ${f.date_end}. ` +
        `Primary observance: ${f.date} (tithi at sunrise). ` +
        `Tithi active: ${f.tithi_start} – ${f.tithi_end}.`
      );
    }
  }

  return lines.length ? lines.join(" ") : null;
}

/** Format "HH:MM – HH:MM" timing line */
function timingBadge(f: FestivalEntry) {
  if (!f.tithi_start || !f.tithi_end) return null;
  const startDay = f.tithi_start_date && f.tithi_start_date !== f.date
    ? ` (prev day)` : "";
  return `${f.tithi_start}${startDay} – ${f.tithi_end}`;
}

// ── Sub-components ───────────────────────────────────────────────────────────
function InfoRow({ label, value, highlight }: { label: string; value?: string | null; highlight?: boolean }) {
  if (!value || value === "N/A") return null;
  return (
    <div className="flex justify-between items-start py-1.5 border-b border-border/20 last:border-0">
      <span className="text-xs text-muted-foreground w-28 shrink-0">{label}</span>
      <span className={`text-xs text-right font-medium ${highlight ? "text-amber-400" : ""}`}>{value}</span>
    </div>
  );
}

function FestivalCard({ f }: { f: FestivalEntry }) {
  const [open, setOpen] = useState(false);
  const timing = timingBadge(f);
  const note   = festivalTimingNote(f);

  const grahanNotes = f.dosh_notes?.filter(
    n => n.includes("Grahan") || n.includes("grahan") || n.includes("eclipse") || n.includes("Eclipse")
  ) ?? [];
  const otherNotes = f.dosh_notes?.filter(n => !grahanNotes.includes(n)) ?? [];
  const hasGrahan  = grahanNotes.length > 0;
  const hasDosh    = otherNotes.length > 0 || !!note;

  return (
    <div
      className={`rounded-lg border p-3 cursor-pointer transition-colors ${
        hasGrahan
          ? "border-red-500/30 bg-red-950/20 hover:bg-red-950/30"
          : "border-amber-500/20 bg-amber-500/5 hover:bg-amber-500/10"
      }`}
      onClick={() => setOpen(!open)}
    >
      {/* Header row */}
      <div className="flex items-center gap-2">
        <span className="text-xl">{f.icon}</span>
        <div className="flex-1 min-w-0">
          <p className={`text-sm font-semibold ${hasGrahan ? "text-red-300" : "text-amber-300"}`}>{f.name}</p>
          {f.hindi && <p className="text-xs text-muted-foreground">{f.hindi}</p>}
        </div>
        <div className="flex flex-col items-end gap-1 shrink-0">
          <Badge variant="outline" className="text-[10px]">{f.tithi_name}</Badge>
          {hasGrahan && (
            <Badge variant="outline" className="text-[10px] border-red-500/40 text-red-400 gap-0.5">
              🌑 Grahan
            </Badge>
          )}
          {!hasGrahan && hasDosh && (
            <Badge variant="outline" className="text-[10px] border-orange-500/40 text-orange-400 gap-0.5">
              <Info className="h-2.5 w-2.5" /> Rule
            </Badge>
          )}
        </div>
      </div>

      {/* Timing strip */}
      {timing && (
        <div className="mt-2 flex items-center gap-1.5 text-[10px] text-muted-foreground/80">
          <span className="text-primary/60">⏱</span>
          <span>Tithi: <span className="text-foreground/80 font-medium">{timing}</span></span>
        </div>
      )}

      {/* Grahan conflict — always visible */}
      {hasGrahan && (
        <div className="mt-2 space-y-1">
          <p className="text-[10px] font-semibold text-red-400">🌑 Eclipse Conflict &amp; Shift Reason</p>
          {grahanNotes.map((n, i) => (
            <div key={i} className="rounded bg-red-500/8 border border-red-500/20 p-2">
              <p className="text-[11px] text-red-300/90 leading-relaxed">{n}</p>
            </div>
          ))}
        </div>
      )}

      {/* Expanded details */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden"
          >
            <div className="mt-3 space-y-2">
              {f.significance && (
                <p className="text-xs text-muted-foreground leading-relaxed">{f.significance}</p>
              )}

              {/* Timing/rule note */}
              {note && (
                <div className="rounded bg-orange-500/8 border border-orange-500/20 p-2">
                  <p className="text-[11px] text-orange-300/90 leading-relaxed flex gap-1.5">
                    <AlertCircle className="h-3 w-3 mt-0.5 shrink-0" />
                    <span>{note}</span>
                  </p>
                </div>
              )}

              {/* Other dosha notes */}
              {otherNotes.length > 0 && (
                <div className="space-y-1">
                  {otherNotes.map((n, i) => (
                    <div key={i} className="rounded bg-orange-500/8 border border-orange-500/15 p-2">
                      <p className="text-[11px] text-orange-300/90 leading-relaxed">{n}</p>
                    </div>
                  ))}
                </div>
              )}

              {/* Fasting note */}
              {f.fast_note && (
                <div className="rounded bg-primary/5 border border-primary/10 p-2">
                  <p className="text-xs text-primary/80 leading-relaxed">🙏 {f.fast_note}</p>
                </div>
              )}

              {/* Tags */}
              <div className="flex gap-2 flex-wrap mt-1">
                {f.deity && <Badge variant="secondary" className="text-[10px]">⚛ {f.deity}</Badge>}
                {f.month && <Badge variant="secondary" className="text-[10px]">🌙 {f.month}</Badge>}
                {f.paksha && <Badge variant="secondary" className="text-[10px]">{f.paksha} paksha</Badge>}
                {f.tithi_type && f.tithi_type !== "normal" && (
                  <Badge variant="outline" className="text-[10px] border-yellow-500/40 text-yellow-400 capitalize">
                    {f.tithi_type}
                  </Badge>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ── Eclipse section inside DayDetailSheet ─────────────────────────────────────
function EclipseSection({ eclipse }: { eclipse: Eclipse }) {
  const isSolar  = eclipse.type.includes("Solar");
  const hasSutak = eclipse.sutak_hours > 0 && eclipse.sutak_start;
  return (
    <div className="rounded-xl border-2 border-red-500/40 bg-red-950/20 p-4 space-y-3">
      {/* Header */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="text-2xl">{isSolar ? "☀️" : "🌕"}🌑</span>
          <div>
            <p className="text-sm font-bold text-red-300">{eclipse.type} Eclipse</p>
            <p className="text-xs text-muted-foreground">
              {isSolar ? "Surya Grahan" : "Chandra Grahan"} · {eclipse.nakshatra} ({eclipse.rashi})
            </p>
          </div>
        </div>
        {isSolar
          ? <Badge className="text-[10px] bg-yellow-500/15 text-yellow-400 border border-yellow-500/30">Solar</Badge>
          : <Badge className="text-[10px] bg-blue-500/15 text-blue-400 border border-blue-500/30">Lunar</Badge>
        }
      </div>

      {/* Sparsha / Madhya / Moksha */}
      <div className="grid grid-cols-3 gap-1.5 text-center">
        {[
          { label: "Sparsha", sub: "First contact", time: eclipse.sparsha, color: "text-yellow-400" },
          { label: "Madhya",  sub: "Maximum",       time: eclipse.madhya,  color: "text-primary" },
          { label: "Moksha",  sub: "Last contact",  time: eclipse.moksha,  color: "text-green-400" },
        ].map(({ label, sub, time, color }) => (
          <div key={label} className="bg-muted/20 rounded-lg p-2">
            <p className={`text-base font-mono font-bold ${color}`}>{time}</p>
            <p className="text-[10px] font-semibold text-foreground/80">{label}</p>
            <p className="text-[9px] text-muted-foreground">{sub}</p>
          </div>
        ))}
      </div>

      {/* Duration */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground bg-muted/10 rounded-lg px-3 py-1.5">
        <Clock className="h-3 w-3 shrink-0" />
        <span>Duration: <span className="text-foreground font-medium">{eclipse.duration_minutes} min</span></span>
      </div>

      {/* Sutak */}
      {hasSutak ? (
        <div className="bg-red-500/5 border border-red-500/20 rounded-lg px-3 py-2 text-xs text-red-300">
          <p className="font-semibold mb-0.5">⏳ Sutak Kaal ({eclipse.sutak_hours}h before Sparsha)</p>
          <p className="font-mono">
            {eclipse.sutak_start}
            <span className="text-muted-foreground mx-1">→</span>
            {eclipse.sparsha}
          </p>
          <p className="text-[10px] text-muted-foreground/70 mt-1">
            Avoid cooking, eating, major activities. Temples closed.
          </p>
        </div>
      ) : (
        <p className="text-[11px] text-muted-foreground bg-muted/10 rounded-lg px-3 py-1.5">
          ℹ️ No Sutak — penumbral eclipse not observed with Sutak in most traditions.
        </p>
      )}

      {/* Festival conflicts */}
      {eclipse.festival_conflict && eclipse.festival_conflict.length > 0 && (
        <div className="space-y-1">
          <p className="text-[10px] font-semibold text-amber-400">🎪 Nearby Festival Impact</p>
          {eclipse.festival_conflict.map((msg, i) => (
            <div key={i} className="rounded bg-amber-500/8 border border-amber-500/20 p-2">
              <p className="text-[11px] text-amber-300/90 leading-relaxed">{msg}</p>
            </div>
          ))}
        </div>
      )}

      {/* Spiritual effect */}
      <div className="bg-primary/10 border border-primary/20 rounded-lg p-2.5">
        <p className="text-[10px] text-primary/80 mb-0.5">🔮 Spiritual Significance</p>
        <p className="text-xs text-muted-foreground leading-relaxed">{eclipse.spiritual_effect}</p>
      </div>
    </div>
  );
}

function DayDetailSheet({
  day, open, onClose, eclipse,
}: {
  day: CalendarDayData | null;
  open: boolean;
  onClose: () => void;
  eclipse?: Eclipse;
}) {
  if (!day) return null;

  const specialBadges = [
    eclipse && <Badge key="gr" className="bg-red-500/20 text-red-300 border-red-500/30">🌑 {eclipse.type} Eclipse</Badge>,
    day.is_purnima   && <Badge key="p" className="bg-blue-500/20 text-blue-300 border-blue-500/30">🌕 Purnima</Badge>,
    day.is_amavasya  && <Badge key="a" className="bg-slate-500/20 text-slate-300 border-slate-500/30">🌑 Amavasya</Badge>,
    day.is_ekadashi  && <Badge key="e" className="bg-emerald-500/20 text-emerald-300 border-emerald-500/30">✦ Ekadashi</Badge>,
    day.is_pradosh   && <Badge key="pr" className="bg-purple-500/20 text-purple-300 border-purple-500/30">☽ Pradosh</Badge>,
    day.is_chaturthi && <Badge key="c" className="bg-orange-500/20 text-orange-300 border-orange-500/30">● Chaturthi</Badge>,
    day.is_ashtami   && <Badge key="as" className="bg-rose-500/20 text-rose-300 border-rose-500/30">◉ Ashtami</Badge>,
  ].filter(Boolean);

  return (
    <Sheet open={open} onOpenChange={onClose}>
      <SheetContent side="right" className="w-full sm:w-[480px] overflow-y-auto">
        <SheetHeader className="pb-3">
          <SheetTitle className="flex items-center gap-2 text-primary">
            <CalendarDays className="h-5 w-5" />
            {day.date}
          </SheetTitle>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span>{day.vara}</span>
            <span>·</span>
            <span>{day.weekday}</span>
            {day.lunar_month && (
              <>
                <span>·</span>
                <span className="text-primary/70">🌙 {day.lunar_month}</span>
              </>
            )}
          </div>
        </SheetHeader>

        <div className="space-y-5 pb-8">
          {specialBadges.length > 0 && (
            <div className="flex flex-wrap gap-1.5">{specialBadges}</div>
          )}

          {/* Panchang grid */}
          <div className="rounded-xl border border-border/30 bg-card/50 p-4 space-y-0">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-widest mb-2">
              Panchang
            </p>
            <InfoRow label="Vara (Day)"    value={day.vara} />
            <InfoRow label="Tithi"         value={`${day.paksha} ${day.tithi} (${day.paksha_short}${day.tithi_num})`} />
            <InfoRow label="Nakshatra"     value={day.nakshatra} />
            <InfoRow label="Yoga"          value={day.yoga} />
            <InfoRow label="Lunar Month"   value={day.lunar_month} />
            <InfoRow label="🌅 Sunrise"    value={day.sunrise} />
            <InfoRow label="🌇 Sunset"     value={day.sunset} />
            <InfoRow label="☢ Rahu Kaal"   value={day.rahu_kaal} highlight />
          </div>

          {/* Eclipse — shown before festivals if present */}
          {eclipse && (
            <div className="space-y-2">
              <p className="text-xs font-semibold text-red-400/80 uppercase tracking-widest">
                Grahan (Eclipse)
              </p>
              <EclipseSection eclipse={eclipse} />
            </div>
          )}

          {/* Festivals */}
          {day.festivals.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-widest">
                Festivals & Observances
              </p>
              {day.festivals.map((f, i) => (
                <FestivalCard key={i} f={f as FestivalEntry} />
              ))}
            </div>
          )}

          {day.festivals.length === 0 && (
            <div className="rounded-lg border border-dashed border-border/30 p-4 text-center">
              <p className="text-xs text-muted-foreground">No major festivals today</p>
            </div>
          )}

          {day.error && (
            <div className="rounded bg-red-500/10 border border-red-500/20 p-3">
              <p className="text-xs text-red-400">{day.error}</p>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}

function DayCell({ day, onClick, eclipse }: { day: CalendarDayData; onClick: () => void; eclipse?: Eclipse }) {
  const icon = dayIcon(day, eclipse);
  const bg   = dayBg(day, eclipse);
  const todayRing = day.is_today ? "ring-2 ring-primary ring-offset-1 ring-offset-background" : "";

  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.97 }}
      onClick={onClick}
      className={`relative rounded-lg border cursor-pointer transition-colors p-1 sm:p-1.5 min-h-[62px] sm:min-h-[72px] md:min-h-[88px] select-none ${bg} ${todayRing}`}
    >
      <div className="flex items-start justify-between">
        <span className={`text-sm font-bold leading-none ${day.is_today ? "text-primary" : eclipse ? "text-red-300" : ""}`}>
          {day.day}
        </span>
        {icon && <span className="text-[11px] leading-none opacity-80">{icon}</span>}
      </div>

      <p className="text-[9px] text-muted-foreground/70 mt-0.5 leading-tight">
        {day.paksha_short}{day.tithi_num} {shortTithi(day.tithi)}
      </p>

      {/* Eclipse label */}
      {eclipse && (
        <p className="text-[8px] text-red-400 leading-tight font-semibold truncate mt-0.5">
          {eclipse.type.includes("Solar") ? "Surya" : "Chandra"} Grahan
        </p>
      )}

      <div className="mt-0.5 space-y-0.5">
        {day.festivals.slice(0, eclipse ? 1 : 2).map((f, i) => (
          <p key={i} className="text-[9px] text-amber-400 leading-tight truncate">
            {f.icon} {f.name.split(" ").slice(0, 2).join(" ")}
          </p>
        ))}
        {day.festivals.length > (eclipse ? 1 : 2) && (
          <p className="text-[9px] text-muted-foreground/50">+{day.festivals.length - (eclipse ? 1 : 2)} more</p>
        )}
      </div>
    </motion.div>
  );
}

/** Compact list-view row */
function DayListRow({ day, onClick, eclipse }: { day: CalendarDayData; onClick: () => void; eclipse?: Eclipse }) {
  const todayBg = day.is_today
    ? "bg-primary/10 border-primary/30"
    : eclipse
    ? "bg-red-950/25 border-red-500/35 hover:bg-red-950/35"
    : "bg-card/20 border-border/15 hover:bg-card/40";
  return (
    <motion.div
      whileTap={{ scale: 0.99 }}
      onClick={onClick}
      className={`flex items-start gap-3 rounded-lg border cursor-pointer transition-colors p-3 ${todayBg}`}
    >
      {/* Date block */}
      <div className="w-10 text-center shrink-0">
        <p className={`text-lg font-bold leading-none ${day.is_today ? "text-primary" : eclipse ? "text-red-300" : ""}`}>{day.day}</p>
        <p className="text-[10px] text-muted-foreground/60">{day.weekday}</p>
      </div>

      <Separator orientation="vertical" className="h-10 self-center" />

      {/* Panchang summary */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="text-[11px] font-medium text-muted-foreground">
            {day.paksha_short}{day.tithi_num} · {day.tithi}
          </span>
          {eclipse    && <span className="text-[10px] text-red-400 font-semibold">🌑 {eclipse.type} Eclipse</span>}
          {day.is_purnima   && <span className="text-[10px] text-blue-400">🌕</span>}
          {day.is_amavasya  && <span className="text-[10px] text-slate-400">🌑</span>}
          {day.is_ekadashi  && <span className="text-[10px] text-emerald-400">✦ Ekadashi</span>}
          {day.is_pradosh   && <span className="text-[10px] text-purple-400">☽ Pradosh</span>}
        </div>
        <p className="text-[10px] text-muted-foreground/60 mt-0.5">
          🌟 {day.nakshatra} · ☀ {day.sunrise}
          {day.rahu_kaal && day.rahu_kaal !== "N/A" && (
            <span className="ml-2 text-orange-400/80">☢ {day.rahu_kaal}</span>
          )}
          {eclipse && (
            <span className="ml-2 text-red-400/80">Sutak: {eclipse.sutak_start ?? "none"}</span>
          )}
        </p>
        {day.festivals.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1.5">
            {day.festivals.slice(0, 3).map((f, i) => (
              <span key={i} className="text-[10px] text-amber-400 bg-amber-500/10 rounded px-1.5 py-0.5">
                {f.icon} {f.name}
              </span>
            ))}
            {day.festivals.length > 3 && (
              <span className="text-[10px] text-muted-foreground/50">+{day.festivals.length - 3}</span>
            )}
          </div>
        )}
      </div>
    </motion.div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function CalendarPage() {
  const now = new Date();
  const [year,       setYear]       = useState(now.getFullYear());
  const [month,      setMonth]      = useState(now.getMonth() + 1);
  const [tradition,  setTradition]  = useState("smarta");
  const [lunarSystem,setLunarSystem]= useState("amanta");
  const [viewMode,   setViewMode]   = useState<"grid" | "list">("grid");
  const [cities,     setCities]     = useState<City[]>([]);
  const [selectedCity, setSelectedCity] = useState<City>({
    name: "New Delhi", lat: 28.6139, lon: 77.209, tz: 5.5,
  });
  const [citySearch, setCitySearch]           = useState("New Delhi");
  const [citySuggestions, setCitySuggestions] = useState<City[]>([]);
  const [selectedDay, setSelectedDay]         = useState<CalendarDayData | null>(null);
  const [yearInput,  setYearInput]            = useState(String(now.getFullYear()));

  useEffect(() => { getCities().then(setCities).catch(() => {}); }, []);

  const { data, isLoading, isError } = useCalendar({
    year, month,
    lat: selectedCity.lat, lon: selectedCity.lon, tz: selectedCity.tz,
    tradition, lunar_system: lunarSystem,
  });

  const { data: grahanData } = useGrahan(year, selectedCity.tz);

  // Build date → Eclipse lookup for this year
  const eclipseMap = useMemo<Map<string, Eclipse>>(() => {
    const m = new Map<string, Eclipse>();
    grahanData?.eclipses.forEach(e => m.set(e.date, e));
    return m;
  }, [grahanData]);

  // Navigation
  const prevMonth = useCallback(() => {
    if (month === 1) { setYear(y => y - 1); setMonth(12); }
    else setMonth(m => m - 1);
  }, [month]);

  const nextMonth = useCallback(() => {
    if (month === 12) { setYear(y => y + 1); setMonth(1); }
    else setMonth(m => m + 1);
  }, [month]);

  const goToday = () => {
    const t = new Date();
    setYear(t.getFullYear());
    setMonth(t.getMonth() + 1);
    setYearInput(String(t.getFullYear()));
  };

  const handleYearCommit = () => {
    const y = parseInt(yearInput);
    if (!isNaN(y) && y >= 1800 && y <= 2200) setYear(y);
    else setYearInput(String(year));
  };

  const handleCitySearch = (v: string) => {
    setCitySearch(v);
    if (v.length < 2) { setCitySuggestions([]); return; }
    setCitySuggestions(
      cities.filter(c => c.name.toLowerCase().includes(v.toLowerCase())).slice(0, 8)
    );
  };

  const firstWeekday = data?.first_weekday ?? 0;
  const totalCells   = 42;
  const days         = data?.days ?? [];

  const vikramSamvat  = data?.vikram_samvat;
  const lunarMonths   = data?.lunar_months ?? [];
  const adhikNote     = data?.adhik_maas_note;
  const kshayaNote    = data?.kshaya_maas_note;

  return (
    <div className="space-y-3 pb-2">
      {/* ── Header ── */}
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center justify-between flex-wrap gap-2">
          <p className="text-xs text-muted-foreground/70">Any year · Any city · 1800–2200</p>
          {vikramSamvat && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground">Vikram Samvat</span>
              <span className="text-sm font-bold text-primary/80">{vikramSamvat}</span>
            </div>
          )}
        </div>

        {/* Lunar month names banner */}
        {lunarMonths.length > 0 && (
          <div className="flex items-center gap-2 mt-2 flex-wrap">
            <span className="text-xs text-muted-foreground/70">🌙 Lunar month{lunarMonths.length > 1 ? "s" : ""}:</span>
            {lunarMonths.map(m => (
              <Badge key={m} variant="outline" className="text-[11px] text-primary/80 border-primary/20">{m}</Badge>
            ))}
          </div>
        )}

        {/* Adhik / Kshaya banners */}
        {adhikNote && (
          <motion.div
            initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }}
            className="mt-2 rounded-lg border border-amber-500/30 bg-amber-500/8 px-4 py-2 text-xs text-amber-300 flex items-center gap-2"
          >
            <Star className="h-3.5 w-3.5 shrink-0" />
            <span><strong>Adhik Maas {year}:</strong> {adhikNote}</span>
          </motion.div>
        )}
        {kshayaNote && (
          <motion.div
            initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }}
            className="mt-1 rounded-lg border border-rose-500/30 bg-rose-500/8 px-4 py-2 text-xs text-rose-300 flex items-center gap-2"
          >
            <AlertCircle className="h-3.5 w-3.5 shrink-0" />
            <span><strong>Kshaya Maas {year}:</strong> {kshayaNote}</span>
          </motion.div>
        )}
      </motion.div>

      {/* ── Controls ── */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="flex flex-wrap items-center gap-1.5 sm:gap-2"
      >
        {/* Month nav */}
        <div className="flex items-center gap-1 bg-card/60 rounded-lg border border-border/30 p-1">
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={prevMonth}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <span className="text-sm font-semibold min-w-[110px] text-center">
            {MONTH_NAMES[month - 1]}
          </span>
          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={nextMonth}>
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>

        {/* Year */}
        <input
          className="w-20 h-9 rounded-md border border-border/30 bg-card/60 px-2 text-sm text-center font-semibold focus:outline-none focus:ring-1 focus:ring-primary"
          value={yearInput}
          onChange={e => setYearInput(e.target.value)}
          onBlur={handleYearCommit}
          onKeyDown={e => e.key === "Enter" && handleYearCommit()}
          aria-label="Year"
        />

        {/* Today */}
        <Button variant="outline" size="sm" onClick={goToday} className="h-9 text-xs">
          Today
        </Button>

        <Separator orientation="vertical" className="h-8 hidden sm:block" />

        {/* City search */}
        <div className="relative">
          <div className="flex items-center gap-1.5 h-9 rounded-md border border-border/30 bg-card/60 px-2">
            <MapPin className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
            <input
              className="bg-transparent text-sm w-32 focus:outline-none"
              value={citySearch}
              placeholder="City..."
              onChange={e => handleCitySearch(e.target.value)}
            />
          </div>
          {citySuggestions.length > 0 && (
            <div className="absolute top-full left-0 mt-1 z-50 w-52 rounded-lg border border-border/30 bg-background shadow-lg overflow-hidden">
              {citySuggestions.map(c => (
                <button
                  key={c.name}
                  className="w-full text-left px-3 py-2 text-xs hover:bg-primary/10 transition-colors border-b border-border/10 last:border-0"
                  onClick={() => {
                    setSelectedCity(c);
                    setCitySearch(c.name);
                    setCitySuggestions([]);
                  }}
                >
                  {c.name}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Tradition */}
        <Select value={tradition} onValueChange={setTradition}>
          <SelectTrigger className="h-9 w-32 text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {TRADITIONS.map(t => (
              <SelectItem key={t.key} value={t.key} className="text-xs">{t.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* Lunar system */}
        <Select value={lunarSystem} onValueChange={setLunarSystem}>
          <SelectTrigger className="h-9 w-36 text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="amanta" className="text-xs">Amanta (South/Deccan)</SelectItem>
            <SelectItem value="purnimanta" className="text-xs">Purnimanta (North India)</SelectItem>
          </SelectContent>
        </Select>

        <Separator orientation="vertical" className="h-8 hidden sm:block" />

        {/* View toggle */}
        <div className="flex items-center gap-0.5 rounded-lg border border-border/30 bg-card/60 p-1">
          <Button
            variant={viewMode === "grid" ? "secondary" : "ghost"}
            size="icon" className="h-7 w-7"
            onClick={() => setViewMode("grid")}
            title="Grid view"
          >
            <Grid3X3 className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant={viewMode === "list" ? "secondary" : "ghost"}
            size="icon" className="h-7 w-7"
            onClick={() => setViewMode("list")}
            title="List view"
          >
            <List className="h-3.5 w-3.5" />
          </Button>
        </div>
      </motion.div>

      {/* ── Calendar ── */}
      <motion.div
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
        className="rounded-xl border border-border/30 bg-card/20 overflow-hidden"
      >
        {/* Loading */}
        {isLoading && (
          <div className="flex items-center justify-center py-24">
            <Loader2 className="h-8 w-8 animate-spin text-primary/50" />
            <p className="ml-3 text-sm text-muted-foreground">Calculating panchang...</p>
          </div>
        )}

        {/* Error */}
        {isError && (
          <div className="flex items-center justify-center py-16 text-center">
            <div>
              <p className="text-muted-foreground text-sm">Could not load calendar</p>
              <p className="text-xs text-muted-foreground/60 mt-1">Check API server connection</p>
            </div>
          </div>
        )}

        {/* Grid view */}
        {data && !isLoading && viewMode === "grid" && (
          <>
            {/* Weekday headers */}
            <div className="grid grid-cols-7 border-b border-border/20">
              {WEEK_HEADERS.map((h, i) => (
                <div
                  key={h}
                  className={`py-2 text-center text-xs font-semibold uppercase tracking-wider ${
                    i === 0 || i === 6 ? "text-primary/60" : "text-muted-foreground/60"
                  }`}
                >
                  {h}
                </div>
              ))}
            </div>
            <div className="grid grid-cols-7 gap-px p-1.5 bg-border/10">
              {Array.from({ length: firstWeekday }).map((_, i) => (
                <div key={`empty-${i}`} className="min-h-[62px] sm:min-h-[72px] md:min-h-[88px]" />
              ))}
              {days.map(day => (
                <DayCell key={day.date} day={day} onClick={() => setSelectedDay(day)} eclipse={eclipseMap.get(day.date)} />
              ))}
              {Array.from({ length: Math.max(0, totalCells - firstWeekday - days.length) }).map((_, i) => (
                <div key={`trail-${i}`} className="min-h-[62px] sm:min-h-[72px] md:min-h-[88px]" />
              ))}
            </div>
          </>
        )}

        {/* List view */}
        {data && !isLoading && viewMode === "list" && (
          <div className="p-3 space-y-1.5">
            {days.map(day => (
              <DayListRow key={day.date} day={day} onClick={() => setSelectedDay(day)} eclipse={eclipseMap.get(day.date)} />
            ))}
          </div>
        )}
      </motion.div>

      {/* ── Legend (grid only) ── */}
      {viewMode === "grid" && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-muted-foreground"
        >
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-red-950/60 border-2 border-red-500/50 inline-block" />
            🌑 Grahan
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-amber-500/30 border border-amber-500/40 inline-block" />
            Festival
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-blue-500/30 border border-blue-500/40 inline-block" />
            🌕 Purnima
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-slate-600/30 border border-slate-500/40 inline-block" />
            🌑 Amavasya
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-emerald-500/30 border border-emerald-500/40 inline-block" />
            ✦ Ekadashi
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded bg-purple-500/30 border border-purple-500/40 inline-block" />
            ☽ Pradosh
          </span>
          <span className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded ring-2 ring-primary inline-block" />
            Today
          </span>
        </motion.div>
      )}

      {/* ── Stats bar ── */}
      {data && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.35 }}
          className="flex flex-wrap gap-2"
        >
          {[
            { icon: <CalendarDays className="h-3.5 w-3.5" />, label: "Tradition",    value: data.tradition },
            { icon: <Moon className="h-3.5 w-3.5" />,         label: "System",       value: data.lunar_system === "amanta" ? "Amanta" : "Purnimanta" },
            { icon: <Star className="h-3.5 w-3.5" />,         label: "Festivals",    value: `${data.days.filter(d => d.has_festival).length} days` },
            { icon: <Sun className="h-3.5 w-3.5" />,          label: "Purnima",      value: (() => { const p = data.days.find(d => d.is_purnima); return p ? `${p.day}th` : "—"; })() },
            ...(vikramSamvat ? [{ icon: <Star className="h-3.5 w-3.5" />, label: "V.S.", value: String(vikramSamvat) }] : []),
          ].map(item => (
            <div
              key={item.label}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-border/20 bg-card/30 text-xs"
            >
              <span className="text-muted-foreground">{item.icon}</span>
              <span className="text-muted-foreground">{item.label}:</span>
              <span className="font-medium capitalize">{item.value}</span>
            </div>
          ))}
        </motion.div>
      )}

      {/* ── Day detail sheet ── */}
      <DayDetailSheet
        day={selectedDay}
        open={!!selectedDay}
        onClose={() => setSelectedDay(null)}
        eclipse={selectedDay ? eclipseMap.get(selectedDay.date) : undefined}
      />
    </div>
  );
}
