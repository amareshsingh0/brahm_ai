import { useState } from "react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Loader2, Clock, CalendarDays, AlertTriangle, Info, Moon, Sun, ChevronDown } from "lucide-react";
import { useGrahan } from "@/hooks/useGrahan";
import { useFestivals } from "@/hooks/useFestivals";
import { useKundliStore } from "@/store/kundliStore";
import type { FestivalEntry, Eclipse } from "@/types/api";
import CalendarPage from "./CalendarPage";

// ── Rules reference modal content ────────────────────────────────────────────
const SUTAK_RULES = [
  {
    title: "Sutak Kaal (सूतक काल)",
    body: "Sutak is the period of ritual impurity before a Grahan (eclipse). During Sutak, cooked food becomes impure, temples are closed, and major auspicious activities are avoided.",
    items: [
      "Solar eclipse (Surya Grahan): Sutak starts 12 hours before first contact",
      "Lunar eclipse (Chandra Grahan): Sutak starts 9 hours before first contact",
      "Penumbral lunar eclipse: No Sutak per most traditions",
      "Children, elderly, sick, and pregnant women are exempt from Sutak restrictions",
    ],
  },
  {
    title: "During Grahan (Eclipse Phase)",
    body: "From Sparsha (first contact) to Moksha (last contact) — the main eclipse window.",
    items: [
      "Avoid eating or drinking (fast during eclipse)",
      "Avoid sleeping, urination, sexual activity",
      "Chant mantras, do japa — amplified effect during eclipse",
      "Avoid looking at the Sun during Solar Grahan (use certified solar filters)",
      "Add Tulsi/Kusha grass to stored water and food — keeps it pure",
    ],
  },
  {
    title: "After Moksha (Grahan Ends)",
    body: "Once the last contact (Moksha) is complete, purification begins.",
    items: [
      "Take a bath (Snan) immediately after Moksha",
      "Sprinkle Ganga jal in the house",
      "Recook or replace food prepared during Sutak",
      "Light a ghee lamp and offer prayers",
      "Charity (Daan) — grain, cloth, sesame, gold — yields 10× merit after eclipse",
    ],
  },
  {
    title: "Grahan Dosha",
    body: "A Grahan falling on a major festival tithi creates a conflict requiring specific resolution.",
    items: [
      "Holika Dahan on Purnima eclipse: Perform before Sutak, or at next Pradosh after Moksha",
      "Raksha Bandhan eclipse: Tie Rakhi only before Sutak or after Moksha + bath",
      "Guru Purnima eclipse: Pada Puja before Sutak; Guru Vandana after purification",
      "Eclipse on Amavasya: Pitra Tarpan still valid — eclipse enhances merit",
    ],
  },
  {
    title: "Mantra for Grahan",
    body: "Universal protective mantras during any eclipse:",
    items: [
      "Surya Grahan: Aditya Hridaya Stotra, Gayatri Mantra (108×), Om Hraam Hreem Hraum Sah Suryaya Namah",
      "Chandra Grahan: Vishnu Sahasranama, Om Som Somaya Namah (108×)",
      "Any Grahan: Mahamrityunjaya Mantra, Om Namah Shivaya",
    ],
  },
];

// ── Vedic guidance per eclipse type ──────────────────────────────────────────
const ECLIPSE_GUIDANCE: Record<string, string[]> = {
  "Total Solar":   ["Chant Surya mantra 108 times", "Fast during eclipse", "Donate wheat and copper", "Perform Surya Tarpan after Moksha"],
  "Annular Solar": ["Recite Aditya Hridaya Stotra", "Avoid new ventures", "Bathe before and after", "Donate to charity"],
  "Partial Solar": ["Observe silence", "Chant Gayatri Mantra", "Avoid cooking", "Sprinkle Ganga jal"],
  "Total Lunar":   ["Chant Vishnu Sahasranama", "Meditate near water", "Avoid eating during Sutak", "Take bath after eclipse"],
  "Partial Lunar": ["Reflect and introspect", "Light a ghee lamp", "Recite mantras", "Recite Vishnu Sahasranama"],
  "Penumbral":     ["Heightened meditation time", "Avoid major decisions", "Chant mantras", "Sprinkle holy water"],
};

// ── Helpers ──────────────────────────────────────────────────────────────────
function formatDate(iso: string): string {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString("en-IN", { day: "numeric", month: "long", year: "numeric" });
}

function formatDateRange(start: string, end: string | null): string {
  const s = formatDate(start);
  if (!end || end === start) return s;
  const [sy, sm, sd] = start.split("-").map(Number);
  const [ey, em, ed] = end.split("-").map(Number);
  const daysDiff = Math.round((new Date(ey, em - 1, ed).getTime() - new Date(sy, sm - 1, sd).getTime()) / 86400000);
  if (daysDiff <= 1) return s; // single-day festivals: tithi crosses midnight but it's still one day
  const eDate = new Date(ey, em - 1, ed).toLocaleDateString("en-IN", { day: "numeric", month: "long" });
  return `${s} – ${eDate}`;
}

/** "HH:MM" → "5:56 PM" */
function fmt12(t: string): string {
  if (!t || t === "N/A") return t;
  const [h, m] = t.split(":").map(Number);
  const ampm = h >= 12 ? "PM" : "AM";
  return `${h % 12 || 12}:${m.toString().padStart(2, "0")} ${ampm}`;
}

/** "2026-03-03" → "3 Mar" */
function shortDate(iso: string): string {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString("en-IN", { day: "numeric", month: "short" });
}

/** Add N calendar days to YYYY-MM-DD — uses local Date to avoid UTC/IST timezone shift */
function shiftDate(iso: string, n: number): string {
  const [y, m, d] = iso.split("-").map(Number);
  const nd = new Date(y, m - 1, d + n);
  return `${nd.getFullYear()}-${String(nd.getMonth() + 1).padStart(2, "0")}-${String(nd.getDate()).padStart(2, "0")}`;
}

/**
 * Returns the full tithi window as a human-readable string.
 * e.g. "Mar 3, 5:56 PM → Mar 4, 5:07 PM"  or  "5:12 AM – 8:45 PM"  or  "All day"
 */
function tithiWindow(f: FestivalEntry): string | null {
  const { tithi_start: ts, tithi_end: te, tithi_name, tithi_prev_day, date, date_end, tithi_start_date } = f;
  if (!ts || ts === "N/A") return null;
  // All-day (Holi etc.)
  if (ts === "00:00" && te === "23:59") return "All day";
  // Sankranti: single point-in-time entry
  if (tithi_name === "Sankranti" || ts === te) return `Entry at ${fmt12(ts)}`;

  // Use exact tithi_start_date from backend if available, otherwise fall back to shiftDate
  const startIso = tithi_start_date ?? (tithi_prev_day ? shiftDate(date, -1) : date);
  let endIso: string;
  if (date_end && date_end !== date) {
    // Multi-day festival (Navratri) — end is on date_end
    endIso = date_end;
  } else if (te < ts) {
    // Tithi crosses midnight → ends next calendar day
    endIso = shiftDate(date, 1);
  } else {
    endIso = date;
  }

  if (startIso === endIso) {
    // Same calendar day: just show times
    return `${fmt12(ts)} – ${fmt12(te)}`;
  }
  // Different days: show "Mar 3, 5:56 PM → Mar 4, 5:07 PM"
  return `${shortDate(startIso)}, ${fmt12(ts)} → ${shortDate(endIso)}, ${fmt12(te)}`;
}

function eclipseIcon(type: string) {
  return type.includes("Solar") ? "☀️" : "🌕";
}

function TithiTypeBadge({ type }: { type: string }) {
  if (type === "vridhi")
    return <Badge className="text-[9px] bg-blue-500/10 text-blue-400 border border-blue-500/20">Vridhi</Badge>;
  if (type === "ksheya")
    return <Badge className="text-[9px] bg-orange-500/10 text-orange-400 border border-orange-500/20">Ksheya</Badge>;
  return null;
}

// ── Festival Card ─────────────────────────────────────────────────────────────
function FestivalCard({ festival, index }: { festival: FestivalEntry; index: number }) {
  const [expanded, setExpanded] = useState(false);
  const hasNotes   = festival.dosh_notes && festival.dosh_notes.length > 0;
  const grahanNotes = hasNotes ? festival.dosh_notes.filter(n => n.includes("Grahan") || n.includes("grahan") || n.includes("eclipse") || n.includes("Eclipse")) : [];
  const otherNotes  = hasNotes ? festival.dosh_notes.filter(n => !grahanNotes.includes(n)) : [];
  const hasGrahan  = grahanNotes.length > 0;
  const window     = tithiWindow(festival);
  const showTithiLabel = festival.tithi_name !== "N/A"
    && festival.tithi_name !== "Full day"
    && festival.tithi_name !== "Sankranti";

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: index * 0.03 }}
    >
      <Card
        className={`glass border-border/30 h-full hover:border-primary/40 transition-colors ${
          hasGrahan ? "border-red-500/30" : hasNotes ? "border-amber-500/20" : ""
        }`}
      >
        <CardContent className="pt-5 space-y-2">
          {/* Header row */}
          <div className="flex items-center justify-between gap-2">
            <span className="text-3xl">{festival.icon}</span>
            <div className="flex items-center gap-1 flex-wrap justify-end">
              <TithiTypeBadge type={festival.tithi_type} />
              {festival.fast_note && (
                <Badge className="text-[9px] bg-orange-500/10 text-orange-300 border border-orange-500/20">🕐 Vrat</Badge>
              )}
              {hasGrahan && <Badge className="text-[9px] bg-red-500/10 text-red-400 border border-red-500/20">⚠ Grahan</Badge>}
              {hasNotes && !hasGrahan && <AlertTriangle className="h-3 w-3 text-amber-400" />}
            </div>
          </div>

          <h3 className="font-display text-sm">{festival.name}</h3>
          <p className="text-xs text-muted-foreground">{festival.hindi} · {festival.deity}</p>

          {/* Date + tithi window block */}
          <div className="bg-muted/20 rounded-lg p-2 space-y-1">
            <div className="flex items-center gap-1.5 text-xs">
              <CalendarDays className="h-3 w-3 text-primary/60 shrink-0" />
              <span className="font-medium">{formatDateRange(festival.date, festival.date_end)}</span>
              {festival.paksha !== "N/A" && (
                <span className="text-muted-foreground/60 ml-0.5">· {festival.paksha}</span>
              )}
            </div>
            {window && (
              <div className="flex items-start gap-1.5 text-xs text-muted-foreground">
                <Clock className="h-3 w-3 mt-0.5 shrink-0" />
                <span>
                  {showTithiLabel && (
                    <span className="text-foreground/50 mr-1">{festival.tithi_name} ·</span>
                  )}
                  {window}
                </span>
              </div>
            )}
          </div>

          {/* Fasting note */}
          {festival.fast_note && (
            <div className="bg-orange-500/5 border border-orange-500/15 rounded-lg p-2 text-xs text-orange-200/80 leading-relaxed">
              <span className="font-semibold text-orange-300">🕐 Vrat/Fast — </span>
              {festival.fast_note}
            </div>
          )}

          {/* ── Grahan conflict — ALWAYS visible ───────────────────────── */}
          {hasGrahan && (
            <div className="space-y-1">
              <p className="text-[10px] font-semibold text-red-400 flex items-center gap-1">
                🌑 Eclipse Conflict &amp; Shift Reason
              </p>
              {grahanNotes.map((note, i) => (
                <div key={i} className="bg-red-500/5 border border-red-500/25 rounded-lg p-2 text-xs text-red-300 leading-relaxed">
                  {note}
                </div>
              ))}
            </div>
          )}

          <p className="text-xs text-muted-foreground leading-relaxed">{festival.significance}</p>

          {/* Other Vedic notes — collapsible */}
          {otherNotes.length > 0 && (
            <>
              <button
                className="flex items-center gap-1 text-[10px] text-amber-400/70 hover:text-amber-400 transition-colors"
                onClick={() => setExpanded(v => !v)}
              >
                <ChevronDown className={`h-3 w-3 transition-transform ${expanded ? "rotate-180" : ""}`} />
                {expanded ? "hide" : "show"} Vedic notes
              </button>
              {expanded && (
                <div className="space-y-1">
                  {otherNotes.map((note, i) => {
                    const isBhadra = note.includes("Bhadra") || note.includes("bhadra");
                    const isOk    = note.startsWith("✅");
                    return (
                      <div
                        key={i}
                        className={`rounded-lg p-2 text-xs leading-relaxed border ${
                          isBhadra && !isOk ? "bg-amber-500/5 border-amber-500/20 text-amber-300" :
                          isOk ? "bg-green-500/5 border-green-500/20 text-green-300" :
                          "bg-muted/20 border-border/20 text-muted-foreground"
                        }`}
                      >
                        {note}
                      </div>
                    );
                  })}
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </motion.div>
  );
}

// ── Eclipse Inline Card (shown in festival grid on eclipse date) ───────────────
function EclipseInlineCard({ eclipse, index }: { eclipse: Eclipse; index: number }) {
  const [expanded, setExpanded] = useState(false);
  const isSolar = eclipse.type.includes("Solar");
  const hasConflict = eclipse.festival_conflict && eclipse.festival_conflict.length > 0;
  const hasSutak = eclipse.sutak_hours > 0 && eclipse.sutak_start;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: index * 0.03 }}
    >
      <Card className={`glass h-full border-2 ${isSolar ? "border-yellow-500/40" : "border-blue-500/40"} relative overflow-hidden`}>
        {/* Glow strip at top */}
        <div className={`absolute top-0 left-0 right-0 h-1 ${isSolar ? "bg-gradient-to-r from-yellow-500/60 to-orange-500/60" : "bg-gradient-to-r from-blue-500/60 to-purple-500/60"}`} />

        <CardContent className="pt-5 space-y-3">
          {/* Header */}
          <div className="flex items-center justify-between gap-2">
            <span className="text-3xl">{isSolar ? "☀️" : "🌕"}</span>
            <div className="flex items-center gap-1 flex-wrap justify-end">
              {isSolar
                ? <Badge className="text-[9px] bg-yellow-500/15 text-yellow-400 border border-yellow-500/30">Solar Eclipse</Badge>
                : <Badge className="text-[9px] bg-blue-500/15 text-blue-400 border border-blue-500/30">Lunar Eclipse</Badge>
              }
              {hasConflict && (
                <Badge className="text-[9px] bg-red-500/15 text-red-400 border border-red-500/30">Festival Alert</Badge>
              )}
            </div>
          </div>

          <div>
            <h3 className="font-display text-sm">{eclipse.type} Eclipse</h3>
            <p className="text-xs text-muted-foreground mt-0.5">
              {isSolar ? "Surya Grahan" : "Chandra Grahan"} · {eclipse.nakshatra} ({eclipse.rashi})
            </p>
          </div>

          {/* Date */}
          <div className="bg-muted/20 rounded-lg p-2 space-y-1">
            <div className="flex items-center gap-1.5 text-xs">
              <CalendarDays className="h-3 w-3 text-primary/60 shrink-0" />
              <span className="font-medium">{formatDate(eclipse.date)}</span>
            </div>
          </div>

          {/* Sparsha / Madhya / Moksha compact */}
          <div className="grid grid-cols-3 gap-1.5 text-center">
            {[
              { label: "Sparsha", time: eclipse.sparsha, color: "text-yellow-400" },
              { label: "Madhya",  time: eclipse.madhya,  color: "text-primary" },
              { label: "Moksha",  time: eclipse.moksha,  color: "text-green-400" },
            ].map(({ label, time, color }) => (
              <div key={label} className="bg-muted/20 rounded-md p-1.5">
                <p className={`text-xs font-mono font-bold ${color}`}>{time}</p>
                <p className="text-[9px] text-muted-foreground mt-0.5">{label}</p>
              </div>
            ))}
          </div>

          {/* Sutak strip */}
          {hasSutak ? (
            <div className="bg-red-500/5 border border-red-500/20 rounded-lg px-2.5 py-2 text-xs text-red-300">
              <span className="font-semibold">⏳ Sutak:</span>{" "}
              <span className="font-mono">{eclipse.sutak_start}</span>
              <span className="text-muted-foreground mx-1">→</span>
              <span className="font-mono">{eclipse.sparsha}</span>
              <span className="text-muted-foreground ml-1">({eclipse.sutak_hours}h before)</span>
            </div>
          ) : (
            <p className="text-[10px] text-muted-foreground bg-muted/10 rounded-lg px-2 py-1.5">
              ℹ️ No Sutak — penumbral eclipse
            </p>
          )}

          {/* Festival conflicts — always visible */}
          {hasConflict && (
            <div className="space-y-1">
              <p className="text-[10px] font-semibold text-amber-400">🎪 Nearby Festival Impact</p>
              {eclipse.festival_conflict!.map((msg, i) => (
                <div key={i} className="bg-amber-500/5 border border-amber-500/20 rounded-lg p-2 text-xs text-amber-300 leading-relaxed">
                  {msg}
                </div>
              ))}
            </div>
          )}

          {/* Spiritual significance + guidance — collapsible */}
          <button
            className="flex items-center gap-1 text-[10px] text-primary/60 hover:text-primary transition-colors"
            onClick={() => setExpanded(v => !v)}
          >
            <ChevronDown className={`h-3 w-3 transition-transform ${expanded ? "rotate-180" : ""}`} />
            {expanded ? "hide" : "show"} spiritual guidance
          </button>
          {expanded && (
            <div className="space-y-2">
              <div className="bg-primary/10 border border-primary/20 rounded-lg p-2.5">
                <p className="text-[10px] text-primary/80 mb-1">🔮 Spiritual Significance</p>
                <p className="text-xs text-muted-foreground leading-relaxed">{eclipse.spiritual_effect}</p>
              </div>
              <div className="flex flex-wrap gap-1">
                {(ECLIPSE_GUIDANCE[eclipse.type] ?? ECLIPSE_GUIDANCE["Partial Solar"]).map(item => (
                  <Badge key={item} variant="outline" className="text-[9px]">{item}</Badge>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </motion.div>
  );
}

// ── Rules Modal ───────────────────────────────────────────────────────────────
function RulesModal() {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <button className="flex items-center gap-1.5 text-xs text-primary/70 hover:text-primary border border-primary/20 hover:border-primary/40 rounded-lg px-3 py-1.5 transition-colors">
          <Info className="h-3 w-3" />
          Grahan Rules & Sutak Guide
        </button>
      </DialogTrigger>
      <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto glass border-border/40">
        <DialogHeader>
          <DialogTitle className="font-display text-lg text-primary">🌑 Grahan Niyam (Eclipse Rules)</DialogTitle>
        </DialogHeader>
        <div className="space-y-5 text-sm mt-2">
          {SUTAK_RULES.map(section => (
            <div key={section.title}>
              <h3 className="font-semibold text-foreground mb-1">{section.title}</h3>
              <p className="text-muted-foreground text-xs mb-2">{section.body}</p>
              <ul className="space-y-1">
                {section.items.map(item => (
                  <li key={item} className="flex gap-2 text-xs text-muted-foreground">
                    <span className="text-primary/60 mt-0.5">•</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          <div className="text-[10px] text-muted-foreground/50 border-t border-border/20 pt-3">
            Source: Dharmasindhu, Nirnaya Sindhu, Muhurta Chintamani. Times shown in IST (UTC+5:30).
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

// ── Eclipse Timings Grid ──────────────────────────────────────────────────────
function TimingGrid({ eclipse }: { eclipse: Eclipse }) {
  return (
    <div className="grid grid-cols-3 gap-2 text-center">
      {[
        { label: "Sparsha", sub: "First Contact", time: eclipse.sparsha, color: "text-yellow-400" },
        { label: "Madhya",  sub: "Maximum",       time: eclipse.madhya,  color: "text-primary" },
        { label: "Moksha",  sub: "Last Contact",  time: eclipse.moksha,  color: "text-green-400" },
      ].map(({ label, sub, time, color }) => (
        <div key={label} className="bg-muted/20 rounded-lg p-2.5">
          <p className={`text-lg font-mono font-bold ${color}`}>{time}</p>
          <p className="text-[10px] font-semibold text-foreground/80 mt-0.5">{label}</p>
          <p className="text-[9px] text-muted-foreground">{sub}</p>
        </div>
      ))}
    </div>
  );
}

// ── Eclipse Card ──────────────────────────────────────────────────────────────
function EclipseCard({ eclipse, index }: { eclipse: Eclipse; index: number }) {
  const [showDetails, setShowDetails] = useState(false);
  const isSolar = eclipse.type.includes("Solar");
  const hasConflict = eclipse.festival_conflict && eclipse.festival_conflict.length > 0;
  const hasSutak = eclipse.sutak_hours > 0 && eclipse.sutak_start;

  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1 }}
    >
      <Card className={`glass border-border/30 ${hasConflict ? "border-amber-500/30" : ""}`}>
        <CardHeader className="pb-2">
          <div className="flex items-start justify-between gap-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <span className="text-xl">{eclipseIcon(eclipse.type)}</span>
              <div>
                <p>{eclipse.type} Eclipse</p>
                <p className="text-xs font-normal font-mono text-primary/80 mt-0.5">{formatDate(eclipse.date)}</p>
              </div>
            </CardTitle>
            <div className="flex items-center gap-1.5 flex-wrap justify-end">
              {isSolar
                ? <Badge className="text-[9px] bg-yellow-500/10 text-yellow-400 border border-yellow-500/20">Solar</Badge>
                : <Badge className="text-[9px] bg-blue-500/10 text-blue-400 border border-blue-500/20">Lunar</Badge>
              }
              {hasConflict && <Badge className="text-[9px] bg-amber-500/10 text-amber-400 border border-amber-500/20">Festival Alert</Badge>}
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-3">
          {/* Timings */}
          <TimingGrid eclipse={eclipse} />

          {/* Duration */}
          <div className="flex items-center justify-between text-xs text-muted-foreground bg-muted/10 rounded-lg px-3 py-2">
            <div className="flex items-center gap-1.5">
              <Clock className="h-3 w-3" />
              <span>Duration: <span className="text-foreground font-medium">{eclipse.duration_minutes} min</span></span>
            </div>
            <div className="flex items-center gap-1.5">
              {isSolar ? <Sun className="h-3 w-3" /> : <Moon className="h-3 w-3" />}
              <span>Nakshatra: <span className="text-foreground font-medium">{eclipse.nakshatra}</span> ({eclipse.rashi})</span>
            </div>
          </div>

          {/* Sutak period */}
          {hasSutak ? (
            <div className="bg-red-500/5 border border-red-500/20 rounded-lg p-3">
              <p className="text-xs font-semibold text-red-400 mb-1">⏳ Sutak Kaal</p>
              <div className="flex items-center gap-2 text-xs">
                <span className="text-muted-foreground">Starts:</span>
                <span className="font-mono text-red-300">{eclipse.sutak_start}</span>
                <span className="text-muted-foreground">→</span>
                <span className="font-mono text-yellow-300">{eclipse.sparsha}</span>
                <span className="text-muted-foreground">(Sparsha)</span>
                <Badge className="text-[9px] bg-red-500/10 text-red-400 border border-red-500/20 ml-auto">
                  {eclipse.sutak_hours}h before
                </Badge>
              </div>
              <p className="text-[10px] text-muted-foreground/60 mt-1">
                Avoid cooking, eating, major activities during Sutak. Temples remain closed.
              </p>
            </div>
          ) : (
            <div className="bg-muted/10 border border-border/20 rounded-lg px-3 py-2">
              <p className="text-xs text-muted-foreground">
                ℹ️ <span className="font-medium text-foreground">No Sutak</span> — Penumbral eclipses are not observed with Sutak in most traditions.
              </p>
            </div>
          )}

          {/* Festival conflict */}
          {hasConflict && (
            <div className="space-y-1.5">
              <p className="text-xs font-semibold text-amber-400">🎪 Festival Conflicts</p>
              {eclipse.festival_conflict!.map((msg, i) => (
                <div key={i} className="bg-amber-500/5 border border-amber-500/20 rounded-lg p-2.5 text-xs text-amber-300 leading-relaxed">
                  {msg}
                </div>
              ))}
            </div>
          )}

          {/* Spiritual effect */}
          <div className="bg-primary/10 border border-primary/20 rounded-lg p-3">
            <p className="text-xs text-primary/80 mb-1">🔮 Spiritual Significance</p>
            <p className="text-xs text-muted-foreground leading-relaxed">{eclipse.spiritual_effect}</p>
          </div>

          {/* Do's & Don'ts — collapsible */}
          <button
            className="text-[10px] text-muted-foreground/60 hover:text-muted-foreground flex items-center gap-1"
            onClick={() => setShowDetails(v => !v)}
          >
            {showDetails ? "▲ hide" : "▼ show"} Vedic Do's &amp; Don'ts
          </button>
          {showDetails && (
            <div className="flex flex-wrap gap-1.5">
              {(ECLIPSE_GUIDANCE[eclipse.type] ?? ECLIPSE_GUIDANCE["Partial Solar"]).map(item => (
                <Badge key={item} variant="outline" className="text-[10px]">{item}</Badge>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </motion.div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function GrahanPage() {
  const year = new Date().getFullYear();
  const { birthDetails } = useKundliStore();

  // Use user's saved location, fall back to New Delhi
  const lat  = birthDetails?.latitude  ?? 28.6139;
  const lon  = birthDetails?.longitude ?? 77.209;
  const tz   = parseFloat(birthDetails?.timezone ?? "5.5");
  const loc  = birthDetails?.birthPlace ?? "New Delhi";

  const { data: grahanData, isLoading: gLoading, isError: gError } = useGrahan(year, tz);
  const { data: festData,   isLoading: fLoading, isError: fError  } = useFestivals(year, lat, lon, tz);

  const conflictCount = grahanData?.eclipses.filter(
    e => e.festival_conflict && e.festival_conflict.length > 0
  ).length ?? 0;

  return (
    <div className="space-y-4 pb-2">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-2xl sm:text-3xl text-primary text-glow-gold">🗓️ Panchang</h1>
        <p className="text-muted-foreground mt-1 text-sm">
          Hindu calendar, festivals & eclipses — {year} ·{" "}
          <span className="text-primary/70">📍 {loc}</span>
        </p>
      </motion.div>

      <Tabs defaultValue="calendar" className="w-full">
        <TabsList className="glass flex-wrap h-auto gap-1 p-1">
          <TabsTrigger value="calendar" className="text-xs sm:text-sm">📅 Panchang</TabsTrigger>
          <TabsTrigger value="festivals" className="text-xs sm:text-sm">🎉 Festivals</TabsTrigger>
          <TabsTrigger value="eclipses" className="text-xs sm:text-sm">
            🌑 Eclipses
            {conflictCount > 0 && (
              <Badge className="ml-1.5 text-[9px] bg-amber-500/20 text-amber-400 border border-amber-500/30">
                {conflictCount}
              </Badge>
            )}
          </TabsTrigger>
        </TabsList>

        {/* ── Hindu Calendar Tab ── */}
        <TabsContent value="calendar" className="mt-3">
          <CalendarPage />
        </TabsContent>

        {/* ── Festival Tab ── */}
        <TabsContent value="festivals" className="mt-3 space-y-3">
          {(fLoading || gLoading) && (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-5 w-5 animate-spin text-primary mr-2" />
              <span className="text-sm text-muted-foreground">Calculating from lunar calendar…</span>
            </div>
          )}
          {fError && (
            <Card className="glass border-destructive/30">
              <CardContent className="pt-5 text-sm text-muted-foreground text-center">
                Could not load festival data. API server may be down.
              </CardContent>
            </Card>
          )}
          {festData && (
            <>
              {/* Build unified timeline: festivals + eclipses sorted by date */}
              {(() => {
                type TimelineItem =
                  | { kind: "festival"; data: FestivalEntry }
                  | { kind: "eclipse";  data: Eclipse };

                const items: TimelineItem[] = [
                  ...festData.festivals.map(f => ({ kind: "festival" as const, data: f })),
                  ...(grahanData?.eclipses ?? []).map(e => ({ kind: "eclipse" as const, data: e })),
                ].sort((a, b) => {
                  const da = a.kind === "festival" ? a.data.date : a.data.date;
                  const db = b.kind === "festival" ? b.data.date : b.data.date;
                  return da < db ? -1 : da > db ? 1 : 0;
                });

                const eclipseCount = grahanData?.eclipses.length ?? 0;

                return (
                  <>
                    <p className="text-xs text-muted-foreground">
                      {festData.festivals.length} festivals
                      {eclipseCount > 0 && (
                        <> + <span className="text-red-400">{eclipseCount} eclipse{eclipseCount > 1 ? "s" : ""}</span></>
                      )}{" "}
                      · sorted by date · eclipse cards shown on their exact date ·{" "}
                      <span className="text-red-400/80">red border = Grahan conflict</span>
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
                      {items.map((item, i) =>
                        item.kind === "eclipse" ? (
                          <EclipseInlineCard key={`eclipse-${item.data.date}`} eclipse={item.data} index={i} />
                        ) : (
                          <FestivalCard key={item.data.name} festival={item.data} index={i} />
                        )
                      )}
                    </div>
                  </>
                );
              })()}
            </>
          )}
        </TabsContent>

        {/* ── Eclipses Tab ── */}
        <TabsContent value="eclipses" className="space-y-3 mt-3">
          {/* Info bar */}
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <p className="text-xs text-muted-foreground">
              All timings in IST · Sparsha = first contact · Madhya = maximum · Moksha = last contact
            </p>
            <RulesModal />
          </div>

          {gLoading && (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-5 w-5 animate-spin text-primary mr-2" />
              <span className="text-sm text-muted-foreground">Calculating eclipses from pyswisseph…</span>
            </div>
          )}
          {gError && (
            <Card className="glass border-destructive/30">
              <CardContent className="pt-5 text-sm text-muted-foreground text-center">
                Could not load eclipse data. API server may be down.
              </CardContent>
            </Card>
          )}
          {grahanData && grahanData.eclipses.length === 0 && (
            <Card className="glass border-border/30">
              <CardContent className="pt-5 text-sm text-muted-foreground text-center">
                No eclipses found for {grahanData.year}.
              </CardContent>
            </Card>
          )}
          {grahanData?.eclipses.map((eclipse, i) => (
            <EclipseCard key={`${eclipse.date}-${eclipse.type}`} eclipse={eclipse} index={i} />
          ))}
        </TabsContent>
      </Tabs>
    </div>
  );
}