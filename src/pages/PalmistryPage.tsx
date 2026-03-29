import { useState, useRef, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import PageBot from '@/components/PageBot';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTranslation } from "react-i18next";
import { useRegisterPageBot } from "@/hooks/useRegisterPageBot";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Hand, Scan, BookOpen, Star, Sparkles, ChevronRight,
  Upload, Camera, X, Download, RotateCcw, CheckCircle2,
  ArrowLeft, ArrowRight, Info,
} from "lucide-react";

// ─────────────────────────────────────────────────────────────────
// DATA — rich Vedic knowledge base
// ─────────────────────────────────────────────────────────────────

const HAND_TYPES = [
  {
    id: "earth", label: "Earth Hand", labelKey: "palmistry.hand_earth_label", vedic: "Prithvi Hasta", vedicKey: "palmistry.hand_earth_vedic", shape: "Square palm + Short fingers", shapeKey: "palmistry.hand_earth_shape",
    element: "Earth (Prithvi)", elementKey: "palmistry.hand_earth_element", icon: "🌍",
    traits: ["Practical & reliable", "Hardworking", "Strong physical constitution", "Prefers routine", "Hands-on learner"],
    traitKeys: ["palmistry.hand_earth_trait1","palmistry.hand_earth_trait2","palmistry.hand_earth_trait3","palmistry.hand_earth_trait4","palmistry.hand_earth_trait5"],
    careers: "Builder, surgeon, chef, craftsman, athlete, engineer", careersKey: "palmistry.hand_earth_careers",
    constitution: "Kapha (Ayurveda) — stable, grounding, enduring", constitutionKey: "palmistry.hand_earth_constitution",
    shadow: "Stubbornness, resistance to change, overattachment to the material", shadowKey: "palmistry.hand_earth_shadow",
    vedic_note: "Bhu-Tattva dominant. Deeply connected to the material plane. The karmic path involves mastery through physical effort and grounded wisdom.", vedic_noteKey: "palmistry.hand_earth_note",
    life_modifier: { health: 2, wealth: 1, love: 0, mental: -1, spiritual: -1, career: 2 },
  },
  {
    id: "air", label: "Air Hand", labelKey: "palmistry.hand_air_label", vedic: "Vayu Hasta", vedicKey: "palmistry.hand_air_vedic", shape: "Square palm + Long fingers", shapeKey: "palmistry.hand_air_shape",
    element: "Air (Vayu)", elementKey: "palmistry.hand_air_element", icon: "💨",
    traits: ["Intellectual", "Curious & versatile", "Excellent communicator", "Social", "Ideas-driven"],
    traitKeys: ["palmistry.hand_air_trait1","palmistry.hand_air_trait2","palmistry.hand_air_trait3","palmistry.hand_air_trait4","palmistry.hand_air_trait5"],
    careers: "Writer, journalist, teacher, scientist, lawyer, philosopher", careersKey: "palmistry.hand_air_careers",
    constitution: "Vata (Ayurveda) — swift, mercurial, adaptive", constitutionKey: "palmistry.hand_air_constitution",
    shadow: "Anxiety, restlessness, difficulty grounding ideas into reality", shadowKey: "palmistry.hand_air_shadow",
    vedic_note: "Vayu-Tattva dominant. Ruled by Budha (Mercury). The karmic path involves using the gift of communication to serve truth and uplift others.", vedic_noteKey: "palmistry.hand_air_note",
    life_modifier: { health: -1, wealth: 1, love: 1, mental: 2, spiritual: 1, career: 1 },
  },
  {
    id: "water", label: "Water Hand", labelKey: "palmistry.hand_water_label", vedic: "Jala Hasta", vedicKey: "palmistry.hand_water_vedic", shape: "Long oval palm + Long fine fingers", shapeKey: "palmistry.hand_water_shape",
    element: "Water (Jala)", elementKey: "palmistry.hand_water_element", icon: "💧",
    traits: ["Deeply intuitive", "Empathetic", "Creative & artistic", "Psychically sensitive", "Absorbs others' energies"],
    traitKeys: ["palmistry.hand_water_trait1","palmistry.hand_water_trait2","palmistry.hand_water_trait3","palmistry.hand_water_trait4","palmistry.hand_water_trait5"],
    careers: "Healer, astrologer, artist, psychologist, poet, musician", careersKey: "palmistry.hand_water_careers",
    constitution: "Kapha-Pitta (Ayurveda) — fluid, receptive, emotionally rich", constitutionKey: "palmistry.hand_water_constitution",
    shadow: "Over-sensitivity, poor boundaries, escapism, emotional overwhelm", shadowKey: "palmistry.hand_water_shadow",
    vedic_note: "Jala-Tattva dominant. Ruled by Chandra (Moon). The karmic path involves learning to channel deep sensitivity into healing and creative gifts without losing self.", vedic_noteKey: "palmistry.hand_water_note",
    life_modifier: { health: 0, wealth: -1, love: 2, mental: 1, spiritual: 2, career: 0 },
  },
  {
    id: "fire", label: "Fire Hand", labelKey: "palmistry.hand_fire_label", vedic: "Agni Hasta", vedicKey: "palmistry.hand_fire_vedic", shape: "Long palm + Short fingers", shapeKey: "palmistry.hand_fire_shape",
    element: "Fire (Agni)", elementKey: "palmistry.hand_fire_element", icon: "🔥",
    traits: ["Passionate & energetic", "Natural leader", "Charismatic & inspiring", "Ambitious", "Impatient"],
    traitKeys: ["palmistry.hand_fire_trait1","palmistry.hand_fire_trait2","palmistry.hand_fire_trait3","palmistry.hand_fire_trait4","palmistry.hand_fire_trait5"],
    careers: "Entrepreneur, military leader, performer, athlete, politician, pioneer", careersKey: "palmistry.hand_fire_careers",
    constitution: "Pitta (Ayurveda) — radiant, forceful, transforming", constitutionKey: "palmistry.hand_fire_constitution",
    shadow: "Arrogance, burnout, aggression, inability to rest or surrender", shadowKey: "palmistry.hand_fire_shadow",
    vedic_note: "Agni-Tattva dominant. Ruled by Surya + Mangal. The karmic path involves tempering the fire of ambition with humility and compassion, so the flame uplifts rather than burns.", vedic_noteKey: "palmistry.hand_fire_note",
    life_modifier: { health: 1, wealth: 2, love: 0, mental: 0, spiritual: -1, career: 2 },
  },
];

const QUESTIONS = [
  {
    id: "heart",
    line: "Heart Line",
    lineKey: "data.palmistry.questions.heart_line",
    sanskrit: "हृदय रेखा",
    instruction: "Look at the TOPMOST horizontal line running across your upper palm.",
    instructionKey: "data.palmistry.questions.heart_instruction",
    hint: "It typically runs from below your little finger toward your index or middle finger.",
    hintKey: "data.palmistry.questions.heart_hint",
    color: "#E8650A",
    options: [
      { id: "long_curved", label: "Long & Deeply Curved", labelKey: "data.palmistry.opt.heart_long_curved_label", desc: "Sweeps up toward the index/middle finger in a strong arc", descKey: "data.palmistry.opt.heart_long_curved_desc", meaning: "Warm, expressive, deeply romantic. You love unconditionally and wear your heart on your sleeve. Relationships are central to your life purpose.", meaningKey: "data.palmistry.opt.heart_long_curved_meaning", vedic: "Strong Shukra (Venus) influence — blessings in love, partnership, and devotion. The heart is your greatest teacher.", vedicKey: "data.palmistry.opt.heart_long_curved_vedic", scores: { love: 3, spiritual: 1, mental: 0, health: 1, wealth: 0, career: 0 } },
      { id: "straight_short", label: "Straight & Short", labelKey: "data.palmistry.opt.heart_straight_short_label", desc: "Relatively straight, doesn't reach as far across the palm", descKey: "data.palmistry.opt.heart_straight_short_desc", meaning: "Practical and measured in emotion. You love deeply but express it through loyal actions rather than grand gestures. Reliability is your love language.", meaningKey: "data.palmistry.opt.heart_straight_short_meaning", vedic: "Balanced Chandra (Moon) — emotional intelligence without overwhelm. Disciplined heart.", vedicKey: "data.palmistry.opt.heart_straight_short_vedic", scores: { love: 1, spiritual: 0, mental: 1, health: 0, wealth: 1, career: 1 } },
      { id: "forked", label: "Forked at End", labelKey: "data.palmistry.opt.heart_forked_label", desc: "The line splits into two branches near the end", descKey: "data.palmistry.opt.heart_forked_desc", meaning: "Rare and auspicious. You balance heart and mind beautifully — empathetic yet rational. Natural counselor or mediator. You see both sides of every relationship.", meaningKey: "data.palmistry.opt.heart_forked_meaning", vedic: "Budha-Chandra yoga — harmonious mind-heart communication. The fork indicates the Viveka (discernment) to choose wisely in love.", vedicKey: "data.palmistry.opt.heart_forked_vedic", scores: { love: 2, spiritual: 1, mental: 2, health: 0, wealth: 0, career: 1 } },
      { id: "high_index", label: "Ends Below Index Finger", labelKey: "data.palmistry.opt.heart_high_index_label", desc: "The line curves up and ends near the base of your index finger", descKey: "data.palmistry.opt.heart_high_index_desc", meaning: "Idealistic in love. You seek nothing less than a divine, soulmate-level connection. High standards — and you deserve them. Can feel perpetually unsatisfied until you find true union.", meaningKey: "data.palmistry.opt.heart_high_index_meaning", vedic: "Guru (Jupiter) sub-influence on the heart — seeks Prema (divine love). Marriage as spiritual sadhana.", vedicKey: "data.palmistry.opt.heart_high_index_vedic", scores: { love: 2, spiritual: 2, mental: 0, health: 0, wealth: 0, career: 0 } },
      { id: "broken", label: "Broken or Chained", labelKey: "data.palmistry.opt.heart_broken_label", desc: "Has gaps, islands, or chain-like appearance", descKey: "data.palmistry.opt.heart_broken_desc", meaning: "Significant emotional journey. Past heartbreaks have shaped you deeply. These scars are also sources of immense compassion and wisdom. Growth through vulnerability.", meaningKey: "data.palmistry.opt.heart_broken_meaning", vedic: "Rahu/Ketu axis — karmic relationship lessons carried across lifetimes. The break is a portal to transformation.", vedicKey: "data.palmistry.opt.heart_broken_vedic", scores: { love: 0, spiritual: 2, mental: -1, health: -1, wealth: 0, career: 0 } },
    ],
  },
  {
    id: "head",
    line: "Head Line",
    lineKey: "data.palmistry.questions.head_line",
    sanskrit: "मस्तिष्क रेखा",
    instruction: "Find the MIDDLE horizontal line running across your palm.",
    instructionKey: "data.palmistry.questions.head_instruction",
    hint: "It usually starts near the edge between your thumb and index finger, running across toward the outer palm.",
    hintKey: "data.palmistry.questions.head_hint",
    color: "#C8860A",
    options: [
      { id: "long_straight", label: "Long & Straight", labelKey: "data.palmistry.opt.head_long_straight_label", desc: "Extends nearly across the full width of the palm in a straight line", descKey: "data.palmistry.opt.head_long_straight_desc", meaning: "Razor-sharp analytical mind. You excel at logic, mathematics, science, law, and strategic thinking. You see patterns others miss. Your intellect is your greatest asset.", meaningKey: "data.palmistry.opt.head_long_straight_meaning", vedic: "Strong Budha (Mercury) — exceptional intellectual capacity. Saraswati's grace on the mind.", vedicKey: "data.palmistry.opt.head_long_straight_vedic", scores: { mental: 3, career: 2, wealth: 1, love: -1, spiritual: 0, health: 0 } },
      { id: "curved_down", label: "Deeply Curved Downward", labelKey: "data.palmistry.opt.head_curved_down_label", desc: "Curves noticeably downward toward the base of the palm", descKey: "data.palmistry.opt.head_curved_down_desc", meaning: "Highly creative and imaginative. You think in images, metaphors, and stories. Writer, artist, visionary — the world of ideas is your natural home. Intuition over logic.", meaningKey: "data.palmistry.opt.head_curved_down_meaning", vedic: "Chandra dominance on Buddhi — imaginative intelligence. Saraswati as Vak Shakti (power of creative expression).", vedicKey: "data.palmistry.opt.head_curved_down_vedic", scores: { mental: 2, career: 1, love: 1, spiritual: 2, health: 0, wealth: -1 } },
      { id: "fork_end", label: "Forked at End (Writer's Fork)", labelKey: "data.palmistry.opt.head_fork_end_label", desc: "Splits into two branches — one straight, one curved — near the end", descKey: "data.palmistry.opt.head_fork_end_desc", meaning: "The most auspicious head line. You hold both analytical and creative intelligence simultaneously. Exceptional in communication, law, literature, philosophy, and teaching.", meaningKey: "data.palmistry.opt.head_fork_end_meaning", vedic: "Saraswati blessing — dual vision, the gift of eloquence. Can translate complex ideas into beautiful language.", vedicKey: "data.palmistry.opt.head_fork_end_vedic", scores: { mental: 3, career: 2, love: 1, spiritual: 1, health: 0, wealth: 1 } },
      { id: "short", label: "Short", labelKey: "data.palmistry.opt.head_short_label", desc: "Doesn't extend very far across the palm", descKey: "data.palmistry.opt.head_short_desc", meaning: "Decisive and action-oriented. You trust your instincts over lengthy analysis. Quick thinking, entrepreneurial spirit. Sometimes seen as impulsive — but often you're simply operating on a different level of knowing.", meaningKey: "data.palmistry.opt.head_short_meaning", vedic: "Mangal (Mars) influence on intellect — courage of thought. The instinct IS the intelligence.", vedicKey: "data.palmistry.opt.head_short_vedic", scores: { mental: 1, career: 2, love: 0, spiritual: 0, health: 1, wealth: 1 } },
      { id: "chained", label: "Wavy or Chained", labelKey: "data.palmistry.opt.head_chained_label", desc: "Appears uneven, wavy, or has chain-like links along it", descKey: "data.palmistry.opt.head_chained_desc", meaning: "Sensitive, creative, and deeply perceptive. Your mind picks up subtle energies others miss. This comes with mental sensitivity — meditation and regular mental rest are essential.", meaningKey: "data.palmistry.opt.head_chained_meaning", vedic: "Ketu influence on Manas — a mind that perceives across dimensions. Highly psychic but needs grounding.", vedicKey: "data.palmistry.opt.head_chained_vedic", scores: { mental: 1, career: 0, love: 1, spiritual: 3, health: -1, wealth: -1 } },
    ],
  },
  {
    id: "life",
    line: "Life Line",
    lineKey: "data.palmistry.questions.life_line",
    sanskrit: "जीवन रेखा",
    instruction: "Find the curved line that sweeps around the base of your thumb.",
    instructionKey: "data.palmistry.questions.life_instruction",
    hint: "It typically starts between your index finger and thumb, curving down around the thumb mount (Venus mount).",
    hintKey: "data.palmistry.questions.life_hint",
    color: "#6D28D9",
    options: [
      { id: "wide_deep", label: "Wide Arc & Deep", labelKey: "data.palmistry.opt.life_wide_deep_label", desc: "Makes a large, sweeping arc around the thumb — goes far into the palm", descKey: "data.palmistry.opt.life_wide_deep_desc", meaning: "Abundant Prana (life force). You have robust vitality, strong physical resilience, and a genuine love of life. Physical activity comes naturally. You recover quickly from setbacks.", meaningKey: "data.palmistry.opt.life_wide_deep_meaning", vedic: "Surya-Mangal blessings — solar vitality and warrior constitution. Strong Ojas (immunity). Dhanvantari's grace.", vedicKey: "data.palmistry.opt.life_wide_deep_vedic", scores: { health: 3, career: 2, love: 1, mental: 0, spiritual: 0, wealth: 1 } },
      { id: "long_clear", label: "Long & Unbroken", labelKey: "data.palmistry.opt.life_long_clear_label", desc: "Extends far down the palm with no breaks or islands", descKey: "data.palmistry.opt.life_long_clear_desc", meaning: "Steady, sustained vitality throughout life. Few major health crises. Your energy is reliable and consistent. Others draw strength from your presence.", meaningKey: "data.palmistry.opt.life_long_clear_meaning", vedic: "Dhanvantari grace — blessed health karma from prior lifetimes of righteous living. Prana is protected.", vedicKey: "data.palmistry.opt.life_long_clear_vedic", scores: { health: 3, career: 1, love: 0, mental: 1, spiritual: 0, wealth: 0 } },
      { id: "double", label: "Double Life Line", labelKey: "data.palmistry.opt.life_double_label", desc: "A parallel line runs alongside the main life line", descKey: "data.palmistry.opt.life_double_desc", meaning: "Extraordinary — this is the Kavach Rekha (protective mark). You carry divine protection. Strong spiritual guidance from ancestors or Ishta Devata. Remarkable resilience and recovery from any situation.", meaningKey: "data.palmistry.opt.life_double_meaning", vedic: "Pitru Kavach and Ishta Devata's active blessing. One of the most auspicious marks in all of Samudrika Shastra.", vedicKey: "data.palmistry.opt.life_double_vedic", scores: { health: 3, career: 1, love: 1, mental: 1, spiritual: 3, wealth: 1 } },
      { id: "close_thumb", label: "Close to Thumb (Tight Arc)", labelKey: "data.palmistry.opt.life_close_thumb_label", desc: "Stays close to the thumb, doesn't sweep far out", descKey: "data.palmistry.opt.life_close_thumb_desc", meaning: "More introverted energy. You conserve and channel your vitality with precision rather than broadcasting it widely. Quality over quantity in all things — relationships, work, energy expenditure.", meaningKey: "data.palmistry.opt.life_close_thumb_meaning", vedic: "Shani influence on Prana — teaches conservation and mastery. The monk archetype — disciplined and focused.", vedicKey: "data.palmistry.opt.life_close_thumb_vedic", scores: { health: 1, career: 0, love: -1, mental: 2, spiritual: 2, wealth: 0 } },
      { id: "broken", label: "Broken or Has Islands", labelKey: "data.palmistry.opt.life_broken_label", desc: "Has visible gaps, breaks, or island shapes along it", descKey: "data.palmistry.opt.life_broken_desc", meaning: "Life has involved (or will involve) major transformations. A break is not an ending — it is a rebirth. Each transition you navigate builds extraordinary wisdom. You are a survivor and a transformer.", meaningKey: "data.palmistry.opt.life_broken_meaning", vedic: "Rahu transit marker — radical life pivots that serve divine redirection. The phoenix pattern.", vedicKey: "data.palmistry.opt.life_broken_vedic", scores: { health: -1, career: -1, love: 0, mental: 0, spiritual: 2, wealth: -1 } },
    ],
  },
  {
    id: "fate",
    line: "Fate Line",
    lineKey: "data.palmistry.questions.fate_line",
    sanskrit: "भाग्य रेखा",
    instruction: "Look for a vertical line running up the CENTER of your palm.",
    instructionKey: "data.palmistry.questions.fate_instruction",
    hint: "It typically runs from the base of your palm upward toward your middle (Saturn) finger. Some palms don't have this line — that's perfectly fine.",
    hintKey: "data.palmistry.questions.fate_hint",
    color: "#C8860A",
    options: [
      { id: "deep_clear", label: "Deep & Clear from Wrist", labelKey: "data.palmistry.opt.fate_deep_clear_label", desc: "A strong, clear vertical line running from near the wrist upward", descKey: "data.palmistry.opt.fate_deep_clear_desc", meaning: "Powerful dharmic purpose. Your life path is clear and purposeful from an early age. Career feels like a calling. Success comes through disciplined, long-term commitment to your path.", meaningKey: "data.palmistry.opt.fate_deep_clear_meaning", vedic: "Shani yoga strong — disciplined effort is magnificently rewarded. Prarabdha karma is guiding you firmly toward a specific destiny.", vedicKey: "data.palmistry.opt.fate_deep_clear_vedic", scores: { career: 3, wealth: 2, mental: 1, health: 0, love: 0, spiritual: 1 } },
      { id: "starts_late", label: "Starts in Middle of Palm", labelKey: "data.palmistry.opt.fate_starts_late_label", desc: "The fate line only begins halfway up the palm, not from the base", descKey: "data.palmistry.opt.fate_starts_late_desc", meaning: "A late bloomer — and that's your superpower. Life clarity and career success come after age 30-35. You're building foundations others don't see. Your greatest achievements are still ahead.", meaningKey: "data.palmistry.opt.fate_starts_late_meaning", vedic: "Shani dasha activation — Saturn's gifts come after his tests. The deepest roots take longest to grow.", vedicKey: "data.palmistry.opt.fate_starts_late_vedic", scores: { career: 2, wealth: 1, mental: 1, health: 0, love: 0, spiritual: 1 } },
      { id: "broken_shifting", label: "Broken or Shifts Direction", labelKey: "data.palmistry.opt.fate_broken_shifting_label", desc: "Has breaks, or noticeably changes direction midway", descKey: "data.palmistry.opt.fate_broken_shifting_desc", meaning: "Multiple life chapters and career pivots. Each phase is complete in itself. You're not inconsistent — you're evolving. Each break represents a conscious or divinely-led reinvention.", meaningKey: "data.palmistry.opt.fate_broken_shifting_meaning", vedic: "Rahu-Shani interaction — disruption precedes breakthrough. The snake sheds its skin.", vedicKey: "data.palmistry.opt.fate_broken_shifting_vedic", scores: { career: 1, wealth: 0, mental: 0, health: 0, love: 1, spiritual: 2 } },
      { id: "absent", label: "Not Visible / Absent", labelKey: "data.palmistry.opt.fate_absent_label", desc: "No clear vertical line in the center of the palm", descKey: "data.palmistry.opt.fate_absent_desc", meaning: "Complete free will. You are not bound by a predetermined destiny — you are the author of your own story in the most literal sense. Self-made in every way. Some of the most successful free spirits have no fate line.", meaningKey: "data.palmistry.opt.fate_absent_meaning", vedic: "Moksha marker — free from karma's direct grip. Pure agency. A life of Kriyamana karma (self-created karma).", vedicKey: "data.palmistry.opt.fate_absent_vedic", scores: { career: 1, wealth: 1, mental: 0, health: 0, love: 0, spiritual: 2 } },
      { id: "double", label: "Double Fate Line", labelKey: "data.palmistry.opt.fate_double_label", desc: "Two parallel vertical lines running up the palm", descKey: "data.palmistry.opt.fate_double_desc", meaning: "Extraordinarily rare and auspicious. You are gifted with dual dharma — two parallel callings that you pursue simultaneously. A blessed multi-dimensional life.", meaningKey: "data.palmistry.opt.fate_double_meaning", vedic: "Double Shani Rekha — blessed with dual purpose. The ancient texts describe this as a rare mark of those who serve two roles in society.", vedicKey: "data.palmistry.opt.fate_double_vedic", scores: { career: 3, wealth: 2, mental: 1, health: 0, love: 1, spiritual: 1 } },
    ],
  },
  {
    id: "sun",
    line: "Sun Line",
    lineKey: "data.palmistry.questions.sun_line",
    sanskrit: "सूर्य रेखा",
    instruction: "Look for a vertical line near your RING finger.",
    instructionKey: "data.palmistry.questions.sun_instruction",
    hint: "It runs upward toward your ring (Apollo) finger, parallel to the fate line. Many people have a faint version of this.",
    hintKey: "data.palmistry.questions.sun_hint",
    color: "#E8650A",
    options: [
      { id: "strong_clear", label: "Strong & Clear", labelKey: "data.palmistry.opt.sun_strong_clear_label", desc: "A noticeable vertical line clearly approaching the ring finger", descKey: "data.palmistry.opt.sun_strong_clear_desc", meaning: "Fame, recognition, and creative success are yours. You have a natural public presence — people notice you. Your creative or professional achievements will be recognized widely. Possible public career.", meaningKey: "data.palmistry.opt.sun_strong_clear_meaning", vedic: "Surya strong — solar radiance, fame as the result of dharmic action in past lives. Kirti (fame) is your birthright.", vedicKey: "data.palmistry.opt.sun_strong_clear_vedic", scores: { career: 2, wealth: 1, love: 1, mental: 0, health: 0, spiritual: 1 } },
      { id: "multiple_short", label: "Multiple Short Lines", labelKey: "data.palmistry.opt.sun_multiple_short_label", desc: "Several small lines near the ring finger rather than one clear line", descKey: "data.palmistry.opt.sun_multiple_short_desc", meaning: "Many talents and creative gifts — but scattered across too many directions. You shine in bursts. Focusing your energy on one core creative path will unlock sustained recognition.", meaningKey: "data.palmistry.opt.sun_multiple_short_meaning", vedic: "Surya dispersed across multiple Rashis — needs a single dharmic focus to crystallize into fame.", vedicKey: "data.palmistry.opt.sun_multiple_short_vedic", scores: { career: 1, wealth: 0, love: 0, mental: 1, health: 0, spiritual: 1 } },
      { id: "starts_heart", label: "Starts from Heart Line", labelKey: "data.palmistry.opt.sun_starts_heart_label", desc: "The sun line appears to begin from where the heart line is", descKey: "data.palmistry.opt.sun_starts_heart_desc", meaning: "Recognition comes through authentic passion and love. Your fame is earned through genuine creative expression, not calculation. Success arrives in mid-to-late life — permanent and deeply fulfilling.", meaningKey: "data.palmistry.opt.sun_starts_heart_meaning", vedic: "Delayed Surya grace — but when it comes, it is the most enduring kind. Built on devotion rather than ambition.", vedicKey: "data.palmistry.opt.sun_starts_heart_vedic", scores: { career: 2, wealth: 1, love: 2, mental: 0, health: 0, spiritual: 2 } },
      { id: "absent", label: "Absent / Not Visible", labelKey: "data.palmistry.opt.sun_absent_label", desc: "No clear line near the ring finger", descKey: "data.palmistry.opt.sun_absent_desc", meaning: "Success without public fame. You achieve significantly — but prefer to work behind the scenes. Inner satisfaction matters more to you than recognition. A powerful, private achiever.", meaningKey: "data.palmistry.opt.sun_absent_meaning", vedic: "Inner Surya — the light shines inward rather than outward. Spiritual achievement over worldly recognition.", vedicKey: "data.palmistry.opt.sun_absent_vedic", scores: { career: 0, wealth: 0, love: 0, mental: 1, health: 0, spiritual: 2 } },
    ],
  },
  {
    id: "mount_venus",
    line: "Mount of Venus",
    lineKey: "data.palmistry.questions.mount_venus_line",
    sanskrit: "शुक्र पर्वत",
    instruction: "Look at the fleshy area at the BASE of your THUMB.",
    instructionKey: "data.palmistry.questions.mount_venus_instruction",
    hint: "Press your thumb gently to your palm — the padded area that rises is the Venus mount.",
    hintKey: "data.palmistry.questions.mount_venus_hint",
    color: "#E8650A",
    options: [
      { id: "large_full", label: "Large & Well Developed", labelKey: "data.palmistry.opt.venus_large_full_label", desc: "Full, firm, and prominent — noticeably raised", descKey: "data.palmistry.opt.venus_large_full_desc", meaning: "Abundant capacity for love, pleasure, beauty, and sensuality. You give generously in relationships. Naturally creative, with an appreciation for art, music, and beauty in all forms. Life is meant to be enjoyed.", meaningKey: "data.palmistry.opt.venus_large_full_meaning", vedic: "Shukra strong — Kama (desire) is healthy and abundant. Great blessings in love, partnerships, and artistic pursuits.", vedicKey: "data.palmistry.opt.venus_large_full_vedic", scores: { love: 3, spiritual: 0, mental: 0, health: 1, wealth: 0, career: 0 } },
      { id: "medium", label: "Medium / Moderate", labelKey: "data.palmistry.opt.venus_medium_label", desc: "Present but not especially prominent or flat", descKey: "data.palmistry.opt.venus_medium_desc", meaning: "Balanced approach to love and pleasure. You enjoy life's sensory gifts without being controlled by them. Healthy relationships, moderate creative expression.", meaningKey: "data.palmistry.opt.venus_medium_meaning", vedic: "Shukra in balance — the middle path of Kama. Healthy desire without attachment.", vedicKey: "data.palmistry.opt.venus_medium_vedic", scores: { love: 1, spiritual: 1, mental: 0, health: 1, wealth: 1, career: 0 } },
      { id: "flat", label: "Flat or Barely Visible", labelKey: "data.palmistry.opt.venus_flat_label", desc: "The base of the thumb area is relatively flat", descKey: "data.palmistry.opt.venus_flat_desc", meaning: "More reserved in emotional and sensory expression. You tend toward the ascetic rather than the sensual. Relationships may feel secondary to work or spiritual pursuits. Self-sufficiency is a core value.", meaningKey: "data.palmistry.opt.venus_flat_meaning", vedic: "Shukra subdued — Vairagya (detachment) over Bhoga (enjoyment). The renunciant or dedicated professional.", vedicKey: "data.palmistry.opt.venus_flat_vedic", scores: { love: -1, spiritual: 2, mental: 1, health: 0, wealth: 0, career: 1 } },
    ],
  },
  {
    id: "mount_jupiter",
    line: "Mount of Jupiter",
    lineKey: "data.palmistry.questions.mount_jupiter_line",
    sanskrit: "गुरु पर्वत",
    instruction: "Look at the base of your INDEX finger.",
    instructionKey: "data.palmistry.questions.mount_jupiter_instruction",
    hint: "The padded area directly below the index finger — when developed, it creates a visible fullness at the finger's base.",
    hintKey: "data.palmistry.questions.mount_jupiter_hint",
    color: "#C8860A",
    options: [
      { id: "prominent", label: "Prominent & Raised", labelKey: "data.palmistry.opt.jupiter_prominent_label", desc: "Noticeably full area below the index finger", descKey: "data.palmistry.opt.jupiter_prominent_desc", meaning: "Natural leader, teacher, and guide. You have strong ambition, confidence, and a genuine desire to uplift others. People naturally look to you for direction. Authority comes naturally.", meaningKey: "data.palmistry.opt.jupiter_prominent_meaning", vedic: "Guru strong — Brahma Jnana, leadership blessed by Jupiter. Born to teach, guide, or lead. The Guru archetype.", vedicKey: "data.palmistry.opt.jupiter_prominent_vedic", scores: { career: 3, love: 1, mental: 1, health: 0, wealth: 2, spiritual: 2 } },
      { id: "moderate", label: "Moderate", labelKey: "data.palmistry.opt.jupiter_moderate_label", desc: "Present but not dramatically raised", descKey: "data.palmistry.opt.jupiter_moderate_desc", meaning: "Healthy confidence and ambition. You lead when needed but don't seek the spotlight for its own sake. Good judgment and fair authority.", meaningKey: "data.palmistry.opt.jupiter_moderate_meaning", vedic: "Guru in balance — wisdom applied practically. Dharmic leadership.", vedicKey: "data.palmistry.opt.jupiter_moderate_vedic", scores: { career: 1, love: 0, mental: 1, health: 0, wealth: 1, spiritual: 1 } },
      { id: "flat", label: "Flat or Underdeveloped", labelKey: "data.palmistry.opt.jupiter_flat_label", desc: "Little or no fullness below the index finger", descKey: "data.palmistry.opt.jupiter_flat_desc", meaning: "May struggle with confidence or claiming authority. Leadership may feel uncomfortable. The inner Jupiter is waiting to be awakened — often through a Saturn transit or significant challenge.", meaningKey: "data.palmistry.opt.jupiter_flat_meaning", vedic: "Guru dormant — the teacher within is not yet activated. Faith and study will awaken it.", vedicKey: "data.palmistry.opt.jupiter_flat_vedic", scores: { career: -1, love: 0, mental: 0, health: 0, wealth: -1, spiritual: 0 } },
    ],
  },
];

// ─────────────────────────────────────────────────────────────────
// REPORT ENGINE — pure frontend, no API
// ─────────────────────────────────────────────────────────────────

interface Selection { questionId: string; optionId: string; }

interface LifeScore { area: string; score: number; icon: string; }

interface GeneratedReport {
  handType: typeof HAND_TYPES[0];
  selections: Selection[];
  lifeScores: LifeScore[];
  strengths: string[];
  challenges: string[];
  remedies: { title: string; detail: string }[];
  summary: string;
  auspiciousNote: string;
}

function buildReport(handTypeId: string, selections: Selection[]): GeneratedReport {
  const handType = HAND_TYPES.find(h => h.id === handTypeId) ?? HAND_TYPES[0];

  // Accumulate life area scores
  const raw: Record<string, number> = {
    love: 5, mental: 5, health: 5, wealth: 5, career: 5, spiritual: 5,
  };

  // Apply hand type modifier
  Object.entries(handType.life_modifier).forEach(([k, v]) => { raw[k] += v; });

  // Apply each selection modifier
  selections.forEach(sel => {
    const q = QUESTIONS.find(q => q.id === sel.questionId);
    const opt = q?.options.find(o => o.id === sel.optionId);
    if (opt?.scores) {
      Object.entries(opt.scores).forEach(([k, v]) => { raw[k] += v; });
    }
  });

  // Clamp 1–10
  const clamp = (v: number) => Math.min(10, Math.max(1, v));
  const lifeScores: LifeScore[] = [
    { area: "Love & Relationships", score: clamp(raw.love), icon: "❤️" },
    { area: "Career & Purpose", score: clamp(raw.career), icon: "⭐" },
    { area: "Wealth & Prosperity", score: clamp(raw.wealth), icon: "💰" },
    { area: "Mental Clarity", score: clamp(raw.mental), icon: "🧠" },
    { area: "Health & Vitality", score: clamp(raw.health), icon: "🌿" },
    { area: "Spiritual Growth", score: clamp(raw.spiritual), icon: "🕉️" },
  ];

  // Derive strengths from highest-scoring options
  const strengths: string[] = [];
  const challenges: string[] = [];

  selections.forEach(sel => {
    const q = QUESTIONS.find(q => q.id === sel.questionId);
    const opt = q?.options.find(o => o.id === sel.optionId);
    if (!opt) return;
    const sum = Object.values(opt.scores).reduce((a, b) => a + b, 0);
    if (sum >= 3) strengths.push(`${q!.line}: ${opt.meaning.split(".")[0]}.`);
    if (sum <= -1) challenges.push(`${q!.line}: ${opt.meaning.split(".").slice(-2).join(".")}`);
  });

  // Hand type strengths
  strengths.unshift(`${handType.label} (${handType.vedic}) — ${handType.traits[0]}, ${handType.traits[1]}.`);

  // Remedies based on scores
  const remedies: { title: string; detail: string }[] = [];
  if (raw.love < 5) remedies.push({ title: "Shukra Mantra", detail: "Chant 'Om Shukraya Namah' 108 times on Fridays. Wear white and offer white flowers to the deity of your choice. This strengthens Venus energy and opens the heart." });
  if (raw.career < 5) remedies.push({ title: "Shani Stotra", detail: "Recite the Shani Chalisa on Saturdays. Donate black sesame seeds (til) or mustard oil on Saturdays. This activates Shani's blessings for karma and career." });
  if (raw.health < 5) remedies.push({ title: "Surya Namaskar", detail: "Practice 12 rounds of Surya Namaskar at sunrise daily. Offer water to the rising sun (Arghya). This strengthens Prana (life force) and solar vitality." });
  if (raw.mental < 5) remedies.push({ title: "Saraswati Vandana", detail: "Chant the Saraswati Vandana daily before study or creative work. Keep a tulsi plant and water it mindfully. This strengthens Buddhi (intellect) and Vak Shakti (speech)." });
  if (raw.spiritual < 5) remedies.push({ title: "Chandra Meditation", detail: "Meditate facing the moon on Purnima (full moon) nights. Chant 'Om Chandraya Namah' 108 times. This awakens intuition and the connection to your inner Guru." });
  if (raw.wealth < 5) remedies.push({ title: "Lakshmi Puja", detail: "Light a ghee lamp on Fridays and recite Sri Sukta. Keep your wallet and home clean and organized. Offer red lotus or marigold to Lakshmi. This invites prosperity consciousness." });

  // Always include 3 remedies
  if (remedies.length === 0) {
    remedies.push(
      { title: "Daily Pranayama", detail: "Practice Anulom-Vilom (alternate nostril breathing) for 10 minutes daily at dawn. This balances Ida and Pingala nadis — the foundation of all well-being." },
      { title: "Mantra Japa", detail: "Select your Ishta Devata and chant their beej mantra 108 times daily. Consistency over intensity — even 5 minutes daily transforms the subconscious over 40 days." },
    );
  }
  if (remedies.length < 3) {
    remedies.push({ title: "Jyotish Consultation", detail: "For complete guidance, consult a Vedic astrologer (Jyotishi) who combines palm reading with your Kundali chart. The palm shows the karma; the chart shows the timing." });
  }

  // Build summary
  const topLine = selections[0] ? QUESTIONS.find(q => q.id === selections[0].questionId)?.line : "";
  const summary = `Your ${handType.label} (${handType.vedic}) reveals a soul deeply aligned with the ${handType.element} element — ${handType.traits.slice(0, 3).join(", ")}. ${handType.vedic_note} The patterns in your palm speak of a life rich with ${lifeScores.filter(a => a.score >= 7).map(a => a.area.toLowerCase()).join(" and ")} — these are your natural domains. ${challenges.length > 0 ? `The Vedic tradition teaches that challenges are not obstacles but gurukul — the school of the soul. Your karmic work in this lifetime involves ${challenges[0].split(":")[0].toLowerCase()} as a primary area of growth.` : "The clarity and balance in your palm indicate a soul that has done significant work across lifetimes."} Trust the dharmic path that is already written in your hands — and walk it with devotion. As the Bhagavad Gita teaches: your duty is to act with full presence; the fruit belongs to the Divine.`;

  const auspiciousNote = handType.id === "fire"
    ? "The fire in your hand is divine Agni — the same sacred flame that carries offerings to the gods. Your ambition, when channeled through dharma, becomes worship."
    : handType.id === "water"
    ? "The depth of your waters mirrors the depth of your soul. Like the ocean that remains calm at its floor even in storms, your inner stillness is your greatest treasure."
    : handType.id === "air"
    ? "The winds of Vayu carry Prana — the breath of life. Your mind, like the wind, touches everything and is touched by everything. Your words have the power to heal."
    : "The earth you are made of is the same earth that holds the roots of the Bodhi tree. Your groundedness is sacred — it is the foundation upon which temples are built.";

  return { handType, selections, lifeScores, strengths, challenges, remedies, summary, auspiciousNote };
}

// ─────────────────────────────────────────────────────────────────
// SCAN TAB — Wizard
// ─────────────────────────────────────────────────────────────────

function ScoreBar({ score }: { score: number }) {
  const color = score >= 8 ? "#C8860A" : score >= 6 ? "#E8650A" : score >= 4 ? "#7A8BAA" : "#6D28D9";
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-2 rounded-full bg-muted/40 overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${score * 10}%` }}
          transition={{ duration: 0.9, ease: "easeOut" }}
          className="h-full rounded-full"
          style={{ background: color }}
        />
      </div>
      <span className="text-xs font-semibold w-8 text-right" style={{ color }}>{score}/10</span>
    </div>
  );
}

function HandUploader({
  label, role, image, imageName, loading, error, onFile, onReset,
}: {
  label: string; role: string; image: string | null; imageName: string;
  loading: boolean; error: string | null;
  onFile: (f: File) => void; onReset: () => void;
}) {
  const fileRef = useRef<HTMLInputElement>(null);
  const camRef  = useRef<HTMLInputElement>(null);
  const [drag, setDrag] = useState(false);

  const handle = (f: File) => { if (f.type.startsWith("image/")) onFile(f); };

  return (
    <div className="space-y-3">
      <div className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold ${
        role === "dominant"
          ? "bg-amber-100 text-amber-800 border border-amber-200"
          : "bg-purple-50 text-purple-800 border border-purple-200"
      }`}>
        <Hand className="h-3.5 w-3.5" />
        {label}
      </div>

      {!image ? (
        <>
          <div
            onDrop={e => { e.preventDefault(); setDrag(false); const f = e.dataTransfer.files[0]; if (f) handle(f); }}
            onDragOver={e => { e.preventDefault(); setDrag(true); }}
            onDragLeave={() => setDrag(false)}
            onClick={() => fileRef.current?.click()}
            className={`border-2 border-dashed rounded-xl p-8 flex flex-col items-center gap-3 cursor-pointer transition-all ${
              drag ? "border-primary/60 bg-primary/5" : "border-border/30 hover:border-primary/30 hover:bg-muted/10"
            }`}
          >
            <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
              <Upload className="h-5 w-5 text-primary" />
            </div>
            <p className="text-xs text-muted-foreground text-center">Drop image or click to upload</p>
            <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={e => { const f = e.target.files?.[0]; if (f) handle(f); }} />
          </div>
          <button onClick={() => camRef.current?.click()}
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl border border-border/30 bg-muted/10 hover:bg-muted/20 transition-all text-sm">
            <Camera className="h-4 w-4 text-primary" /> Use Camera
          </button>
          <input ref={camRef} type="file" accept="image/*" capture="environment" className="hidden" onChange={e => { const f = e.target.files?.[0]; if (f) handle(f); }} />
        </>
      ) : (
        <div className="relative rounded-xl overflow-hidden border border-border/30">
          <img src={image} alt="Palm" className="w-full object-contain max-h-48" />
          <button onClick={onReset} className="absolute top-2 right-2 h-7 w-7 rounded-full bg-background/80 border border-border/50 flex items-center justify-center">
            <X className="h-3.5 w-3.5" />
          </button>
          <div className={`absolute bottom-2 left-2 text-[10px] px-2 py-0.5 rounded-full font-medium ${
            role === "dominant" ? "bg-amber-600 text-white" : "bg-purple-600 text-white"
          }`}>
            {imageName.slice(0, 20)}
          </div>
        </div>
      )}

      {loading && (
        <div className="flex items-center gap-2 text-xs text-muted-foreground animate-pulse">
          <span className="animate-spin">✦</span> Scanning palm lines…
        </div>
      )}
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}

function ScanTab() {
  type Step = "choose_hand" | "scan_dominant" | "scan_non_dominant" | "report";

  const [step, setStep]                         = useState<Step>("choose_hand");
  const [dominantHand, setDominantHand]         = useState<"right" | "left">("right");

  // Dominant hand
  const [domImage, setDomImage]                 = useState<string | null>(null);
  const [domImageName, setDomImageName]         = useState("");
  const [domFile, setDomFile]                   = useState<File | null>(null);
  const [domLoading, setDomLoading]             = useState(false);
  const [domError, setDomError]                 = useState<string | null>(null);
  const [domResult, setDomResult]               = useState<any>(null);

  // Non-dominant hand
  const [nonDomImage, setNonDomImage]           = useState<string | null>(null);
  const [nonDomImageName, setNonDomImageName]   = useState("");
  const [nonDomFile, setNonDomFile]             = useState<File | null>(null);
  const [nonDomLoading, setNonDomLoading]       = useState(false);
  const [nonDomError, setNonDomError]           = useState<string | null>(null);
  const [nonDomResult, setNonDomResult]         = useState<any>(null);

  // Combined
  const [combLoading, setCombLoading]           = useState(false);
  const [combError, setCombError]               = useState<string | null>(null);
  const [combinedResult, setCombinedResult]     = useState<any>(null);

  const nonDominantHand = dominantHand === "right" ? "left" : "right";

  const fileToDataUrl = (f: File) => new Promise<string>((res, rej) => {
    const r = new FileReader();
    r.onload = e => res(e.target?.result as string);
    r.onerror = rej;
    r.readAsDataURL(f);
  });

  const handleDomFile = async (f: File) => {
    setDomFile(f);
    setDomImage(await fileToDataUrl(f));
    setDomImageName(f.name);
    setDomError(null);
  };

  const handleNonDomFile = async (f: File) => {
    setNonDomFile(f);
    setNonDomImage(await fileToDataUrl(f));
    setNonDomImageName(f.name);
    setNonDomError(null);
  };

  const analyzeDominant = async () => {
    if (!domFile) return;
    setDomLoading(true);
    setDomError(null);
    try {
      const fd = new FormData();
      fd.append("file", domFile);
      fd.append("hand_role", "dominant");
      const res = await fetch("/api/palmistry/analyze", { method: "POST", body: fd });
      if (!res.ok) throw new Error((await res.json().catch(() => ({}))).detail || "Analysis failed");
      setDomResult(await res.json());
      setStep("scan_non_dominant");
    } catch (e: any) {
      setDomError(e.message || "Analysis failed. Please try again.");
    } finally {
      setDomLoading(false);
    }
  };

  const analyzeNonDominant = async () => {
    if (!nonDomFile) return;
    setNonDomLoading(true);
    setNonDomError(null);
    try {
      const fd = new FormData();
      fd.append("file", nonDomFile);
      fd.append("hand_role", "non_dominant");
      const res = await fetch("/api/palmistry/analyze", { method: "POST", body: fd });
      if (!res.ok) throw new Error((await res.json().catch(() => ({}))).detail || "Analysis failed");
      const ndResult = await res.json();
      setNonDomResult(ndResult);
      // Now get combined report
      await generateCombined(ndResult);
    } catch (e: any) {
      setNonDomError(e.message || "Analysis failed. Please try again.");
    } finally {
      setNonDomLoading(false);
    }
  };

  const generateCombined = async (ndResult: any) => {
    if (!domFile || !nonDomFile) return;
    setCombLoading(true);
    setCombError(null);
    try {
      const fd = new FormData();
      fd.append("dominant", domFile);
      fd.append("non_dominant", nonDomFile);
      fd.append("dominant_hand", dominantHand);
      const res = await fetch("/api/palmistry/analyze-both", { method: "POST", body: fd });
      if (!res.ok) throw new Error("Combined analysis failed");
      const data = await res.json();
      setCombinedResult(data);
    } catch {
      setCombError("Could not generate combined report. Individual readings are still available.");
    } finally {
      setCombLoading(false);
      setStep("report");
    }
  };

  const reset = () => {
    setStep("choose_hand");
    setDomImage(null); setDomFile(null); setDomImageName(""); setDomResult(null); setDomError(null);
    setNonDomImage(null); setNonDomFile(null); setNonDomImageName(""); setNonDomResult(null); setNonDomError(null);
    setCombinedResult(null); setCombError(null);
  };

  // ── Step 1: Choose dominant hand ──
  if (step === "choose_hand") {
    return (
      <div className="max-w-lg mx-auto space-y-6 pt-4">
        <div className="text-center space-y-2">
          <div className="flex justify-center">
            <div className="h-16 w-16 rounded-2xl bg-amber-50 border border-amber-200 flex items-center justify-center">
              <Hand className="h-8 w-8 text-amber-600" />
            </div>
          </div>
          <h2 className="font-display text-xl text-foreground">Dual Hand Reading</h2>
          <p className="text-sm text-muted-foreground max-w-sm mx-auto">
            Vedic palmistry reads both hands together — the non-dominant hand reveals your soul’s past karma and innate gifts, while the dominant hand shows what you are actively creating in this life.
          </p>
        </div>

        <div className="space-y-3">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide text-center">Which is your dominant (writing) hand?</p>
          <div className="grid grid-cols-2 gap-3">
            {(["right", "left"] as const).map(h => (
              <button
                key={h}
                onClick={() => setDominantHand(h)}
                className={`p-4 rounded-xl border-2 transition-all text-center ${
                  dominantHand === h
                    ? "border-primary bg-primary/10"
                    : "border-border/30 hover:border-primary/30"
                }`}
              >
                <div className="text-3xl mb-2">{h === "right" ? "🤚" : "🤚"}</div>
                <p className="text-sm font-semibold capitalize">{h} Hand</p>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {h === "right" ? "Most common" : "Left-handed"}
                </p>
                {dominantHand === h && <CheckCircle2 className="h-4 w-4 text-primary mx-auto mt-2" />}
              </button>
            ))}
          </div>
        </div>

        <div className="rounded-xl border border-amber-200/60 bg-amber-50/50 p-4 space-y-2">
          <p className="text-xs font-semibold text-amber-800 flex items-center gap-1.5"><Info className="h-3.5 w-3.5" /> Vedic Palmistry — Two Hands</p>
          <div className="space-y-1.5">
            <div className="flex items-start gap-2">
              <span className="text-xs font-medium text-amber-700 min-w-[110px]">Non-Dominant Hand</span>
              <span className="text-xs text-amber-900">Past karma, soul gifts, innate potential (Prarabdha)</span>
            </div>
            <div className="flex items-start gap-2">
              <span className="text-xs font-medium text-amber-700 min-w-[110px]">Dominant Hand</span>
              <span className="text-xs text-amber-900">Present karma, current path, conscious choices (Kriyamana)</span>
            </div>
          </div>
        </div>

        <Button onClick={() => setStep("scan_dominant")} className="w-full h-11 font-semibold">
          Begin Dual Scan <ArrowRight className="h-4 w-4 ml-2" />
        </Button>
      </div>
    );
  }

  // ── Step 2: Scan Dominant Hand ──
  if (step === "scan_dominant") {
    return (
      <div className="max-w-lg mx-auto space-y-5">
        {/* Progress */}
        <div className="flex items-center gap-3">
          <button onClick={() => setStep("choose_hand")} className="text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div className="flex-1 h-1.5 rounded-full bg-muted/30 overflow-hidden">
            <div className="h-full bg-primary rounded-full w-1/3 transition-all" />
          </div>
          <span className="text-xs text-muted-foreground">Step 1 of 2</span>
        </div>

        <div className="space-y-1">
          <h2 className="font-display text-lg text-foreground">Scan Your Dominant Hand</h2>
          <p className="text-sm text-muted-foreground">Your <span className="font-medium text-amber-700 capitalize">{dominantHand}</span> hand — present karma & current life path (Kriyamana)</p>
        </div>

        <HandUploader
          label={`Dominant Hand (${dominantHand.charAt(0).toUpperCase() + dominantHand.slice(1)}) — Present Karma`}
          role="dominant"
          image={domImage}
          imageName={domImageName}
          loading={domLoading}
          error={domError}
          onFile={handleDomFile}
          onReset={() => { setDomImage(null); setDomFile(null); setDomImageName(""); }}
        />

        <div className="rounded-xl border border-border/30 bg-muted/5 p-3 space-y-1.5">
          <p className="text-xs font-semibold text-foreground">📸 Photo Tips</p>
          {["Place hand flat on white surface", "Good natural light, no shadows", "Camera directly above, all lines visible", "All 5 fingers in frame"].map(tip => (
            <p key={tip} className="text-xs text-muted-foreground flex items-center gap-1.5"><span className="text-primary">·</span>{tip}</p>
          ))}
        </div>

        <Button onClick={analyzeDominant} disabled={!domImage || domLoading} className="w-full h-11 font-semibold bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-500 hover:to-orange-500 text-white border-0">
          {domLoading ? <><span className="animate-spin mr-2">❖</span> AI Scanning Dominant Hand…</> : <><Sparkles className="h-4 w-4 mr-2" /> Scan Dominant Hand</>}
        </Button>
      </div>
    );
  }

  // ── Step 3: Scan Non-Dominant Hand ──
  if (step === "scan_non_dominant") {
    return (
      <div className="max-w-lg mx-auto space-y-5">
        {/* Progress */}
        <div className="flex items-center gap-3">
          <button onClick={() => setStep("scan_dominant")} className="text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div className="flex-1 h-1.5 rounded-full bg-muted/30 overflow-hidden">
            <div className="h-full bg-primary rounded-full w-2/3 transition-all" />
          </div>
          <span className="text-xs text-muted-foreground">Step 2 of 2</span>
        </div>

        {/* Dominant done badge */}
        <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-green-50 border border-green-200 text-xs text-green-800">
          <CheckCircle2 className="h-3.5 w-3.5 text-green-600 shrink-0" />
          <span>Dominant hand ({dominantHand}) scanned ✓ — now scan your {nonDominantHand} hand</span>
        </div>

        <div className="space-y-1">
          <h2 className="font-display text-lg text-foreground">Scan Your Non-Dominant Hand</h2>
          <p className="text-sm text-muted-foreground">Your <span className="font-medium text-purple-700 capitalize">{nonDominantHand}</span> hand — past karma & soul gifts (Prarabdha)</p>
        </div>

        <HandUploader
          label={`Non-Dominant Hand (${nonDominantHand.charAt(0).toUpperCase() + nonDominantHand.slice(1)}) — Past Karma`}
          role="non_dominant"
          image={nonDomImage}
          imageName={nonDomImageName}
          loading={nonDomLoading || combLoading}
          error={nonDomError || combError}
          onFile={handleNonDomFile}
          onReset={() => { setNonDomImage(null); setNonDomFile(null); setNonDomImageName(""); }}
        />

        {combLoading && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground animate-pulse px-1">
            <span className="animate-spin">❖</span> Generating combined Vedic report…
          </div>
        )}

        <Button onClick={analyzeNonDominant} disabled={!nonDomImage || nonDomLoading || combLoading} className="w-full h-11 font-semibold bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white border-0">
          {(nonDomLoading || combLoading) ? <><span className="animate-spin mr-2">❖</span> Scanning & Generating Report…</> : <><Sparkles className="h-4 w-4 mr-2" /> Scan & Generate Combined Report</>}
        </Button>
      </div>
    );
  }

  // ── Step 4: Combined Report ──
  if (step === "report") {
    const combined = combinedResult?.combined;
    const dom      = combinedResult?.dominant   || domResult;
    const nonDom   = combinedResult?.non_dominant || nonDomResult;

    return (
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-5 max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-display text-xl text-glow-gold">Dual Palm Reading Complete</h2>
            <p className="text-xs text-muted-foreground">Dominant: <span className="capitalize font-medium text-amber-700">{dominantHand}</span> · Non-Dominant: <span className="capitalize font-medium text-purple-700">{nonDominantHand}</span></p>
          </div>
          <Button variant="outline" size="sm" onClick={reset} className="border-border/30 text-xs">
            <RotateCcw className="h-3.5 w-3.5 mr-1.5" /> New Reading
          </Button>
        </div>

        {/* Combined Synthesis */}
        {combined && (
          <Card className="cosmic-card border-amber-200/60 bg-gradient-to-br from-amber-50/30 to-orange-50/20">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-amber-600" />
                Combined Vedic Synthesis
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-sm leading-relaxed text-foreground">{combined.synthesis}</p>

              {combined.karmic_gap && (
                <div className="rounded-xl border-l-4 border-amber-500 bg-amber-50/60 px-4 py-3">
                  <p className="text-xs font-bold text-amber-700 uppercase tracking-wide mb-1">Karmic Gap — Past vs Present</p>
                  <p className="text-sm text-amber-900 leading-relaxed">{combined.karmic_gap}</p>
                </div>
              )}

              {combined.soul_mission && (
                <div className="rounded-xl bg-purple-50 border border-purple-200 px-4 py-3">
                  <p className="text-xs font-bold text-purple-700 uppercase tracking-wide mb-1">Soul Mission This Lifetime</p>
                  <p className="text-sm text-purple-900 leading-relaxed">{combined.soul_mission}</p>
                </div>
              )}

              {/* Combined Life Areas */}
              {combined.combined_life_areas && (
                <div className="space-y-2.5">
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Life Area Scores — Both Hands</p>
                  {combined.combined_life_areas.map((a: any) => (
                    <div key={a.area} className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-medium text-foreground">{a.area}</span>
                        <div className="flex items-center gap-3 text-muted-foreground">
                          <span className="text-purple-600">Past {a.non_dominant_score}/10</span>
                          <span className="text-amber-600">Now {a.dominant_score}/10</span>
                          <span className="font-bold text-foreground">{a.combined_score}/10</span>
                        </div>
                      </div>
                      <div className="h-1.5 rounded-full bg-muted/30 overflow-hidden flex">
                        <div className="h-full bg-purple-400/60" style={{ width: `${a.non_dominant_score * 10}%` }} />
                        <div className="h-full bg-amber-500" style={{ width: `${Math.max(0, a.dominant_score - a.non_dominant_score) * 10}%` }} />
                      </div>
                      {a.insight && <p className="text-xs text-muted-foreground">{a.insight}</p>}
                    </div>
                  ))}
                </div>
              )}

              {combined.final_message && (
                <div className="rounded-xl bg-muted/20 border border-border/30 px-4 py-3">
                  <p className="text-sm text-foreground leading-relaxed italic">{combined.final_message}</p>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Individual Hand Reports */}
        <div className="grid lg:grid-cols-2 gap-4">
          {/* Dominant */}
          {dom && (
            <Card className="cosmic-card border-amber-200/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded-md bg-amber-100 text-amber-800 text-xs font-bold capitalize">{dominantHand} Hand</span>
                  <span className="text-muted-foreground font-normal">Present Karma</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {dom.overview && <p className="text-xs leading-relaxed text-foreground">{dom.overview}</p>}
                {dom.lines?.slice(0, 4).map((line: any) => (
                  <div key={line.name} className="border-l-2 border-amber-400 pl-3 space-y-0.5">
                    <p className="text-xs font-semibold text-foreground">{line.name} <span className="text-muted-foreground font-normal">({line.sanskrit})</span></p>
                    <p className="text-xs text-muted-foreground leading-relaxed">{line.interpretation}</p>
                    {line.vedic_note && <p className="text-[11px] text-amber-700 italic">{line.vedic_note}</p>}
                  </div>
                ))}
                {combined?.dominant_summary && (
                  <div className="rounded-lg bg-amber-50 border border-amber-200 p-2.5">
                    <p className="text-xs text-amber-900 leading-relaxed">{combined.dominant_summary}</p>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          {/* Non-Dominant */}
          {nonDom && (
            <Card className="cosmic-card border-purple-200/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-800 text-xs font-bold capitalize">{nonDominantHand} Hand</span>
                  <span className="text-muted-foreground font-normal">Past Karma</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {nonDom.overview && <p className="text-xs leading-relaxed text-foreground">{nonDom.overview}</p>}
                {nonDom.lines?.slice(0, 4).map((line: any) => (
                  <div key={line.name} className="border-l-2 border-purple-400 pl-3 space-y-0.5">
                    <p className="text-xs font-semibold text-foreground">{line.name} <span className="text-muted-foreground font-normal">({line.sanskrit})</span></p>
                    <p className="text-xs text-muted-foreground leading-relaxed">{line.interpretation}</p>
                    {line.vedic_note && <p className="text-[11px] text-purple-700 italic">{line.vedic_note}</p>}
                  </div>
                ))}
                {combined?.non_dominant_summary && (
                  <div className="rounded-lg bg-purple-50 border border-purple-200 p-2.5">
                    <p className="text-xs text-purple-900 leading-relaxed">{combined.non_dominant_summary}</p>
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </div>

        {/* Remedies */}
        {combined?.remedies && (
          <Card className="cosmic-card border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm text-muted-foreground">Vedic Remedies</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {combined.remedies.map((r: any, i: number) => (
                <div key={i} className="flex gap-3">
                  <span className="text-primary font-bold text-sm mt-0.5 shrink-0">❖</span>
                  <div>
                    <p className="text-xs font-semibold text-foreground">{r.title}</p>
                    <p className="text-xs text-muted-foreground leading-relaxed mt-0.5">{r.detail}</p>
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>
        )}

        <p className="text-xs text-muted-foreground/50 italic text-center pb-4">
          Analyzed by AI · Hasta Samudrika Shastra interpretation · Both hands read together
        </p>
      </motion.div>
    );
  }

  return null;
}

// ─────────────────────────────────────────────────────────────────
// REFERENCE TABS (condensed)
// ─────────────────────────────────────────────────────────────────

function LinesTab() {
  const { t } = useTranslation();
  const [active, setActive] = useState<string | null>(null);
  return (
    <div className="space-y-3">
      {QUESTIONS.filter(q => !q.id.startsWith("mount")).map((q, i) => (
        <motion.div key={q.id} initial={{ opacity: 0, x: -16 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.06 }}>
          <Card className="cosmic-card border-border/30">
            <button className="w-full text-left" onClick={() => setActive(p => p === q.id ? null : q.id)}>
              <CardHeader className="pb-2">
                <CardTitle className="flex items-center gap-3 text-base">
                  <span className="h-3 w-3 rounded-full flex-shrink-0" style={{ background: q.color }} />
                  {t(q.lineKey, { defaultValue: q.line })}
                  <span className="text-xs font-normal text-muted-foreground">({q.sanskrit})</span>
                  <ChevronRight className={`h-4 w-4 text-muted-foreground ml-auto transition-transform ${active === q.id ? "rotate-90" : ""}`} />
                </CardTitle>
                <p className="text-xs text-muted-foreground ml-6">{t(q.hintKey, { defaultValue: q.hint })}</p>
              </CardHeader>
            </button>
            <AnimatePresence>
              {active === q.id && (
                <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                  <CardContent className="pt-0 space-y-2">
                    <div className="gold-divider" />
                    <div className="grid sm:grid-cols-2 gap-2">
                      {q.options.map(opt => (
                        <div key={opt.id} className="rounded-lg p-3 bg-muted/10 border border-border/20 space-y-1.5">
                          <Badge variant="outline" className="text-xs border-border/30">{t(opt.labelKey, { defaultValue: opt.label })}</Badge>
                          <p className="text-xs text-foreground leading-relaxed">{t(opt.meaningKey, { defaultValue: opt.meaning })}</p>
                          <p className="text-xs italic" style={{ color: q.color }}>✦ {t(opt.vedicKey, { defaultValue: opt.vedic })}</p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </motion.div>
              )}
            </AnimatePresence>
          </Card>
        </motion.div>
      ))}
    </div>
  );
}

function MountsTab() {
  const { t } = useTranslation();
  const MOUNTS = [
    { id: "jupiter", name: "Mount of Jupiter", nameKey: "data.palmistry.mount.jupiter_name", sanskrit: "गुरु पर्वत", finger: "Below Index Finger", planet: "Jupiter (Guru)", icon: "♃︎", color: "#C8860A", well: "Leader, ambitious, spiritually wise, generous, charismatic. Born to guide and teach.", wellKey: "data.palmistry.mount.jupiter_well", flat: "Lack of confidence, avoids leadership roles.", flatKey: "data.palmistry.mount.jupiter_flat", over: "Arrogant, overbearing, domineering.", overKey: "data.palmistry.mount.jupiter_over", vedic: "Guru's seat of Brahma Jnana. Prominent in teachers, judges, and spiritual leaders.", vedicKey: "data.palmistry.mount.jupiter_vedic" },
    { id: "saturn", name: "Mount of Saturn", nameKey: "data.palmistry.mount.saturn_name", sanskrit: "शनि पर्वत", finger: "Below Middle Finger", planet: "Saturn (Shani)", icon: "♄︎", color: "#7A8BAA", well: "Wise, responsible, patient, deeply introspective. Excellent researchers and dedicated servants.", wellKey: "data.palmistry.mount.saturn_well", flat: "Avoids responsibility, lacks discipline.", flatKey: "data.palmistry.mount.saturn_flat", over: "Melancholic, hermit tendencies, can't enjoy life.", overKey: "data.palmistry.mount.saturn_over", vedic: "Shani's seat of karma and dharma. Those who bear great responsibility with grace.", vedicKey: "data.palmistry.mount.saturn_vedic" },
    { id: "apollo", name: "Mount of Apollo", nameKey: "data.palmistry.mount.apollo_name", sanskrit: "सूर्य पर्वत", finger: "Below Ring Finger", planet: "Sun (Surya)", icon: "☉︎", color: "#E8650A", well: "Creative, charismatic, drawn to beauty and art. Fame comes naturally. Joyful and warm-hearted.", wellKey: "data.palmistry.mount.apollo_well", flat: "Lack of aesthetic sense, avoids public life.", flatKey: "data.palmistry.mount.apollo_flat", over: "Vanity, obsession with appearances, craving fame over substance.", overKey: "data.palmistry.mount.apollo_over", vedic: "Surya's seat of Atmic brilliance. Artists, performers, and those with deep creative Shakti.", vedicKey: "data.palmistry.mount.apollo_vedic" },
    { id: "mercury", name: "Mount of Mercury", nameKey: "data.palmistry.mount.mercury_name", sanskrit: "बुध पर्वत", finger: "Below Little Finger", planet: "Mercury (Budha)", icon: "☿︎", color: "#7A8BAA", well: "Brilliant communicator, witty, business-minded, quick-thinking. Natural healer.", wellKey: "data.palmistry.mount.mercury_well", flat: "Communication difficulties, poor business instinct.", flatKey: "data.palmistry.mount.mercury_flat", over: "Cunning, deceptive use of words.", overKey: "data.palmistry.mount.mercury_over", vedic: "Budha's seat of Vak Shakti. Scholars, traders, orators, and diplomats.", vedicKey: "data.palmistry.mount.mercury_vedic" },
    { id: "venus", name: "Mount of Venus", nameKey: "data.palmistry.mount.venus_name", sanskrit: "शुक्र पर्वत", finger: "Base of Thumb", planet: "Venus (Shukra)", icon: "♀︎", color: "#E8650A", well: "Loving, sensual, generous, artistic. Great capacity for pleasure and deep human connection.", wellKey: "data.palmistry.mount.venus_well", flat: "Cold, unaffectionate, may struggle with intimacy.", flatKey: "data.palmistry.mount.venus_flat", over: "Excessive sensuality, overindulgence, attachment.", overKey: "data.palmistry.mount.venus_over", vedic: "Shukra's seat of Kama (desire) — one of the four Purusharthas. The quality of love is here.", vedicKey: "data.palmistry.mount.venus_vedic" },
    { id: "moon", name: "Mount of Moon", nameKey: "data.palmistry.mount.moon_name", sanskrit: "चन्द्र पर्वत", finger: "Outer Base (opposite thumb)", planet: "Moon (Chandra)", icon: "☽︎", color: "#7A8BAA", well: "Deeply intuitive, imaginative, psychic sensitivity, rich inner world. Love of travel and poetry.", wellKey: "data.palmistry.mount.moon_well", flat: "Lack of imagination, emotionally rigid.", flatKey: "data.palmistry.mount.moon_flat", over: "Overly emotional, difficulty distinguishing real from imagined.", overKey: "data.palmistry.mount.moon_over", vedic: "Chandra's seat of Manas and intuition. Astrologers, poets, healers, and psychic seers.", vedicKey: "data.palmistry.mount.moon_vedic" },
    { id: "mars", name: "Mounts of Mars", nameKey: "data.palmistry.mount.mars_name", sanskrit: "मंगल पर्वत", finger: "Upper & Lower Inner Edge", planet: "Mars (Mangal)", icon: "♂︎", color: "#E8650A", well: "Physical and moral courage. Resilience under pressure. Strength when it matters most.", wellKey: "data.palmistry.mount.mars_well", flat: "Cowardice, gives up easily, avoids confrontation.", flatKey: "data.palmistry.mount.mars_flat", over: "Aggressive, hot-tempered, prone to confrontation.", overKey: "data.palmistry.mount.mars_over", vedic: "Mangal's Agni Shakti — the fire of righteous action. Warriors, surgeons, athletes.", vedicKey: "data.palmistry.mount.mars_vedic" },
  ];
  const [active, setActive] = useState<string | null>(null);
  return (
    <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {MOUNTS.map((m, i) => (
        <motion.div key={m.id} initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: i * 0.05 }}>
          <button className="w-full text-left" onClick={() => setActive(p => p === m.id ? null : m.id)}>
            <Card className={`cosmic-card border h-full transition-all ${active === m.id ? "border-primary/40" : "border-border/30 hover:border-border/50"}`}>
              <CardContent className="pt-4 space-y-2.5">
                <div className="flex items-center gap-2.5">
                  <span className="text-3xl zodiac-glow" style={{ color: m.color }}>{m.icon}</span>
                  <div>
                    <p className="text-sm font-semibold">{t(m.nameKey, { defaultValue: m.name })}</p>
                    <p className="text-xs text-muted-foreground">{m.sanskrit} · {t(`palmistry.finger_${m.id}`, { defaultValue: m.finger })}</p>
                  </div>
                </div>
                <p className="text-xs text-muted-foreground line-clamp-2">{t(m.wellKey, { defaultValue: m.well })}</p>
                <AnimatePresence>
                  {active === m.id && (
                    <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                      <div className="gold-divider mb-2" />
                      <div className="space-y-2">
                        <div><p className="text-xs font-medium text-primary mb-0.5">{t("palmistry.mount_well")}</p><p className="text-xs text-muted-foreground">{t(m.wellKey, { defaultValue: m.well })}</p></div>
                        <div><p className="text-xs font-medium text-muted-foreground mb-0.5">{t("palmistry.mount_flat")}</p><p className="text-xs text-muted-foreground">{t(m.flatKey, { defaultValue: m.flat })}</p></div>
                        <div><p className="text-xs font-medium text-destructive/80 mb-0.5">{t("palmistry.mount_over")}</p><p className="text-xs text-muted-foreground">{t(m.overKey, { defaultValue: m.over })}</p></div>
                        <div className="rounded-md p-2 bg-muted/20 border border-border/15"><p className="text-xs italic text-muted-foreground">{t(m.vedicKey, { defaultValue: m.vedic })}</p></div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
                <div className="flex items-center gap-1 text-primary/60 text-xs">
                  <ChevronRight className={`h-3 w-3 transition-transform ${active === m.id ? "rotate-90" : ""}`} />
                  <span>{active === m.id ? t("palmistry.collapse") : t("palmistry.full_reading")}</span>
                </div>
              </CardContent>
            </Card>
          </button>
        </motion.div>
      ))}
    </div>
  );
}

function HandTypeTab() {
  const { t } = useTranslation();
  return (
    <div className="grid sm:grid-cols-2 gap-5">
      {HAND_TYPES.map((ht, i) => (
        <motion.div key={ht.id} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.08 }}>
          <Card className="cosmic-card border-border/30 h-full">
            <CardHeader className="pb-3">
              <div className="flex items-start gap-3">
                <span className="text-4xl">{ht.icon}</span>
                <div><CardTitle className="text-lg">{t(ht.labelKey, { defaultValue: ht.label })}</CardTitle><p className="text-xs text-muted-foreground">{t(ht.vedicKey, { defaultValue: ht.vedic })} · {t(ht.shapeKey, { defaultValue: ht.shape })}</p></div>
                <Badge variant="outline" className="ml-auto border-primary/30 text-primary text-xs">{t(ht.elementKey, { defaultValue: ht.element })}</Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-x-4 gap-y-1.5">
                {ht.traits.map((trait, ti) => (
                  <div key={trait} className="flex items-start gap-1.5 text-xs text-muted-foreground">
                    <span className="text-primary mt-0.5 flex-shrink-0">◆</span><span>{t(ht.traitKeys[ti], { defaultValue: trait })}</span>
                  </div>
                ))}
              </div>
              <div className="gold-divider" />
              <div className="space-y-1.5 text-xs">
                <div><span className="text-primary font-medium">{t("palmistry.careers_label", { defaultValue: "Careers" })}: </span><span className="text-muted-foreground">{t(ht.careersKey, { defaultValue: ht.careers })}</span></div>
                <div><span className="text-primary font-medium">{t("palmistry.constitution_label", { defaultValue: "Constitution" })}: </span><span className="text-muted-foreground">{t(ht.constitutionKey, { defaultValue: ht.constitution })}</span></div>
                <div><span className="text-secondary font-medium">{t("palmistry.shadow_label", { defaultValue: "Shadow" })}: </span><span className="text-muted-foreground">{t(ht.shadowKey, { defaultValue: ht.shadow })}</span></div>
              </div>
              <div className="rounded-lg p-3 bg-muted/20 border border-border/20">
                <p className="text-xs text-muted-foreground italic leading-relaxed">{t(ht.vedic_noteKey, { defaultValue: ht.vedic_note })}</p>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      ))}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────
// MAIN PAGE
// ─────────────────────────────────────────────────────────────────

export default function PalmistryPage() {
  const { t } = useTranslation();
  useRegisterPageBot('palmistry', {});
  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center gap-3 mb-1">
          <Hand className="h-7 w-7 text-primary zodiac-glow" />
          <h1 className="font-display text-3xl text-glow-gold">{t("palmistry.title")}</h1>
        </div>
        <p className="text-muted-foreground text-sm ml-10">
          {t("palmistry.subtitle")}
        </p>
      </motion.div>

      <Tabs defaultValue="scan" className="w-full">
        <TabsList className="glass grid grid-cols-4 w-full">
          <TabsTrigger value="scan" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <Scan className="h-3.5 w-3.5" /> <span>{t("palmistry.tab_scan_palm")}</span>
          </TabsTrigger>
          <TabsTrigger value="lines" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <Hand className="h-3.5 w-3.5" /> <span>{t("palmistry.tab_lines")}</span>
          </TabsTrigger>
          <TabsTrigger value="mounts" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <Star className="h-3.5 w-3.5" /> <span>{t("palmistry.tab_mounts")}</span>
          </TabsTrigger>
          <TabsTrigger value="hand" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <BookOpen className="h-3.5 w-3.5" /> <span>{t("palmistry.tab_hand_type")}</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="scan" className="mt-4"><ScanTab /></TabsContent>
        <TabsContent value="lines" className="mt-4"><LinesTab /></TabsContent>
        <TabsContent value="mounts" className="mt-4"><MountsTab /></TabsContent>
        <TabsContent value="hand" className="mt-4"><HandTypeTab /></TabsContent>
      </Tabs>
      <PageBot pageContext="palmistry" pageData={{}} />
    </div>
  );
}
