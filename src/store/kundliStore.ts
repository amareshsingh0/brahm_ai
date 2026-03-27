import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { KundaliResponse } from '../types/api';

export interface BirthDetails {
  name: string;
  dateOfBirth: string;
  timeOfBirth: string;
  birthPlace: string;
  lat?: number;
  lon?: number;
  tz?: number;
}

export interface PlanetData {
  name: string;
  symbol: string;
  rashi: string;
  house: number;
  degree: string;
  nakshatra: string;
  color: string;
  sanskritName?: string;
}

export interface YogaData {
  name: string;
  sanskritName: string;
  planets: string[];
  description: string;
  effect: "benefic" | "malefic" | "mixed";
  present: boolean;
}

export interface RemedyData {
  planet: string;
  mantra: string;
  mantraDevanagari?: string;
  gemstone: string;
  gemstoneKey?: string;
  day: string;
  dayKey?: string;
  color: string;
  colorKey?: string;
  donation: string;
  donationKey?: string;
  fasting: string;
  fastingKey?: string;
}

export interface NakshatraData {
  name: string;
  number: number;
  symbol: string;
  deity: string;
  nature: string;
  ruler: string;
  traits: string;
  pada: string;
}

export type ChartStyle = "north" | "south" | "east" | "west";
export type AyanamshaMode = "lahiri" | "raman" | "kp" | "true_citra";
export type RahuMode = "mean" | "true";
export type NameLang = "vedic" | "en";

export interface KundaliSettings {
  chartStyle: ChartStyle;
  ayanamsha: AyanamshaMode;
  rahuMode: RahuMode;
  nameLang: NameLang;
}

const DEFAULT_SETTINGS: KundaliSettings = {
  chartStyle: "north",
  ayanamsha: "lahiri",
  rahuMode: "mean",
  nameLang: "vedic",
};

// Load persisted settings from localStorage
function loadSettings(): KundaliSettings {
  try {
    const raw = localStorage.getItem("kundali_settings");
    if (raw) return { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch {}
  return DEFAULT_SETTINGS;
}

export interface KundliState {
  birthDetails: BirthDetails | null;
  selectedPlanet: PlanetData | null;
  hasKundli: boolean;
  kundaliData: KundaliResponse | null;
  kundaliSettings: KundaliSettings;
  setBirthDetails: (details: BirthDetails) => void;
  setSelectedPlanet: (planet: PlanetData | null) => void;
  setHasKundli: (val: boolean) => void;
  setKundaliData: (data: KundaliResponse) => void;
  setCity: (lat: number, lon: number, tz: number) => void;
  setKundaliSettings: (settings: Partial<KundaliSettings>) => void;
}

export const useKundliStore = create<KundliState>()(
  persist(
    (set) => ({
      birthDetails: null,
      selectedPlanet: null,
      hasKundli: false,
      kundaliData: null,
      kundaliSettings: loadSettings(),
      setBirthDetails: (details) => set({ birthDetails: details, hasKundli: true }),
      setSelectedPlanet: (planet) => set({ selectedPlanet: planet }),
      setHasKundli: (val) => set({ hasKundli: val }),
      setKundaliData: (data) => set({ kundaliData: data, hasKundli: true }),
      setCity: (lat, lon, tz) => set((s) => ({
        birthDetails: s.birthDetails ? { ...s.birthDetails, lat, lon, tz } : null,
      })),
      setKundaliSettings: (partial) => set((s) => {
        const next = { ...s.kundaliSettings, ...partial };
        localStorage.setItem("kundali_settings", JSON.stringify(next));
        return { kundaliSettings: next };
      }),
    }),
    {
      name: 'brahm-birth',
      storage: createJSONStorage(() => localStorage),
      // Only persist birth details — not large kundaliData (recalculated as needed)
      partialize: (s) => ({
        birthDetails: s.birthDetails,
        hasKundli: s.hasKundli,
      }),
    }
  )
);

export const samplePlanets: PlanetData[] = [
  { name: "Sun", symbol: "☉︎", rashi: "Leo", house: 5, degree: "15°23'", nakshatra: "Magha", color: "hsl(42 90% 64%)", sanskritName: "Surya" },
  { name: "Moon", symbol: "☽︎", rashi: "Cancer", house: 4, degree: "22°10'", nakshatra: "Ashlesha", color: "hsl(220 20% 90%)", sanskritName: "Chandra" },
  { name: "Mars", symbol: "♂︎", rashi: "Aries", house: 1, degree: "8°45'", nakshatra: "Ashwini", color: "hsl(0 70% 55%)", sanskritName: "Mangal" },
  { name: "Mercury", symbol: "☿︎", rashi: "Virgo", house: 6, degree: "3°12'", nakshatra: "Uttara Phalguni", color: "hsl(140 50% 55%)", sanskritName: "Budh" },
  { name: "Jupiter", symbol: "♃︎", rashi: "Sagittarius", house: 9, degree: "18°56'", nakshatra: "Purva Ashadha", color: "hsl(42 70% 60%)", sanskritName: "Guru" },
  { name: "Venus", symbol: "♀︎", rashi: "Taurus", house: 2, degree: "27°34'", nakshatra: "Mrigashira", color: "hsl(292 84% 61%)", sanskritName: "Shukra" },
  { name: "Saturn", symbol: "♄︎", rashi: "Capricorn", house: 10, degree: "11°08'", nakshatra: "Shravana", color: "hsl(220 30% 45%)", sanskritName: "Shani" },
  { name: "Rahu", symbol: "☊︎", rashi: "Gemini", house: 3, degree: "5°20'", nakshatra: "Mrigashira", color: "hsl(225 25% 50%)", sanskritName: "Rahu" },
  { name: "Ketu", symbol: "☋︎", rashi: "Sagittarius", house: 9, degree: "5°20'", nakshatra: "Mula", color: "hsl(30 60% 45%)", sanskritName: "Ketu" },
];

export const rashiData = [
  { name: "Aries",       symbol: "♈︎", element: "Fire",  ruler: "Mars",    traits: "Bold, Ambitious, Energetic",          sanskritName: "Mesha",     quality: "Movable", nature: "Male",   bodyPart: "Head & Face",         luckyColor: "Red" },
  { name: "Taurus",      symbol: "♉︎", element: "Earth", ruler: "Venus",   traits: "Reliable, Patient, Devoted",          sanskritName: "Vrishabha", quality: "Fixed",   nature: "Female", bodyPart: "Neck & Throat",       luckyColor: "White" },
  { name: "Gemini",      symbol: "♊︎", element: "Air",   ruler: "Mercury", traits: "Curious, Adaptable, Communicative",   sanskritName: "Mithuna",   quality: "Dual",    nature: "Male",   bodyPart: "Arms & Shoulders",    luckyColor: "Green" },
  { name: "Cancer",      symbol: "♋︎", element: "Water", ruler: "Moon",    traits: "Intuitive, Emotional, Protective",    sanskritName: "Karka",     quality: "Movable", nature: "Female", bodyPart: "Chest & Lungs",       luckyColor: "Silver" },
  { name: "Leo",         symbol: "♌︎", element: "Fire",  ruler: "Sun",     traits: "Creative, Passionate, Generous",      sanskritName: "Simha",     quality: "Fixed",   nature: "Male",   bodyPart: "Heart & Spine",       luckyColor: "Gold" },
  { name: "Virgo",       symbol: "♍︎", element: "Earth", ruler: "Mercury", traits: "Analytical, Practical, Kind",         sanskritName: "Kanya",     quality: "Dual",    nature: "Female", bodyPart: "Stomach & Intestines", luckyColor: "Green" },
  { name: "Libra",       symbol: "♎︎", element: "Air",   ruler: "Venus",   traits: "Diplomatic, Gracious, Fair",          sanskritName: "Tula",      quality: "Movable", nature: "Male",   bodyPart: "Kidneys & Lower Back", luckyColor: "Pink" },
  { name: "Scorpio",     symbol: "♏︎", element: "Water", ruler: "Mars",    traits: "Resourceful, Powerful, Brave",        sanskritName: "Vrishchika",quality: "Fixed",   nature: "Female", bodyPart: "Reproductive System", luckyColor: "Red" },
  { name: "Sagittarius", symbol: "♐︎", element: "Fire",  ruler: "Jupiter", traits: "Optimistic, Adventurous, Free",       sanskritName: "Dhanu",     quality: "Dual",    nature: "Male",   bodyPart: "Thighs & Hips",       luckyColor: "Yellow" },
  { name: "Capricorn",   symbol: "♑︎", element: "Earth", ruler: "Saturn",  traits: "Disciplined, Responsible, Patient",   sanskritName: "Makara",    quality: "Movable", nature: "Female", bodyPart: "Knees & Joints",      luckyColor: "Black" },
  { name: "Aquarius",    symbol: "♒︎", element: "Air",   ruler: "Saturn",  traits: "Progressive, Original, Humanitarian", sanskritName: "Kumbha",    quality: "Fixed",   nature: "Male",   bodyPart: "Ankles & Calves",     luckyColor: "Blue" },
  { name: "Pisces",      symbol: "♓︎", element: "Water", ruler: "Jupiter", traits: "Compassionate, Artistic, Wise",       sanskritName: "Meena",     quality: "Dual",    nature: "Female", bodyPart: "Feet & Lymphatics",   luckyColor: "Purple" },
];

export const dashaData = [
  { planet: "Moon", start: 1998, end: 2008, nature: "positive" as const },
  { planet: "Mars", start: 2008, end: 2015, nature: "neutral" as const },
  { planet: "Rahu", start: 2015, end: 2033, nature: "challenging" as const },
  { planet: "Jupiter", start: 2033, end: 2049, nature: "positive" as const },
  { planet: "Saturn", start: 2049, end: 2068, nature: "neutral" as const },
  { planet: "Mercury", start: 2068, end: 2085, nature: "positive" as const },
];

export const yogasData: YogaData[] = [
  { name: "Gajakesari Yoga", sanskritName: "गजकेसरी योग", planets: ["Jupiter", "Moon"], description: "Jupiter in a Kendra from Moon. Grants wisdom, fame, and lasting reputation. The native becomes influential and respected.", effect: "benefic", present: true },
  { name: "Budhaditya Yoga", sanskritName: "बुधादित्य योग", planets: ["Sun", "Mercury"], description: "Conjunction of Sun and Mercury. Bestows sharp intellect, eloquence, and success in education and communication.", effect: "benefic", present: true },
  { name: "Chandra Mangala Yoga", sanskritName: "चन्द्र मंगल योग", planets: ["Moon", "Mars"], description: "Moon and Mars conjunction or mutual aspect. Gives wealth through business, courage, and enterprising nature.", effect: "mixed", present: false },
  { name: "Raja Yoga", sanskritName: "राज योग", planets: ["Jupiter", "Venus"], description: "Lords of Kendra and Trikona in conjunction. Grants power, authority, and high social status.", effect: "benefic", present: true },
  { name: "Viparita Raja Yoga", sanskritName: "विपरीत राज योग", planets: ["Saturn", "Rahu"], description: "Lord of dusthana in another dusthana. Success through adversity and unconventional paths.", effect: "mixed", present: false },
  { name: "Kemadruma Yoga", sanskritName: "केमद्रुम योग", planets: ["Moon"], description: "Moon without planets in adjacent houses. Can indicate emotional challenges but also self-reliance.", effect: "malefic", present: false },
];

export const remediesData: RemedyData[] = [
  { planet: "Sun",     mantra: "Om Hraam Hreem Hraum Sah Suryaya Namah",   mantraDevanagari: "ॐ ह्रां ह्रीं ह्रौं सः सूर्याय नमः", gemstone: "Ruby (Manikya)",           gemstoneKey: "remedies.gem_sun",     day: "Sunday",    dayKey: "remedies.day_sunday",    color: "Red / Copper",        colorKey: "remedies.color_sun",     donation: "Wheat, Jaggery, Copper",        donationKey: "remedies.donation_sun",     fasting: "Sunday",    fastingKey: "remedies.day_sunday" },
  { planet: "Moon",    mantra: "Om Shraam Shreem Shraum Sah Chandraya Namah", mantraDevanagari: "ॐ श्रां श्रीं श्रौं सः चन्द्राय नमः", gemstone: "Pearl (Moti)",           gemstoneKey: "remedies.gem_moon",    day: "Monday",    dayKey: "remedies.day_monday",    color: "White / Silver",      colorKey: "remedies.color_moon",    donation: "Rice, Milk, Silver",            donationKey: "remedies.donation_moon",    fasting: "Monday",    fastingKey: "remedies.day_monday" },
  { planet: "Mars",    mantra: "Om Kraam Kreem Kraum Sah Bhaumaya Namah",  mantraDevanagari: "ॐ क्रां क्रीं क्रौं सः भौमाय नमः", gemstone: "Red Coral (Moonga)",      gemstoneKey: "remedies.gem_mars",    day: "Tuesday",   dayKey: "remedies.day_tuesday",   color: "Red / Scarlet",       colorKey: "remedies.color_mars",    donation: "Red Lentils, Jaggery",          donationKey: "remedies.donation_mars",    fasting: "Tuesday",   fastingKey: "remedies.day_tuesday" },
  { planet: "Mercury", mantra: "Om Braam Breem Braum Sah Budhaya Namah",   mantraDevanagari: "ॐ ब्रां ब्रीं ब्रौं सः बुधाय नमः", gemstone: "Emerald (Panna)",         gemstoneKey: "remedies.gem_mercury", day: "Wednesday", dayKey: "remedies.day_wednesday", color: "Green",               colorKey: "remedies.color_mercury", donation: "Green Moong Dal, Green Cloth",   donationKey: "remedies.donation_mercury", fasting: "Wednesday", fastingKey: "remedies.day_wednesday" },
  { planet: "Jupiter", mantra: "Om Graam Greem Graum Sah Gurave Namah",    mantraDevanagari: "ॐ ग्रां ग्रीं ग्रौं सः गुरवे नमः", gemstone: "Yellow Sapphire (Pukhraj)", gemstoneKey: "remedies.gem_jupiter", day: "Thursday", dayKey: "remedies.day_thursday",  color: "Yellow / Gold",       colorKey: "remedies.color_jupiter", donation: "Chana Dal, Turmeric, Gold",      donationKey: "remedies.donation_jupiter", fasting: "Thursday",  fastingKey: "remedies.day_thursday" },
  { planet: "Venus",   mantra: "Om Draam Dreem Draum Sah Shukraya Namah",  mantraDevanagari: "ॐ द्रां द्रीं द्रौं सः शुक्राय नमः", gemstone: "Diamond (Heera)",         gemstoneKey: "remedies.gem_venus",   day: "Friday",    dayKey: "remedies.day_friday",    color: "White / Pink",        colorKey: "remedies.color_venus",   donation: "Rice, Sugar, White Cloth",      donationKey: "remedies.donation_venus",   fasting: "Friday",    fastingKey: "remedies.day_friday" },
  { planet: "Saturn",  mantra: "Om Sham Shanicharaya Namah",               mantraDevanagari: "ॐ शं शनैश्चराय नमः", gemstone: "Blue Sapphire (Neelam)",  gemstoneKey: "remedies.gem_saturn",  day: "Saturday",  dayKey: "remedies.day_saturday",  color: "Black / Blue",        colorKey: "remedies.color_saturn",  donation: "Black Sesame, Mustard Oil",     donationKey: "remedies.donation_saturn",  fasting: "Saturday",  fastingKey: "remedies.day_saturday" },
  { planet: "Rahu",    mantra: "Om Bhram Bhreem Bhraum Sah Rahave Namah",  mantraDevanagari: "ॐ भ्रां भ्रीं भ्रौं सः राहवे नमः", gemstone: "Hessonite (Gomed)",       gemstoneKey: "remedies.gem_rahu",    day: "Saturday",  dayKey: "remedies.day_saturday",  color: "Dark Blue / Smoky",   colorKey: "remedies.color_rahu",    donation: "Coconut, Black Blanket",        donationKey: "remedies.donation_rahu",    fasting: "Saturday",  fastingKey: "remedies.day_saturday" },
  { planet: "Ketu",    mantra: "Om Stram Streem Straum Sah Ketave Namah",  mantraDevanagari: "ॐ स्त्रां स्त्रीं स्त्रौं सः केतवे नमः", gemstone: "Cat's Eye (Lehsunia)",    gemstoneKey: "remedies.gem_ketu",    day: "Tuesday",   dayKey: "remedies.day_tuesday",   color: "Grey / Brown",        colorKey: "remedies.color_ketu",    donation: "Blanket, Dog food",             donationKey: "remedies.donation_ketu",    fasting: "Tuesday",   fastingKey: "remedies.day_tuesday" },
];

export const nakshatraData: NakshatraData[] = [
  { name: "Ashwini", number: 1, symbol: "🐎", deity: "Ashwini Kumaras", nature: "Swift (Kshipra)", ruler: "Ketu", traits: "Energetic, Healing, Pioneering", pada: "Aries" },
  { name: "Bharani", number: 2, symbol: "🔺", deity: "Yama", nature: "Fierce (Ugra)", ruler: "Venus", traits: "Transformative, Creative, Intense", pada: "Aries" },
  { name: "Krittika", number: 3, symbol: "🔥", deity: "Agni", nature: "Mixed (Mishra)", ruler: "Sun", traits: "Sharp, Purifying, Determined", pada: "Aries/Taurus" },
  { name: "Rohini", number: 4, symbol: "🐂", deity: "Brahma", nature: "Fixed (Dhruva)", ruler: "Moon", traits: "Beautiful, Fertile, Charming", pada: "Taurus" },
  { name: "Mrigashira", number: 5, symbol: "🦌", deity: "Soma", nature: "Soft (Mridu)", ruler: "Mars", traits: "Searching, Gentle, Curious", pada: "Taurus/Gemini" },
  { name: "Ardra", number: 6, symbol: "💧", deity: "Rudra", nature: "Sharp (Tikshna)", ruler: "Rahu", traits: "Stormy, Transformative, Intellectual", pada: "Gemini" },
  { name: "Punarvasu", number: 7, symbol: "🏹", deity: "Aditi", nature: "Movable (Chara)", ruler: "Jupiter", traits: "Renewal, Optimistic, Wise", pada: "Gemini/Cancer" },
  { name: "Pushya", number: 8, symbol: "🌸", deity: "Brihaspati", nature: "Light (Laghu)", ruler: "Saturn", traits: "Nourishing, Auspicious, Protective", pada: "Cancer" },
  { name: "Ashlesha", number: 9, symbol: "🐍", deity: "Nagas", nature: "Sharp (Tikshna)", ruler: "Mercury", traits: "Mystical, Cunning, Perceptive", pada: "Cancer" },
  { name: "Magha", number: 10, symbol: "👑", deity: "Pitris", nature: "Fierce (Ugra)", ruler: "Ketu", traits: "Royal, Ancestral, Authoritative", pada: "Leo" },
  { name: "Purva Phalguni", number: 11, symbol: "🛏️", deity: "Bhaga", nature: "Fierce (Ugra)", ruler: "Venus", traits: "Creative, Romantic, Joyful", pada: "Leo" },
  { name: "Uttara Phalguni", number: 12, symbol: "☀️", deity: "Aryaman", nature: "Fixed (Dhruva)", ruler: "Sun", traits: "Generous, Kind, Prosperous", pada: "Leo/Virgo" },
  { name: "Hasta", number: 13, symbol: "✋", deity: "Savitar", nature: "Light (Laghu)", ruler: "Moon", traits: "Skilled, Crafty, Humorous", pada: "Virgo" },
  { name: "Chitra", number: 14, symbol: "💎", deity: "Tvashtar", nature: "Soft (Mridu)", ruler: "Mars", traits: "Artistic, Brilliant, Beautiful", pada: "Virgo/Libra" },
  { name: "Swati", number: 15, symbol: "🌬️", deity: "Vayu", nature: "Movable (Chara)", ruler: "Rahu", traits: "Independent, Flexible, Diplomatic", pada: "Libra" },
  { name: "Vishakha", number: 16, symbol: "🌿", deity: "Indragni", nature: "Mixed (Mishra)", ruler: "Jupiter", traits: "Determined, Goal-oriented, Powerful", pada: "Libra/Scorpio" },
  { name: "Anuradha", number: 17, symbol: "🌺", deity: "Mitra", nature: "Soft (Mridu)", ruler: "Saturn", traits: "Devoted, Friendly, Successful", pada: "Scorpio" },
  { name: "Jyeshtha", number: 18, symbol: "🛡️", deity: "Indra", nature: "Sharp (Tikshna)", ruler: "Mercury", traits: "Protective, Senior, Courageous", pada: "Scorpio" },
  { name: "Mula", number: 19, symbol: "⚡", deity: "Nirriti", nature: "Sharp (Tikshna)", ruler: "Ketu", traits: "Root-seeking, Transformative, Deep", pada: "Sagittarius" },
  { name: "Purva Ashadha", number: 20, symbol: "🌊", deity: "Apas", nature: "Fierce (Ugra)", ruler: "Venus", traits: "Invincible, Purifying, Proud", pada: "Sagittarius" },
  { name: "Uttara Ashadha", number: 21, symbol: "🏔️", deity: "Vishvadevas", nature: "Fixed (Dhruva)", ruler: "Sun", traits: "Victorious, Enduring, Universal", pada: "Sagittarius/Capricorn" },
  { name: "Shravana", number: 22, symbol: "👂", deity: "Vishnu", nature: "Movable (Chara)", ruler: "Moon", traits: "Listening, Learning, Connected", pada: "Capricorn" },
  { name: "Dhanishta", number: 23, symbol: "🥁", deity: "Vasus", nature: "Movable (Chara)", ruler: "Mars", traits: "Musical, Wealthy, Generous", pada: "Capricorn/Aquarius" },
  { name: "Shatabhisha", number: 24, symbol: "💫", deity: "Varuna", nature: "Movable (Chara)", ruler: "Rahu", traits: "Healer, Mystical, Secretive", pada: "Aquarius" },
  { name: "Purva Bhadrapada", number: 25, symbol: "🔱", deity: "Aja Ekapada", nature: "Fierce (Ugra)", ruler: "Jupiter", traits: "Intense, Spiritual, Fiery", pada: "Aquarius/Pisces" },
  { name: "Uttara Bhadrapada", number: 26, symbol: "🐍", deity: "Ahir Budhnya", nature: "Fixed (Dhruva)", ruler: "Saturn", traits: "Deep, Wise, Controlled", pada: "Pisces" },
  { name: "Revati", number: 27, symbol: "🐟", deity: "Pushan", nature: "Soft (Mridu)", ruler: "Mercury", traits: "Nurturing, Prosperous, Final", pada: "Pisces" },
];

export const dailyHoroscopes: Record<string, { prediction: string; lucky: string; advice: string }> = {
  Aries: { prediction: "A surge of energy propels you forward. Mars empowers bold decisions today.", lucky: "Number 9, Color Red", advice: "Channel aggression into productive action." },
  Taurus: { prediction: "Venus brings harmony to relationships. Financial matters look stable.", lucky: "Number 6, Color Green", advice: "Enjoy simple pleasures; avoid overspending." },
  Gemini: { prediction: "Mercury sharpens your communication. Great day for negotiations.", lucky: "Number 5, Color Yellow", advice: "Listen twice as much as you speak." },
  Cancer: { prediction: "Moon's transit deepens emotions. Home and family matters are highlighted.", lucky: "Number 2, Color Silver", advice: "Nurture yourself before nurturing others." },
  Leo: { prediction: "Sun illuminates your creativity. Leadership opportunities arise.", lucky: "Number 1, Color Gold", advice: "Let your light shine without overshadowing others." },
  Virgo: { prediction: "Mercury enhances analytical abilities. Perfect for detailed work.", lucky: "Number 5, Color Green", advice: "Don't let perfectionism paralyze progress." },
  Libra: { prediction: "Venus favors partnerships. Balance in all things brings peace.", lucky: "Number 6, Color Pastel Blue", advice: "Make that decision you've been avoiding." },
  Scorpio: { prediction: "Transformative energy surrounds you. Deep insights come naturally.", lucky: "Number 8, Color Dark Red", advice: "Trust the process of transformation." },
  Sagittarius: { prediction: "Jupiter expands your horizons. Travel or higher learning favored.", lucky: "Number 3, Color Purple", advice: "Aim your arrow with intention." },
  Capricorn: { prediction: "Saturn rewards discipline. Long-term projects gain momentum.", lucky: "Number 8, Color Black", advice: "Patience is your greatest asset today." },
  Aquarius: { prediction: "Unconventional solutions emerge. Innovation is your superpower.", lucky: "Number 4, Color Electric Blue", advice: "Embrace what makes you different." },
  Pisces: { prediction: "Intuition is heightened. Spiritual practices bring clarity.", lucky: "Number 7, Color Sea Green", advice: "Trust your inner voice over external noise." },
};
