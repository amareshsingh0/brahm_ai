// ── Shared ────────────────────────────────────────────────────
export interface BirthDetails {
  name?: string;
  date: string;   // YYYY-MM-DD
  time: string;   // HH:MM
  lat: number;
  lon: number;
  tz?: number;
  place?: string;
}

// ── Cities ────────────────────────────────────────────────────
export interface City {
  name: string;
  lat: number;
  lon: number;
  tz: number;
}
export interface CitiesResponse {
  cities: City[];
}

// ── Kundali ───────────────────────────────────────────────────
export interface GrahaData {
  rashi: string;
  rashi_en?: string;
  house: number;
  degree: number;
  longitude?: number;
  nakshatra: string;
  nakshatra_hindi?: string;
  nakshatra_lord?: string;
  pada: number;
  retro: boolean;
  status: string;
  relationship?: string;
  house_lord?: string;
  graha_en?: string;
  graha_hindi?: string;
  karaka?: string;
  speed?: number;
  combust?: boolean;
  ruler_of?: number[];
  lat_ecl?: number;
  ra?: number;
  dec?: number;
}
export interface NavamshaGraha {
  rashi: string;
  house: number;
  status: string;
  retro: boolean;
}
export interface LagnaData {
  rashi: string;
  rashi_en?: string;
  nakshatra: string;
  nakshatra_hindi?: string;
  pada?: number;
  degree: number;
  full_degree?: number;
}
export interface HouseData {
  house: number;
  rashi: string;
  rashi_en?: string;
  lord: string;
  lord_en?: string;
  signification?: string;
  planets?: string[];
  gender?: string;
  modality?: string;
  tattva?: string;
  aspected_by?: string[];
}
export interface SukshmaData {
  lord: string;
  hours: number;
  start: string;
  end: string;
}
export interface PratyantarData {
  lord: string;
  days: number;
  start: string;
  end: string;
  sukshmadashas?: SukshmaData[];
}
export interface AntardashaData {
  lord: string;
  years: number;
  start: string;
  end: string;
  pratyantardashas?: PratyantarData[];
}
export interface DashaData {
  lord: string;
  years: number;
  start: string;
  end: string;
  is_current?: boolean;
  antardashas?: AntardashaData[];
}
export interface YogaData {
  name: string;
  desc: string;
  present: boolean;
  strength: string;
  category?: string;
  mantra?: string;
  gemstone?: string;
  deity?: string;
  remedy?: string;
}
export interface AlertData {
  type: "graha_yuddha" | "combust";
  message: string;
  planet?: string;
  planet1?: string;
  planet2?: string;
  winner?: string;
  loser?: string;
  orb?: number;
}
export interface BirthPanchang {
  tithi: string;
  tithi_num: number;
  paksha: string;
  yoga: string;
  karana: string;
  vara: string;
  sunrise: string;
  sunset: string;
  moonsign?: string;
  sunsign?: string;
  moon_nakshatra?: string;
  surya_nakshatra?: string;
}
export interface PersonalChars {
  nadi: string;
  gana: string;
  yoni: string;
  varna: string;
  tattva: string;
  vashya: string;
  nakshatra_paya: string;
  rashi_paya: string;
  yunja: string;
  tara?: string;
}
export interface UpagrahaEntry {
  longitude: number;
  rashi: number;
  rashi_name: string;
  degree: number;
  dms: string;
  nakshatra: string;
  nakshatra_lord: string;
  significance: string;
}
export interface VargaGraha {
  rashi: string;
  house: number;
  status: string;
  retro: boolean;
}
export interface VargaChartData {
  division: number;
  name: string;
  full_name: string;
  signification: string;
  lagna: { rashi: string };
  houses: { house: number; rashi: string; lord: string }[];
  grahas: Record<string, VargaGraha>;
}
// ── Shadbala ──────────────────────────────────────────────────
export interface ShadbalaPlanet {
  sthana_bala: number;
  dig_bala: number;
  kaala_bala: number;
  chesta_bala: number;
  naisargika_bala: number;
  drik_bala: number;
  total_virupas: number;
  total_rupas: number;
  required_rupas: number;
  ratio: number;
  is_strong: boolean;
  sthana_detail?: { uccha: number; oja_yugma: number; kendradi: number; drekkana: number; total: number };
}
export interface ShadbalaResponse {
  planets: Record<string, ShadbalaPlanet>;
}
export interface BhavabalaEntry {
  house: number;
  rashi: string;
  lord: string;
  strength: number;
  rank: number;
}

// ── Ashtakavarga ───────────────────────────────────────────────
export interface AshtakavargaResponse {
  bav: Record<string, { points: number[]; total: number }>;
  sav: { points: number[]; total: number };
  reduced_bav?: Record<string, { points: number[]; total: number }>;
  reduced_sav?: { points: number[]; total: number };
}

export interface KundaliResponse {
  name: string;
  place: string;
  birth_date: string;
  lat: number;
  lon: number;
  tz: number;
  ayanamsha: number;
  ayanamsha_mode?: string;
  ayanamsha_label?: string;
  rahu_mode?: string;
  lagna: LagnaData;
  grahas: Record<string, GrahaData>;
  houses: HouseData[];
  navamsha?: Record<string, NavamshaGraha>;
  navamsha_lagna?: { rashi: string };
  navamsha_houses?: HouseData[];
  dashas: DashaData[];
  yogas: YogaData[];
  varga_charts?: Record<string, VargaChartData>;
  birth_panchang?: BirthPanchang;
  personal?: PersonalChars;
  bhav_chalit?: { cusps_sid: number[]; planets: Record<string, number> };
  upagraha?: Record<string, UpagrahaEntry>;
  shadbala?: ShadbalaResponse;
  bhavabala?: BhavabalaEntry[];
  ashtakavarga?: AshtakavargaResponse;
  alerts?: AlertData[];
}

// ── Panchang ──────────────────────────────────────────────────
export interface VaraData      { name: string; hindi: string; lord: string }
export interface TithiData     { name: string; hindi: string; paksha: string; end_time: string; tithi_type?: string }
export interface NakshatraData { name: string; hindi: string; pada: number; lord: string; end_time?: string }
export interface PanchangYoga  { name: string; hindi: string; is_auspicious: boolean; end_time?: string }
export interface KaranaData    { name: string; hindi: string; is_bhadra?: boolean }
export interface TimeSlot      { start: string; end: string }
export interface NishitaSlot   { start: string; end: string; midpoint?: string }

export interface ChoghadiyaPeriod {
  name: string; hindi: string; quality: string; auspicious: boolean; start: string; end: string;
}
export interface ChoghadiyaData { day: ChoghadiyaPeriod[]; night: ChoghadiyaPeriod[] }

export interface PanchangResponse {
  vara: VaraData;
  tithi: TithiData;
  nakshatra: NakshatraData;
  yoga: PanchangYoga;
  karana: KaranaData;
  sunrise: string;
  sunset: string;
  abhijit_muhurta: TimeSlot;
  rahukaal: TimeSlot;
  yamagandam?: TimeSlot | null;
  gulika_kaal?: TimeSlot | null;
  brahma_muhurta?: TimeSlot | null;
  pradosh_kaal?: TimeSlot | null;
  nishita_kaal?: NishitaSlot | null;
  choghadiya?: ChoghadiyaData | null;
  panchaka?: boolean | null;
}

// ── Chat ──────────────────────────────────────────────────────
export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  followUps?: string[];
  isComplete?: boolean;
}
export interface Source {
  book: string;
  source: string;
  language: string;
}

// ── Compatibility ─────────────────────────────────────────────
export interface GunaScore {
  name: string;
  score: number;
  max: number;
  desc: string;
  interpretation: string;   // couple-specific dynamic text
  alt_score?: number;       // Varna: rashi-system alternative score
}
export interface DoshaSummary {
  name: string;
  present: boolean;
  severity: 'High' | 'Medium' | 'Low' | 'None';
  cancellation?: string;
  note: string;
}
export interface RajjuDosha {
  present: boolean;
  rajju_a: string;
  rajju_b: string;
  severity: string;
  note: string;
}
export interface VedhaDosha {
  present: boolean;
  note: string;
}
export interface LifeAreaScore {
  area: string;
  icon: string;
  score: number;   // 0–100
  label: string;   // "Excellent" | "Good" | "Average" | "Needs Work"
}
export interface CompatibilityResponse {
  total_score: number;
  max_score: number;
  percentage: number;
  verdict: string;
  verdict_detail: string;
  gunas: GunaScore[];
  mangal_dosha: { person_a: boolean; person_b: boolean };
  nakshatra_a: string;
  nakshatra_b: string;
  rashi_a: string;
  rashi_b: string;
  gana_a: string;
  gana_b: string;
  nadi_a: string;
  nadi_b: string;
  varna_a: string;
  varna_b: string;
  yoni_a: string;
  yoni_b: string;
  rajju_dosha: RajjuDosha;
  vedha_dosha: VedhaDosha;
  life_areas: LifeAreaScore[];
  strengths: string[];
  challenges: string[];
  dosha_summary: DoshaSummary[];
}

// ── Search ────────────────────────────────────────────────────
export interface SearchResult {
  text: string;
  source: string;
  language: string;
  score: number;
}
export interface SearchResponse {
  results: SearchResult[];
}

// ── Planets ───────────────────────────────────────────────────
export interface PlanetPos {
  rashi: string;
  degree: number;       // degree within rashi (0–29.999)
  nakshatra: string;
  retro?: boolean;
}
export interface PlanetsResponse {
  grahas: Record<string, PlanetPos>;
  lagna?: { rashi: string; degree: number };
  computed_at: string;
}

// ── Festivals ─────────────────────────────────────────────────
export interface FestivalEntry {
  name: string;
  hindi: string;
  date: string;                  // "2026-01-14" — observation date (udaya tithi rule)
  date_end: string | null;       // last date for multi-day (Navratri etc.)
  tithi_start: string;           // "HH:MM" — when the tithi begins (may be prev day if tithi_prev_day)
  tithi_end: string;             // "HH:MM" — when the tithi ends (may be next day if < tithi_start)
  tithi_start_date: string | null; // "YYYY-MM-DD" — exact calendar date the tithi begins
  month: string;                 // "Pausha"
  deity: string;
  significance: string;
  icon: string;
  paksha: string;                // "Shukla" | "Krishna" | "N/A"
  tithi_name: string;            // "Purnima", "Ashtami", "Amavasya", etc.
  tithi_type: string;            // "normal" | "vridhi" | "ksheya"
  tithi_prev_day: boolean;       // true if tithi started the previous calendar day
  fast_note: string | null;      // fasting instructions for this festival
  dosh_notes: string[];          // Bhadra dosh, vridhi/ksheya warnings, nishita notes
}
export interface FestivalsResponse {
  year: number;
  festivals: FestivalEntry[];
}

// ── Grahan ────────────────────────────────────────────────────
export interface Eclipse {
  date: string;
  type: string;
  sparsha: string;              // First contact HH:MM (IST)
  madhya: string;               // Maximum eclipse HH:MM (IST)
  moksha: string;               // Last contact HH:MM (IST)
  duration_minutes: number;
  sutak_start: string | null;   // HH:MM or null (penumbral = no sutak)
  sutak_hours: number;
  nakshatra: string;            // Moon nakshatra at Madhya
  rashi: string;                // Moon rashi at Madhya
  spiritual_effect: string;
  festival_conflict: string[] | null;
}
export interface GrahanResponse {
  year: number;
  eclipses: Eclipse[];
  tz: number;
}

// ── Horoscope ─────────────────────────────────────────────────
export interface HoroscopeResponse {
  rashi: string;
  period: string;
  prediction: string;
  lucky_number: number;
  lucky_color: string;
  sign_ruler?: string;
}

// ── Muhurta ───────────────────────────────────────────────────
export interface MuhurtaSlot {
  name: string;
  start: string;
  end: string;
  quality: string;
  note: string;
}
export interface MuhurtaResponse {
  event: string;
  date: string;
  slots: MuhurtaSlot[];
  avoid: { name: string; start: string; end: string }[];
  panchang_summary: { tithi: string; vara: string; nakshatra: string; yoga: string };
}

// ── Monthly Panchang Calendar ─────────────────────────────────
export interface CalendarDayData {
  date: string;
  day: number;
  weekday: string;
  vara: string;
  sunrise: string;
  sunset: string;
  tithi_idx: number;
  tithi: string;
  tithi_num: number;
  paksha: string;
  paksha_short: string;
  nakshatra: string;
  yoga: string;
  rahu_kaal: string;
  lunar_month: string;
  festivals: FestivalEntry[];
  is_today: boolean;
  is_purnima: boolean;
  is_amavasya: boolean;
  is_ekadashi: boolean;
  is_chaturthi: boolean;
  is_pradosh: boolean;
  is_ashtami: boolean;
  has_festival: boolean;
  error?: string;
}
export interface MonthlyCalendarResponse {
  year: number;
  month: number;
  month_name: string;
  days_in_month: number;
  first_weekday: number;
  tradition: string;
  lunar_system: string;
  vikram_samvat: number;
  lunar_months: string[];
  adhik_maas_note: string | null;
  kshaya_maas_note: string | null;
  days: CalendarDayData[];
  error?: string;
}

// ── User ──────────────────────────────────────────────────────
export interface UserProfile {
  session_id: string;
  name?: string;
  date?: string;
  time?: string;
  lat?: number;
  lon?: number;
  tz?: number;
  place?: string;
  rashi?: string;
  nakshatra?: string;
  language?: string;
}
