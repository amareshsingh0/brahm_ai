"""
Stage 5: Vedic Interpretation Engine
Input:  feature dict from 04_extract_features.py
Output: interpretations dict + life area scores + Qwen prompt

This is our rule engine encoding traditional Hasta Samudrika Shastra.
"""

import json
from typing import Optional


# ─── Life Area Scores ─────────────────────────────────────────────────────────

BASE_SCORES = {
    "love":      5.0,
    "career":    5.0,
    "wealth":    5.0,
    "mental":    5.0,
    "health":    5.0,
    "spiritual": 5.0,
}

HAND_TYPE_MODIFIERS = {
    "earth": {"health": +2, "career": +2, "wealth": +1, "love": 0,  "mental": -1, "spiritual": -1},
    "air":   {"mental": +2, "career": +1, "wealth": +1, "love": +1, "health": -1, "spiritual": +1},
    "water": {"love":   +2, "spiritual": +2, "mental": +1, "health": 0, "career": 0, "wealth": -1},
    "fire":  {"career": +2, "wealth": +2, "health": +1, "love": 0, "mental": 0, "spiritual": -1},
}


# ─── Rule Engine ──────────────────────────────────────────────────────────────

def interpret_heart_line(f: dict) -> Optional[dict]:
    if not f.get("present"):
        return {"type": "Not clearly visible",
                "meaning": "The Heart Line is faint or absent. Emotional patterns are internalized and private.",
                "vedic_note": "Inward Shukra — love expressed through silence and devotion rather than words.",
                "scores": {}}

    tort  = f.get("tortuosity", 1.0)
    forks = f.get("forks", 0)
    breaks= f.get("breaks", 0)
    length= f.get("length_pct", 0)

    if breaks >= 2:
        return {"type": "Broken (Multiple)",
                "meaning": "Significant emotional transformations. Each break marks a rebirth of the heart — from attachment to wisdom.",
                "vedic_note": "Rahu-Ketu axis — deep karmic clearing through relationships. The lotus rises from mud.",
                "scores": {"spiritual": +2, "love": -1, "mental": -1}}

    if forks >= 2:
        return {"type": "Forked (Multiple Branches)",
                "meaning": "Exceptional emotional intelligence. You hold multiple emotional truths simultaneously — empath, counselor, bridge-builder.",
                "vedic_note": "Budha-Chandra-Shukra yoga — rare triple harmony of mind, heart, and love.",
                "scores": {"love": +2, "mental": +2, "spiritual": +1}}

    if forks == 1:
        return {"type": "Forked at End",
                "meaning": "You balance heart and mind beautifully. Practical empathy — you feel deeply and respond wisely.",
                "vedic_note": "Budha-Chandra yoga — mind and heart in dialogue.",
                "scores": {"love": +2, "mental": +2}}

    if length > 70 and tort > 1.15:
        return {"type": "Long & Deeply Curved",
                "meaning": "Deeply romantic, emotionally generous, warm-hearted. Relationships are your primary dharma in this lifetime.",
                "vedic_note": "Strong Shukra (Venus) — Anahata Chakra open and radiant. Unconditional love is your gift.",
                "scores": {"love": +3, "spiritual": +1, "health": +1}}

    if length > 70 and tort <= 1.10:
        return {"type": "Long & Straight",
                "meaning": "Loyal, steadfast in love. Expresses affection through consistent action — not grand gestures. The most reliable partner.",
                "vedic_note": "Balanced Chandra — disciplined heart. Love as duty (Dharma) more than passion.",
                "scores": {"love": +1, "career": +1, "mental": +1}}

    if length < 40:
        return {"type": "Short",
                "meaning": "Self-contained emotionally. Deep but private. Prefers a small circle of deep bonds over wide social networks.",
                "vedic_note": "Shani influence on Shukra — introversion and selectivity in love.",
                "scores": {"love": 0, "mental": +1, "spiritual": +1}}

    if breaks == 1:
        return {"type": "Broken",
                "meaning": "One major emotional transformation. A heartbreak that became your greatest teacher. Wisdom born from love's pain.",
                "vedic_note": "Rahu/Ketu marker — karmic relationship lesson that reshapes the soul.",
                "scores": {"spiritual": +2, "love": -1}}

    return {"type": "Clear & Moderate",
            "meaning": "Emotionally balanced and open. Healthy relationships, moderate expressiveness, genuine warmth.",
            "vedic_note": "Shukra in balance — healthy Kama (desire) without excess.",
            "scores": {"love": +1}}


def interpret_head_line(f: dict) -> Optional[dict]:
    if not f.get("present"):
        return {"type": "Not clearly visible", "meaning": "Head Line is faint — intuitive rather than analytical thinking style.",
                "vedic_note": "Ketu influence on Buddhi — trans-rational wisdom.", "scores": {}}

    tort  = f.get("tortuosity", 1.0)
    forks = f.get("forks", 0)
    length= f.get("length_pct", 0)
    depth = f.get("depth_normalized", 0.4)

    if forks >= 1 and length > 50:
        return {"type": "Writer's Fork",
                "meaning": "Can hold analytical and creative intelligence simultaneously. Exceptional in communication, law, literature, philosophy. Rare gift.",
                "vedic_note": "Saraswati's dual blessing — Vak Shakti (eloquence) + Viveka (discernment).",
                "scores": {"mental": +3, "career": +2, "wealth": +1}}

    if length > 75 and tort < 1.08:
        return {"type": "Long & Straight",
                "meaning": "Razor-sharp analytical mind. Excels at logic, mathematics, science, law, strategy. Sees patterns instantly.",
                "vedic_note": "Budha strong — Saraswati's grace on the analytical faculty.",
                "scores": {"mental": +3, "career": +2, "wealth": +1}}

    if tort > 1.20 and depth > 0.55:
        return {"type": "Deeply Curved Downward",
                "meaning": "Highly creative and imaginative. Thinks in images, metaphors, stories. Writer, artist, mystic, dreamer.",
                "vedic_note": "Chandra dominance on Buddhi — the moon illuminates the mind from within.",
                "scores": {"mental": +2, "spiritual": +2, "career": +1}}

    if length < 35:
        return {"type": "Short",
                "meaning": "Decisive and instinct-driven. Trusts gut over analysis. Quick thinker, entrepreneur, natural leader under pressure.",
                "vedic_note": "Mangal (Mars) influence — courage of thought. The instinct IS the intelligence.",
                "scores": {"career": +2, "mental": +1, "wealth": +1}}

    if tort > 1.12:
        return {"type": "Gently Curved",
                "meaning": "Beautiful balance of analytical and creative thinking. Adaptable intelligence — can switch modes as needed.",
                "vedic_note": "Budha-Chandra balance — rare harmony of left and right brain.",
                "scores": {"mental": +2, "career": +1, "love": +1}}

    return {"type": "Clear & Balanced",
            "meaning": "Steady, reliable intellect. Good problem-solver, practical thinker. Consistent mental performance.",
            "vedic_note": "Budha in balance — clear and dependable Buddhi (intellect).",
            "scores": {"mental": +1, "career": +1}}


def interpret_life_line(f: dict) -> Optional[dict]:
    if not f.get("present"):
        return {"type": "Not visible", "meaning": "Life Line not detected — strong inner vitality that defies conventional mapping.",
                "vedic_note": "Atma-force beyond form.", "scores": {}}

    tort  = f.get("tortuosity", 1.0)
    length= f.get("length_pct", 0)
    breaks= f.get("breaks", 0)

    # Check for double life line (very high pixel count for similar length)
    thickness = f.get("thickness_px", 0)
    if thickness > 8 and length > 50:
        return {"type": "Double Life Line (Kavach Rekha)",
                "meaning": "Extraordinary and rare. Divine protection follows you. Ancestors or Ishta Devata are actively guiding your path. Remarkable resilience from any difficulty.",
                "vedic_note": "Kavach Rekha — the most auspicious mark. Pitru Kavach (ancestral protection) confirmed.",
                "scores": {"health": +3, "spiritual": +3, "career": +1, "wealth": +1}}

    if breaks >= 2:
        return {"type": "Multiple Breaks",
                "meaning": "Multiple major life pivots. Each break is a phoenix moment — destruction that precedes transformation. You carry the wisdom of several complete life chapters.",
                "vedic_note": "Rahu-Ketu axis repeated — multiple divine course corrections. Each break serves your highest destiny.",
                "scores": {"spiritual": +3, "health": -1, "career": -1}}

    if length > 65 and tort > 1.10:
        return {"type": "Long & Wide Arc",
                "meaning": "Abundant Prana (life force). Robust health, great physical vitality, natural love of life. You recover quickly from illness or setbacks.",
                "vedic_note": "Surya-Mangal blessings — solar vitality and warrior constitution. Strong Ojas (immunity).",
                "scores": {"health": +3, "career": +2, "wealth": +1}}

    if length > 65 and tort <= 1.10:
        return {"type": "Long & Close to Thumb",
                "meaning": "Sustained vitality but conserved energy. You pace yourself well. Quality over quantity in all areas of life — deep focus rather than broad expansion.",
                "vedic_note": "Shani influence on Prana — the monk archetype. Energy used with precision.",
                "scores": {"health": +2, "mental": +2, "spiritual": +2}}

    if breaks == 1:
        return {"type": "One Break",
                "meaning": "One major life transformation — could be health, relocation, career pivot, or spiritual awakening. This break marks your before/after. Everything changes at that point.",
                "vedic_note": "Rahu transit — radical divine redirection. The snake sheds its skin.",
                "scores": {"spiritual": +2, "health": -1}}

    if length < 40:
        return {"type": "Short",
                "meaning": "NOT about lifespan — about independence. Self-reliant, resilient, a true survivor. You rely on inner resources, not outer support.",
                "vedic_note": "Ketu marker — not bound by conventional life patterns. Soul is free.",
                "scores": {"mental": +2, "spiritual": +2}}

    return {"type": "Clear & Moderate",
            "meaning": "Steady vital energy. Good health baseline, consistent physical endurance.",
            "vedic_note": "Prana in balance — Dhanvantari's grace on health.",
            "scores": {"health": +2}}


def interpret_fate_line(f: dict) -> Optional[dict]:
    if not f.get("present"):
        return {"type": "Absent (Free Will Path)",
                "meaning": "No fate line — complete free will. You are not bound by predetermined destiny. Self-authored life. Some of the greatest self-made individuals have no fate line.",
                "vedic_note": "Moksha marker — free from Prarabdha karma's direct grip. Pure Kriyamana (self-created) karma.",
                "scores": {"spiritual": +2, "career": +1}}

    breaks = f.get("breaks", 0)
    length = f.get("length_pct", 0)
    depth  = f.get("depth_normalized", 0.5)

    if breaks >= 2:
        return {"type": "Multiple Career Pivots",
                "meaning": "Several distinct career phases — each complete in itself. Not inconsistency, but evolution. Multi-chapter professional life.",
                "vedic_note": "Rahu-Shani interaction — disruption before breakthrough. Each pivot serves the ultimate dharma.",
                "scores": {"career": +1, "spiritual": +2}}

    if length > 60 and breaks == 0:
        return {"type": "Deep & Unbroken",
                "meaning": "Powerful dharmic purpose from early in life. Career feels like a calling. Success through disciplined, sustained effort over decades.",
                "vedic_note": "Shani yoga — disciplined effort magnificently rewarded. Prarabdha karma is clear and supportive.",
                "scores": {"career": +3, "wealth": +2, "mental": +1}}

    if depth > 0.65:   # starts late (high row = lower on palm = starts from bottom)
        return {"type": "Starts Late",
                "meaning": "Late bloomer — career clarity and success come after 35. Building invisible foundations others cannot see. Greatest achievements still ahead.",
                "vedic_note": "Shani dasha activation — Saturn's deepest gifts come after his trials. Patience rewarded.",
                "scores": {"career": +2, "wealth": +1, "spiritual": +1}}

    if breaks == 1:
        return {"type": "One Career Break",
                "meaning": "One major professional reinvention. Before and after this break you are different people professionally — both valuable.",
                "vedic_note": "Rahu transit on Shani — disruption that ultimately serves greater purpose.",
                "scores": {"career": +1, "spiritual": +1}}

    return {"type": "Present & Active",
            "meaning": "Clear life direction and career purpose. Destiny is guiding you forward.",
            "vedic_note": "Shani's blessing — karma and dharma aligned.",
            "scores": {"career": +2, "wealth": +1}}


def interpret_sun_line(f: dict) -> Optional[dict]:
    if not f.get("present"):
        return {"type": "Absent",
                "meaning": "Success without public fame. A private achiever who prefers substance over recognition. Inner light over outer spotlight.",
                "vedic_note": "Inner Surya — spiritual achievement over worldly recognition.",
                "scores": {"spiritual": +1}}

    length = f.get("length_pct", 0)
    forks  = f.get("forks", 0)

    if length > 40 and forks == 0:
        return {"type": "Clear & Strong",
                "meaning": "Public recognition, fame, and creative success are in your destiny. Natural charisma — people notice and remember you.",
                "vedic_note": "Surya strong — Kirti (fame) as the result of dharmic action in past lives.",
                "scores": {"career": +2, "wealth": +1, "love": +1}}

    if forks >= 1:
        return {"type": "Branched (Multiple Talents)",
                "meaning": "Gifted in multiple fields — but scattered energy. Choose your primary creative path to crystallize recognition.",
                "vedic_note": "Surya dispersed — needs single Dharmic focus to shine at full power.",
                "scores": {"career": +1, "mental": +1}}

    return {"type": "Faint / Developing",
            "meaning": "Fame and recognition are building slowly. Recognition comes later in life — and when it does, it lasts.",
            "vedic_note": "Delayed Surya grace — permanent and deeply earned.",
            "scores": {"career": +1}}


# ─── Main Interpretation Function ─────────────────────────────────────────────

def interpret_palm(features: dict, hand_type: str = "unknown") -> dict:
    """
    Apply all rules to extracted features.
    Returns full interpretation dict with life scores.
    """
    scores = dict(BASE_SCORES)

    # Apply hand type modifier
    ht = hand_type.lower()
    if ht in HAND_TYPE_MODIFIERS:
        for k, v in HAND_TYPE_MODIFIERS[ht].items():
            scores[k] += v

    line_features = {
        "heart":   features.get("heart_line",  {}),
        "head":    features.get("head_line",   {}),
        "life":    features.get("life_line",   {}),
        "fate":    features.get("fate_line",   {}),
        "sun":     features.get("sun_line",    {}),
    }

    interpreters = {
        "heart": interpret_heart_line,
        "head":  interpret_head_line,
        "life":  interpret_life_line,
        "fate":  interpret_fate_line,
        "sun":   interpret_sun_line,
    }

    interpretations = {}
    for key, func in interpreters.items():
        result = func(line_features[key])
        if result:
            # Apply score modifiers
            for score_key, val in result.get("scores", {}).items():
                if score_key in scores:
                    scores[score_key] += val
            interpretations[key] = {
                "line":      key.replace("_", " ").title(),
                "type":      result["type"],
                "meaning":   result["meaning"],
                "vedic_note":result["vedic_note"],
            }

    # Clamp scores 1–10
    life_scores = {k: round(min(10.0, max(1.0, v)), 1) for k, v in scores.items()}

    # Strengths = high-scoring areas (≥7)
    strengths = [k for k, v in life_scores.items() if v >= 7]
    challenges = [k for k, v in life_scores.items() if v <= 4]

    return {
        "hand_type":      hand_type,
        "interpretations": interpretations,
        "life_scores":    life_scores,
        "strengths":      strengths,
        "challenges":     challenges,
    }


def build_qwen_prompt(result: dict, features: dict) -> str:
    """Build prompt for Qwen LLM narrative generation."""
    lines_text = "\n".join([
        f"- {v['line']}: {v['type']} — {v['meaning']}"
        for v in result["interpretations"].values()
    ])
    scores_text = ", ".join([f"{k}: {v}/10" for k, v in result["life_scores"].items()])

    return f"""You are a master of Hasta Samudrika Shastra — the ancient Vedic science of palm reading.
Speak with warmth, spiritual depth, and wisdom. This reading is for entertainment and educational purposes.

HAND TYPE: {result['hand_type'].title() if result['hand_type'] != 'unknown' else 'Not determined'}

PALM LINE ANALYSIS (detected by computer vision):
{lines_text}

LIFE AREA SCORES: {scores_text}

STRONGEST AREAS: {', '.join(result['strengths']) or 'Balanced across all areas'}
GROWTH AREAS: {', '.join(result['challenges']) or 'No major challenges detected'}

Write a 200–250 word personalized palm reading:
1. One opening statement about their overall karmic path based on the hand type and dominant lines
2. Specific insights about their 2 strongest detected lines
3. One growth area with a specific Vedic remedy (mantra, practice, or gemstone)
4. Close with one Sanskrit shloka or phrase that captures their soul's journey

Use second person (you/your). Be specific — not generic. Speak as a wise, compassionate guru.
End with: "This reading is based on Hasta Samudrika Shastra tradition — for educational and spiritual reflection."
"""


if __name__ == "__main__":
    # Demo with sample features
    sample_features = {
        "heart_line": {"present": True, "length_pct": 72.0, "tortuosity": 1.18, "forks": 1, "breaks": 0, "confidence": 0.7},
        "head_line":  {"present": True, "length_pct": 68.0, "tortuosity": 1.06, "forks": 0, "breaks": 0, "confidence": 0.6},
        "life_line":  {"present": True, "length_pct": 58.0, "tortuosity": 1.12, "forks": 0, "breaks": 0, "confidence": 0.8},
        "fate_line":  {"present": True, "length_pct": 45.0, "tortuosity": 1.03, "forks": 0, "breaks": 0, "confidence": 0.5},
        "sun_line":   {"present": False},
    }

    result = interpret_palm(sample_features, hand_type="water")
    print(json.dumps(result, indent=2))
    print("\n── Qwen Prompt ──")
    print(build_qwen_prompt(result, sample_features))
