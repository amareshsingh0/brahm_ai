import { useState, useRef, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import PageBot from '@/components/PageBot';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
    id: "earth", label: "Earth Hand", vedic: "Prithvi Hasta", shape: "Square palm + Short fingers",
    element: "Earth (Prithvi)", icon: "🌍",
    traits: ["Practical & reliable", "Hardworking", "Strong physical constitution", "Prefers routine", "Hands-on learner"],
    careers: "Builder, surgeon, chef, craftsman, athlete, engineer",
    constitution: "Kapha (Ayurveda) — stable, grounding, enduring",
    shadow: "Stubbornness, resistance to change, overattachment to the material",
    vedic_note: "Bhu-Tattva dominant. Deeply connected to the material plane. The karmic path involves mastery through physical effort and grounded wisdom.",
    life_modifier: { health: 2, wealth: 1, love: 0, mental: -1, spiritual: -1, career: 2 },
  },
  {
    id: "air", label: "Air Hand", vedic: "Vayu Hasta", shape: "Square palm + Long fingers",
    element: "Air (Vayu)", icon: "💨",
    traits: ["Intellectual", "Curious & versatile", "Excellent communicator", "Social", "Ideas-driven"],
    careers: "Writer, journalist, teacher, scientist, lawyer, philosopher",
    constitution: "Vata (Ayurveda) — swift, mercurial, adaptive",
    shadow: "Anxiety, restlessness, difficulty grounding ideas into reality",
    vedic_note: "Vayu-Tattva dominant. Ruled by Budha (Mercury). The karmic path involves using the gift of communication to serve truth and uplift others.",
    life_modifier: { health: -1, wealth: 1, love: 1, mental: 2, spiritual: 1, career: 1 },
  },
  {
    id: "water", label: "Water Hand", vedic: "Jala Hasta", shape: "Long oval palm + Long fine fingers",
    element: "Water (Jala)", icon: "💧",
    traits: ["Deeply intuitive", "Empathetic", "Creative & artistic", "Psychically sensitive", "Absorbs others' energies"],
    careers: "Healer, astrologer, artist, psychologist, poet, musician",
    constitution: "Kapha-Pitta (Ayurveda) — fluid, receptive, emotionally rich",
    shadow: "Over-sensitivity, poor boundaries, escapism, emotional overwhelm",
    vedic_note: "Jala-Tattva dominant. Ruled by Chandra (Moon). The karmic path involves learning to channel deep sensitivity into healing and creative gifts without losing self.",
    life_modifier: { health: 0, wealth: -1, love: 2, mental: 1, spiritual: 2, career: 0 },
  },
  {
    id: "fire", label: "Fire Hand", vedic: "Agni Hasta", shape: "Long palm + Short fingers",
    element: "Fire (Agni)", icon: "🔥",
    traits: ["Passionate & energetic", "Natural leader", "Charismatic & inspiring", "Ambitious", "Impatient"],
    careers: "Entrepreneur, military leader, performer, athlete, politician, pioneer",
    constitution: "Pitta (Ayurveda) — radiant, forceful, transforming",
    shadow: "Arrogance, burnout, aggression, inability to rest or surrender",
    vedic_note: "Agni-Tattva dominant. Ruled by Surya + Mangal. The karmic path involves tempering the fire of ambition with humility and compassion, so the flame uplifts rather than burns.",
    life_modifier: { health: 1, wealth: 2, love: 0, mental: 0, spiritual: -1, career: 2 },
  },
];

const QUESTIONS = [
  {
    id: "heart",
    line: "Heart Line",
    sanskrit: "हृदय रेखा",
    instruction: "Look at the TOPMOST horizontal line running across your upper palm.",
    hint: "It typically runs from below your little finger toward your index or middle finger.",
    color: "#E8650A",
    options: [
      {
        id: "long_curved", label: "Long & Deeply Curved",
        desc: "Sweeps up toward the index/middle finger in a strong arc",
        meaning: "Warm, expressive, deeply romantic. You love unconditionally and wear your heart on your sleeve. Relationships are central to your life purpose.",
        vedic: "Strong Shukra (Venus) influence — blessings in love, partnership, and devotion. The heart is your greatest teacher.",
        scores: { love: 3, spiritual: 1, mental: 0, health: 1, wealth: 0, career: 0 },
      },
      {
        id: "straight_short", label: "Straight & Short",
        desc: "Relatively straight, doesn't reach as far across the palm",
        meaning: "Practical and measured in emotion. You love deeply but express it through loyal actions rather than grand gestures. Reliability is your love language.",
        vedic: "Balanced Chandra (Moon) — emotional intelligence without overwhelm. Disciplined heart.",
        scores: { love: 1, spiritual: 0, mental: 1, health: 0, wealth: 1, career: 1 },
      },
      {
        id: "forked", label: "Forked at End",
        desc: "The line splits into two branches near the end",
        meaning: "Rare and auspicious. You balance heart and mind beautifully — empathetic yet rational. Natural counselor or mediator. You see both sides of every relationship.",
        vedic: "Budha-Chandra yoga — harmonious mind-heart communication. The fork indicates the Viveka (discernment) to choose wisely in love.",
        scores: { love: 2, spiritual: 1, mental: 2, health: 0, wealth: 0, career: 1 },
      },
      {
        id: "high_index", label: "Ends Below Index Finger",
        desc: "The line curves up and ends near the base of your index finger",
        meaning: "Idealistic in love. You seek nothing less than a divine, soulmate-level connection. High standards — and you deserve them. Can feel perpetually unsatisfied until you find true union.",
        vedic: "Guru (Jupiter) sub-influence on the heart — seeks Prema (divine love). Marriage as spiritual sadhana.",
        scores: { love: 2, spiritual: 2, mental: 0, health: 0, wealth: 0, career: 0 },
      },
      {
        id: "broken", label: "Broken or Chained",
        desc: "Has gaps, islands, or chain-like appearance",
        meaning: "Significant emotional journey. Past heartbreaks have shaped you deeply. These scars are also sources of immense compassion and wisdom. Growth through vulnerability.",
        vedic: "Rahu/Ketu axis — karmic relationship lessons carried across lifetimes. The break is a portal to transformation.",
        scores: { love: 0, spiritual: 2, mental: -1, health: -1, wealth: 0, career: 0 },
      },
    ],
  },
  {
    id: "head",
    line: "Head Line",
    sanskrit: "मस्तिष्क रेखा",
    instruction: "Find the MIDDLE horizontal line running across your palm.",
    hint: "It usually starts near the edge between your thumb and index finger, running across toward the outer palm.",
    color: "#C8860A",
    options: [
      {
        id: "long_straight", label: "Long & Straight",
        desc: "Extends nearly across the full width of the palm in a straight line",
        meaning: "Razor-sharp analytical mind. You excel at logic, mathematics, science, law, and strategic thinking. You see patterns others miss. Your intellect is your greatest asset.",
        vedic: "Strong Budha (Mercury) — exceptional intellectual capacity. Saraswati's grace on the mind.",
        scores: { mental: 3, career: 2, wealth: 1, love: -1, spiritual: 0, health: 0 },
      },
      {
        id: "curved_down", label: "Deeply Curved Downward",
        desc: "Curves noticeably downward toward the base of the palm",
        meaning: "Highly creative and imaginative. You think in images, metaphors, and stories. Writer, artist, visionary — the world of ideas is your natural home. Intuition over logic.",
        vedic: "Chandra dominance on Buddhi — imaginative intelligence. Saraswati as Vak Shakti (power of creative expression).",
        scores: { mental: 2, career: 1, love: 1, spiritual: 2, health: 0, wealth: -1 },
      },
      {
        id: "fork_end", label: "Forked at End (Writer's Fork)",
        desc: "Splits into two branches — one straight, one curved — near the end",
        meaning: "The most auspicious head line. You hold both analytical and creative intelligence simultaneously. Exceptional in communication, law, literature, philosophy, and teaching.",
        vedic: "Saraswati blessing — dual vision, the gift of eloquence. Can translate complex ideas into beautiful language.",
        scores: { mental: 3, career: 2, love: 1, spiritual: 1, health: 0, wealth: 1 },
      },
      {
        id: "short", label: "Short",
        desc: "Doesn't extend very far across the palm",
        meaning: "Decisive and action-oriented. You trust your instincts over lengthy analysis. Quick thinking, entrepreneurial spirit. Sometimes seen as impulsive — but often you're simply operating on a different level of knowing.",
        vedic: "Mangal (Mars) influence on intellect — courage of thought. The instinct IS the intelligence.",
        scores: { mental: 1, career: 2, love: 0, spiritual: 0, health: 1, wealth: 1 },
      },
      {
        id: "chained", label: "Wavy or Chained",
        desc: "Appears uneven, wavy, or has chain-like links along it",
        meaning: "Sensitive, creative, and deeply perceptive. Your mind picks up subtle energies others miss. This comes with mental sensitivity — meditation and regular mental rest are essential.",
        vedic: "Ketu influence on Manas — a mind that perceives across dimensions. Highly psychic but needs grounding.",
        scores: { mental: 1, career: 0, love: 1, spiritual: 3, health: -1, wealth: -1 },
      },
    ],
  },
  {
    id: "life",
    line: "Life Line",
    sanskrit: "जीवन रेखा",
    instruction: "Find the curved line that sweeps around the base of your thumb.",
    hint: "It typically starts between your index finger and thumb, curving down around the thumb mount (Venus mount).",
    color: "#6D28D9",
    options: [
      {
        id: "wide_deep", label: "Wide Arc & Deep",
        desc: "Makes a large, sweeping arc around the thumb — goes far into the palm",
        meaning: "Abundant Prana (life force). You have robust vitality, strong physical resilience, and a genuine love of life. Physical activity comes naturally. You recover quickly from setbacks.",
        vedic: "Surya-Mangal blessings — solar vitality and warrior constitution. Strong Ojas (immunity). Dhanvantari's grace.",
        scores: { health: 3, career: 2, love: 1, mental: 0, spiritual: 0, wealth: 1 },
      },
      {
        id: "long_clear", label: "Long & Unbroken",
        desc: "Extends far down the palm with no breaks or islands",
        meaning: "Steady, sustained vitality throughout life. Few major health crises. Your energy is reliable and consistent. Others draw strength from your presence.",
        vedic: "Dhanvantari grace — blessed health karma from prior lifetimes of righteous living. Prana is protected.",
        scores: { health: 3, career: 1, love: 0, mental: 1, spiritual: 0, wealth: 0 },
      },
      {
        id: "double", label: "Double Life Line",
        desc: "A parallel line runs alongside the main life line",
        meaning: "Extraordinary — this is the Kavach Rekha (protective mark). You carry divine protection. Strong spiritual guidance from ancestors or Ishta Devata. Remarkable resilience and recovery from any situation.",
        vedic: "Pitru Kavach and Ishta Devata's active blessing. One of the most auspicious marks in all of Samudrika Shastra.",
        scores: { health: 3, career: 1, love: 1, mental: 1, spiritual: 3, wealth: 1 },
      },
      {
        id: "close_thumb", label: "Close to Thumb (Tight Arc)",
        desc: "Stays close to the thumb, doesn't sweep far out",
        meaning: "More introverted energy. You conserve and channel your vitality with precision rather than broadcasting it widely. Quality over quantity in all things — relationships, work, energy expenditure.",
        vedic: "Shani influence on Prana — teaches conservation and mastery. The monk archetype — disciplined and focused.",
        scores: { health: 1, career: 0, love: -1, mental: 2, spiritual: 2, wealth: 0 },
      },
      {
        id: "broken", label: "Broken or Has Islands",
        desc: "Has visible gaps, breaks, or island shapes along it",
        meaning: "Life has involved (or will involve) major transformations. A break is not an ending — it is a rebirth. Each transition you navigate builds extraordinary wisdom. You are a survivor and a transformer.",
        vedic: "Rahu transit marker — radical life pivots that serve divine redirection. The phoenix pattern.",
        scores: { health: -1, career: -1, love: 0, mental: 0, spiritual: 2, wealth: -1 },
      },
    ],
  },
  {
    id: "fate",
    line: "Fate Line",
    sanskrit: "भाग्य रेखा",
    instruction: "Look for a vertical line running up the CENTER of your palm.",
    hint: "It typically runs from the base of your palm upward toward your middle (Saturn) finger. Some palms don't have this line — that's perfectly fine.",
    color: "#C8860A",
    options: [
      {
        id: "deep_clear", label: "Deep & Clear from Wrist",
        desc: "A strong, clear vertical line running from near the wrist upward",
        meaning: "Powerful dharmic purpose. Your life path is clear and purposeful from an early age. Career feels like a calling. Success comes through disciplined, long-term commitment to your path.",
        vedic: "Shani yoga strong — disciplined effort is magnificently rewarded. Prarabdha karma is guiding you firmly toward a specific destiny.",
        scores: { career: 3, wealth: 2, mental: 1, health: 0, love: 0, spiritual: 1 },
      },
      {
        id: "starts_late", label: "Starts in Middle of Palm",
        desc: "The fate line only begins halfway up the palm, not from the base",
        meaning: "A late bloomer — and that's your superpower. Life clarity and career success come after age 30-35. You're building foundations others don't see. Your greatest achievements are still ahead.",
        vedic: "Shani dasha activation — Saturn's gifts come after his tests. The deepest roots take longest to grow.",
        scores: { career: 2, wealth: 1, mental: 1, health: 0, love: 0, spiritual: 1 },
      },
      {
        id: "broken_shifting", label: "Broken or Shifts Direction",
        desc: "Has breaks, or noticeably changes direction midway",
        meaning: "Multiple life chapters and career pivots. Each phase is complete in itself. You're not inconsistent — you're evolving. Each break represents a conscious or divinely-led reinvention.",
        vedic: "Rahu-Shani interaction — disruption precedes breakthrough. The snake sheds its skin.",
        scores: { career: 1, wealth: 0, mental: 0, health: 0, love: 1, spiritual: 2 },
      },
      {
        id: "absent", label: "Not Visible / Absent",
        desc: "No clear vertical line in the center of the palm",
        meaning: "Complete free will. You are not bound by a predetermined destiny — you are the author of your own story in the most literal sense. Self-made in every way. Some of the most successful free spirits have no fate line.",
        vedic: "Moksha marker — free from karma's direct grip. Pure agency. A life of Kriyamana karma (self-created karma).",
        scores: { career: 1, wealth: 1, mental: 0, health: 0, love: 0, spiritual: 2 },
      },
      {
        id: "double", label: "Double Fate Line",
        desc: "Two parallel vertical lines running up the palm",
        meaning: "Extraordinarily rare and auspicious. You are gifted with dual dharma — two parallel callings that you pursue simultaneously. A blessed multi-dimensional life.",
        vedic: "Double Shani Rekha — blessed with dual purpose. The ancient texts describe this as a rare mark of those who serve two roles in society.",
        scores: { career: 3, wealth: 2, mental: 1, health: 0, love: 1, spiritual: 1 },
      },
    ],
  },
  {
    id: "sun",
    line: "Sun Line",
    sanskrit: "सूर्य रेखा",
    instruction: "Look for a vertical line near your RING finger.",
    hint: "It runs upward toward your ring (Apollo) finger, parallel to the fate line. Many people have a faint version of this.",
    color: "#E8650A",
    options: [
      {
        id: "strong_clear", label: "Strong & Clear",
        desc: "A noticeable vertical line clearly approaching the ring finger",
        meaning: "Fame, recognition, and creative success are yours. You have a natural public presence — people notice you. Your creative or professional achievements will be recognized widely. Possible public career.",
        vedic: "Surya strong — solar radiance, fame as the result of dharmic action in past lives. Kirti (fame) is your birthright.",
        scores: { career: 2, wealth: 1, love: 1, mental: 0, health: 0, spiritual: 1 },
      },
      {
        id: "multiple_short", label: "Multiple Short Lines",
        desc: "Several small lines near the ring finger rather than one clear line",
        meaning: "Many talents and creative gifts — but scattered across too many directions. You shine in bursts. Focusing your energy on one core creative path will unlock sustained recognition.",
        vedic: "Surya dispersed across multiple Rashis — needs a single dharmic focus to crystallize into fame.",
        scores: { career: 1, wealth: 0, love: 0, mental: 1, health: 0, spiritual: 1 },
      },
      {
        id: "starts_heart", label: "Starts from Heart Line",
        desc: "The sun line appears to begin from where the heart line is",
        meaning: "Recognition comes through authentic passion and love. Your fame is earned through genuine creative expression, not calculation. Success arrives in mid-to-late life — permanent and deeply fulfilling.",
        vedic: "Delayed Surya grace — but when it comes, it is the most enduring kind. Built on devotion rather than ambition.",
        scores: { career: 2, wealth: 1, love: 2, mental: 0, health: 0, spiritual: 2 },
      },
      {
        id: "absent", label: "Absent / Not Visible",
        desc: "No clear line near the ring finger",
        meaning: "Success without public fame. You achieve significantly — but prefer to work behind the scenes. Inner satisfaction matters more to you than recognition. A powerful, private achiever.",
        vedic: "Inner Surya — the light shines inward rather than outward. Spiritual achievement over worldly recognition.",
        scores: { career: 0, wealth: 0, love: 0, mental: 1, health: 0, spiritual: 2 },
      },
    ],
  },
  {
    id: "mount_venus",
    line: "Mount of Venus",
    sanskrit: "शुक्र पर्वत",
    instruction: "Look at the fleshy area at the BASE of your THUMB.",
    hint: "Press your thumb gently to your palm — the padded area that rises is the Venus mount.",
    color: "#E8650A",
    options: [
      {
        id: "large_full", label: "Large & Well Developed",
        desc: "Full, firm, and prominent — noticeably raised",
        meaning: "Abundant capacity for love, pleasure, beauty, and sensuality. You give generously in relationships. Naturally creative, with an appreciation for art, music, and beauty in all forms. Life is meant to be enjoyed.",
        vedic: "Shukra strong — Kama (desire) is healthy and abundant. Great blessings in love, partnerships, and artistic pursuits.",
        scores: { love: 3, spiritual: 0, mental: 0, health: 1, wealth: 0, career: 0 },
      },
      {
        id: "medium", label: "Medium / Moderate",
        desc: "Present but not especially prominent or flat",
        meaning: "Balanced approach to love and pleasure. You enjoy life's sensory gifts without being controlled by them. Healthy relationships, moderate creative expression.",
        vedic: "Shukra in balance — the middle path of Kama. Healthy desire without attachment.",
        scores: { love: 1, spiritual: 1, mental: 0, health: 1, wealth: 1, career: 0 },
      },
      {
        id: "flat", label: "Flat or Barely Visible",
        desc: "The base of the thumb area is relatively flat",
        meaning: "More reserved in emotional and sensory expression. You tend toward the ascetic rather than the sensual. Relationships may feel secondary to work or spiritual pursuits. Self-sufficiency is a core value.",
        vedic: "Shukra subdued — Vairagya (detachment) over Bhoga (enjoyment). The renunciant or dedicated professional.",
        scores: { love: -1, spiritual: 2, mental: 1, health: 0, wealth: 0, career: 1 },
      },
    ],
  },
  {
    id: "mount_jupiter",
    line: "Mount of Jupiter",
    sanskrit: "गुरु पर्वत",
    instruction: "Look at the base of your INDEX finger.",
    hint: "The padded area directly below the index finger — when developed, it creates a visible fullness at the finger's base.",
    color: "#C8860A",
    options: [
      {
        id: "prominent", label: "Prominent & Raised",
        desc: "Noticeably full area below the index finger",
        meaning: "Natural leader, teacher, and guide. You have strong ambition, confidence, and a genuine desire to uplift others. People naturally look to you for direction. Authority comes naturally.",
        vedic: "Guru strong — Brahma Jnana, leadership blessed by Jupiter. Born to teach, guide, or lead. The Guru archetype.",
        scores: { career: 3, love: 1, mental: 1, health: 0, wealth: 2, spiritual: 2 },
      },
      {
        id: "moderate", label: "Moderate",
        desc: "Present but not dramatically raised",
        meaning: "Healthy confidence and ambition. You lead when needed but don't seek the spotlight for its own sake. Good judgment and fair authority.",
        vedic: "Guru in balance — wisdom applied practically. Dharmic leadership.",
        scores: { career: 1, love: 0, mental: 1, health: 0, wealth: 1, spiritual: 1 },
      },
      {
        id: "flat", label: "Flat or Underdeveloped",
        desc: "Little or no fullness below the index finger",
        meaning: "May struggle with confidence or claiming authority. Leadership may feel uncomfortable. The inner Jupiter is waiting to be awakened — often through a Saturn transit or significant challenge.",
        vedic: "Guru dormant — the teacher within is not yet activated. Faith and study will awaken it.",
        scores: { career: -1, love: 0, mental: 0, health: 0, wealth: -1, spiritual: 0 },
      },
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

function ScanTab() {
  const [image, setImage] = useState<string | null>(null);
  const [imageName, setImageName] = useState<string>("");
  const [dragOver, setDragOver] = useState(false);
  const [step, setStep] = useState<"upload" | "hand_type" | "questions" | "report">("upload");
  const [handTypeId, setHandTypeId] = useState<string | null>(null);
  const [questionIndex, setQuestionIndex] = useState(0);
  const [selections, setSelections] = useState<Selection[]>([]);
  const [currentSelection, setCurrentSelection] = useState<string | null>(null);
  const [report, setReport] = useState<GeneratedReport | null>(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<any>(null);
  const [aiError, setAiError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const rawFileRef = useRef<File | null>(null);

  const handleFile = (file: File) => {
    if (!file.type.startsWith("image/")) return;
    rawFileRef.current = file;
    const reader = new FileReader();
    reader.onload = (e) => {
      setImage(e.target?.result as string);
      setImageName(file.name);
    };
    reader.readAsDataURL(file);
  };

  const analyzeWithAI = async () => {
    if (!rawFileRef.current) return;
    setAiLoading(true);
    setAiError(null);
    setAiResult(null);
    try {
      const formData = new FormData();
      formData.append("file", rawFileRef.current);
      const res = await fetch("/api/palmistry/analyze", { method: "POST", body: formData });
      if (!res.ok) {
        const err = await res.json().catch(() => ({ detail: "Analysis failed" }));
        throw new Error(err.detail || "Analysis failed");
      }
      const data = await res.json();
      setAiResult(data);
      setStep("ai_report" as any);
    } catch (e: any) {
      setAiError(e.message || "Analysis failed. Please try again.");
    } finally {
      setAiLoading(false);
    }
  };

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }, []);

  const reset = () => {
    setImage(null);
    setImageName("");
    setStep("upload");
    setHandTypeId(null);
    setQuestionIndex(0);
    setSelections([]);
    setCurrentSelection(null);
    setReport(null);
    setAiResult(null);
    setAiError(null);
  };

  const handleNextQuestion = () => {
    if (!currentSelection) return;
    const newSel = [...selections.filter(s => s.questionId !== QUESTIONS[questionIndex].id),
      { questionId: QUESTIONS[questionIndex].id, optionId: currentSelection }];
    setSelections(newSel);

    if (questionIndex < QUESTIONS.length - 1) {
      setQuestionIndex(questionIndex + 1);
      // Pre-fill if already answered
      const existing = newSel.find(s => s.questionId === QUESTIONS[questionIndex + 1].id);
      setCurrentSelection(existing?.optionId ?? null);
    } else {
      // Done — generate report
      const r = buildReport(handTypeId!, newSel);
      setReport(r);
      setStep("report");
    }
  };

  const handlePrevQuestion = () => {
    if (questionIndex === 0) { setStep("hand_type"); return; }
    setQuestionIndex(questionIndex - 1);
    const existing = selections.find(s => s.questionId === QUESTIONS[questionIndex - 1].id);
    setCurrentSelection(existing?.optionId ?? null);
  };

  const downloadReport = () => {
    if (!report) return;
    const lines = report.selections.map(sel => {
      const q = QUESTIONS.find(q => q.id === sel.questionId);
      const opt = q?.options.find(o => o.id === sel.optionId);
      return `${q?.line}\n  Selected: ${opt?.label}\n  ${opt?.meaning}\n  Vedic: ${opt?.vedic}`;
    }).join("\n\n");
    const content = [
      "HASTA SAMUDRIKA SHASTRA — PALM READING REPORT",
      "═".repeat(52),
      `Hand Type: ${report.handType.label} (${report.handType.vedic})`,
      `Element: ${report.handType.element}`,
      "",
      report.handType.vedic_note,
      "",
      "LIFE AREA SCORES",
      ...report.lifeScores.map(a => `  ${a.icon} ${a.area}: ${a.score}/10`),
      "",
      "PALM LINE ANALYSIS",
      lines,
      "",
      "STRENGTHS",
      ...report.strengths.map(s => `  ✦ ${s}`),
      "",
      "CHALLENGES & GROWTH AREAS",
      ...report.challenges.map(c => `  ◆ ${c}`),
      "",
      "VEDIC REMEDIES",
      ...report.remedies.map(r => `  ${r.title}\n  ${r.detail}`),
      "",
      "SUMMARY",
      report.summary,
      "",
      `✦ ${report.auspiciousNote}`,
    ].join("\n");
    const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "hasta-shastra-reading.txt";
    a.click();
    URL.revokeObjectURL(url);
  };

  // ── Upload Step ──
  if (step === "upload") {
    return (
      <div className="grid lg:grid-cols-2 gap-5 max-w-4xl mx-auto">
        <Card className="cosmic-card border-border/30">
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2">
              <Scan className="h-4 w-4 text-primary zodiac-glow" />
              Upload Your Palm
            </CardTitle>
            <p className="text-xs text-muted-foreground">Right hand for males · Left hand for females</p>
          </CardHeader>
          <CardContent className="space-y-4">
            {!image ? (
              <>
                <div
                  onDrop={onDrop}
                  onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                  onDragLeave={() => setDragOver(false)}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 border-dashed rounded-xl p-10 flex flex-col items-center justify-center gap-3 cursor-pointer transition-all ${
                    dragOver ? "border-primary/60 bg-primary/5" : "border-border/30 hover:border-primary/30 hover:bg-muted/10"
                  }`}
                >
                  <div className="h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center">
                    <Upload className="h-7 w-7 text-primary" />
                  </div>
                  <div className="text-center">
                    <p className="text-sm font-medium">Drop palm image here</p>
                    <p className="text-xs text-muted-foreground mt-1">or click to browse</p>
                  </div>
                  <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={e => { const f = e.target.files?.[0]; if (f) handleFile(f); }} />
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex-1 border-t border-border/20" />
                  <span className="text-xs text-muted-foreground">or</span>
                  <div className="flex-1 border-t border-border/20" />
                </div>
                <button
                  onClick={() => cameraInputRef.current?.click()}
                  className="w-full flex items-center justify-center gap-2.5 py-3.5 rounded-xl border border-border/30 bg-muted/10 hover:bg-muted/20 hover:border-primary/30 transition-all"
                >
                  <Camera className="h-5 w-5 text-primary" />
                  <span className="text-sm font-medium">Use Camera</span>
                </button>
                <input ref={cameraInputRef} type="file" accept="image/*" capture="environment" className="hidden" onChange={e => { const f = e.target.files?.[0]; if (f) handleFile(f); }} />
              </>
            ) : (
              <div className="space-y-3">
                <div className="relative rounded-xl overflow-hidden border border-border/30">
                  <img src={image} alt="Palm" className="w-full object-contain max-h-64" />
                  <button onClick={reset} className="absolute top-2 right-2 h-7 w-7 rounded-full bg-background/80 border border-border/50 flex items-center justify-center">
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
                <p className="text-xs text-muted-foreground text-center">{imageName}</p>
                {aiError && <p className="text-xs text-red-400 text-center">{aiError}</p>}
                <Button
                  onClick={analyzeWithAI}
                  disabled={aiLoading}
                  className="w-full h-11 font-semibold bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white border-0"
                >
                  {aiLoading ? (
                    <><span className="animate-spin mr-2">✦</span> AI is reading your palm...</>
                  ) : (
                    <><Sparkles className="h-4 w-4 mr-2" /> AI Palm Reading </>
                  )}
                </Button>
                <div className="flex items-center gap-3">
                  <div className="flex-1 border-t border-border/20" />
                  <span className="text-xs text-muted-foreground">or answer manually</span>
                  <div className="flex-1 border-t border-border/20" />
                </div>
                <Button onClick={() => setStep("hand_type")} variant="outline" className="w-full h-10 text-sm border-border/30">
                  <ArrowRight className="h-4 w-4 mr-2" /> Manual Guided Reading
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="cosmic-card border-border/30">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-muted-foreground flex items-center gap-2"><Info className="h-3.5 w-3.5 text-primary" /> Photo Tips</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {[
              { icon: "☀️", t: "Good lighting", d: "Natural or bright indoor light. Avoid shadows on the palm." },
              { icon: "✋", t: "Open palm flat", d: "Extend fingers naturally — don't press them together." },
              { icon: "📸", t: "Top-down shot", d: "Camera directly above palm, perpendicular to the hand." },
              { icon: "🔍", t: "Fill the frame", d: "Palm should fill most of the image so lines are visible." },
              { icon: "🤚", t: "Which hand?", d: "Right (dominant) = active karma. Left = inherited karma." },
              { icon: "🕉️", t: "How it works", d: "Upload your palm, then answer simple guided questions about what you see. We generate your full Vedic reading instantly." },
            ].map(item => (
              <div key={item.t} className="flex items-start gap-3">
                <span className="text-lg">{item.icon}</span>
                <div>
                  <p className="text-xs font-semibold text-foreground">{item.t}</p>
                  <p className="text-xs text-muted-foreground">{item.d}</p>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    );
  }

  // ── Hand Type Step ──
  if (step === "hand_type") {
    return (
      <div className="max-w-4xl mx-auto space-y-4">
        {/* Progress */}
        <div className="flex items-center gap-3 mb-2">
          <button onClick={() => setStep("upload")} className="text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div className="flex-1 h-1.5 rounded-full bg-muted/30 overflow-hidden">
            <div className="h-full bg-primary rounded-full" style={{ width: "10%" }} />
          </div>
          <span className="text-xs text-muted-foreground">Step 1 of {QUESTIONS.length + 1}</span>
        </div>

        <div className="flex items-center gap-3">
          {image && <img src={image} alt="Palm" className="h-14 w-14 rounded-lg object-cover border border-border/30 flex-shrink-0" />}
          <div>
            <h2 className="font-display text-xl text-glow-gold">What is your hand shape?</h2>
            <p className="text-xs text-muted-foreground mt-0.5">Look at your palm and fingers to identify your hand type</p>
          </div>
        </div>

        <div className="grid sm:grid-cols-2 gap-4">
          {HAND_TYPES.map(ht => (
            <button
              key={ht.id}
              onClick={() => setHandTypeId(ht.id)}
              className={`text-left p-4 rounded-xl border transition-all ${
                handTypeId === ht.id
                  ? "border-primary/50 bg-primary/10"
                  : "border-border/30 bg-muted/5 hover:border-border/50 hover:bg-muted/10"
              }`}
            >
              <div className="flex items-start gap-3">
                <span className="text-3xl">{ht.icon}</span>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-sm font-semibold text-foreground">{ht.label}</span>
                    {handTypeId === ht.id && <CheckCircle2 className="h-4 w-4 text-primary flex-shrink-0" />}
                  </div>
                  <p className="text-xs text-muted-foreground">{ht.shape}</p>
                  <p className="text-xs text-muted-foreground mt-1">{ht.element}</p>
                  <div className="flex flex-wrap gap-1 mt-2">
                    {ht.traits.slice(0, 3).map(t => (
                      <span key={t} className="text-xs px-1.5 py-0.5 rounded bg-muted/30 text-muted-foreground">{t}</span>
                    ))}
                  </div>
                </div>
              </div>
            </button>
          ))}
        </div>

        <Button
          onClick={() => { setStep("questions"); setQuestionIndex(0); setCurrentSelection(selections.find(s => s.questionId === QUESTIONS[0].id)?.optionId ?? null); }}
          disabled={!handTypeId}
          className="w-full btn-saffron h-11 font-semibold mt-2"
        >
          Continue <ArrowRight className="h-4 w-4 ml-2" />
        </Button>
      </div>
    );
  }

  // ── Question Step ──
  if (step === "questions") {
    const q = QUESTIONS[questionIndex];
    const progress = ((questionIndex + 1) / (QUESTIONS.length + 1)) * 100;
    return (
      <div className="max-w-4xl mx-auto space-y-4">
        {/* Progress */}
        <div className="flex items-center gap-3">
          <button onClick={handlePrevQuestion} className="text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div className="flex-1 h-1.5 rounded-full bg-muted/30 overflow-hidden">
            <motion.div
              className="h-full bg-primary rounded-full"
              initial={{ width: `${(questionIndex / (QUESTIONS.length + 1)) * 100}%` }}
              animate={{ width: `${progress}%` }}
            />
          </div>
          <span className="text-xs text-muted-foreground">Step {questionIndex + 2} of {QUESTIONS.length + 1}</span>
        </div>

        <AnimatePresence mode="wait">
          <motion.div
            key={q.id}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            className="space-y-4"
          >
            <div className="grid lg:grid-cols-5 gap-4">
              {/* Palm image reference */}
              {image && (
                <div className="lg:col-span-2">
                  <Card className="cosmic-card border-border/30">
                    <CardContent className="pt-3 pb-3">
                      <img src={image} alt="Your palm" className="w-full rounded-lg object-contain max-h-48 mb-2" />
                      <div className="rounded-lg p-2.5 border border-border/20 bg-muted/10 space-y-1">
                        <p className="text-xs font-semibold" style={{ color: q.color }}>{q.line}</p>
                        <p className="text-xs text-muted-foreground">{q.instruction}</p>
                        <p className="text-xs text-muted-foreground/60 italic">{q.hint}</p>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              )}

              {/* Options */}
              <div className={`${image ? "lg:col-span-3" : "lg:col-span-5"} space-y-2`}>
                <div className="mb-3">
                  <h2 className="font-display text-lg text-glow-gold" style={{ color: q.color }}>{q.line}</h2>
                  <p className="text-xs text-muted-foreground">{q.sanskrit} · {q.instruction}</p>
                </div>
                {q.options.map(opt => (
                  <button
                    key={opt.id}
                    onClick={() => setCurrentSelection(opt.id)}
                    className={`w-full text-left p-3 rounded-xl border transition-all ${
                      currentSelection === opt.id
                        ? "border-primary/50 bg-primary/8"
                        : "border-border/30 hover:border-border/50 hover:bg-muted/10"
                    }`}
                    style={currentSelection === opt.id ? { borderColor: `${q.color}50`, background: `${q.color}0d` } : {}}
                  >
                    <div className="flex items-start gap-3">
                      <div className={`mt-0.5 h-4 w-4 rounded-full border-2 flex-shrink-0 flex items-center justify-center ${
                        currentSelection === opt.id ? "border-primary bg-primary/20" : "border-border/50"
                      }`} style={currentSelection === opt.id ? { borderColor: q.color } : {}}>
                        {currentSelection === opt.id && <div className="h-2 w-2 rounded-full" style={{ background: q.color }} />}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-foreground">{opt.label}</p>
                        <p className="text-xs text-muted-foreground mt-0.5">{opt.desc}</p>
                        {currentSelection === opt.id && (
                          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: "auto" }} className="mt-2 space-y-1">
                            <p className="text-xs text-foreground leading-relaxed">{opt.meaning}</p>
                            <p className="text-xs italic" style={{ color: q.color }}>✦ {opt.vedic}</p>
                          </motion.div>
                        )}
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            <Button
              onClick={handleNextQuestion}
              disabled={!currentSelection}
              className="w-full btn-saffron h-11 font-semibold"
            >
              {questionIndex === QUESTIONS.length - 1 ? (
                <><Sparkles className="h-4 w-4 mr-2" /> Generate My Reading</>
              ) : (
                <>Next <ArrowRight className="h-4 w-4 ml-1.5" /></>
              )}
            </Button>
          </motion.div>
        </AnimatePresence>
      </div>
    );
  }

  // ── Report Step ──
  if (step === "report" && report) {
    return (
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-5 max-w-5xl mx-auto">
        {/* Header */}
        <Card className="cosmic-card border-primary/20">
          <CardContent className="pt-4 pb-4">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                {image && <img src={image} alt="Palm" className="h-16 w-16 rounded-lg object-cover border border-border/30 flex-shrink-0" />}
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <CheckCircle2 className="h-4 w-4 text-primary" />
                    <h2 className="font-display text-xl text-glow-gold">Your Hasta Shastra Reading</h2>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Badge className="bg-primary/20 text-primary border-primary/30 text-xs">{report.handType.label}</Badge>
                    <Badge variant="outline" className="border-border/30 text-xs">{report.handType.vedic}</Badge>
                    <Badge variant="outline" className="border-border/30 text-xs">{report.handType.element}</Badge>
                  </div>
                </div>
              </div>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" onClick={downloadReport} className="border-border/30 text-xs">
                  <Download className="h-3.5 w-3.5 mr-1.5" /> Download
                </Button>
                <Button variant="outline" size="sm" onClick={reset} className="border-border/30 text-xs">
                  <RotateCcw className="h-3.5 w-3.5 mr-1.5" /> New Reading
                </Button>
              </div>
            </div>
            <div className="mt-3 rounded-lg p-3 bg-primary/5 border border-primary/15">
              <p className="text-xs text-muted-foreground italic leading-relaxed">{report.handType.vedic_note}</p>
            </div>
          </CardContent>
        </Card>

        {/* Auspicious note */}
        <div className="flex items-start gap-3 rounded-xl p-4 border border-primary/20 bg-primary/5">
          <span className="text-xl">✦</span>
          <p className="text-sm text-foreground leading-relaxed">{report.auspiciousNote}</p>
        </div>

        {/* Life areas + Overview */}
        <div className="grid lg:grid-cols-5 gap-5">
          <Card className="cosmic-card border-border/30 lg:col-span-3">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm text-muted-foreground">Hand Type — {report.handType.label}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-x-4 gap-y-2">
                {report.handType.traits.map(t => (
                  <div key={t} className="flex items-center gap-1.5 text-xs text-foreground">
                    <span className="text-primary">◆</span> {t}
                  </div>
                ))}
              </div>
              <div className="gold-divider" />
              <div className="space-y-1.5 text-xs text-muted-foreground">
                <div><span className="text-primary font-medium">Careers: </span>{report.handType.careers}</div>
                <div><span className="text-primary font-medium">Constitution: </span>{report.handType.constitution}</div>
                <div><span className="text-secondary font-medium">Shadow: </span>{report.handType.shadow}</div>
              </div>
            </CardContent>
          </Card>

          <Card className="cosmic-card border-border/30 lg:col-span-2">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm text-muted-foreground flex items-center gap-1.5">
                <Star className="h-3.5 w-3.5 text-primary" /> Life Area Scores
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {report.lifeScores.map((area, i) => (
                <motion.div key={area.area} initial={{ opacity: 0, x: 10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.07 }}>
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs text-foreground flex items-center gap-1.5"><span>{area.icon}</span>{area.area}</span>
                  </div>
                  <ScoreBar score={area.score} />
                </motion.div>
              ))}
            </CardContent>
          </Card>
        </div>

        {/* Palm Line Readings */}
        <div>
          <h3 className="font-display text-base text-glow-gold mb-3 flex items-center gap-2">
            <Hand className="h-4 w-4 text-primary" /> Your Palm Lines
          </h3>
          <div className="grid sm:grid-cols-2 gap-4">
            {report.selections.map((sel, i) => {
              const q = QUESTIONS.find(q => q.id === sel.questionId);
              const opt = q?.options.find(o => o.id === sel.optionId);
              if (!q || !opt) return null;
              return (
                <motion.div key={sel.questionId} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.07 }}>
                  <Card className="cosmic-card border-border/30 h-full">
                    <CardContent className="pt-4 space-y-2.5">
                      <div className="flex items-start gap-2">
                        <span className="h-2.5 w-2.5 rounded-full flex-shrink-0 mt-1" style={{ background: q.color }} />
                        <div>
                          <span className="text-sm font-semibold text-foreground">{q.line}</span>
                          <span className="text-xs text-muted-foreground ml-2">{q.sanskrit}</span>
                        </div>
                      </div>
                      <Badge variant="outline" className="text-xs border-border/30 ml-4">{opt.label}</Badge>
                      <p className="text-xs text-foreground leading-relaxed">{opt.meaning}</p>
                      <div className="rounded-md px-2.5 py-2 bg-muted/20 border border-border/15">
                        <p className="text-xs italic" style={{ color: q.color }}>✦ {opt.vedic}</p>
                      </div>
                    </CardContent>
                  </Card>
                </motion.div>
              );
            })}
          </div>
        </div>

        {/* Strengths, Challenges, Remedies */}
        <div className="grid sm:grid-cols-3 gap-4">
          <Card className="cosmic-card border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm text-primary">Strengths</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {report.strengths.map(s => (
                <div key={s} className="flex items-start gap-2">
                  <span className="text-primary text-xs mt-0.5 flex-shrink-0">✦</span>
                  <p className="text-xs text-foreground leading-relaxed">{s}</p>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="cosmic-card border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm text-secondary">Growth Areas</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {report.challenges.length > 0 ? report.challenges.map(c => (
                <div key={c} className="flex items-start gap-2">
                  <span className="text-secondary text-xs mt-0.5 flex-shrink-0">◆</span>
                  <p className="text-xs text-foreground leading-relaxed">{c}</p>
                </div>
              )) : (
                <p className="text-xs text-muted-foreground italic">Your palm shows a well-balanced karmic profile with no major challenges. Continue on your current path with devotion.</p>
              )}
            </CardContent>
          </Card>

          <Card className="cosmic-card border-border/30">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm text-muted-foreground flex items-center gap-1.5">🕉️ Vedic Remedies</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {report.remedies.map(r => (
                <div key={r.title} className="space-y-0.5">
                  <p className="text-xs font-semibold text-foreground">{r.title}</p>
                  <p className="text-xs text-muted-foreground leading-relaxed">{r.detail}</p>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>

        {/* Summary */}
        <Card className="cosmic-card border-primary/20">
          <CardHeader className="pb-3">
            <CardTitle className="text-base text-glow-gold flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-primary zodiac-glow" /> Your Karmic Path
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-foreground leading-relaxed">{report.summary}</p>
            <div className="gold-divider mt-4" />
            <p className="text-xs text-muted-foreground/60 italic mt-3 text-center">
              Based on Hasta Samudrika Shastra. For complete Jyotish analysis, combine with your Kundali chart.
            </p>
          </CardContent>
        </Card>
      </motion.div>
    );
  }

  // ── AI Report Step (Gemini Vision) ──
  if ((step as string) === "ai_report" && aiResult) {
    const lineColors: Record<string, string> = {
      "Heart Line": "#E8650A", "Head Line": "#C8860A", "Life Line": "#7CB87A",
      "Fate Line": "#7A8BAA", "Sun Line": "#F5C842", "Mercury Line": "#9B8ED4",
    };
    const lifeAreaIcons: Record<string, string> = {
      "Love & Relationships": "💕", "Career & Purpose": "🏆", "Health & Vitality": "🌿",
      "Mental Clarity": "🧠", "Wealth & Prosperity": "✨", "Spiritual Growth": "🕉️",
    };
    const mountConditionLabel: Record<string, string> = {
      well_developed: "Well Developed", flat: "Flat", overdeveloped: "Overdeveloped",
    };
    return (
      <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-5 max-w-5xl mx-auto">
        {/* Header */}
        <Card className="cosmic-card border-primary/20">
          <CardContent className="pt-4 pb-4">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                {image && <img src={image} alt="Palm" className="h-16 w-16 rounded-lg object-cover border border-border/30 flex-shrink-0" />}
                <div>
                  <div className="flex items-center gap-2 mb-1.5">
                    <Sparkles className="h-4 w-4 text-primary zodiac-glow" />
                    <h2 className="font-display text-xl text-glow-gold">AI Palm Reading</h2>
                    <Badge className="bg-purple-500/20 text-purple-300 border-purple-500/30 text-xs"></Badge>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Badge className="bg-primary/20 text-primary border-primary/30 text-xs">{aiResult.hand_type}</Badge>
                    <Badge variant="outline" className="border-border/30 text-xs">{aiResult.hand_type_vedic}</Badge>
                    <Badge variant="outline" className="border-border/30 text-xs">{aiResult.hand_type_element}</Badge>
                  </div>
                </div>
              </div>
              <Button variant="outline" size="sm" onClick={reset} className="border-border/30 text-xs">
                <RotateCcw className="h-3.5 w-3.5 mr-1.5" /> New Reading
              </Button>
            </div>
            {aiResult.hand_type_reading && (
              <div className="mt-3 rounded-lg p-3 bg-primary/5 border border-primary/15">
                <p className="text-xs text-muted-foreground italic leading-relaxed">{aiResult.hand_type_reading}</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Auspicious note */}
        {aiResult.auspicious_note && (
          <div className="flex items-start gap-3 rounded-xl p-4 border border-primary/20 bg-primary/5">
            <span className="text-xl flex-shrink-0">✦</span>
            <p className="text-sm text-foreground leading-relaxed">{aiResult.auspicious_note}</p>
          </div>
        )}

        {/* Overview + Life Scores */}
        <div className="grid lg:grid-cols-5 gap-5">
          {aiResult.overview && (
            <Card className="cosmic-card border-border/30 lg:col-span-3">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-muted-foreground flex items-center gap-1.5">
                  <BookOpen className="h-3.5 w-3.5 text-primary" /> Overview
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-foreground leading-relaxed">{aiResult.overview}</p>
              </CardContent>
            </Card>
          )}
          {aiResult.life_areas?.length > 0 && (
            <Card className="cosmic-card border-border/30 lg:col-span-2">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-muted-foreground flex items-center gap-1.5">
                  <Star className="h-3.5 w-3.5 text-primary" /> Life Area Scores
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {aiResult.life_areas.map((area: any, i: number) => (
                  <motion.div key={area.area} initial={{ opacity: 0, x: 10 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: i * 0.06 }}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs text-foreground flex items-center gap-1.5">
                        <span>{lifeAreaIcons[area.area] ?? "✦"}</span>{area.area}
                      </span>
                      <span className="text-xs text-muted-foreground">{area.label}</span>
                    </div>
                    <ScoreBar score={area.score} />
                    {area.note && <p className="text-xs text-muted-foreground mt-0.5 leading-relaxed">{area.note}</p>}
                  </motion.div>
                ))}
              </CardContent>
            </Card>
          )}
        </div>

        {/* Palm Lines */}
        {aiResult.lines?.length > 0 && (
          <div>
            <h3 className="font-display text-base text-glow-gold mb-3 flex items-center gap-2">
              <Hand className="h-4 w-4 text-primary" /> Palm Line Analysis
            </h3>
            <div className="grid sm:grid-cols-2 gap-4">
              {aiResult.lines.map((line: any, i: number) => {
                const color = lineColors[line.name] ?? "#C8860A";
                return (
                  <motion.div key={line.name} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.07 }}>
                    <Card className="cosmic-card border-border/30 h-full">
                      <CardContent className="pt-4 space-y-2.5">
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-start gap-2">
                            <span className="h-2.5 w-2.5 rounded-full flex-shrink-0 mt-1" style={{ background: color }} />
                            <div>
                              <span className="text-sm font-semibold text-foreground">{line.name}</span>
                              <span className="text-xs text-muted-foreground ml-2">{line.sanskrit}</span>
                            </div>
                          </div>
                          {line.score !== undefined && <ScoreBar score={line.score} />}
                        </div>
                        {line.visibility && (
                          <Badge variant="outline" className="text-xs border-border/30 ml-4 capitalize">{line.visibility}</Badge>
                        )}
                        {line.observation && (
                          <p className="text-xs text-muted-foreground leading-relaxed italic">{line.observation}</p>
                        )}
                        {line.interpretation && (
                          <p className="text-xs text-foreground leading-relaxed">{line.interpretation}</p>
                        )}
                        {line.vedic_note && (
                          <div className="rounded-md px-2.5 py-2 bg-muted/20 border border-border/15">
                            <p className="text-xs italic" style={{ color }}>✦ {line.vedic_note}</p>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  </motion.div>
                );
              })}
            </div>
          </div>
        )}

        {/* Dominant Mounts */}
        {aiResult.dominant_mounts?.length > 0 && (
          <div>
            <h3 className="font-display text-base text-glow-gold mb-3 flex items-center gap-2">
              <Hand className="h-4 w-4 text-primary" /> Dominant Mounts
            </h3>
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {aiResult.dominant_mounts.map((mount: any, i: number) => (
                <motion.div key={mount.name + i} initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: i * 0.06 }}>
                  <Card className="cosmic-card border-border/30 h-full">
                    <CardContent className="pt-4 space-y-2">
                      <div>
                        <p className="text-sm font-semibold text-foreground">{mount.name}</p>
                        <p className="text-xs text-muted-foreground">{mount.planet}</p>
                      </div>
                      {mount.condition && (
                        <Badge variant="outline" className="text-xs border-border/30 capitalize">
                          {mountConditionLabel[mount.condition] ?? mount.condition}
                        </Badge>
                      )}
                      {mount.note && <p className="text-xs text-foreground leading-relaxed">{mount.note}</p>}
                    </CardContent>
                  </Card>
                </motion.div>
              ))}
            </div>
          </div>
        )}

        {/* Strengths, Challenges, Remedies */}
        <div className="grid sm:grid-cols-3 gap-4">
          {aiResult.strengths?.length > 0 && (
            <Card className="cosmic-card border-border/30">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-primary">Strengths</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {aiResult.strengths.map((s: string) => (
                  <div key={s} className="flex items-start gap-2">
                    <span className="text-primary text-xs mt-0.5 flex-shrink-0">✦</span>
                    <p className="text-xs text-foreground leading-relaxed">{s}</p>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
          {aiResult.challenges?.length > 0 && (
            <Card className="cosmic-card border-border/30">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-secondary">Growth Areas</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {aiResult.challenges.map((c: string) => (
                  <div key={c} className="flex items-start gap-2">
                    <span className="text-secondary text-xs mt-0.5 flex-shrink-0">◆</span>
                    <p className="text-xs text-foreground leading-relaxed">{c}</p>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
          {aiResult.remedies?.length > 0 && (
            <Card className="cosmic-card border-border/30">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-muted-foreground flex items-center gap-1.5">🕉️ Vedic Remedies</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {aiResult.remedies.map((r: any) => (
                  <div key={r.title} className="space-y-0.5">
                    <p className="text-xs font-semibold text-foreground">{r.title}</p>
                    <p className="text-xs text-muted-foreground leading-relaxed">{r.detail}</p>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </div>

        {/* Summary */}
        {aiResult.summary && (
          <Card className="cosmic-card border-primary/20">
            <CardHeader className="pb-3">
              <CardTitle className="text-base text-glow-gold flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-primary zodiac-glow" /> Your Karmic Path
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-foreground leading-relaxed">{aiResult.summary}</p>
              <div className="gold-divider mt-4" />
              <p className="text-xs text-muted-foreground/60 italic mt-3 text-center">
                Analyzed by AI · Hasta Samudrika Shastra interpretation
              </p>
            </CardContent>
          </Card>
        )}
      </motion.div>
    );
  }

  return null;
}

// ─────────────────────────────────────────────────────────────────
// REFERENCE TABS (condensed)
// ─────────────────────────────────────────────────────────────────

function LinesTab() {
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
                  {q.line}
                  <span className="text-xs font-normal text-muted-foreground">({q.sanskrit})</span>
                  <ChevronRight className={`h-4 w-4 text-muted-foreground ml-auto transition-transform ${active === q.id ? "rotate-90" : ""}`} />
                </CardTitle>
                <p className="text-xs text-muted-foreground ml-6">{q.hint}</p>
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
                          <Badge variant="outline" className="text-xs border-border/30">{opt.label}</Badge>
                          <p className="text-xs text-foreground leading-relaxed">{opt.meaning}</p>
                          <p className="text-xs italic" style={{ color: q.color }}>✦ {opt.vedic}</p>
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
  const MOUNTS = [
    { id: "jupiter", name: "Mount of Jupiter", sanskrit: "गुरु पर्वत", finger: "Below Index Finger", planet: "Jupiter (Guru)", icon: "♃︎", color: "#C8860A",
      well: "Leader, ambitious, spiritually wise, generous, charismatic. Born to guide and teach.", flat: "Lack of confidence, avoids leadership roles.", over: "Arrogant, overbearing, domineering.", vedic: "Guru's seat of Brahma Jnana. Prominent in teachers, judges, and spiritual leaders." },
    { id: "saturn", name: "Mount of Saturn", sanskrit: "शनि पर्वत", finger: "Below Middle Finger", planet: "Saturn (Shani)", icon: "♄︎", color: "#7A8BAA",
      well: "Wise, responsible, patient, deeply introspective. Excellent researchers and dedicated servants.", flat: "Avoids responsibility, lacks discipline.", over: "Melancholic, hermit tendencies, can't enjoy life.", vedic: "Shani's seat of karma and dharma. Those who bear great responsibility with grace." },
    { id: "apollo", name: "Mount of Apollo", sanskrit: "सूर्य पर्वत", finger: "Below Ring Finger", planet: "Sun (Surya)", icon: "☉︎", color: "#E8650A",
      well: "Creative, charismatic, drawn to beauty and art. Fame comes naturally. Joyful and warm-hearted.", flat: "Lack of aesthetic sense, avoids public life.", over: "Vanity, obsession with appearances, craving fame over substance.", vedic: "Surya's seat of Atmic brilliance. Artists, performers, and those with deep creative Shakti." },
    { id: "mercury", name: "Mount of Mercury", sanskrit: "बुध पर्वत", finger: "Below Little Finger", planet: "Mercury (Budha)", icon: "☿︎", color: "#7A8BAA",
      well: "Brilliant communicator, witty, business-minded, quick-thinking. Natural healer.", flat: "Communication difficulties, poor business instinct.", over: "Cunning, deceptive use of words.", vedic: "Budha's seat of Vak Shakti. Scholars, traders, orators, and diplomats." },
    { id: "venus", name: "Mount of Venus", sanskrit: "शुक्र पर्वत", finger: "Base of Thumb", planet: "Venus (Shukra)", icon: "♀︎", color: "#E8650A",
      well: "Loving, sensual, generous, artistic. Great capacity for pleasure and deep human connection.", flat: "Cold, unaffectionate, may struggle with intimacy.", over: "Excessive sensuality, overindulgence, attachment.", vedic: "Shukra's seat of Kama (desire) — one of the four Purusharthas. The quality of love is here." },
    { id: "moon", name: "Mount of Moon", sanskrit: "चन्द्र पर्वत", finger: "Outer Base (opposite thumb)", planet: "Moon (Chandra)", icon: "☽︎", color: "#7A8BAA",
      well: "Deeply intuitive, imaginative, psychic sensitivity, rich inner world. Love of travel and poetry.", flat: "Lack of imagination, emotionally rigid.", over: "Overly emotional, difficulty distinguishing real from imagined.", vedic: "Chandra's seat of Manas and intuition. Astrologers, poets, healers, and psychic seers." },
    { id: "mars", name: "Mounts of Mars", sanskrit: "मंगल पर्वत", finger: "Upper & Lower Inner Edge", planet: "Mars (Mangal)", icon: "♂︎", color: "#E8650A",
      well: "Physical and moral courage. Resilience under pressure. Strength when it matters most.", flat: "Cowardice, gives up easily, avoids confrontation.", over: "Aggressive, hot-tempered, prone to confrontation.", vedic: "Mangal's Agni Shakti — the fire of righteous action. Warriors, surgeons, athletes." },
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
                    <p className="text-sm font-semibold">{m.name}</p>
                    <p className="text-xs text-muted-foreground">{m.sanskrit} · {m.finger}</p>
                  </div>
                </div>
                <p className="text-xs text-muted-foreground line-clamp-2">{m.well}</p>
                <AnimatePresence>
                  {active === m.id && (
                    <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: "auto", opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden">
                      <div className="gold-divider mb-2" />
                      <div className="space-y-2">
                        <div><p className="text-xs font-medium text-primary mb-0.5">Well Developed</p><p className="text-xs text-muted-foreground">{m.well}</p></div>
                        <div><p className="text-xs font-medium text-muted-foreground mb-0.5">Flat / Absent</p><p className="text-xs text-muted-foreground">{m.flat}</p></div>
                        <div><p className="text-xs font-medium text-destructive/80 mb-0.5">Overdeveloped</p><p className="text-xs text-muted-foreground">{m.over}</p></div>
                        <div className="rounded-md p-2 bg-muted/20 border border-border/15"><p className="text-xs italic text-muted-foreground">{m.vedic}</p></div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
                <div className="flex items-center gap-1 text-primary/60 text-xs">
                  <ChevronRight className={`h-3 w-3 transition-transform ${active === m.id ? "rotate-90" : ""}`} />
                  <span>{active === m.id ? "Collapse" : "Full reading"}</span>
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
  return (
    <div className="grid sm:grid-cols-2 gap-5">
      {HAND_TYPES.map((ht, i) => (
        <motion.div key={ht.id} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.08 }}>
          <Card className="cosmic-card border-border/30 h-full">
            <CardHeader className="pb-3">
              <div className="flex items-start gap-3">
                <span className="text-4xl">{ht.icon}</span>
                <div><CardTitle className="text-lg">{ht.label}</CardTitle><p className="text-xs text-muted-foreground">{ht.vedic} · {ht.shape}</p></div>
                <Badge variant="outline" className="ml-auto border-primary/30 text-primary text-xs">{ht.element}</Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-x-4 gap-y-1.5">
                {ht.traits.map(t => (
                  <div key={t} className="flex items-start gap-1.5 text-xs text-muted-foreground">
                    <span className="text-primary mt-0.5 flex-shrink-0">◆</span><span>{t}</span>
                  </div>
                ))}
              </div>
              <div className="gold-divider" />
              <div className="space-y-1.5 text-xs">
                <div><span className="text-primary font-medium">Careers: </span><span className="text-muted-foreground">{ht.careers}</span></div>
                <div><span className="text-primary font-medium">Constitution: </span><span className="text-muted-foreground">{ht.constitution}</span></div>
                <div><span className="text-secondary font-medium">Shadow: </span><span className="text-muted-foreground">{ht.shadow}</span></div>
              </div>
              <div className="rounded-lg p-3 bg-muted/20 border border-border/20">
                <p className="text-xs text-muted-foreground italic leading-relaxed">{ht.vedic_note}</p>
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
  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}>
        <div className="flex items-center gap-3 mb-1">
          <Hand className="h-7 w-7 text-primary zodiac-glow" />
          <h1 className="font-display text-3xl text-glow-gold">Hasta Samudrika Shastra</h1>
        </div>
        <p className="text-muted-foreground text-sm ml-10">
          हस्त सामुद्रिक शास्त्र — Upload your palm, answer guided questions, receive your complete Vedic reading
        </p>
      </motion.div>

      <Tabs defaultValue="scan" className="w-full">
        <TabsList className="glass grid grid-cols-4 w-full">
          <TabsTrigger value="scan" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <Scan className="h-3.5 w-3.5" /> <span>Scan Palm</span>
          </TabsTrigger>
          <TabsTrigger value="lines" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <Hand className="h-3.5 w-3.5" /> <span>Lines</span>
          </TabsTrigger>
          <TabsTrigger value="mounts" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <Star className="h-3.5 w-3.5" /> <span>Mounts</span>
          </TabsTrigger>
          <TabsTrigger value="hand" className="flex items-center gap-1.5 text-xs sm:text-sm">
            <BookOpen className="h-3.5 w-3.5" /> <span>Hand Type</span>
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
