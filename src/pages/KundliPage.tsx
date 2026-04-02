import React, { useState, useCallback, useEffect, useRef, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { KundliChart } from "@/components/charts/KundliChart";
import { samplePlanets, useKundliStore, type PlanetData } from "@/store/kundliStore";
import { useKundali, useSavedKundali } from "@/hooks/useKundali";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";
import { Link } from "react-router-dom";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Zap, FileDown, ChevronDown, ChevronUp, Star, Calendar,
  Layers, Home, Loader2, Edit2, User, MapPin, AlertTriangle, BookOpen, Share2, Check,
} from "lucide-react";
import PageBot from "@/components/PageBot";
import { searchCities, getCities, type City } from "@/lib/cities";
import type { KundaliResponse, VargaChartData, AntardashaData, UpagrahaEntry, ShadbalaPlanet, PratyantarData } from "@/types/api";
import { useTranslation } from "react-i18next";

// ── Constants ──────────────────────────────────────────────────────────────────

// Rashi index (0=Mesha … 11=Meena) → planet lord
const RASHI_LORDS_IDX: Record<number, string> = {
  0:"Mangal",1:"Shukra",2:"Budh",3:"Chandra",4:"Surya",5:"Budh",
  6:"Shukra",7:"Mangal",8:"Guru",9:"Shani",10:"Shani",11:"Guru",
};
const RASHI_LIST_IDX = [
  "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
  "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena",
];
const NAKSHATRA_NAMES = [
  "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra",
  "Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni","Uttara Phalguni",
  "Hasta","Chitra","Swati","Vishakha","Anuradha","Jyeshtha",
  "Moola","Purva Ashadha","Uttara Ashadha","Shravana","Dhanishtha",
  "Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati",
];
const NAKSHATRA_LORDS = [
  "Ketu","Shukra","Surya","Chandra","Mangal","Rahu",
  "Guru","Shani","Budh","Ketu","Shukra","Surya",
  "Chandra","Mangal","Rahu","Guru","Shani","Budh",
  "Ketu","Shukra","Surya","Chandra","Mangal","Rahu",
  "Guru","Shani","Budh",
];
/** Compute nakshatra, pada, lord from absolute sidereal longitude (0–360°) */
function getNakshatraFromLon(absLon: number): { nakshatra: string; pada: number; lord: string } {
  const segSize = 360 / 27; // 13.333...°
  const idx = Math.floor(absLon / segSize) % 27;
  const pada = Math.floor((absLon % segSize) / (segSize / 4)) + 1;
  return { nakshatra: NAKSHATRA_NAMES[idx], pada, lord: NAKSHATRA_LORDS[idx] };
}
/** Houses ruled by `gn` given the lagna rashi name */
function getRulerOf(gn: string, lagnaRashi: string): number[] {
  const lagnaIdx = RASHI_LIST_IDX.indexOf(lagnaRashi);
  if (lagnaIdx < 0) return [];
  const houses = Object.entries(RASHI_LORDS_IDX)
    .filter(([, lord]) => lord === gn)
    .map(([ri]) => (Number(ri) - lagnaIdx + 12) % 12 + 1);
  // Rahu co-rules Kumbha (idx 10), Ketu co-rules Vrischika (idx 7)
  if (gn === "Rahu") houses.push((10 - lagnaIdx + 12) % 12 + 1);
  if (gn === "Ketu") houses.push((7  - lagnaIdx + 12) % 12 + 1);
  return houses;
}

// Extra aspect offsets (all planets have 7th = offset 6)
const SPECIAL_ASPECTS: Record<string, number[]> = {
  Mangal: [3, 7],   // 4th and 8th
  Guru:   [4, 8],   // 5th and 9th
  Shani:  [2, 9],   // 3rd and 10th
};

/** Compute map of house → aspecting planet names from grahaRows.
 *  Rahu/Ketu are chaya grahas — no graha drishti in classical Parashara. */
function computeAspects(rows: Array<{ gn: string; house: number }>): Record<number, string[]> {
  const result: Record<number, string[]> = {};
  for (let h = 1; h <= 12; h++) result[h] = [];
  for (const { gn, house } of rows) {
    if (gn === "Rahu" || gn === "Ketu") continue;
    const offsets = [6, ...(SPECIAL_ASPECTS[gn] ?? [])];
    for (const off of offsets) {
      const aspH = (house - 1 + off) % 12 + 1;
      result[aspH].push(gn);
    }
  }
  return result;
}

const GRAHA_SYMBOLS: Record<string, string> = {
  Surya:"☉︎", Chandra:"☽︎", Mangal:"♂︎", Budh:"☿︎",
  Guru:"♃︎", Shukra:"♀︎", Shani:"♄︎", Rahu:"☊︎", Ketu:"☋︎",
};
const GRAHA_EN: Record<string, string> = {
  Surya:"Sun", Chandra:"Moon", Mangal:"Mars", Budh:"Mercury",
  Guru:"Jupiter", Shukra:"Venus", Shani:"Saturn", Rahu:"Rahu", Ketu:"Ketu",
};
const GRAHA_COLOR: Record<string, string> = {
  Surya:"hsl(42 90% 64%)", Chandra:"hsl(220 20% 85%)", Mangal:"hsl(0 70% 60%)",
  Budh:"hsl(140 55% 55%)", Guru:"hsl(42 70% 58%)", Shukra:"hsl(292 75% 65%)",
  Shani:"hsl(220 30% 55%)", Rahu:"hsl(225 35% 60%)", Ketu:"hsl(30 60% 50%)",
};
const RASHI_SYMBOLS: Record<string, string> = {
  Mesha:"♈︎", Vrishabha:"♉︎", Mithuna:"♊︎", Karka:"♋︎",
  Simha:"♌︎", Kanya:"♍︎", Tula:"♎︎", Vrischika:"♏︎",
  Dhanu:"♐︎", Makara:"♑︎", Kumbha:"♒︎", Meena:"♓︎",
};
const STATUS_BADGE_CLS: Record<string, { cls: string; tKey: string }> = {
  "Uchcha (Exalted)":       { cls:"bg-emerald-500/20 text-emerald-400 border-emerald-500/30", tKey:"data.kundli.status_exalted" },
  "Neecha (Debilitated)":   { cls:"bg-red-500/20 text-red-400 border-red-500/30",             tKey:"data.kundli.status_debilitated" },
  "Svakshetra (Own)":       { cls:"bg-amber-500/20 text-amber-400 border-amber-500/30",       tKey:"data.kundli.status_own" },
  "Normal":                 { cls:"bg-muted/20 text-muted-foreground border-border/20",       tKey:"data.kundli.status_normal" },
};
const STATUS_BADGE = STATUS_BADGE_CLS; // alias kept for compat
const RELATIONSHIP_BADGE_CLS: Record<string, { cls: string; tKey: string }> = {
  "Own":     { cls:"bg-amber-500/20 text-amber-400", tKey:"data.kundli.rel_own" },
  "Friend":  { cls:"bg-emerald-500/15 text-emerald-400", tKey:"data.kundli.rel_friend" },
  "Enemy":   { cls:"bg-red-500/15 text-red-400", tKey:"data.kundli.rel_enemy" },
  "Neutral": { cls:"bg-muted/15 text-muted-foreground", tKey:"data.kundli.rel_neutral" },
};
const RELATIONSHIP_BADGE = RELATIONSHIP_BADGE_CLS;
const DASHA_COLORS: Record<string, string> = {
  Ketu:"#f97316", Shukra:"#a855f7", Surya:"#f59e0b", Chandra:"#94a3b8",
  Mangal:"#ef4444", Rahu:"#6366f1", Guru:"#eab308", Shani:"#64748b", Budh:"#22c55e",
};
const YOGA_CATEGORY_COLORS: Record<string, string> = {
  Power: "text-amber-400", Wealth: "text-emerald-400", Intellect: "text-blue-400",
  Spiritual: "text-purple-400", Marriage: "text-pink-400", Adversity: "text-red-400",
};

type DashaPred = { theme: string; positive: string; challenge: string; tip: string };
const _DP: Record<string, Record<string, DashaPred>> = {
  Surya: {
    Mesha:    {theme:"Authority & Self",    positive:"Leadership, recognition, government favor",  challenge:"Ego conflicts, bone/eye health",  tip:"Offer water to Sun daily, Ruby if lagna lord"},
    Vrishabha:{theme:"Wealth & Family",     positive:"Income from authority, father's blessings",  challenge:"Eye/bone issues, ego clashes",     tip:"Aditya Hridayam daily, red garnet"},
    Mithuna:  {theme:"Fame & Siblings",     positive:"Intellectual recognition, courage",          challenge:"Sibling tensions, travel stress",  tip:"Gayatri Mantra, donate wheat Sunday"},
    Karka:    {theme:"Home & Property",     positive:"Property gains, maternal happiness",         challenge:"Mother's health, mental stress",   tip:"Surya Yantra at home, water offering"},
    Simha:    {theme:"Royalty & Power",     positive:"Peak career, fame, leadership",              challenge:"Overconfidence, heart health",     tip:"Surya Chalisa, donate copper"},
    Kanya:    {theme:"Service & Health",    positive:"Systematic success, health improvement",     challenge:"Enemies, service disruptions",     tip:"Help the poor, Gayatri Mantra"},
    Tula:     {theme:"Partnerships",        positive:"Balanced decisions, legal victories",        challenge:"Marital tensions, legal disputes",  tip:"Sunday charity, Surya namaskar"},
    Vrischika:{theme:"Transformation",      positive:"Research, occult abilities",                challenge:"Accidents, hidden enemies",         tip:"Surya namaskar, control ego"},
    Dhanu:    {theme:"Wisdom & Fortune",    positive:"Spiritual growth, higher education",         challenge:"Father's health, travel fatigue",  tip:"Donate wheat Sundays, Aditya mantra"},
    Makara:   {theme:"Discipline & Power",  positive:"Political influence, rewards for discipline",challenge:"Setbacks, joint/bone issues",       tip:"Offer Arghya to Sun, Ruby"},
    Kumbha:   {theme:"Losses & Liberation", positive:"Spiritual insights, foreign success",        challenge:"Expenses, isolation",               tip:"Meditate, offer to the poor"},
    Meena:    {theme:"Gains & Network",     positive:"Income, social influence, eye for detail",  challenge:"Eye problems, overextension",       tip:"Aditya Hridayam, Sun worship"},
  },
  Chandra: {
    Mesha:    {theme:"Mind & Popularity",   positive:"Fame, real estate gains, public support",   challenge:"Mental instability, mother's health", tip:"Monday fasts, Pearl, milk to Shiva"},
    Vrishabha:{theme:"Wealth & Comfort",    positive:"Financial growth, property, luxury",        challenge:"Overindulgence, weight gain",         tip:"Pearl ring, donate white rice Monday"},
    Mithuna:  {theme:"Communication",       positive:"Travel, writing, social success",           challenge:"Mental restlessness, anxiety",        tip:"Chandra yantra, white flowers"},
    Karka:    {theme:"Home & Happiness",    positive:"Family joy, good health, peace",            challenge:"Emotional mood swings",              tip:"Fast Mondays, drink milk"},
    Simha:    {theme:"Creative Fame",       positive:"Creative recognition, popularity",          challenge:"Pride, stomach issues",               tip:"White flowers to Devi"},
    Kanya:    {theme:"Service & Analysis",  positive:"Healthcare success, detailed work",         challenge:"Overthinking, worry",                 tip:"Worship Saraswati, Pearl"},
    Tula:     {theme:"Relationships",       positive:"Love, social harmony, gains",               challenge:"Indecisiveness",                      tip:"Pearl, donate white cloth"},
    Vrischika:{theme:"Deep Transformation", positive:"Research, occult mastery",                  challenge:"Emotional turbulence",                tip:"Offer milk to Shiva Lingam"},
    Dhanu:    {theme:"Fortune & Travel",    positive:"Spiritual wisdom, good fortune",            challenge:"Restlessness, wandering",             tip:"White flowers at temple"},
    Makara:   {theme:"Discipline & Career", positive:"Career stability, discipline rewarded",     challenge:"Depression, emotional coldness",      tip:"Chandra mantra, Monday fasts"},
    Kumbha:   {theme:"Humanitarian Gains",  positive:"Social work, abroad earnings",              challenge:"Mental fog, isolation",               tip:"Offer water to Moon nightly"},
    Meena:    {theme:"Spiritual Gains",     positive:"Income, spirituality, foreign travel",      challenge:"Escapism, overidealism",              tip:"Pearl, worship Devi"},
  },
  Mangal: {
    Mesha:    {theme:"Energy & Leadership", positive:"Courage, sports success, new ventures",     challenge:"Accidents, conflicts, blood pressure", tip:"Hanuman Chalisa Tuesdays, Red Coral"},
    Vrishabha:{theme:"Wealth Battles",      positive:"Real estate gains, physical strength",      challenge:"Financial disputes, health",           tip:"Donate red lentils Tuesday"},
    Mithuna:  {theme:"Action & Speech",     positive:"Technical skills, bold communication",      challenge:"Siblings conflict, accidents",         tip:"Kartikeya worship, Red Coral"},
    Karka:    {theme:"Home & Property",     positive:"Property gains, strong family",             challenge:"Family conflicts, blood disorders",    tip:"Tuesday Hanuman puja"},
    Simha:    {theme:"Power & Authority",   positive:"Leadership, political power, recognition",  challenge:"Arrogance, heat-related issues",       tip:"Red Coral if benefic, Mangal mantra"},
    Kanya:    {theme:"Service & Enemies",   positive:"Victory over enemies, health discipline",   challenge:"Enemies, debt",                        tip:"Donate red cloth on Tuesdays"},
    Tula:     {theme:"Partnership Battles", positive:"Bold business moves, real estate",          challenge:"Marital conflicts, legal issues",      tip:"Hanuman worship, Tuesday fast"},
    Vrischika:{theme:"Deep Power",          positive:"Research, occult, surgery success",         challenge:"Surgeries, injuries",                  tip:"Karthikeya mantra, Red Coral"},
    Dhanu:    {theme:"Fortune & Drive",     positive:"Sports, adventure, higher learning",        challenge:"Recklessness, injuries",               tip:"Mangal yantra, Tuesday fast"},
    Makara:   {theme:"Ambition & Discipline",positive:"Career authority, disciplined effort",    challenge:"Knee/joint issues",                    tip:"Offer blood-red flowers Tuesday"},
    Kumbha:   {theme:"Gains & Action",      positive:"Income through action, technical jobs",     challenge:"Accidents, unexpected costs",          tip:"Red Coral, Hanuman Chalisa"},
    Meena:    {theme:"Losses & Moksha",     positive:"Spiritual drive, hidden strengths",         challenge:"Accidents, expenditure",               tip:"Hanuman puja, avoid rash decisions"},
  },
  Budh: {
    Mesha:    {theme:"Intelligence & Action",positive:"Business acumen, communication",          challenge:"Nervous system, indecision",           tip:"Emerald if benefic, feed green parrots"},
    Vrishabha:{theme:"Wealth & Speech",     positive:"Business gains, eloquent communication",   challenge:"Skin issues, overthinking",            tip:"Saraswati worship, Emerald"},
    Mithuna:  {theme:"Intellect & Skills",  positive:"Peak intellectual power, media success",   challenge:"Nervousness, skin issues",             tip:"Wednesday fasts, green moong donation"},
    Karka:    {theme:"Mind & Home",         positive:"Education, writing from home",             challenge:"Indecisiveness, digestive issues",     tip:"Budha yantra, Saraswati mantra"},
    Simha:    {theme:"Fame & Intellect",    positive:"Renowned speaker/writer, recognition",     challenge:"Ego in communication, pride",          tip:"Recite Budha Ashtakam"},
    Kanya:    {theme:"Service & Analysis",  positive:"Maximum intellectual power, health jobs",  challenge:"Overthinking, perfectionism",          tip:"Emerald, feed cows green grass"},
    Tula:     {theme:"Partnerships",        positive:"Business partnerships, balanced intellect", challenge:"Indecisiveness in relationships",     tip:"Wednesday worship, green charity"},
    Vrischika:{theme:"Research & Depth",    positive:"Research, occult studies, investigative",  challenge:"Suspicious nature, nervous disorders", tip:"Saraswati puja"},
    Dhanu:    {theme:"Wisdom & Learning",   positive:"Academic success, philosophical writing",  challenge:"Scattered focus",                      tip:"Donate books on Wednesdays"},
    Makara:   {theme:"Practical Intellect", positive:"Business systems, financial planning",     challenge:"Dry communication",                   tip:"Emerald, Wednesday fast"},
    Kumbha:   {theme:"Humanitarian Intellect",positive:"Social media, tech innovation",          challenge:"Eccentric thinking",                  tip:"Budha mantra, green charity"},
    Meena:    {theme:"Spiritual Wisdom",    positive:"Creative writing, imagination, spirituality",challenge:"Confusion, impracticality",           tip:"Donate green cloth Wednesday"},
  },
  Guru: {
    Mesha:    {theme:"Fortune & Expansion", positive:"Legal wins, teaching, children blessed",   challenge:"Weight gain, overoptimism",            tip:"Yellow Sapphire, Thursday fast"},
    Vrishabha:{theme:"Wealth & Wisdom",     positive:"Financial boom, family happiness",         challenge:"Overindulgence",                       tip:"Donate yellow sweets Thursday"},
    Mithuna:  {theme:"Intellect & Fortune", positive:"Higher education, publishing, travel",     challenge:"Liver/fat issues",                     tip:"Guru mantra, Yellow Sapphire"},
    Karka:    {theme:"Home & Blessings",    positive:"Family blessings, property, children",     challenge:"Overprotection",                       tip:"Brihaspati Vrata Thursdays"},
    Simha:    {theme:"Royalty & Dharma",    positive:"Leadership recognition, spiritual authority",challenge:"Pride in knowledge",                 tip:"Worship Dakshinamurti"},
    Kanya:    {theme:"Service & Analysis",  positive:"Teaching, medical success, systematic work",challenge:"Over-analysis, weight",               tip:"Yellow Sapphire, donate turmeric"},
    Tula:     {theme:"Relationships",       positive:"Marriage blessings, business partnerships", challenge:"Relationship overextension",          tip:"Thursday fast, yellow cloth donation"},
    Vrischika:{theme:"Transformation",      positive:"Research, spiritual depth, healing",        challenge:"Hidden enemies, liver",               tip:"Guru yantra, Brihaspati Vrata"},
    Dhanu:    {theme:"Peak Fortune",        positive:"Maximum expansion, travel, wisdom, wealth", challenge:"Overconfidence, travel excess",        tip:"Yellow Sapphire, Thursday worship"},
    Makara:   {theme:"Discipline & Wisdom", positive:"Structured growth, senior recognition",    challenge:"Delayed rewards",                      tip:"Donate yellow on Thursdays"},
    Kumbha:   {theme:"Humanitarian",        positive:"Social causes, innovation, income",        challenge:"Unconventional path",                 tip:"Feed Brahmin on Thursdays"},
    Meena:    {theme:"Spiritual Fortune",   positive:"Spiritual growth, abroad success, wisdom",  challenge:"Idealism, overgiving",                tip:"Worship Brihaspati, Yellow Sapphire"},
  },
  Shukra: {
    Mesha:    {theme:"Relationships & Luxury",positive:"Love, beauty, artistic success",        challenge:"Laziness, relationship turbulence",     tip:"Diamond/White Sapphire, Friday fast"},
    Vrishabha:{theme:"Wealth & Pleasure",   positive:"Maximum luxury, love, financial growth",   challenge:"Overindulgence",                       tip:"Worship Lakshmi on Fridays"},
    Mithuna:  {theme:"Arts & Communication",positive:"Artistic/media success, pleasant speech",  challenge:"Fickleness in relationships",          tip:"Diamond, Friday Lakshmi puja"},
    Karka:    {theme:"Home & Beauty",       positive:"Beautiful home, happy family",             challenge:"Emotional in love",                   tip:"Offer white flowers to Lakshmi"},
    Simha:    {theme:"Fame & Arts",         positive:"Fame in arts, creative recognition",       challenge:"Pride in beauty",                      tip:"Friday fast, white flowers"},
    Kanya:    {theme:"Service & Refinement",positive:"Fine arts, healthcare aesthetics",         challenge:"Perfectionism in love",                tip:"White Sapphire, Lakshmi mantra"},
    Tula:     {theme:"Partnerships & Luxury",positive:"Peak relationships, wealth, beauty",      challenge:"Over-dependence on partners",          tip:"Diamond, Friday puja"},
    Vrischika:{theme:"Depth & Passion",     positive:"Deep love, research in arts",              challenge:"Jealousy, hidden conflicts",            tip:"Offer lotus to Lakshmi"},
    Dhanu:    {theme:"Fortune & Romance",   positive:"Foreign love, artistic recognition",       challenge:"Excess, overindulgence",               tip:"White Sapphire, Friday charity"},
    Makara:   {theme:"Disciplined Beauty",  positive:"Wealth through discipline, business growth",challenge:"Cold in relationships",               tip:"Friday fast, donate white sweets"},
    Kumbha:   {theme:"Humanitarian Beauty", positive:"Income from arts, social charm",           challenge:"Eccentric relationships",              tip:"Diamond, donate on Fridays"},
    Meena:    {theme:"Spiritual Love",      positive:"Spiritual love, artistic vision, gains",   challenge:"Overidealism in relationships",        tip:"Worship Lakshmi, White Sapphire"},
  },
  Shani: {
    Mesha:    {theme:"Discipline & Delays", positive:"Hard work rewarded, endurance built",      challenge:"Delays, health issues, obstacles",     tip:"Blue Sapphire if benefic, Saturday fasts"},
    Vrishabha:{theme:"Wealth Through Work", positive:"Slow but steady wealth, discipline",       challenge:"Financial delays, family issues",      tip:"Donate sesame oil Saturday"},
    Mithuna:  {theme:"Karma & Skills",      positive:"Technical mastery, communications",        challenge:"Slow progress, isolation",             tip:"Shani mantra, Saturday fast"},
    Karka:    {theme:"Home Karma",          positive:"Property through hard work, discipline",   challenge:"Mother's health, family burdens",      tip:"Offer sesame to Shani Dev"},
    Simha:    {theme:"Power Through Karma", positive:"Political success through discipline",     challenge:"Ego challenged, delays",               tip:"Shani yantra, Saturday puja"},
    Kanya:    {theme:"Service & Karma",     positive:"Methodical success, health jobs",          challenge:"Perfectionist delays",                 tip:"Blue Sapphire if 5th/6th lord, donate"},
    Tula:     {theme:"Exaltation Period",   positive:"Peak career, justice served, rewards",     challenge:"Slow start then great rewards",        tip:"Blue Sapphire, Saturday worship"},
    Vrischika:{theme:"Deep Transformation", positive:"Research, discipline, occult mastery",     challenge:"Painful transformation, losses",       tip:"Shani puja, feed crows"},
    Dhanu:    {theme:"Wisdom Through Work", positive:"Academic/spiritual discipline rewarded",   challenge:"Travel restrictions, philosophical doubts",tip:"Saturday fasts, donate black sesame"},
    Makara:   {theme:"Maximum Power",       positive:"Authority, career peak, discipline wins",  challenge:"Isolation, cold demeanor",             tip:"Blue Sapphire, Shani Chalisa"},
    Kumbha:   {theme:"Humanitarian Karma",  positive:"Social service, technology career",        challenge:"Delayed social recognition",           tip:"Donate to the poor on Saturdays"},
    Meena:    {theme:"Liberation & Loss",   positive:"Spiritual liberation, hidden wisdom",      challenge:"Losses, isolation, self-undoing",      tip:"Hanuman worship, Saturday fast"},
  },
  Rahu: {
    Mesha:    {theme:"Ambition & Drive",    positive:"Unconventional success, sudden rise",      challenge:"Accidents, rash decisions",            tip:"Gomed (Hessonite), Saturday Rahu puja"},
    Vrishabha:{theme:"Wealth Obsession",    positive:"Sudden wealth, foreign business",          challenge:"Greed, overindulgence",                tip:"Rahu yantra, donate on Saturdays"},
    Mithuna:  {theme:"Communication Fame",  positive:"Media success, public influence",          challenge:"Deception, confusion",                 tip:"Feed parrots, avoid lies"},
    Karka:    {theme:"Home Disruption",     positive:"Foreign property gains",                   challenge:"Family disruption, mental fog",        tip:"Offer coconut to Rahu on Saturdays"},
    Simha:    {theme:"Fame Distortion",     positive:"Unconventional fame, sudden recognition",  challenge:"False reputation, inflated ego",       tip:"Rahu mantra, avoid ego trips"},
    Kanya:    {theme:"Service Innovation",  positive:"Tech/medical innovation success",          challenge:"Health complications, enemies",        tip:"Gomed, service to outcastes"},
    Tula:     {theme:"Relationship Illusions",positive:"Unusual partnerships, foreign love",     challenge:"Deceptive partners",                   tip:"Rahu puja, Gomed if chart suits"},
    Vrischika:{theme:"Occult Mastery",      positive:"Research, hidden knowledge, tantra",       challenge:"Dangerous obsessions",                 tip:"Rahu-Ketu Shanti puja"},
    Dhanu:    {theme:"False Wisdom",        positive:"Foreign education, philosophical breakthroughs",challenge:"False gurus, overconfidence",       tip:"Naga Puja, Gomed"},
    Makara:   {theme:"Power Games",         positive:"Political influence, career success",      challenge:"Unethical shortcuts",                  tip:"Saturday fast, Shani-Rahu puja"},
    Kumbha:   {theme:"Technology & Innovation",positive:"Tech success, foreign gains",           challenge:"Eccentric behavior",                   tip:"Rahu yantra, gomed"},
    Meena:    {theme:"Spiritual Confusion",  positive:"Foreign spirituality, liberation",         challenge:"Deception, addiction",                 tip:"Naga Puja, offer milk to Shiva"},
  },
  Ketu: {
    Mesha:    {theme:"Detachment & Liberation",positive:"Spiritual powers, moksha wisdom",      challenge:"Accidents, separation, identity loss",  tip:"Cat's Eye, Ketu puja"},
    Vrishabha:{theme:"Material Detachment",  positive:"Spiritual wealth, moksha insights",       challenge:"Financial losses, detachment from family",tip:"Cat's Eye, Ganesha worship"},
    Mithuna:  {theme:"Communication Loss",   positive:"Intuitive wisdom, spiritual expression",  challenge:"Communication barriers, travel accidents",tip:"Ketu mantra, fast Saturday"},
    Karka:    {theme:"Home & Mother",        positive:"Spiritual home, past-life connections",   challenge:"Mother's health, domestic isolation",  tip:"Offer to ancestors, Shraddha"},
    Simha:    {theme:"Fame Detachment",      positive:"Spiritual fame, non-attachment to glory", challenge:"Loss of authority, ego dissolution",   tip:"Cat's Eye, Sun worship"},
    Kanya:    {theme:"Service & Karma",      positive:"Medical intuition, service-oriented gains",challenge:"Health issues, perfectionism",         tip:"Cat's Eye, serve the sick"},
    Tula:     {theme:"Relationship Karma",   positive:"Spiritual partnerships, karmic love",     challenge:"Relationship losses, detachment",      tip:"Ketu puja, serve saints"},
    Vrischika:{theme:"Occult Liberation",    positive:"Deep spiritual powers, past-life wisdom", challenge:"Obsessions, accidents",                tip:"Cat's Eye, Naga Puja"},
    Dhanu:    {theme:"Wisdom & Detachment",  positive:"Spiritual wisdom, guru connections",      challenge:"Lack of focus, wandering",             tip:"Worship Dakshinamurti, Cat's Eye"},
    Makara:   {theme:"Karma Clearance",      positive:"Past karma cleared, spiritual discipline", challenge:"Career setbacks, isolation",           tip:"Shani-Ketu puja, serve elders"},
    Kumbha:   {theme:"Humanitarian Liberation",positive:"Social service, spiritual innovation", challenge:"Aimlessness, eccentric behavior",       tip:"Donate to orphans"},
    Meena:    {theme:"Moksha",               positive:"Maximum spiritual liberation, moksha",    challenge:"Confusion, loss of worldly direction",  tip:"Cat's Eye, Kashi pilgrimage"},
  },
};

// All varga charts — BC + D-1 to D-60
const VARGA_QUICK = [
  { div: 0,  code: "BC",   name: "Bhav Chalit",  desc: "Placidus House Positions" },
  { div: 1,  code: "D-1",  name: "Rashi",        desc: "Body, Personality" },
  { div: 2,  code: "D-2",  name: "Hora",         desc: "Wealth" },
  { div: 3,  code: "D-3",  name: "Drekkana",     desc: "Siblings" },
  { div: 4,  code: "D-4",  name: "Chaturthamsha",desc: "Fortune & Property" },
  { div: 5,  code: "D-5",  name: "Panchamsha",   desc: "Fame, Power" },
  { div: 6,  code: "D-6",  name: "Shashtiamsha", desc: "Health, Enemies" },
  { div: 7,  code: "D-7",  name: "Saptamsha",    desc: "Children" },
  { div: 8,  code: "D-8",  name: "Ashtamsha",    desc: "Sudden Events" },
  { div: 9,  code: "D-9",  name: "Navamsha",     desc: "Marriage, Soul" },
  { div: 10, code: "D-10", name: "Dashamsha",    desc: "Career" },
  { div: 11, code: "D-11", name: "Ekadashamsha", desc: "Gains, Income" },
  { div: 12, code: "D-12", name: "Dwadashamsha", desc: "Parents" },
  { div: 16, code: "D-16", name: "Shodashamsha", desc: "Vehicles, Comforts" },
  { div: 20, code: "D-20", name: "Vimsamsha",    desc: "Spirituality" },
  { div: 24, code: "D-24", name: "Chaturvimsha", desc: "Education" },
  { div: 27, code: "D-27", name: "Nakshatramsha",desc: "Strength" },
  { div: 30, code: "D-30", name: "Trimsamsha",   desc: "Evils, Karma" },
  { div: 40, code: "D-40", name: "Khavedamsha",  desc: "Auspicious Effects" },
  { div: 45, code: "D-45", name: "Akshavedamsha",desc: "Character" },
  { div: 60, code: "D-60", name: "Shashtiamsha", desc: "Past Life" },
];

// Rashi short names & qualities
const RASHI_SHORT: Record<string, string> = {
  Mesha:"Mesh", Vrishabha:"Vrish", Mithuna:"Mith", Karka:"Kark",
  Simha:"Simh", Kanya:"Kany", Tula:"Tula", Vrischika:"Vris",
  Dhanu:"Dhan", Makara:"Maka", Kumbha:"Kumb", Meena:"Meen",
};
const RASHI_QUALITY: Record<string, string> = {
  Mesha:"Mas, Movable", Vrishabha:"Fem, Fixed", Mithuna:"Mas, Common", Karka:"Fem, Movable",
  Simha:"Mas, Fixed", Kanya:"Fem, Common", Tula:"Mas, Movable", Vrischika:"Fem, Fixed",
  Dhanu:"Mas, Common", Makara:"Fem, Movable", Kumbha:"Mas, Fixed", Meena:"Fem, Common",
};
function degToDMS(degree: number, rashiName: string): string {
  const d = Math.floor(degree);
  const mf = (degree - d) * 60;
  const m = Math.floor(mf);
  const s = Math.floor((mf - m) * 60);
  const rs = RASHI_SHORT[rashiName] ?? rashiName.slice(0, 4);
  return `${d}° ${rs} ${String(m).padStart(2,"0")}' ${String(s).padStart(2,"0")}″`;
}

const GRAHA_ORDER = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani","Rahu","Ketu"];

// ── Helpers ────────────────────────────────────────────────────────────────────
function isCurrentDasha(start: string, end: string): boolean {
  const now = new Date();
  return new Date(start) <= now && now <= new Date(end);
}
function formatDate(d: string): string {
  try { return new Date(d).toLocaleDateString("en-IN", { day:"2-digit", month:"short", year:"numeric" }); } catch { return d; }
}
function toPlanetData(gname: string, g: any): PlanetData {
  return {
    name: GRAHA_EN[gname] ?? gname,
    symbol: GRAHA_SYMBOLS[gname] ?? "?",
    rashi: g.rashi, house: g.house,
    degree: `${g.degree?.toFixed?.(1) ?? g.degree ?? 0}°`,
    nakshatra: g.nakshatra ?? "",
    color: GRAHA_COLOR[gname] ?? "hsl(0 0% 60%)",
    sanskritName: gname,
  };
}

// ── PDF Generator ─────────────────────────────────────────────────────────────
function generateKundaliPDF(data: KundaliResponse) {
  const planetRows = GRAHA_ORDER.map(gn => {
    const g = data.grahas[gn];
    if (!g) return "";
    const retro = g.retro ? " ℞" : "";
    const statusColor = g.status.includes("Exalted") ? "#10b981"
      : g.status.includes("Debilitated") ? "#ef4444"
      : g.status.includes("Own") ? "#f59e0b" : "#6b7280";
    const relColor = g.relationship === "Friend" ? "#10b981" : g.relationship === "Enemy" ? "#ef4444" : "#6b7280";
    return `<tr>
      <td>${GRAHA_SYMBOLS[gn] ?? ""} ${gn} (${GRAHA_EN[gn]})</td>
      <td>${g.rashi} ${RASHI_SYMBOLS[g.rashi] ?? ""} ${g.rashi_en ? `<span style="color:#9ca3af;font-size:9px">${g.rashi_en}</span>` : ""}</td>
      <td style="text-align:center">H${g.house}</td>
      <td style="text-align:center">${g.degree.toFixed(2)}°${g.longitude ? ` <span style="color:#9ca3af;font-size:8px">(${g.longitude.toFixed(2)}°)</span>` : ""}</td>
      <td>${g.nakshatra} (${g.nakshatra_lord ?? ""})</td>
      <td style="text-align:center">P${g.pada}${retro}</td>
      <td style="color:${statusColor};font-weight:600">${g.status.split(" ")[0]}</td>
      <td style="color:${relColor};font-size:9px">${g.relationship ?? ""}</td>
    </tr>`;
  }).join("");

  const navRows = GRAHA_ORDER.map(gn => {
    const g = data.navamsha?.[gn];
    if (!g) return "";
    const statusColor = g.status.includes("Exalted") ? "#10b981"
      : g.status.includes("Debilitated") ? "#ef4444"
      : g.status.includes("Own") ? "#f59e0b" : "#6b7280";
    return `<tr>
      <td>${GRAHA_SYMBOLS[gn] ?? ""} ${gn}</td>
      <td>${g.rashi} ${RASHI_SYMBOLS[g.rashi] ?? ""}</td>
      <td style="text-align:center">H${g.house}</td>
      <td style="color:${statusColor};font-weight:600">${g.status.split(" ")[0]}</td>
      <td>${g.retro ? "℞" : ""}</td>
    </tr>`;
  }).join("");

  const dashaRows = data.dashas.map(d => {
    const cur = isCurrentDasha(d.start, d.end);
    const antarRows = d.antardashas?.map(a => {
      const aCur = isCurrentDasha(a.start, a.end);
      return `<tr style="font-size:9px;${aCur ? "background:#fffbeb;" : ""}">
        <td style="padding-left:24px">↳ ${a.lord}</td>
        <td>${formatDate(a.start)}</td><td>${formatDate(a.end)}</td>
        <td style="text-align:center">${a.years}y</td>
        <td style="color:${aCur ? "#b45309" : "#6b7280"}">${aCur ? "Active" : ""}</td>
      </tr>`;
    }).join("") ?? "";
    return `<tr style="${cur ? "background:#fffbeb;font-weight:600" : ""}">
      <td>${cur ? "★ " : ""}${d.lord} (${GRAHA_EN[d.lord] ?? d.lord})</td>
      <td>${formatDate(d.start)}</td><td>${formatDate(d.end)}</td>
      <td style="text-align:center">${d.years} yrs</td>
      <td style="color:${cur ? "#b45309" : "#6b7280"}">${cur ? "Active Now" : ""}</td>
    </tr>${antarRows}`;
  }).join("");

  const yogaRows = data.yogas.map(y => {
    const strengthColor = y.strength === "Very Strong" ? "#10b981" : y.strength === "Strong" ? "#f59e0b" : "#6b7280";
    return `<tr>
      <td style="font-weight:600">${y.name}</td>
      <td style="color:${strengthColor}">${y.strength}</td>
      <td style="font-size:10px;color:#6b7280">${y.category ?? ""}</td>
      <td style="font-size:10px">${y.desc}</td>
    </tr>`;
  }).join("");

  const houseRows = data.houses.map(h => `<tr>
    <td style="text-align:center;font-weight:600">H${h.house}</td>
    <td>${h.rashi} ${RASHI_SYMBOLS[h.rashi] ?? ""} ${h.rashi_en ? `<span style="color:#9ca3af;font-size:9px">(${h.rashi_en})</span>` : ""}</td>
    <td>${h.lord} ${h.lord_en ? `(${h.lord_en})` : ""}</td>
    <td style="font-size:9px;color:#6b7280">${h.planets?.join(", ") ?? ""}</td>
    <td style="font-size:9px;color:#6b7280">${h.signification ?? ""}</td>
  </tr>`).join("");

  const ayan = data.ayanamsha_label ?? "Lahiri";

  const html = `<!DOCTYPE html><html><head><meta charset="UTF-8">
<title>Janam Kundali — ${data.name || "Birth Chart"}</title>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { font-family: Georgia, 'Times New Roman', serif; background:#fff; color:#1a1a2e; font-size:11px; line-height:1.4; }
  .page { width:210mm; min-height:297mm; margin:0 auto; padding:12mm 14mm; }
  h2 { font-size:13px; color:#6d28d9; border-bottom:2px solid #ede9fe; padding-bottom:3px; margin:14px 0 6px; }
  .header-bar { background:linear-gradient(135deg,#1e1b4b,#312e81); color:#fff; padding:16px 20px; border-radius:10px; text-align:center; margin-bottom:14px; }
  .header-bar h1 { color:#fbbf24; font-size:20px; margin-bottom:3px; }
  .header-bar .sub { color:#c4b5fd; font-size:10px; }
  .meta-row { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; margin-bottom:12px; }
  .meta-card { border:1px solid #e5e7eb; border-radius:6px; padding:8px 10px; }
  .meta-label { font-size:9px; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; }
  .meta-val { font-size:12px; font-weight:600; color:#1a1a2e; margin-top:1px; }
  table { width:100%; border-collapse:collapse; font-size:10px; margin-bottom:4px; }
  th { background:#ede9fe; color:#4c1d95; padding:4px 6px; text-align:left; font-size:9px; font-weight:600; }
  td { padding:3px 6px; border-bottom:1px solid #f3f4f6; vertical-align:top; }
  tr:nth-child(even) td { background:#fafafa; }
  .lagna-row { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; margin:8px 0 12px; }
  .lagna-card { border:2px solid #fbbf24; border-radius:8px; padding:8px; text-align:center; }
  .lagna-main { font-size:18px; font-weight:bold; color:#b45309; }
  .lagna-sub { font-size:9px; color:#6b7280; margin-top:1px; }
  .two-col { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
  .footer { margin-top:14px; padding-top:8px; border-top:1px solid #e5e7eb; font-size:8px; color:#9ca3af; text-align:center; }
  @media print { body { -webkit-print-color-adjust:exact; print-color-adjust:exact; } .page { padding:8mm; } }
  .page-break { page-break-before:always; }
</style>
</head><body><div class="page">

<div class="header-bar">
  <h1>☽︎ Janam Kundali — Vedic Birth Chart</h1>
  <div class="sub">${ayan} Ayanamsha · Whole Sign Houses · ${data.rahu_mode === "true" ? "True" : "Mean"} Rahu/Ketu</div>
  <div style="margin-top:6px;font-size:16px;color:#fbbf24;font-weight:bold">${data.name || "—"}</div>
  <div style="font-size:10px;color:#c4b5fd;margin-top:2px">${data.place || ""} · ${data.birth_date}</div>
</div>

<div class="meta-row">
  <div class="meta-card">
    <div class="meta-label">Coordinates</div>
    <div class="meta-val">${data.lat.toFixed(4)}°N, ${data.lon.toFixed(4)}°E</div>
  </div>
  <div class="meta-card">
    <div class="meta-label">Timezone</div>
    <div class="meta-val">UTC +${data.tz}</div>
  </div>
  <div class="meta-card">
    <div class="meta-label">Ayanamsha (${ayan})</div>
    <div class="meta-val">${data.ayanamsha}°</div>
  </div>
  <div class="meta-card">
    <div class="meta-label">Lagna Degree</div>
    <div class="meta-val">${data.lagna.degree}° ${data.lagna.rashi}</div>
  </div>
</div>

<h2>Lagna &amp; Key Positions</h2>
<div class="lagna-row">
  <div class="lagna-card">
    <div class="lagna-sub">Lagna (Ascendant)</div>
    <div class="lagna-main">${data.lagna.rashi} ${RASHI_SYMBOLS[data.lagna.rashi] ?? ""}</div>
    <div class="lagna-sub">${data.lagna.nakshatra} · Pada ${data.lagna.pada ?? ""} · ${data.lagna.degree}°</div>
  </div>
  <div class="lagna-card" style="border-color:#6366f1">
    <div class="lagna-sub">Moon (Chandra)</div>
    <div class="lagna-main" style="color:#4f46e5">${data.grahas["Chandra"]?.rashi ?? ""}</div>
    <div class="lagna-sub">${data.grahas["Chandra"]?.nakshatra ?? ""} · P${data.grahas["Chandra"]?.pada ?? ""}</div>
  </div>
  <div class="lagna-card" style="border-color:#f59e0b">
    <div class="lagna-sub">Sun (Surya)</div>
    <div class="lagna-main" style="color:#b45309">${data.grahas["Surya"]?.rashi ?? ""}</div>
    <div class="lagna-sub">${data.grahas["Surya"]?.nakshatra ?? ""} · ${data.grahas["Surya"]?.status?.split(" ")[0] ?? ""}</div>
  </div>
  <div class="lagna-card" style="border-color:#eab308">
    <div class="lagna-sub">Jupiter (Guru)</div>
    <div class="lagna-main" style="color:#a16207">${data.grahas["Guru"]?.rashi ?? ""}</div>
    <div class="lagna-sub">H${data.grahas["Guru"]?.house} · ${data.grahas["Guru"]?.status?.split(" ")[0] ?? ""}</div>
  </div>
</div>

<h2>All Planet Positions (D-1 Rashi Chart)</h2>
<table>
  <thead><tr>
    <th>Planet</th><th>Rashi</th><th style="text-align:center">House</th>
    <th style="text-align:center">Degree</th><th>Nakshatra (Lord)</th>
    <th style="text-align:center">Pada/℞</th><th>Status</th><th>Relation</th>
  </tr></thead>
  <tbody>${planetRows}</tbody>
</table>

<h2>Bhava Chart (Houses)</h2>
<table>
  <thead><tr><th style="text-align:center">House</th><th>Rashi</th><th>Lord</th><th>Planets</th><th>Signification</th></tr></thead>
  <tbody>${houseRows}</tbody>
</table>

<div class="page-break"></div>

<h2>Navamsha Chart (D-9) — ${data.navamsha_lagna?.rashi ?? ""} Lagna</h2>
<table>
  <thead><tr><th>Planet</th><th>Rashi</th><th style="text-align:center">House</th><th>Status</th><th>Retro</th></tr></thead>
  <tbody>${navRows || "<tr><td colspan='5' style='color:#9ca3af'>Not available</td></tr>"}</tbody>
</table>

<h2>Vimshottari Dasha (with Antardasha)</h2>
<table>
  <thead><tr><th>Dasha Lord</th><th>Start</th><th>End</th><th style="text-align:center">Duration</th><th>Status</th></tr></thead>
  <tbody>${dashaRows}</tbody>
</table>

<h2>Active Yogas</h2>
<table>
  <thead><tr><th>Yoga</th><th>Strength</th><th>Category</th><th>Description</th></tr></thead>
  <tbody>${yogaRows || "<tr><td colspan='4' style='color:#9ca3af'>No yogas detected</td></tr>"}</tbody>
</table>

<div class="footer">
  Generated by Brahm AI · ${ayan} Ayanamsha · ${data.rahu_mode === "true" ? "True" : "Mean"} Rahu/Ketu · Whole Sign Houses ·
  ${new Date().toLocaleDateString("en-IN", { day:"2-digit", month:"long", year:"numeric" })}<br>
  This chart is for reference. Consult a qualified Jyotishi for complete life guidance.
</div>

</div></body></html>`;

  const win = window.open("", "_blank");
  if (!win) return;
  win.document.write(html);
  win.document.close();
  setTimeout(() => win.print(), 800);
}

// ── Antardasha Expansion ───────────────────────────────────────────────────────
function AntardashaList({ antardashas }: { antardashas: AntardashaData[] }) {
  const [expandedAntar, setExpandedAntar] = useState<string | null>(null);
  return (
    <div className="ml-4 mt-1 space-y-0.5 border-l border-border/20 pl-2 overflow-x-auto scrollbar-none">
      {antardashas.map((a, i) => {
        const cur = isCurrentDasha(a.start, a.end);
        const hasPratyantar = a.pratyantardashas && a.pratyantardashas.length > 0;
        const isExp = expandedAntar === `${i}`;
        return (
          <div key={i}>
            <div
              onClick={() => hasPratyantar && setExpandedAntar(isExp ? null : `${i}`)}
              className={`flex items-center gap-2 text-xs py-0.5 px-1.5 rounded ${cur ? "bg-amber-500/10 text-amber-300" : "text-muted-foreground"} ${hasPratyantar ? "cursor-pointer hover:bg-muted/20" : ""}`}
            >
              <div className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: DASHA_COLORS[a.lord] ?? "#888" }} />
              <span className="font-medium shrink-0" style={{minWidth:"4rem"}}>{a.lord}</span>
              <span className="shrink-0 text-[11px]">{formatDate(a.start)} → {formatDate(a.end)}</span>
              <span className="text-muted-foreground/60 shrink-0 ml-1">{a.years}y</span>
              {cur && <span className="text-xs text-amber-400 font-medium shrink-0 ml-1">★</span>}
              {hasPratyantar && <span className="ml-auto text-xs opacity-70 shrink-0">{isExp ? "▲" : "▼"}</span>}
            </div>
            {isExp && hasPratyantar && (
              <PratyantarList pratyantardashas={a.pratyantardashas!} />
            )}
          </div>
        );
      })}
    </div>
  );
}

// ── Pratyantar List (with Sukshma expand) ─────────────────────────────────────
function PratyantarList({ pratyantardashas }: { pratyantardashas: PratyantarData[] }) {
  const [expandedP, setExpandedP] = useState<string | null>(null);
  return (
    <div className="ml-4 mt-0.5 space-y-0.5 border-l border-border/10 pl-2">
      {pratyantardashas.map((p, j) => {
        const pCur = isCurrentDasha(p.start, p.end);
        const hasSukshma = p.sukshmadashas && p.sukshmadashas.length > 0;
        const isPExp = expandedP === `${j}`;
        return (
          <div key={j}>
            <div
              onClick={() => hasSukshma && setExpandedP(isPExp ? null : `${j}`)}
              className={`flex items-center gap-1.5 text-xs py-0.5 px-1 rounded ${pCur ? "bg-amber-500/10 text-amber-300" : "text-muted-foreground/70"} ${hasSukshma ? "cursor-pointer hover:bg-muted/10" : ""}`}
            >
              <div className="w-1 h-1 rounded-full shrink-0" style={{ background: DASHA_COLORS[p.lord] ?? "#888" }} />
              <span className="font-medium shrink-0" style={{minWidth:"3.8rem"}}>{p.lord}</span>
              <span className="shrink-0 text-[11px]">{formatDate(p.start)} → {formatDate(p.end)}</span>
              <span className="opacity-60 shrink-0 ml-1">{p.days}d</span>
              {pCur && <span className="text-amber-400 shrink-0 ml-1">★</span>}
              {hasSukshma && <span className="ml-auto opacity-60 shrink-0">{isPExp ? "▲" : "▼"}</span>}
            </div>
            {isPExp && hasSukshma && (
              <div className="ml-3 mt-0.5 space-y-0.5 border-l border-border/10 pl-2">
                {p.sukshmadashas!.map((s, k) => {
                  const sCur = isCurrentDasha(s.start.split(" ")[0], s.end.split(" ")[0]);
                  return (
                    <div key={k} className={`flex items-center gap-1.5 text-xs py-0.5 px-1 rounded ${sCur ? "text-amber-300" : "text-muted-foreground/70"}`}>
                      <div className="w-0.5 h-0.5 rounded-full shrink-0 opacity-60" style={{ background: DASHA_COLORS[s.lord] ?? "#888" }} />
                      <span className="font-medium shrink-0" style={{minWidth:"3.5rem"}}>{s.lord}</span>
                      <span className="shrink-0 text-[10px]">{s.start.slice(0,10)}</span>
                      <span className="opacity-60 shrink-0 ml-1">{s.hours}h</span>
                      {sCur && <span className="text-amber-400 shrink-0 ml-1">★</span>}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

// ── Birth Form ────────────────────────────────────────────────────────────────
interface LocalForm { name: string; date: string; time: string; place: string }

function KundaliBirthForm({
  initial, loading, error, onGenerate,
}: {
  initial: LocalForm;
  loading: boolean;
  error: string | null;
  onGenerate: (form: LocalForm, city: City | null) => void;
}) {
  const { t } = useTranslation();
  const [form, setForm] = useState<LocalForm>(initial);
  const [city, setCity] = useState<City | null>(null);
  const [suggestions, setSuggestions] = useState<City[]>([]);
  const wrapRef = useRef<HTMLDivElement>(null);

  useEffect(() => { getCities().catch(() => {}); }, []);
  useEffect(() => {
    const h = (e: MouseEvent) => { if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setSuggestions([]); };
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);

  const handleCityInput = async (v: string) => {
    setForm(f => ({ ...f, place: v }));
    setCity(null);
    if (v.length >= 2) { const res = await searchCities(v); setSuggestions(res); }
    else setSuggestions([]);
  };

  const handleSelect = (c: City) => {
    setForm(f => ({ ...f, place: c.name }));
    setCity(c); setSuggestions([]);
  };

  const canSubmit = !!form.date && !!form.time;

  return (
    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
      className="max-w-lg mx-auto">
      <div className="cosmic-card rounded-2xl p-5 space-y-4">
        <div className="flex items-center gap-2 mb-1">
          <Star className="h-4 w-4 text-primary" />
          <h2 className="font-display text-base text-foreground">{t("kundli.birth_details", { defaultValue: "Birth Details" })}</h2>
        </div>

        {/* Name */}
        <div>
          <Label className="text-xs text-muted-foreground">{t("kundli.name_label", { defaultValue: "Name" })} <span className="opacity-70">({t("kundli.optional", { defaultValue: "optional" })})</span></Label>
          <div className="relative mt-1">
            <User className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground/70" />
            <Input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
              placeholder={t("kundli.name_placeholder", { defaultValue: "Your name" })} className="pl-8 bg-muted/20 border-border/30" />
          </div>
        </div>

        {/* Date + Time */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label className="text-xs text-muted-foreground">{t("compatibility.dob_label")} <span className="text-red-400">*</span></Label>
            <Input type="date" value={form.date} onChange={e => setForm(f => ({ ...f, date: e.target.value }))}
              className="bg-muted/20 border-border/30 mt-1" />
          </div>
          <div>
            <Label className="text-xs text-muted-foreground">{t("compatibility.tob_label")} <span className="text-red-400">*</span></Label>
            <Input type="time" value={form.time} onChange={e => setForm(f => ({ ...f, time: e.target.value }))}
              className="bg-muted/20 border-border/30 mt-1" />
          </div>
        </div>

        {/* Place with autocomplete */}
        <div ref={wrapRef} className="relative">
          <Label className="text-xs text-muted-foreground">{t("compatibility.place_label")}</Label>
          <div className="relative mt-1">
            <MapPin className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground/70" />
            <Input value={form.place} onChange={e => handleCityInput(e.target.value)}
              placeholder={t("compatibility.place_placeholder")} autoComplete="off"
              className={`pl-8 bg-muted/20 border-border/30 ${city ? "border-primary/50" : ""}`} />
            {city && (
              <span className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-primary/70">
                ✓ {city.lat.toFixed(1)}°, {city.lon.toFixed(1)}°
              </span>
            )}
          </div>
          {suggestions.length > 0 && (
            <div className="absolute z-50 w-full mt-1 rounded-xl border border-border/30 bg-background/95 backdrop-blur shadow-xl overflow-hidden">
              {suggestions.map(c => (
                <button key={`${c.name}-${c.lat}`} onMouseDown={() => handleSelect(c)}
                  className="w-full text-left px-3 py-2.5 text-xs hover:bg-muted/40 transition-colors border-b border-border/10 last:border-0">
                  <span className="font-medium text-foreground">{c.name}</span>
                  <span className="text-muted-foreground ml-2">{c.lat.toFixed(1)}°N, {c.lon.toFixed(1)}°E</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {!city && form.place && (
          <p className="text-xs text-amber-400/80">{t("kundli.city_hint", { defaultValue: "Select a city from the dropdown for accurate coordinates." })}</p>
        )}

        {error && <p className="text-xs text-red-400 bg-red-500/10 rounded-lg px-3 py-2">{error}</p>}

        <button
          onClick={() => onGenerate(form, city)}
          disabled={!canSubmit || loading}
          className="w-full py-2.5 rounded-xl text-sm font-semibold bg-primary/20 hover:bg-primary/30 text-primary border border-primary/30 transition-colors disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {loading ? <><Loader2 className="h-4 w-4 animate-spin" /> {t("kundli.calculating", { defaultValue: "Calculating..." })}</> : <><Star className="h-4 w-4" /> {t("kundli.generate_btn")}</>}
        </button>
      </div>

      <p className="text-xs text-muted-foreground/60 text-center mt-3">
        {t("kundli.system_info", { defaultValue: "Lahiri Ayanamsha · Whole Sign Houses · Drik Siddhanta" })}
      </p>
    </motion.div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function KundliPage() {
  const { t } = useTranslation();
  const kundaliData = useKundliStore((s) => s.kundaliData);
  const birthDetails = useKundliStore((s) => s.birthDetails);
  const setBirthDetails = useKundliStore((s) => s.setBirthDetails);
  const settings = useKundliStore((s) => s.kundaliSettings);
  const setSettings = useKundliStore((s) => s.setKundaliSettings);
  const nameLang = settings.nameLang;

  // View: "input" = show form, "result" = show kundali
  const [view, setView] = useState<"input" | "result">(kundaliData ? "result" : "input");
  const [formError, setFormError] = useState<string | null>(null);

  // Check URL params for shared kundali
  const _sp = new URLSearchParams(typeof window !== "undefined" ? window.location.search : "");
  const _sharedForm: Partial<LocalForm> = _sp.has("dob") ? {
    name:  _sp.get("name") ?? "",
    date:  _sp.get("dob")  ?? "",
    time:  _sp.get("tob")  ?? "12:00",
    place: _sp.get("place") ?? "",
  } : {};
  const _sharedLoc = _sp.has("lat") ? { lat: Number(_sp.get("lat")), lon: Number(_sp.get("lon")), tz: Number(_sp.get("tz") ?? "5.5") } : null;

  // Local form — pre-fill from store or URL share params
  const [localForm, setLocalForm] = useState<LocalForm>({
    name:  _sharedForm.name  ?? birthDetails?.name          ?? "",
    date:  _sharedForm.date  ?? birthDetails?.dateOfBirth   ?? "",
    time:  _sharedForm.time  ?? birthDetails?.timeOfBirth   ?? "",
    place: _sharedForm.place ?? birthDetails?.birthPlace    ?? "",
  });

  const [selectedPlanet, setSelectedPlanet] = useState<PlanetData | null>(null);
  const [activeTab, setActiveTab] = useState<"charts" | "grahas" | "dasha" | "houses" | "chalit" | "strength" | "ashtaka" | "upagraha" | "yogas" | "lagna">("charts");
  const [expandedGraha, setExpandedGraha] = useState<{ side: "L"|"R"; gn: string } | null>(null);
  const [showAllYogas, setShowAllYogas] = useState(false);
  const [expandedYoga, setExpandedYoga] = useState<string | null>(null);
  const [expandedDasha, setExpandedDasha] = useState<string | null>(null);
  const [shareCopied, setShareCopied] = useState(false);
  // Dual chart state — div=0 means BC
  const [selectedVargaL, setSelectedVargaL] = useState(1);
  const [selectedVargaR, setSelectedVargaR] = useState(9);
  const [chartTabL, setChartTabL] = useState<"graha" | "bhava">("graha");
  const [chartTabR, setChartTabR] = useState<"graha" | "bhava">("graha");
  const [loadingVargaL, setLoadingVargaL] = useState(false);
  const [loadingVargaR, setLoadingVargaR] = useState(false);
  const [vargaCache, setVargaCache] = useState<Record<number, VargaChartData>>();

  const kundaliMutation = useKundali();
  useSavedKundali(); // Loads saved kundali from backend if not already in store

  const pageData = useMemo(() => kundaliData ? { kundali_raw: kundaliData } : {}, [kundaliData]);
  useRegisterPageBot('kundali', pageData);

  // When saved kundali loads from backend (new device / after logout), switch to result view
  useEffect(() => {
    if (kundaliData && view === "input") setView("result");
  }, [kundaliData]);

  const handleGenerate = useCallback(async (form: LocalForm, city: { lat: number; lon: number; tz: number } | null) => {
    setFormError(null);
    const DEF = { lat: 25.317, lon: 83.013, tz: 5.5 }; // Varanasi fallback (select city from dropdown for accuracy)
    const loc = city ?? DEF;
    const details = {
      name: form.name || "Unknown",
      dateOfBirth: form.date,
      timeOfBirth: form.time || "12:00",
      birthPlace: form.place || "India",
      lat: loc.lat, lon: loc.lon, tz: loc.tz,
    };
    setLocalForm(form);
    setBirthDetails(details);
    try {
      await kundaliMutation.mutateAsync(details as any);
      setView("result");
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : "Calculation failed. Please check birth details.");
    }
  }, [kundaliMutation, setBirthDetails]);

  // Auto-generate from shared URL params (run once on mount)
  useEffect(() => {
    if (_sp.has("dob") && _sharedForm.date && _sharedLoc && !kundaliData) {
      handleGenerate(_sharedForm as LocalForm, _sharedLoc);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Recalculate handler for settings changes (ayanamsha/rahu mode)
  const handleRecalculate = useCallback(() => {
    if (!birthDetails) return;
    kundaliMutation.mutate({
      name: birthDetails.name,
      dateOfBirth: birthDetails.dateOfBirth,
      timeOfBirth: birthDetails.timeOfBirth,
      birthPlace: birthDetails.birthPlace,
      lat: birthDetails.lat,
      lon: birthDetails.lon,
      tz: birthDetails.tz,
    });
  }, [birthDetails, kundaliMutation]);

  // Map kundaliData → PlanetData[] for chart
  const planets: PlanetData[] = kundaliData
    ? GRAHA_ORDER.map(gn => {
        const g = kundaliData.grahas[gn];
        if (!g) return null;
        return toPlanetData(gn, g);
      }).filter(Boolean) as PlanetData[]
    : samplePlanets;

  // Navamsha planets
  const navPlanets: PlanetData[] = kundaliData?.navamsha
    ? GRAHA_ORDER.map(gn => {
        const g = kundaliData.navamsha![gn];
        if (!g) return null;
        return { ...toPlanetData(gn, g), degree: "0°", nakshatra: "" };
      }).filter(Boolean) as PlanetData[]
    : [];

  // Get BC planets — remap D1 planets to their Bhav Chalit house
  const getBCPlanets = (): PlanetData[] => {
    if (!kundaliData?.bhav_chalit) return planets;
    const rashis = ["Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya","Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"];
    const lagnaIdx = rashis.indexOf(kundaliData.lagna.rashi);
    return planets.map(p => {
      const sName = p.sanskritName ?? "";
      const chHouse = kundaliData.bhav_chalit!.planets[sName];
      if (chHouse === undefined) return p;
      const bcRashiIdx = (lagnaIdx + chHouse - 1) % 12;
      return { ...p, house: chHouse, rashi: rashis[bcRashiIdx] };
    });
  };

  // Varga chart planets (from varga_charts or navamsha for D-9)
  const getVargaPlanets = (div: number): PlanetData[] => {
    if (div === 0) return getBCPlanets();  // BC
    if (div === 1) return planets;
    if (div === 9) return navPlanets;
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
    if (!vc) return [];
    return GRAHA_ORDER.map(gn => {
      const g = vc.grahas[gn];
      if (!g) return null;
      return { ...toPlanetData(gn, g), degree: "0°", nakshatra: "" };
    }).filter(Boolean) as PlanetData[];
  };

  const getVargaLagna = (div: number): string => {
    if (div === 0) return kundaliData?.lagna.rashi ?? "Tula"; // BC uses D1 lagna
    if (div === 1) return kundaliData?.lagna.rashi ?? "Tula";
    if (div === 9) return kundaliData?.navamsha_lagna?.rashi ?? kundaliData?.lagna.rashi ?? "Tula";
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
    return vc?.lagna?.rashi ?? kundaliData?.lagna.rashi ?? "Tula";
  };
  const getVargaHouses = (div: number) => {
    if (div === 0 || div === 1) return kundaliData?.houses ?? [];
    if (div === 9) return kundaliData?.navamsha_houses ?? [];
    const vc = kundaliData?.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
    return vc?.houses ?? [];
  };

  const lagnaRashi = kundaliData?.lagna.rashi ?? "Tula";
  const navLagnaRashi = kundaliData?.navamsha_lagna?.rashi ?? lagnaRashi;
  const yogas = kundaliData?.yogas ?? [];
  const visibleYogas = showAllYogas ? yogas : yogas.slice(0, 4);
  const currentDasha = kundaliData?.dashas.find(d => isCurrentDasha(d.start, d.end));

  // Load varga chart on-demand
  const loadVarga = async (div: number, side: "L" | "R") => {
    if (div === 0 || div === 1 || div === 9) return; // BC/D1/D9 always available
    if (vargaCache?.[div] || kundaliData?.varga_charts?.[`D-${div}`]) return;
    if (!kundaliData || !birthDetails) return;
    const setLoading = side === "L" ? setLoadingVargaL : setLoadingVargaR;
    setLoading(true);
    try {
      const { api } = await import("@/lib/api");
      const resp = await api.post<KundaliResponse>("/api/kundali", {
        name: birthDetails.name,
        date: birthDetails.dateOfBirth,
        time: birthDetails.timeOfBirth,
        place: birthDetails.birthPlace,
        lat: birthDetails.lat,
        lon: birthDetails.lon,
        tz: birthDetails.tz,
        ayanamsha: kundaliData?.ayanamsha_mode ?? "lahiri",
        rahu_mode: kundaliData?.rahu_mode ?? "mean",
        calc_options: [`d${div}`],
      });
      if (resp.varga_charts?.[`D-${div}`]) {
        setVargaCache(prev => ({ ...prev, [div]: resp.varga_charts![`D-${div}`] }));
      }
    } catch (e) {
      console.error("Failed to load varga chart:", e);
    }
    setLoading(false);
  };

  const handleVargaSelectL = (div: number) => { setSelectedVargaL(div); loadVarga(div, "L"); };
  const handleVargaSelectR = (div: number) => { setSelectedVargaR(div); loadVarga(div, "R"); };

  return (
    <div className="space-y-3 w-full max-w-full" style={{ overflowX: "clip" }}>
      {/* Header — always shown */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col gap-1.5">
        <div className="flex items-start justify-between gap-2 min-w-0">
          <div className="min-w-0 flex-1">
            <h1 className="font-display text-xl sm:text-2xl text-foreground text-glow-gold mb-0.5 truncate">
              {view === "result" && kundaliData?.name ? `${kundaliData.name}'s ${t("kundli.title")}` : t("kundli.janam_kundali", { defaultValue: "Janam Kundali" })}
            </h1>
            {view === "result" && kundaliData ? (
              <p className="text-xs text-muted-foreground leading-relaxed">
                {kundaliData.place} · {kundaliData.birth_date}
              </p>
            ) : (
              <p className="text-xs text-muted-foreground">{t("kundli.form_subtitle", { defaultValue: "Vedic birth chart · Dasha · Yogas" })}</p>
            )}
          </div>
          {/* Action buttons — icon-only on mobile */}
          <div className="flex items-center gap-1.5 shrink-0">
            {view === "result" && (
              <button onClick={() => setView("input")}
                className="flex items-center gap-1 px-2 py-1.5 rounded-lg text-xs text-muted-foreground hover:text-foreground border border-border/20 hover:border-border/40 transition-colors">
                <Edit2 className="h-3.5 w-3.5" /> <span className="hidden sm:inline">{t("kundli.new_edit", { defaultValue: "Edit" })}</span>
              </button>
            )}
            {view === "result" && kundaliData && birthDetails && (
              <button
                onClick={() => {
                  const bd = birthDetails as any;
                  const p = new URLSearchParams({
                    name: bd.name ?? bd.dateOfBirth?.split(" ")[0] ?? "",
                    dob:  bd.dob ?? bd.dateOfBirth ?? "",
                    tob:  bd.tob ?? bd.timeOfBirth ?? "12:00",
                    place: bd.place ?? bd.birthPlace ?? "",
                    lat:  String(bd.lat ?? 28.6139),
                    lon:  String(bd.lon ?? 77.209),
                    tz:   String(bd.tz ?? 5.5),
                  });
                  const url = `${window.location.origin}/kundli?${p.toString()}`;
                  navigator.clipboard.writeText(url).then(() => {
                    setShareCopied(true);
                    setTimeout(() => setShareCopied(false), 2500);
                  });
                }}
                className="flex items-center gap-1 px-2 py-1.5 rounded-lg text-xs font-medium bg-muted/20 hover:bg-muted/40 text-muted-foreground hover:text-foreground transition-colors border border-border/20">
                {shareCopied ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Share2 className="h-3.5 w-3.5" />}
                <span className="hidden sm:inline">{shareCopied ? "Copied!" : t('common.share')}</span>
              </button>
            )}
            {view === "result" && kundaliData && (
              <button onClick={() => generateKundaliPDF(kundaliData)}
                className="flex items-center gap-1 px-2 py-1.5 rounded-lg text-xs font-medium bg-primary/20 hover:bg-primary/30 text-primary transition-colors border border-primary/20">
                <FileDown className="h-3.5 w-3.5" /> <span className="hidden sm:inline">PDF</span>
              </button>
            )}
          </div>
        </div>
      </motion.div>

      {/* INPUT VIEW — Birth Form */}
      <AnimatePresence mode="wait">
        {view === "input" && (
          <motion.div key="form" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}>
            <KundaliBirthForm
              initial={localForm}
              loading={kundaliMutation.isPending}
              error={formError}
              onGenerate={handleGenerate}
            />
          </motion.div>
        )}
      </AnimatePresence>

      {/* RESULT VIEW — Full Kundali */}
      {view === "result" && (
        <>

      {/* Inline Settings Strip */}
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.05 }}
        className="cosmic-card rounded-xl p-3 flex flex-col sm:flex-row flex-wrap gap-3 items-start">
        {/* Ayanamsha */}
        <div className="flex-1 min-w-0">
          <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Ayanamsha</p>
          <div className="flex gap-1 flex-wrap">
            {([ ["lahiri","Lahiri"], ["raman","Raman"], ["kp","KP"], ["true_citra","True Citra"] ] as const).map(([val, lbl]) => (
              <button key={val}
                onClick={() => { setSettings({ ayanamsha: val as any }); setTimeout(handleRecalculate, 100); }}
                className={`px-2 py-0.5 rounded text-xs transition-colors ${settings.ayanamsha === val ? "bg-primary/20 text-primary border border-primary/30" : "text-muted-foreground border border-border/20 hover:text-foreground"}`}>
                {lbl}
              </button>
            ))}
          </div>
        </div>
        {/* Rahu/Ketu */}
        <div>
          <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Rahu/Ketu</p>
          <div className="flex gap-1">
            {([ ["mean","Mean"], ["true","True"] ] as const).map(([val, lbl]) => (
              <button key={val}
                onClick={() => { setSettings({ rahuMode: val as any }); setTimeout(handleRecalculate, 100); }}
                className={`px-2 py-0.5 rounded text-xs transition-colors ${settings.rahuMode === val ? "bg-primary/20 text-primary border border-primary/30" : "text-muted-foreground border border-border/20 hover:text-foreground"}`}>
                {lbl}
              </button>
            ))}
          </div>
        </div>
        {/* Name Language */}
        <div>
          <p className="text-xs text-muted-foreground uppercase tracking-wider mb-1.5 font-medium">Names</p>
          <div className="flex gap-1">
            {([ ["vedic","Vedic"], ["en","English"] ] as const).map(([val, lbl]) => (
              <button key={val}
                onClick={() => setSettings({ nameLang: val as any })}
                className={`px-2 py-0.5 rounded text-xs transition-colors ${settings.nameLang === val ? "bg-primary/20 text-primary border border-primary/30" : "text-muted-foreground border border-border/20 hover:text-foreground"}`}>
                {lbl}
              </button>
            ))}
          </div>
        </div>
      </motion.div>

      {/* Key positions strip */}
      {kundaliData && (
        <motion.div initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
          className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {[
            { label: t('kundli.lagna'), value: kundaliData.lagna.rashi, sub: `${kundaliData.lagna.nakshatra} P${kundaliData.lagna.pada ?? ""}`, color: "text-primary", icon: RASHI_SYMBOLS[kundaliData.lagna.rashi] },
            { label: t('onboarding.moon_chandra'), value: kundaliData.grahas["Chandra"]?.rashi ?? "—", sub: kundaliData.grahas["Chandra"]?.nakshatra ?? "", color: "text-blue-400", icon: "☽︎" },
            { label: t('onboarding.sun_surya'), value: kundaliData.grahas["Surya"]?.rashi ?? "—", sub: kundaliData.grahas["Surya"]?.status?.split(" ")[0] ?? "", color: "text-amber-400", icon: "☉︎" },
            { label: t('onboarding.jupiter_guru'), value: kundaliData.grahas["Guru"]?.rashi ?? "—", sub: `H${kundaliData.grahas["Guru"]?.house} · ${kundaliData.grahas["Guru"]?.status?.split(" ")[0] ?? ""}`, color: "text-yellow-400", icon: "♃︎" },
          ].map(item => (
            <div key={item.label} className="cosmic-card rounded-xl p-3">
              <div className="flex items-center gap-1.5 mb-0.5">
                <span className="text-base opacity-60">{item.icon}</span>
                <p className="text-xs text-muted-foreground uppercase tracking-wide">{item.label}</p>
              </div>
              <p className={`text-sm font-semibold ${item.color}`}>{item.value}</p>
              <p className="text-xs text-muted-foreground/70">{item.sub}</p>
            </div>
          ))}
        </motion.div>
      )}

      {/* Current Mahadasha strip */}
      {currentDasha && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.15 }}
          className="cosmic-card rounded-xl p-3 flex flex-col sm:flex-row sm:items-center gap-2">
          <div className="flex items-center gap-2 flex-1 min-w-0">
            <div className="w-2 h-2 rounded-full shrink-0 animate-pulse" style={{ background: DASHA_COLORS[currentDasha.lord] ?? "#888" }} />
            <div className="min-w-0">
              <span className="text-xs text-muted-foreground uppercase tracking-wide">{t('sky.mahadasha')} · </span>
              <span className="text-sm font-semibold text-foreground">{currentDasha.lord} ({GRAHA_EN[currentDasha.lord]})</span>
              <span className="text-xs text-muted-foreground ml-1 hidden sm:inline">{formatDate(currentDasha.start)} → {formatDate(currentDasha.end)}</span>
            </div>
          </div>
          {currentDasha.antardashas && (() => {
            const curAntar = currentDasha.antardashas.find(a => isCurrentDasha(a.start, a.end));
            if (!curAntar) return null;
            return (
              <div className="flex items-center gap-1.5 pl-4 sm:pl-0 sm:text-right border-l border-border/20 sm:border-l-0">
                <span className="text-xs text-muted-foreground uppercase shrink-0">{t('kundli.dashas')}:</span>
                <span className="text-xs font-medium text-foreground">{curAntar.lord}</span>
                <span className="text-xs text-muted-foreground hidden sm:inline">({formatDate(curAntar.start)} → {formatDate(curAntar.end)})</span>
              </div>
            );
          })()}
        </motion.div>
      )}

      {/* Main content — full width */}
      <div>

        {/* Charts + Tabs — full width */}
        <div className="cosmic-card rounded-xl p-2 md:p-4 min-w-0 overflow-hidden w-full">
          {/* Tab bar — scrollable within card, no page overflow */}
          <div className="flex gap-1 mb-3 border-b border-border/30 pb-2 overflow-x-auto scrollbar-none">
            {[
              { id:"charts",   label: t("kundli.tab_charts"),       icon: <Layers className="h-3 w-3" /> },
              { id:"grahas",   label: t("kundli.tab_grahas"),       icon: <Star className="h-3 w-3" /> },
              { id:"dasha",    label: t("kundli.tab_dashas"),       icon: <Calendar className="h-3 w-3" /> },
              { id:"houses",   label: t("kundli.tab_bhavas"),       icon: <Home className="h-3 w-3" /> },
              { id:"chalit",   label: t("kundli.tab_chalit"),       icon: <Layers className="h-3 w-3" /> },
              { id:"strength", label: t("kundli.tab_shadbala"),     icon: <Zap className="h-3 w-3" /> },
              { id:"ashtaka",  label: t("kundli.tab_ashtakavarga"), icon: <Star className="h-3 w-3" /> },
              { id:"upagraha", label: t("kundli.tab_upagraha"),     icon: <Star className="h-3 w-3" /> },
              { id:"yogas",    label: t("kundli.tab_yogas_tab"),    icon: <Zap className="h-3 w-3" /> },
              { id:"lagna",    label: t("kundli.tab_lagna"),        icon: <Home className="h-3 w-3" /> },
            ].map(tab => (
              <button key={tab.id}
                onClick={() => setActiveTab(tab.id as typeof activeTab)}
                className={`shrink-0 flex items-center gap-1 text-xs px-2 py-1.5 rounded-full transition-colors whitespace-nowrap ${
                  activeTab === tab.id ? "bg-primary/20 text-primary" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                {tab.icon} {tab.label}
              </button>
            ))}
          </div>

          {/* Planetary War & Combustion Alerts */}
          {kundaliData?.alerts && kundaliData.alerts.length > 0 && (
            <div className="space-y-1.5 mb-2">
              {kundaliData.alerts.map((alert, i) => (
                <div
                  key={i}
                  className={`flex items-start gap-2 rounded-lg px-3 py-2 text-xs border ${
                    alert.type === "graha_yuddha"
                      ? "bg-amber-500/10 border-amber-500/30 text-amber-300"
                      : "bg-orange-500/10 border-orange-500/30 text-orange-300"
                  }`}
                >
                  <AlertTriangle className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                  <span>{alert.message}</span>
                </div>
              ))}
            </div>
          )}

          <AnimatePresence mode="wait">
            {/* Dual Charts Tab */}
            {activeTab === "charts" && (
              <motion.div key="charts" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="min-w-0">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 items-start w-full min-w-0">
                  {([
                    { div: selectedVargaL, setDiv: handleVargaSelectL, tab: chartTabL, setTab: setChartTabL, loading: loadingVargaL, side: "L" as const },
                    { div: selectedVargaR, setDiv: handleVargaSelectR, tab: chartTabR, setTab: setChartTabR, loading: loadingVargaR, side: "R" as const },
                  ] as const).map(({ div, setDiv, tab, setTab, loading, side }) => {
                    const meta = VARGA_QUICK.find(v => v.div === div);
                    const isBc = div === 0;
                    const chartPlanets = getVargaPlanets(div);
                    const chartLagna = getVargaLagna(div);
                    const vargaHouses = getVargaHouses(div);

                    // Projected varga longitude: scale degree-within-segment to 0–30°
                    const projectedDeg = (lon: number, n: number): number => {
                      const segSize = 30 / n;
                      return (lon % segSize) / segSize * 30;
                    };

                    // Graha detail rows for this chart
                    const grahaRows = kundaliData ? GRAHA_ORDER.map(gn => {
                      const d1g = kundaliData.grahas[gn];
                      const d1Lagna = kundaliData.lagna.rashi;

                      if (isBc || div === 1) {
                        if (!d1g) return null;
                        const rulerOf = getRulerOf(gn, d1Lagna);
                        const rashiHouse = d1g.house;
                        const bcHouse = kundaliData.bhav_chalit?.planets[gn] ?? rashiHouse;
                        return { gn, rashi: d1g.rashi, house: rashiHouse, bcHouse, degree: d1g.degree, d1Rashi: d1g.rashi, nakshatra: d1g.nakshatra, nakshatra_lord: d1g.nakshatra_lord, pada: d1g.pada, retro: d1g.retro, combust: d1g.combust, status: d1g.status, ruler_of: rulerOf, isD1: true };
                      }
                      if (div === 9) {
                        const g = kundaliData.navamsha?.[gn];
                        if (!g) return null;
                        const navLagna = kundaliData.navamsha_lagna?.rashi ?? d1Lagna;
                        const rulerOf = getRulerOf(gn, navLagna);
                        const lon = d1g?.longitude ?? (d1g ? d1g.degree + RASHI_LIST_IDX.indexOf(d1g.rashi) * 30 : 0);
                        const deg = projectedDeg(lon, 9);
                        const vRashiIdx9 = RASHI_LIST_IDX.indexOf(g.rashi);
                        const absVargaLon9 = vRashiIdx9 * 30 + (d1g?.degree ?? 0);
                        const nak9 = getNakshatraFromLon(absVargaLon9);
                        return { gn, rashi: g.rashi, house: g.house, bcHouse: g.house, degree: deg, d1Rashi: g.rashi, nakshatra: nak9.nakshatra, nakshatra_lord: nak9.lord, pada: nak9.pada, retro: g.retro, combust: false, status: g.status, ruler_of: rulerOf, isD1: false };
                      }
                      const vc = kundaliData.varga_charts?.[`D-${div}`] ?? vargaCache?.[div];
                      const g = vc?.grahas?.[gn];
                      if (!g) return null;
                      const vargaLagna = vc?.lagna?.rashi ?? d1Lagna;
                      const rulerOf = getRulerOf(gn, vargaLagna);
                      const lon = d1g?.longitude ?? (d1g ? d1g.degree + RASHI_LIST_IDX.indexOf(d1g.rashi) * 30 : 0);
                      const deg = projectedDeg(lon, div);
                      const vRashiIdx = RASHI_LIST_IDX.indexOf(g.rashi);
                      const absVargaLon = vRashiIdx * 30 + (d1g?.degree ?? 0);
                      const nak = getNakshatraFromLon(absVargaLon);
                      return { gn, rashi: g.rashi, house: g.house, bcHouse: g.house, degree: deg, d1Rashi: g.rashi, nakshatra: nak.nakshatra, nakshatra_lord: nak.lord, pada: nak.pada, retro: g.retro, combust: false, status: g.status, ruler_of: rulerOf, isD1: false };
                    }).filter(Boolean) : [];

                    return (
                      <div key={side} className={`rounded-xl border bg-card/30 backdrop-blur-sm overflow-hidden w-full min-w-0 ${
                        selectedVargaL === selectedVargaR ? "border-amber-500/30" : "border-border/20"
                      }`}>
                        {/* Chart header — label + selector + lagna */}
                        <div className="flex items-center gap-1.5 p-2 border-b border-border/15 bg-muted/10">
                          <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded shrink-0 ${
                            side === "L"
                              ? "bg-primary/20 text-primary"
                              : "bg-amber-500/20 text-amber-400"
                          }`}>{side === "L" ? "A" : "B"}</span>
                          <select
                            value={div}
                            onChange={e => setDiv(Number(e.target.value))}
                            className="flex-1 min-w-0 bg-background/60 border border-border/30 rounded-lg px-2 py-1 text-xs text-foreground focus:outline-none focus:border-primary/50 cursor-pointer"
                          >
                            {VARGA_QUICK.map(v => (
                              <option key={v.div} value={v.div}>{v.code} — {v.name}</option>
                            ))}
                          </select>
                          <div className="text-xs text-muted-foreground shrink-0">
                            <span className="text-amber-400 font-medium">{chartLagna}</span>
                          </div>
                        </div>

                        {/* Chart desc + same-chart warning */}
                        <div className="px-3 py-1 text-[10px] text-muted-foreground/70 border-b border-border/10 flex items-center gap-2">
                          <span>{isBc ? t("data.kundli.placidus_desc") : meta?.desc}</span>
                          {isBc && <span className="text-amber-400/80">{t("data.kundli.bhav_chalit")}</span>}
                          {selectedVargaL === selectedVargaR && (
                            <span className="ml-auto text-amber-500/80">{t("data.kundli.same_chart_warn")}</span>
                          )}
                        </div>

                        {/* Chart */}
                        <div className="w-full overflow-hidden">
                          {loading ? (
                            <div className="flex items-center justify-center py-16 text-muted-foreground text-xs gap-2">
                              <Loader2 className="h-4 w-4 animate-spin" /> Loading chart…
                            </div>
                          ) : chartPlanets.length > 0 ? (
                            <KundliChart
                              onPlanetClick={setSelectedPlanet}
                              selectedPlanet={selectedPlanet}
                              planets={chartPlanets}
                              lagnaRashi={chartLagna}
                              showStyleToggle={true}
                            />
                          ) : (
                            <div className="flex items-center justify-center py-16 text-muted-foreground text-xs">
                              {kundaliData ? "Varga data loading…" : "Generate Kundali first"}
                            </div>
                          )}
                        </div>

                        {/* Detail tabs */}
                        {kundaliData && (
                          <div className="border-t border-border/15">
                            <div className="flex border-b border-border/15">
                              {(["graha","bhava"] as const).map(t => (
                                <button key={t} onClick={() => setTab(t)}
                                  className={`flex-1 py-1.5 text-xs font-medium transition-colors ${tab === t ? "text-primary border-b-2 border-primary bg-primary/5" : "text-muted-foreground hover:text-foreground"}`}>
                                  {t === "graha" ? "Graha Details" : "Bhava Details"}
                                </button>
                              ))}
                            </div>

                            {tab === "graha" && (
                              <div className="overflow-x-auto scrollbar-none">
                                <table className="w-full min-w-[280px] text-xs table-fixed">
                                  <colgroup>
                                    <col style={{ width: "22%" }} />
                                    <col style={{ width: "28%" }} />
                                    <col style={{ width: "22%" }} />
                                    <col style={{ width: "13%" }} />
                                    <col style={{ width: "15%" }} />
                                  </colgroup>
                                  <thead>
                                    <tr className="border-b border-border/15 bg-muted/10">
                                      <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Graha</th>
                                      <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Longitude</th>
                                      <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Nakshatra</th>
                                      <th className="text-center px-2 py-1.5 text-muted-foreground font-medium">Rules</th>
                                      <th className="text-center px-2 py-1.5 text-muted-foreground font-medium">House/Status</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {/* Lagna row — always shown for all charts */}
                                    {kundaliData && (() => {
                                      const isD1full = isBc || div === 1;
                                      const l = kundaliData.lagna;
                                      return (
                                        <tr className="border-b border-border/10 bg-amber-500/5">
                                          <td className="px-2 py-1.5 font-medium text-amber-400">Lagna</td>
                                          <td className="px-2 py-1.5 font-mono text-foreground truncate">
                                            {isD1full
                                              ? degToDMS(l.degree, l.rashi)
                                              : (() => {
                                                  const absLon = l.full_degree ?? (RASHI_LIST_IDX.indexOf(l.rashi) * 30 + l.degree);
                                                  const vDeg = projectedDeg(absLon, div);
                                                  return degToDMS(vDeg, chartLagna);
                                                })()}
                                          </td>
                                          <td className="px-2 py-1.5 text-muted-foreground truncate">
                                            {isD1full
                                              ? `${l.nakshatra}${l.pada ? ` P${l.pada}` : ""}`
                                              : (() => {
                                                  const lagnaVRashiIdx = RASHI_LIST_IDX.indexOf(chartLagna);
                                                  const absVLon = lagnaVRashiIdx * 30 + l.degree;
                                                  const lNak = getNakshatraFromLon(absVLon);
                                                  return `${lNak.nakshatra} P${lNak.pada}`;
                                                })()}
                                          </td>
                                          <td className="px-2 py-1.5 text-center text-muted-foreground">—</td>
                                          <td className="px-2 py-1.5 text-center font-medium text-foreground">H1</td>
                                        </tr>
                                      );
                                    })()}
                                    {grahaRows.map(r => {
                                      if (!r) return null;
                                      const isQ = [1,4,7,10].includes(r.house);
                                      const bcDiff = r.isD1 && r.bcHouse !== r.house;
                                      const sb = STATUS_BADGE[r.status] ?? STATUS_BADGE["Normal"];
                                      const isExpanded = expandedGraha?.side === side && expandedGraha?.gn === r.gn;
                                      // Get full graha data for expanded panel
                                      const fullG = kundaliData.grahas[r.gn];
                                      return (
                                        <React.Fragment key={r.gn}>
                                          <tr
                                            onClick={() => setExpandedGraha(isExpanded ? null : { side, gn: r.gn })}
                                            className={`border-b border-border/10 cursor-pointer transition-colors ${isExpanded ? "bg-primary/8 border-primary/20" : "hover:bg-muted/10"}`}
                                          >
                                            <td className="px-2 py-1.5">
                                              <span className="flex items-center gap-1">
                                                <span style={{ color: GRAHA_COLOR[r.gn] }}>{GRAHA_SYMBOLS[r.gn]}</span>
                                                <span className="font-medium text-foreground">{r.gn}</span>
                                                {r.retro && <span className="text-amber-400 text-[10px]">℞</span>}
                                                {r.combust && <span className="text-orange-400 text-[10px]">🔥</span>}
                                                {isQ && <span className="text-[9px] text-primary/60 ml-0.5">Q</span>}
                                              </span>
                                            </td>
                                            <td className="px-2 py-1.5 font-mono text-foreground text-xs">
                                              {r.degree > 0 ? degToDMS(r.degree, (r as any).d1Rashi ?? r.rashi) : r.rashi}
                                            </td>
                                            <td className="px-2 py-1.5 text-muted-foreground truncate">
                                              {r.nakshatra ? `${r.nakshatra}${r.pada ? ` P${r.pada}` : ""}` : "—"}
                                            </td>
                                            <td className="px-2 py-1.5 text-center text-muted-foreground text-[10px]">
                                              {r.ruler_of?.length ? r.ruler_of.map(h => `H${h}`).join(",") : "—"}
                                            </td>
                                            <td className="px-2 py-1.5 text-center">
                                              <span className="font-medium text-foreground">H{r.house}</span>
                                              {r.isD1 && bcDiff && <span className="ml-0.5 text-amber-400 text-[9px]">→H{r.bcHouse}</span>}
                                              <br />
                                              <span className={`text-[9px] px-1 py-0.5 rounded border ${sb.cls}`}>{t(sb.tKey)}</span>
                                            </td>
                                          </tr>
                                          {/* Expanded detail row */}
                                          {isExpanded && (
                                            <tr key={`${r.gn}-exp`} className="border-b border-primary/15 bg-primary/5">
                                              <td colSpan={5} className="px-3 py-3">
                                                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                                                  {fullG ? (
                                                    <>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Rashi</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.rashi} {fullG.rashi_en ? `(${fullG.rashi_en})` : ""}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">House</p>
                                                        <p className="text-foreground font-medium mt-0.5">H{fullG.house}{bcDiff ? ` → H${r.bcHouse} (BC)` : ""}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Nakshatra/Pada</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.nakshatra} P{fullG.pada}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Nak. Lord</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.nakshatra_lord ?? "—"}</p>
                                                      </div>
                                                      <div className={`rounded-lg p-2 border ${(STATUS_BADGE[fullG.status] ?? STATUS_BADGE["Normal"]).cls}`}>
                                                        <p className="text-[9px] uppercase tracking-wide opacity-70">{t("data.kundli.dignity_label")}</p>
                                                        <p className="font-medium text-xs mt-0.5">{fullG.status}</p>
                                                      </div>
                                                      <div className={`rounded-lg p-2 ${(RELATIONSHIP_BADGE[fullG.relationship ?? "Neutral"] ?? RELATIONSHIP_BADGE["Neutral"]).cls}`}>
                                                        <p className="text-[9px] uppercase tracking-wide opacity-70">{t("data.kundli.relationship_label")}</p>
                                                        <p className="font-medium text-xs mt-0.5">{t((RELATIONSHIP_BADGE[fullG.relationship ?? "Neutral"] ?? RELATIONSHIP_BADGE["Neutral"]).tKey)}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Speed / Lat</p>
                                                        <p className="text-foreground font-medium mt-0.5">{fullG.speed?.toFixed(3) ?? "—"}°/d · {fullG.lat_ecl?.toFixed(2) ?? "0"}°</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Rules Houses</p>
                                                        <p className="text-foreground font-medium mt-0.5">{r.ruler_of?.length ? r.ruler_of.map(h => `H${h}`).join(", ") : "—"}</p>
                                                      </div>
                                                      {fullG.karaka && (
                                                        <div className="col-span-2 sm:col-span-4 bg-primary/10 rounded-lg p-2 border border-primary/15">
                                                          <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Karaka (Significator)</p>
                                                          <p className="text-foreground text-xs mt-0.5">{fullG.karaka}</p>
                                                        </div>
                                                      )}
                                                      {fullG.combust && (
                                                        <div className="col-span-2 sm:col-span-4 bg-orange-500/10 rounded-lg p-2 border border-orange-500/20">
                                                          <p className="text-[9px] text-orange-400 uppercase tracking-wide">Combust (Asta)</p>
                                                          <p className="text-orange-300 text-xs mt-0.5">Too close to Sun — weakened</p>
                                                        </div>
                                                      )}
                                                    </>
                                                  ) : (
                                                    <>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Varga Rashi</p>
                                                        <p className="text-foreground font-medium mt-0.5">{r.rashi}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Varga House</p>
                                                        <p className="text-foreground font-medium mt-0.5">H{r.house}</p>
                                                      </div>
                                                      <div className={`rounded-lg p-2 border ${sb.cls}`}>
                                                        <p className="text-[9px] uppercase tracking-wide opacity-70">Status</p>
                                                        <p className="font-medium text-xs mt-0.5">{r.status}</p>
                                                      </div>
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">D1 Longitude</p>
                                                        <p className="text-foreground font-medium mt-0.5 font-mono text-[10px]">
                                                          {r.degree > 0 ? degToDMS(r.degree, (r as any).d1Rashi ?? r.rashi) : "—"}
                                                        </p>
                                                      </div>
                                                      {r.nakshatra && (
                                                        <div className="bg-muted/30 rounded-lg p-2">
                                                          <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Nakshatra / Pada</p>
                                                          <p className="text-foreground font-medium mt-0.5">{r.nakshatra} P{r.pada}</p>
                                                        </div>
                                                      )}
                                                      <div className="bg-muted/30 rounded-lg p-2">
                                                        <p className="text-[9px] text-muted-foreground uppercase tracking-wide">Retrograde</p>
                                                        <p className="text-foreground font-medium mt-0.5">{r.retro ? "Yes ℞" : "No"}</p>
                                                      </div>
                                                    </>
                                                  )}
                                                </div>
                                              </td>
                                            </tr>
                                          )}
                                        </React.Fragment>
                                      );
                                    })}
                                  </tbody>
                                </table>
                                <p className="text-[10px] text-muted-foreground/60 px-2 py-1.5 break-words">Click any row to expand · Q = Kendra</p>
                              </div>
                            )}

                            {tab === "bhava" && (
                              <div className="overflow-x-auto scrollbar-none">
                                {(() => {
                                  const validRows = grahaRows.filter(Boolean) as NonNullable<typeof grahaRows[number]>[];
                                  const aspectMap = computeAspects(validRows);
                                  // Build residents map from grahaRows (varga houses don't include planet lists)
                                  const houseResidentsMap: Record<number, string[]> = {};
                                  if (!isBc) {
                                    for (const r of validRows) {
                                      if (!houseResidentsMap[r.house]) houseResidentsMap[r.house] = [];
                                      houseResidentsMap[r.house].push(r.gn);
                                    }
                                  }
                                  return (
                                    <table className="w-full min-w-[320px] text-xs table-fixed">
                                      <colgroup>
                                        <col style={{ width: "7%" }} />
                                        <col style={{ width: "20%" }} />
                                        <col style={{ width: "11%" }} />
                                        <col style={{ width: "13%" }} />
                                        <col style={{ width: "14%" }} />
                                        <col style={{ width: "35%" }} />
                                      </colgroup>
                                      <thead>
                                        <tr className="border-b border-border/15 bg-muted/10">
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">H</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Residents</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Owner</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Rashi</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Quality</th>
                                          <th className="text-left px-2 py-1.5 text-muted-foreground font-medium">Aspected By</th>
                                        </tr>
                                      </thead>
                                      <tbody>
                                        {vargaHouses.map(h => {
                                          const isQ = [1,4,7,10].includes(h.house);
                                          let residents: string[] = [];
                                          if (isBc && kundaliData.bhav_chalit) {
                                            residents = GRAHA_ORDER.filter(gn => kundaliData.bhav_chalit!.planets[gn] === h.house);
                                          } else {
                                            residents = houseResidentsMap[h.house] ?? [];
                                          }
                                          const aspectors = aspectMap[h.house] ?? [];
                                          return (
                                            <tr key={h.house} className={`border-b border-border/10 hover:bg-muted/10 ${isQ ? "bg-primary/3" : ""}`}>
                                              <td className="px-2 py-1.5 font-medium text-foreground">
                                                {h.house}{isQ && <span className="text-[9px] text-primary/60 ml-0.5">Q</span>}
                                              </td>
                                              <td className="px-2 py-1.5 text-muted-foreground truncate">
                                                {residents.length > 0 ? residents.map(g => g.slice(0,3)).join(", ") : <span className="opacity-40">—</span>}
                                              </td>
                                              <td className="px-2 py-1.5 text-foreground truncate">{h.lord?.slice(0,3) ?? "—"}</td>
                                              <td className="px-2 py-1.5 text-muted-foreground truncate">{RASHI_SHORT[h.rashi] ?? h.rashi?.slice(0,3)}</td>
                                              <td className="px-2 py-1.5 text-muted-foreground truncate text-[10px]">
                                                {RASHI_QUALITY[h.rashi] ?? "—"}
                                              </td>
                                              <td className="px-2 py-1.5 text-muted-foreground text-[10px]">
                                                {aspectors.length > 0
                                                  ? aspectors.map(g => (
                                                      <span key={g} className="mr-1 whitespace-nowrap" style={{ color: GRAHA_COLOR[g] }}>
                                                        {GRAHA_SYMBOLS[g]}{g.slice(0,3)}
                                                      </span>
                                                    ))
                                                  : <span className="opacity-40">—</span>}
                                              </td>
                                            </tr>
                                          );
                                        })}
                                      </tbody>
                                    </table>
                                  );
                                })()}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </motion.div>
            )}

            {/* Dasha table with Antardasha expansion */}
            {activeTab === "dasha" && (
              <motion.div key="dasha" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Calendar className="h-3.5 w-3.5 text-primary" /> Vimshottari Dasha · Click to expand Antardasha
                </p>
                {(kundaliData?.dashas ?? []).length > 0 ? (
                  <div className="space-y-2">
                    {(kundaliData?.dashas ?? []).map((d, i) => {
                      const cur = isCurrentDasha(d.start, d.end);
                      const color = DASHA_COLORS[d.lord] ?? "#888";
                      const isExpanded = expandedDasha === d.lord;
                      const hasAntar = d.antardashas && d.antardashas.length > 0;
                      return (
                        <motion.div key={i} initial={{ opacity: 0, x: -4 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.03 * i }}>
                          <div
                            onClick={() => hasAntar && setExpandedDasha(isExpanded ? null : d.lord)}
                            className={`rounded-lg p-3 border cursor-pointer ${cur
                              ? "bg-amber-500/10 border-amber-500/30"
                              : "bg-muted/10 border-border/20"} ${hasAntar ? "hover:bg-muted/20" : ""}`}>
                            <div className="flex items-center gap-3">
                              <div className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: color }} />
                              <div className="flex-1">
                                <div className="flex items-center gap-2 flex-wrap">
                                  <span className="text-sm font-medium text-foreground">{d.lord}</span>
                                  <span className="text-xs text-muted-foreground">({GRAHA_EN[d.lord] ?? d.lord})</span>
                                  {cur && <span className="text-xs bg-amber-500/20 text-amber-400 px-1.5 py-0.5 rounded border border-amber-500/30 font-medium">Active Now ★</span>}
                                </div>
                                <p className="text-xs text-muted-foreground mt-0.5">
                                  {formatDate(d.start)} → {formatDate(d.end)} · {d.years} years
                                </p>
                              </div>
                              <div className="flex items-center gap-2 shrink-0">
                                <div className="hidden sm:block w-20 h-1.5 bg-muted/30 rounded-full overflow-hidden">
                                  <div className="h-full rounded-full" style={{ background: color, width: `${(d.years / 20) * 100}%` }} />
                                </div>
                                {hasAntar && (
                                  isExpanded
                                    ? <ChevronUp className="h-3.5 w-3.5 text-muted-foreground" />
                                    : <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
                                )}
                              </div>
                            </div>
                          </div>
                          {/* Dasha prediction card for active dasha */}
                          {cur && (() => {
                            const pred = _DP[d.lord]?.[lagnaRashi];
                            if (!pred) return null;
                            const pk = d.lord.toLowerCase();
                            const rk = lagnaRashi.toLowerCase();
                            return (
                              <div className="mt-1.5 mx-0.5 rounded-lg border border-amber-500/20 bg-amber-500/5 p-3 space-y-2">
                                <p className="text-xs font-semibold text-amber-400 uppercase tracking-wide">{t(`data.dp.${pk}_${rk}_theme`, { defaultValue: pred.theme })}</p>
                                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs">
                                  <div>
                                    <p className="text-emerald-400 font-medium mb-0.5">{t("data.kundli.dp_positive")}</p>
                                    <p className="text-muted-foreground">{t(`data.dp.${pk}_${rk}_pos`, { defaultValue: pred.positive })}</p>
                                  </div>
                                  <div>
                                    <p className="text-red-400 font-medium mb-0.5">{t("data.kundli.dp_challenge")}</p>
                                    <p className="text-muted-foreground">{t(`data.dp.${pk}_${rk}_cha`, { defaultValue: pred.challenge })}</p>
                                  </div>
                                  <div>
                                    <p className="text-primary font-medium mb-0.5">{t("data.kundli.dp_remedy")}</p>
                                    <p className="text-muted-foreground">{t(`data.dp.${pk}_${rk}_tip`, { defaultValue: pred.tip })}</p>
                                  </div>
                                </div>
                              </div>
                            );
                          })()}
                          {/* Antardasha expansion */}
                          <AnimatePresence>
                            {isExpanded && d.antardashas && (
                              <motion.div
                                initial={{ opacity: 0, height: 0 }}
                                animate={{ opacity: 1, height: "auto" }}
                                exit={{ opacity: 0, height: 0 }}
                                className="overflow-hidden"
                              >
                                <AntardashaList antardashas={d.antardashas} />
                              </motion.div>
                            )}
                          </AnimatePresence>
                        </motion.div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see dasha sequence</p>
                )}
              </motion.div>
            )}

            {/* Bhava (House) table — enhanced with significations */}
            {activeTab === "houses" && (
              <motion.div key="houses" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Home className="h-3.5 w-3.5 text-primary" /> Bhava Chart — Whole Sign Houses
                </p>
                {(kundaliData?.houses ?? []).length > 0 ? (() => {
                  // Compute D1 aspects client-side
                  const d1AspectMap = computeAspects(
                    GRAHA_ORDER.filter(gn => kundaliData!.grahas[gn]).map(gn => ({
                      gn, house: kundaliData!.grahas[gn].house,
                    }))
                  );
                  return (
                  <div className="space-y-1.5">
                    {(kundaliData?.houses ?? []).map((h) => {
                      const planetsIn = h.planets ?? GRAHA_ORDER.filter(gn => kundaliData?.grahas[gn]?.house === h.house);
                      const aspectors = d1AspectMap[h.house] ?? [];
                      return (
                        <div key={h.house} className="flex items-start gap-3 px-2 py-2.5 rounded-lg hover:bg-muted/20 transition-colors">
                          <div className="w-8 h-8 rounded-lg bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0 mt-0.5">
                            <span className="text-xs font-bold text-primary">{h.house}</span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="text-sm text-foreground font-medium">
                                {h.rashi} {RASHI_SYMBOLS[h.rashi] ?? ""}
                                {h.rashi_en && <span className="text-xs text-muted-foreground/70 ml-1">({h.rashi_en})</span>}
                              </span>
                              <span className="text-xs text-muted-foreground">Lord: {h.lord} {h.lord_en ? `(${h.lord_en})` : ""}</span>
                            </div>
                            {h.signification && (
                              <p className="text-xs text-muted-foreground/60 mt-0.5">{h.signification}</p>
                            )}
                            {(h.gender || h.modality || h.tattva) && (
                              <p className="text-xs text-muted-foreground/70 mt-0.5">
                                {[h.gender, h.modality, h.tattva].filter(Boolean).join(" · ")}
                              </p>
                            )}
                            {aspectors.length > 0 && (
                              <div className="flex items-center gap-1 mt-1 flex-wrap">
                                <span className="text-[10px] text-muted-foreground/60 mr-0.5">Aspected by:</span>
                                {aspectors.map(gn => (
                                  <span key={gn} className="text-[10px] px-1 py-0.5 rounded" style={{ color: GRAHA_COLOR[gn], background: `${GRAHA_COLOR[gn]}22` }}>
                                    {GRAHA_SYMBOLS[gn]} {gn}
                                  </span>
                                ))}
                              </div>
                            )}
                            {planetsIn.length > 0 && (
                              <div className="flex items-center gap-1 mt-1 flex-wrap">
                                {planetsIn.map(gn => (
                                  <span key={gn} className="text-xs px-1.5 py-0.5 rounded" style={{ color: GRAHA_COLOR[gn], background: `${GRAHA_COLOR[gn]}22` }}>
                                    {GRAHA_SYMBOLS[gn]} {gn}
                                  </span>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see house chart</p>
                )}
              </motion.div>
            )}

            {/* Grahas Detail Table */}
            {activeTab === "grahas" && (
              <motion.div key="grahas" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Star className="h-3.5 w-3.5 text-primary" /> Graha Details — Full Planetary Data
                </p>
                {kundaliData ? (
                  <div className="overflow-x-auto">
                    <table className="text-xs min-w-[560px] w-full">
                      <thead>
                        <tr className="border-b border-border/20 text-muted-foreground">
                          <th className="text-left py-1.5 px-2">Graha</th>
                          <th className="text-center px-1">R</th>
                          <th className="text-center px-1">C</th>
                          <th className="text-left px-2">Longitude (DMS)</th>
                          <th className="text-left px-2">Nakshatra / Lord</th>
                          <th className="text-center px-1">Pada</th>
                          <th className="text-center px-1">Raw L</th>
                          <th className="text-center px-1">Lat</th>
                          <th className="text-center px-1">RA</th>
                          <th className="text-center px-1">Dec</th>
                          <th className="text-center px-1">Speed</th>
                          <th className="text-left px-2">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {/* Lagna row */}
                        <tr className="border-b border-border/10 bg-primary/5">
                          <td className="py-1.5 px-2 font-semibold text-primary">Lagna (Lg)</td>
                          <td className="text-center px-1">—</td>
                          <td className="text-center px-1">—</td>
                          <td className="px-2 font-medium text-foreground">
                            {(() => {
                              const d = kundaliData.lagna.degree;
                              const deg = Math.floor(d);
                              const minF = (d - deg) * 60;
                              const min = Math.floor(minF);
                              const sec = Math.floor((minF - min) * 60);
                              return `${deg.toString().padStart(2,"0")}° ${min.toString().padStart(2,"0")}' ${sec.toString().padStart(2,"0")}"`;
                            })()}
                          </td>
                          <td className="px-2">{kundaliData.lagna.nakshatra} / {kundaliData.lagna.nakshatra?.split(" ")[0]}</td>
                          <td className="text-center px-1">{kundaliData.lagna.pada ?? "—"}</td>
                          <td className="text-center px-1">{kundaliData.lagna.full_degree?.toFixed(2) ?? "—"}</td>
                          <td className="text-center px-1">0.00</td>
                          <td className="text-center px-1">—</td>
                          <td className="text-center px-1">—</td>
                          <td className="text-center px-1">—</td>
                          <td className="px-2">—</td>
                        </tr>
                        {GRAHA_ORDER.map(gn => {
                          const g = kundaliData.grahas[gn];
                          if (!g) return null;
                          const lon = g.longitude ?? 0;
                          const deg = Math.floor(g.degree);
                          const minF = (g.degree - deg) * 60;
                          const min = Math.floor(minF);
                          const sec = Math.floor((minF - min) * 60);
                          const dms = `${deg.toString().padStart(2,"0")}° ${min.toString().padStart(2,"0")}' ${sec.toString().padStart(2,"0")}"`;
                          const sb = STATUS_BADGE[g.status] ?? STATUS_BADGE["Normal"];
                          return (
                            <tr key={gn} className="border-b border-border/10 hover:bg-muted/20">
                              <td className="py-1.5 px-2">
                                <span style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                <span className="font-medium text-foreground ml-1">{gn}</span>
                              </td>
                              <td className="text-center px-1 text-amber-400">{g.retro ? "℞" : ""}</td>
                              <td className="text-center px-1 text-orange-400">{g.combust ? "C" : ""}</td>
                              <td className="px-2 font-mono text-foreground">
                                {g.rashi} {dms}
                              </td>
                              <td className="px-2 text-muted-foreground">
                                {g.nakshatra} {g.pada}, {g.nakshatra_lord}
                              </td>
                              <td className="text-center px-1 text-foreground">{g.pada}</td>
                              <td className="text-center px-1 text-muted-foreground font-mono">{lon.toFixed(2)}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.lat_ecl?.toFixed(2) ?? "0.00"}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.ra?.toFixed(2) ?? "—"}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.dec?.toFixed(2) ?? "—"}</td>
                              <td className="text-center px-1 text-muted-foreground">{g.speed?.toFixed(2) ?? "—"}</td>
                              <td className="px-2"><span className={`px-1.5 py-0.5 rounded border text-xs ${sb.cls}`}>{t(sb.tKey)}</span></td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    <p className="text-xs text-muted-foreground/70 mt-2 px-1">
                      R = Retrograde · C = Combust · Raw L = Full longitude (0°–360°) · Lat = Ecliptic Latitude · RA = Right Ascension · Dec = Declination
                    </p>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-8">Generate Kundali to see graha details</p>
                )}
              </motion.div>
            )}

            {/* Shadbala + Bhavabala */}
            {activeTab === "strength" && (
              <motion.div key="strength" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Zap className="h-3.5 w-3.5 text-primary" /> Shadbala — Six-fold Planetary Strength
                </p>
                {kundaliData?.shadbala?.planets ? (() => {
                  const planets7 = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani"];
                  const sb = kundaliData.shadbala!.planets;
                  return (
                    <div className="space-y-4">
                      {/* Summary grid */}
                      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                        {planets7.filter(gn => sb[gn]).map(gn => {
                          const s = sb[gn];
                          const pct = Math.min(100, Math.round(s.ratio * 100));
                          const color = s.is_strong ? "text-emerald-400" : "text-red-400";
                          return (
                            <div key={gn} className="bg-muted/20 rounded-lg p-2 text-center">
                              <div className="flex items-center justify-center gap-1 mb-1">
                                <span className="text-sm" style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                <span className="text-xs text-muted-foreground">{GRAHA_EN[gn]}</span>
                              </div>
                              <p className={`text-sm font-bold ${color}`}>{s.total_rupas}R</p>
                              <p className="text-xs text-muted-foreground">Need {s.required_rupas}R</p>
                              <div className="mt-1 h-1 bg-muted/30 rounded-full">
                                <div className={`h-full rounded-full ${s.is_strong ? "bg-emerald-400" : "bg-red-400"}`} style={{ width: `${pct}%` }} />
                              </div>
                              <p className={`text-xs mt-0.5 font-medium ${color}`}>{s.is_strong ? "✓ Strong" : "✗ Weak"}</p>
                            </div>
                          );
                        })}
                      </div>
                      {/* Detailed table */}
                      <div className="overflow-x-auto">
                        <table className="w-full min-w-[360px] text-xs">
                          <thead>
                            <tr className="border-b border-border/20">
                              <th className="text-left text-muted-foreground py-1 pr-2">Component</th>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <th key={gn} className="text-center text-muted-foreground py-1 px-1">
                                  <span style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {[
                              ["Sthana", "sthana_bala"],
                              ["  ↳ Uccha", null, "uccha"],
                              ["  ↳ Oja-Yugma", null, "oja_yugma"],
                              ["  ↳ Kendradi", null, "kendradi"],
                              ["  ↳ Drekkana", null, "drekkana"],
                              ["Dig", "dig_bala"],
                              ["Kala", "kaala_bala"],
                              ["Chesta", "chesta_bala"],
                              ["Naisargika", "naisargika_bala"],
                              ["Drik", "drik_bala"],
                            ].map(([label, key, subKey]) => (
                              <tr key={`${key ?? subKey}`} className={`border-b border-border/10 ${!key ? "opacity-60" : ""}`}>
                                <td className={`text-muted-foreground py-1 pr-2 ${key ? "font-medium" : "text-xs pl-2"}`}>{label}</td>
                                {planets7.filter(gn => sb[gn]).map(gn => (
                                  <td key={gn} className="text-center text-foreground py-1 px-1 text-xs">
                                    {key
                                      ? (sb[gn] as any)[key as string]
                                      : (sb[gn].sthana_detail as any)?.[subKey as string] ?? "—"
                                    }
                                  </td>
                                ))}
                              </tr>
                            ))}
                            <tr className="border-t border-border/30 font-bold">
                              <td className="text-muted-foreground py-1.5 pr-2">Total (V)</td>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <td key={gn} className="text-center text-foreground py-1.5 px-1">{sb[gn].total_virupas}</td>
                              ))}
                            </tr>
                            <tr>
                              <td className="text-muted-foreground py-1 pr-2">Rupas</td>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <td key={gn} className="text-center text-foreground py-1 px-1">{sb[gn].total_rupas}</td>
                              ))}
                            </tr>
                            <tr>
                              <td className="text-muted-foreground py-1 pr-2">Ratio</td>
                              {planets7.filter(gn => sb[gn]).map(gn => (
                                <td key={gn} className={`text-center py-1 px-1 font-medium ${sb[gn].is_strong ? "text-emerald-400" : "text-red-400"}`}>
                                  {sb[gn].ratio}
                                </td>
                              ))}
                            </tr>
                          </tbody>
                        </table>
                      </div>
                      {/* Bhavabala */}
                      {kundaliData.bhavabala && (
                        <div>
                          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">Bhavabala — House Strength</p>
                          <div className="grid grid-cols-3 sm:grid-cols-4 gap-1.5">
                            {kundaliData.bhavabala.map(bh => (
                              <div key={bh.house} className="bg-muted/20 rounded-lg p-2 text-center">
                                <div className="flex items-center justify-between mb-0.5">
                                  <span className="text-xs font-bold text-primary">H{bh.house}</span>
                                  <span className="text-xs text-muted-foreground">#{bh.rank}</span>
                                </div>
                                <p className="text-xs text-foreground font-medium">{bh.strength}R</p>
                                <p className="text-xs text-muted-foreground">{bh.lord}</p>
                                <div className="mt-1 h-1 bg-muted/30 rounded-full">
                                  <div className="h-full bg-primary/60 rounded-full" style={{ width: `${Math.min(100, (bh.strength / 12) * 100)}%` }} />
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">
                    {kundaliData ? "Shadbala not calculated. Regenerate with shadbala option." : "Generate Kundali to see Shadbala"}
                  </p>
                )}
              </motion.div>
            )}

            {/* Ashtakavarga */}
            {activeTab === "ashtaka" && (
              <motion.div key="ashtaka" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Star className="h-3.5 w-3.5 text-primary" /> Ashtakavarga — Bindu Points per Rashi
                </p>
                {kundaliData?.ashtakavarga ? (() => {
                  const av = kundaliData.ashtakavarga!;
                  const rashiNames = ["Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya","Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"];
                  const planets7 = ["Surya","Chandra","Mangal","Budh","Guru","Shukra","Shani"];
                  return (
                    <div className="space-y-4">
                      {/* SAV grid */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Sarvashtakavarga (SAV) — Total Bindus per Rashi</p>
                        <div className="grid grid-cols-6 sm:grid-cols-12 gap-1">
                          {rashiNames.map((rashi, i) => {
                            const pts = av.sav.points[i] ?? 0;
                            const isGood = pts >= 30;
                            return (
                              <div key={rashi} className={`rounded-lg p-1.5 text-center ${isGood ? "bg-emerald-500/10 border border-emerald-500/20" : "bg-red-500/10 border border-red-500/20"}`}>
                                <p className="text-xs text-muted-foreground">{RASHI_SYMBOLS[rashi] ?? rashi.slice(0,2)}</p>
                                <p className={`text-sm font-bold ${isGood ? "text-emerald-400" : "text-red-400"}`}>{pts}</p>
                              </div>
                            );
                          })}
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">Total: {av.sav.total} · ≥30 = favourable</p>
                      </div>
                      {/* Reduced SAV */}
                      {av.reduced_sav && (
                        <div>
                          <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Reduced Sarvashtakavarga (after Trikona Shodhana)</p>
                          <div className="grid grid-cols-6 sm:grid-cols-12 gap-1">
                            {rashiNames.map((rashi, i) => {
                              const pts = av.reduced_sav!.points[i] ?? 0;
                              return (
                                <div key={rashi} className="rounded-lg p-1.5 text-center bg-muted/20">
                                  <p className="text-xs text-muted-foreground">{RASHI_SYMBOLS[rashi] ?? rashi.slice(0,2)}</p>
                                  <p className="text-sm font-bold text-foreground">{pts}</p>
                                </div>
                              );
                            })}
                          </div>
                          <p className="text-xs text-muted-foreground mt-1">Total: {av.reduced_sav.total}</p>
                        </div>
                      )}

                      {/* BAV table */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Bhinnashtakavarga (BAV) — Per-planet bindus</p>
                        <div className="overflow-x-auto">
                          <table className="w-full min-w-[420px] text-xs">
                            <thead>
                              <tr className="border-b border-border/20">
                                <th className="text-left text-muted-foreground py-1 pr-2 w-16">Planet</th>
                                {rashiNames.map((r) => (
                                  <th key={r} className="text-center text-muted-foreground py-1 px-0.5 w-6">
                                    {RASHI_SYMBOLS[r] ?? r.slice(0,2)}
                                  </th>
                                ))}
                                <th className="text-center text-muted-foreground py-1 px-1">Σ</th>
                              </tr>
                            </thead>
                            <tbody>
                              {planets7.map(gn => {
                                const bavData = av.bav[gn];
                                if (!bavData) return null;
                                return (
                                  <tr key={gn} className="border-b border-border/10">
                                    <td className="py-1 pr-2">
                                      <span style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                      <span className="text-muted-foreground ml-1">{gn.slice(0,3)}</span>
                                    </td>
                                    {bavData.points.map((pt, i) => (
                                      <td key={i} className={`text-center py-1 px-0.5 ${pt >= 4 ? "text-emerald-400" : pt <= 2 ? "text-red-400/70" : "text-foreground"}`}>
                                        {pt}
                                      </td>
                                    ))}
                                    <td className="text-center py-1 px-1 font-bold text-foreground">{bavData.total}</td>
                                  </tr>
                                );
                              })}
                              <tr className="border-t border-border/30 font-bold">
                                <td className="text-muted-foreground py-1.5 pr-2">SAV</td>
                                {av.sav.points.map((pt, i) => (
                                  <td key={i} className={`text-center py-1.5 px-0.5 font-bold ${pt >= 30 ? "text-emerald-400" : "text-red-400"}`}>{pt}</td>
                                ))}
                                <td className="text-center py-1.5 px-1 text-foreground font-bold">{av.sav.total}</td>
                              </tr>
                              {av.reduced_sav && (
                                <tr className="border-t border-border/20">
                                  <td className="text-muted-foreground py-1 pr-2 text-xs">Red.SAV</td>
                                  {av.reduced_sav.points.map((pt, i) => (
                                    <td key={i} className="text-center py-1 px-0.5 text-muted-foreground/70">{pt}</td>
                                  ))}
                                  <td className="text-center py-1 px-1 text-muted-foreground">{av.reduced_sav.total}</td>
                                </tr>
                              )}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    </div>
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">
                    {kundaliData ? "Ashtakavarga not calculated. Regenerate." : "Generate Kundali to see Ashtakavarga"}
                  </p>
                )}
              </motion.div>
            )}

            {/* Upagraha Tab */}
            {activeTab === "upagraha" && (
              <motion.div key="upagraha" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                {kundaliData?.upagraha && Object.keys(kundaliData.upagraha).length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full min-w-[380px] text-xs">
                      <thead>
                        <tr className="border-b border-border/20 bg-muted/10">
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium">Upagraha</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium">Rashi</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium">Longitude</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium hidden sm:table-cell">Nakshatra</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium hidden sm:table-cell">Nak Lord</th>
                          <th className="text-left px-3 py-2 text-muted-foreground font-medium hidden md:table-cell">Significance</th>
                        </tr>
                      </thead>
                      <tbody>
                        {Object.entries(kundaliData.upagraha).map(([name, u]: [string, UpagrahaEntry]) => {
                          const isMalefic = ["Dhuma","Vyatipata","Mandi","Gulika","Kala","Mrityu","YamaGhantaka"].includes(name);
                          return (
                            <tr key={name} className="border-b border-border/10 hover:bg-muted/10 transition-colors">
                              <td className="px-3 py-2 font-medium text-foreground">
                                <span className={isMalefic ? "text-red-400/80" : "text-emerald-400/80"}>●</span>
                                <span className="ml-1.5">{name}</span>
                              </td>
                              <td className="px-3 py-2 text-muted-foreground">{u.rashi_name} {RASHI_SYMBOLS[u.rashi_name] ?? ""}</td>
                              <td className="px-3 py-2 font-mono text-foreground">{u.dms}</td>
                              <td className="px-3 py-2 text-muted-foreground hidden sm:table-cell">{u.nakshatra}</td>
                              <td className="px-3 py-2 text-muted-foreground hidden sm:table-cell">{u.nakshatra_lord}</td>
                              <td className="px-3 py-2 text-muted-foreground/70 text-[10px] hidden md:table-cell max-w-xs truncate" title={u.significance}>
                                {u.significance.split(" — ")[1] ?? u.significance}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    <p className="text-[10px] text-muted-foreground/60 mt-3 px-1">
                      Sun-derived: Dhuma, Vyatipata, Parivesha, Indrachapa, Upaketu · Hora-based: Mandi, Gulika, Kala, Mrityu, ArdhaPrahara, YamaGhantaka
                    </p>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-10">
                    {kundaliData ? "Upagraha not calculated. Regenerate Kundali." : "Generate Kundali to see Upagrahas"}
                  </p>
                )}
              </motion.div>
            )}

            {/* Yogas Tab */}
            {activeTab === "yogas" && (
              <motion.div key="yogas" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                {yogas.length > 0 ? (
                  <div className="space-y-1.5">
                    <p className="text-xs text-muted-foreground mb-3">
                      {yogas.filter(y => y.present !== false).length} active of {yogas.length} total yogas — click any yoga for remedies
                    </p>
                    {yogas.map(y => {
                      const isPresent = y.present !== false;
                      const isExpY = expandedYoga === y.name;
                      const strengthCls = !isPresent ? "text-muted-foreground/60 border-border/10 bg-muted/5"
                        : y.strength === "Very Strong" ? "text-emerald-400 border-emerald-500/30 bg-emerald-500/10"
                        : y.strength === "Strong" ? "text-amber-400 border-amber-500/30 bg-amber-500/10"
                        : "text-muted-foreground border-border/20 bg-muted/10";
                      const catColor = YOGA_CATEGORY_COLORS[y.category ?? ""] ?? "text-muted-foreground";
                      return (
                        <div key={y.name} className={`rounded-lg border ${isPresent ? "bg-muted/20 border-border/20" : "bg-muted/5 border-border/10 opacity-50"}`}>
                          <button
                            className="w-full text-left p-2.5"
                            onClick={() => isPresent && setExpandedYoga(isExpY ? null : y.name)}
                          >
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-2">
                                <span className={`text-xs font-bold ${isPresent ? "text-emerald-400" : "text-red-400/50"}`}>{isPresent ? "✓" : "✗"}</span>
                                <p className={`text-xs font-medium ${isPresent ? "text-foreground" : "text-muted-foreground/70"}`}>{y.name}</p>
                                {y.category && <span className={`text-[10px] ${catColor}`}>{y.category}</span>}
                              </div>
                              <div className="flex items-center gap-2">
                                {isPresent && <span className={`text-[10px] px-1.5 py-0.5 rounded border ${strengthCls}`}>{y.strength}</span>}
                                {isPresent && (isExpY ? <ChevronUp className="h-3 w-3 text-muted-foreground" /> : <ChevronDown className="h-3 w-3 text-muted-foreground" />)}
                              </div>
                            </div>
                            {isPresent && <p className="text-[11px] text-muted-foreground mt-0.5">{y.desc}</p>}
                          </button>
                          {isPresent && isExpY && (
                            <div className="px-2.5 pb-2.5 border-t border-border/20 mt-0 pt-2 space-y-2">
                              <div className="flex items-center gap-1.5 mb-1">
                                <BookOpen className="w-3 h-3 text-star-gold" />
                                <span className="text-[10px] font-semibold text-star-gold uppercase tracking-wide">Remedies</span>
                              </div>
                              {[
                                { icon: "🕉️", label: "Mantra", val: y.mantra },
                                { icon: "💎", label: "Gemstone", val: y.gemstone },
                                { icon: "🪔", label: "Deity", val: y.deity },
                                { icon: "🌿", label: "Remedy", val: y.remedy },
                              ].filter(r => r.val).map(r => (
                                <div key={r.label} className="flex gap-2 text-[11px]">
                                  <span className="shrink-0 text-sm leading-none mt-0.5">{r.icon}</span>
                                  <div>
                                    <span className="text-muted-foreground/70 font-medium">{r.label}: </span>
                                    <span className="text-foreground">{r.val}</span>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-10">
                    {kundaliData ? t("kundli.no_yogas") : t("kundli.gen_yogas")}
                  </p>
                )}
              </motion.div>
            )}

            {/* Lagna Tab */}
            {activeTab === "lagna" && (
              <motion.div key="lagna" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="space-y-4">
                {kundaliData ? (
                  <>
                    {/* Lagna details */}
                    <div>
                      <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">{t("kundli.lagna")}</p>
                      <div className="grid grid-cols-2 gap-2 text-xs">
                        {[
                          [t("kundli.rashi"), `${kundaliData.lagna.rashi}${kundaliData.lagna.rashi_en ? ` (${kundaliData.lagna.rashi_en})` : ""}`],
                          [t("kundli.nakshatra"), `${kundaliData.lagna.nakshatra}${kundaliData.lagna.nakshatra_hindi ? ` ${kundaliData.lagna.nakshatra_hindi}` : ""}`],
                          [t("kundli.pada"), `${kundaliData.lagna.pada ?? "—"}`],
                          [t("kundli.degree"), `${kundaliData.lagna.degree}°${kundaliData.lagna.full_degree ? ` (${kundaliData.lagna.full_degree.toFixed(2)}° abs)` : ""}`],
                          [t("kundli.navamsha_lagna"), kundaliData.navamsha_lagna?.rashi ?? "—"],
                          [t("kundli.ayanamsha"), `${kundaliData.ayanamsha}° (${kundaliData.ayanamsha_label ?? "Lahiri"})`],
                          [t("kundli.rahu_ketu_mode"), kundaliData.rahu_mode === "true" ? "True Node" : "Mean Node"],
                          [t("kundli.place"), kundaliData.place ?? "—"],
                        ].map(([k, v]) => (
                          <div key={k} className="bg-muted/20 rounded-lg p-2">
                            <p className="text-[10px] text-muted-foreground uppercase tracking-wide">{k}</p>
                            <p className="text-foreground font-medium mt-0.5">{v}</p>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Birth Panchang */}
                    {kundaliData.birth_panchang && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">{t("kundli.birth_panchang")}</p>
                        <div className="grid grid-cols-2 gap-1.5 text-xs">
                          {[
                            [t("kundli.tithi"), `${kundaliData.birth_panchang.tithi} (${kundaliData.birth_panchang.paksha})`],
                            [t("kundli.vara"), kundaliData.birth_panchang.vara],
                            [t("kundli.nakshatra"), kundaliData.birth_panchang.moon_nakshatra ?? kundaliData.birth_panchang.yoga],
                            [t("kundli.yoga_panchang"), kundaliData.birth_panchang.yoga],
                            [t("kundli.karana"), kundaliData.birth_panchang.karana],
                            [t("kundli.sunrise"), kundaliData.birth_panchang.sunrise],
                            [t("kundli.sunset"), kundaliData.birth_panchang.sunset],
                            [t("kundli.moon_sign"), kundaliData.birth_panchang.moonsign ?? "—"],
                            [t("kundli.sun_sign"), kundaliData.birth_panchang.sunsign ?? "—"],
                            [t("kundli.surya_nak"), kundaliData.birth_panchang.surya_nakshatra ?? "—"],
                          ].map(([k, v]) => (
                            <div key={k} className="flex justify-between items-center py-1 px-2 rounded bg-muted/10 border border-border/10">
                              <span className="text-muted-foreground">{k}</span>
                              <span className="font-medium text-foreground">{v}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Personal characteristics */}
                    {kundaliData.personal && (
                      <div>
                        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">{t("kundli.personal_chars")}</p>
                        <div className="grid grid-cols-2 gap-1.5 text-xs">
                          {Object.entries(kundaliData.personal).map(([k, v]) => (
                            <div key={k} className="flex justify-between items-center py-1 px-2 rounded bg-muted/10 border border-border/10">
                              <span className="text-muted-foreground capitalize">{k}</span>
                              <span className="font-medium text-foreground">{v as string}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-10">{t("kundli.gen_lagna")}</p>
                )}
              </motion.div>
            )}

            {/* Bhav Chalit Chart */}
            {activeTab === "chalit" && (
              <motion.div key="chalit" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                <p className="text-xs font-medium text-foreground mb-3 flex items-center gap-2">
                  <Star className="h-3.5 w-3.5 text-primary" /> Bhav Chalit — Placidus House Cusps (Sidereal)
                </p>
                {kundaliData?.bhav_chalit?.cusps_sid?.length ? (() => {
                  const bc = kundaliData.bhav_chalit!;
                  return (
                    <div className="space-y-3">
                      {/* House cusps table */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">12 House Cusps</p>
                        <div className="grid grid-cols-2 gap-1.5">
                          {bc.cusps_sid.map((cusp, i) => {
                            const rashiIdx = Math.floor(cusp / 30);
                            const deg = (cusp % 30).toFixed(2);
                            const rashiNames = ["Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya","Tula","Vrischika","Dhanu","Makara","Kumbha","Meena"];
                            const rashi = rashiNames[rashiIdx] ?? "?";
                            return (
                              <div key={i} className="flex items-center gap-2 px-2 py-1.5 rounded-lg bg-muted/20 text-xs">
                                <span className="text-primary font-bold w-5 text-center">{i + 1}</span>
                                <span className="text-foreground">{rashi}</span>
                                <span className="text-muted-foreground ml-auto">{deg}°</span>
                                <span className="text-muted-foreground/70">{RASHI_SYMBOLS[rashi] ?? ""}</span>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                      {/* Planet bhav chalit placements */}
                      <div>
                        <p className="text-xs text-muted-foreground uppercase tracking-wide mb-2">Planet Placements (Bhav Chalit)</p>
                        <div className="space-y-1">
                          {GRAHA_ORDER.map(gn => {
                            const chHouse = bc.planets[gn];
                            const rashiHouse = kundaliData.grahas[gn]?.house;
                            if (chHouse === undefined) return null;
                            const shifted = chHouse !== rashiHouse;
                            return (
                              <div key={gn} className={`flex items-center gap-2 px-2 py-1.5 rounded-lg text-xs ${shifted ? "bg-amber-500/10 border border-amber-500/20" : "bg-muted/10"}`}>
                                <span className="text-base w-6 text-center" style={{ color: GRAHA_COLOR[gn] }}>{GRAHA_SYMBOLS[gn]}</span>
                                <span className="text-foreground flex-1">{gn} <span className="text-muted-foreground/70">({GRAHA_EN[gn]})</span></span>
                                <span className="text-muted-foreground">Rashi: H{rashiHouse}</span>
                                <span className={`font-semibold ${shifted ? "text-amber-400" : "text-foreground"}`}>Chalit: H{chHouse}</span>
                                {shifted && <span className="text-xs text-amber-400">shifted</span>}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    </div>
                  );
                })() : (
                  <p className="text-sm text-muted-foreground text-center py-8">
                    {kundaliData ? "Bhav Chalit not available" : "Generate Kundali to see Bhav Chalit"}
                  </p>
                )}
              </motion.div>
            )}
          </AnimatePresence>
          <p className="text-xs text-muted-foreground/60 leading-relaxed pt-3 border-t border-border/15 mt-2">
            Whole Sign Houses · {kundaliData?.ayanamsha_label ?? "Lahiri"} · {kundaliData?.rahu_mode === "true" ? "True" : "Mean"} Rahu/Ketu · Drik Siddhanta · For guidance consult a qualified Jyotishi.
          </p>
        </div>
      </div>

        </>
      )}

      <PageBot pageContext="kundali" pageData={kundaliData ?? {}} />
    </div>
  );
}
