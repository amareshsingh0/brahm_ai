"""
Prompt Builder — Pass 2 of two-pass reasoning.
Builds the final answer prompt using:
  - Pass 1 decision (intent, query_type, user_language_style)
  - Tool execution results (kundali, dasha, gochar, muhurta)
  - Kundali compressed summary
  - Page context + data
  - RAG docs (only when needed)

4-Layer Analysis for predictions:
  Layer 1: KUNDALI   — natal potential (what's possible)
  Layer 2: DASHA     — current timing (is this period favorable?)
  Layer 3: GOCHAR    — current transits (is the sky supporting it now?)
  Layer 4: MUHURTA   — best moment to act (specific date/time)
"""
import json
from typing import List, Dict, Optional

MASTER_PERSONA = """Tum Brahm AI ho — ek sampurna Vedic jyotishi aur aadhunik calculator ka sangam.

Tumhare paas yeh sab available hai:
- User ka kundali data (lagna, planets, dasha)
- Fresh gochar (current planetary transits) — aaj ke planet positions natal chart ke upar
- Fresh calculation results (jo bhi calculate kiya)
- Page ka current data (jo user dekh raha hai)
- Vedic books ka context (sirf jab explicitly poocha)

Jawab dene ka 4-layer structure (CHART_ANALYSIS / RECOMMENDATION ke liye):
1. KUNDALI — natal chart kya kehta hai (potential, yogas, relevant houses)
2. DASHA   — current dasha period kaisa hai (favorable/unfavorable, timeline)
3. GOCHAR  — current transits ka support hai ya nahi (Guru/Shani/Rahu position)
4. MUHURTA — agar action lena ho toh best time kab (sirf agar relevant ho)

General style rules (hamesha):
- User ke EXACT language style mein jawab do (niche dekho)
- Generic mat bolo — "Shani aapke 7th house mein hai" jaise SPECIFIC bolo
- Sanskrit terms use karo lekin TURANT same line mein explain karo
- Compassionate lekin honest — false hope se bura kuch nahi jyotish mein
- Max 400 words — jab tak user ne detail na manga ho
- Kabhi false prediction nahi — agar data nahi toh honestly kaho
- Kabhi "Aryabhata ki tarah" ya "Varahmihira ki tarah" mat kaho — bas KHUD bolo"""

# Language style instructions — match user's exact writing style
LANG_STYLE = {
    "pure_hindi":   "पूरी तरह हिंदी में जवाब दो (देवनागरी)। कोई English words नहीं।",
    "pure_english": "Answer entirely in English. No Hindi words.",
    "hinglish":     (
        "Hinglish mein jawab do — jaise user likh raha hai. "
        "Hindi words Roman mein likhna (e.g. 'aapka', 'hoga', 'karo'). "
        "Technical terms English mein theek hai (e.g. 'transit', 'dasha'). "
        "Formal 'Namaste' se mat shuru karo."
    ),
}

# Fallback for response_lang if style not detected
LANG_FALLBACK = {
    "hi": LANG_STYLE["hinglish"],
    "en": LANG_STYLE["pure_english"],
    "sa": "संस्कृतभाषायाम् उत्तरं देहि।",
}


def _get_lang_instruction(decision: dict) -> str:
    style = decision.get("user_language_style", "")
    if style in LANG_STYLE:
        return LANG_STYLE[style]
    lang = decision.get("response_lang", "hi")
    return LANG_FALLBACK.get(lang, LANG_STYLE["hinglish"])


def _format_kundali(summary: dict) -> str:
    if not summary:
        return ""
    lines = ["[LAYER 1 — KUNDALI: Natal Chart]"]
    lines.append(f"  Lagna: {summary.get('lagna','?')} ({summary.get('lagna_degree','?')}°)")
    lines.append(f"  Moon: {summary.get('moon_rashi','?')} — {summary.get('moon_house','?')}H")
    lines.append(f"  Sun: {summary.get('sun_rashi','?')}")
    lines.append(f"  Current Dasha: {summary.get('current_dasha','?')}")
    lines.append(f"  Next Dasha: {summary.get('next_dasha','?')}")
    if summary.get("planets"):
        lines.append("  Planets:")
        for name, pos in summary["planets"].items():
            lines.append(f"    {name}: {pos}")
    if summary.get("yogas"):
        lines.append(f"  Yogas: {', '.join(summary['yogas'])}")
    return "\n".join(lines)


def _format_gochar(gochar: dict) -> str:
    if not gochar:
        return ""
    lines = ["[LAYER 3 — GOCHAR: Current Transits Today]"]
    lines.append(f"  Key planets: {gochar.get('summary','?')}")
    lines.append("  Current positions (natal house mein):")
    for planet, pos in gochar.get("current_positions", {}).items():
        lines.append(f"    {planet}: {pos}")
    if gochar.get("opportunities"):
        lines.append("  Opportunities (gochar se):")
        for o in gochar["opportunities"]:
            lines.append(f"    + {o}")
    if gochar.get("cautions"):
        lines.append("  Cautions (gochar se):")
        for c in gochar["cautions"]:
            lines.append(f"    ! {c}")
    if gochar.get("sade_sati"):
        lines.append("  *** SADE SATI ACTIVE ***")
    return "\n".join(lines)


def _format_dasha(dasha: dict) -> str:
    if not dasha:
        return ""
    lines = ["[LAYER 2 — DASHA: Current Timing]"]
    cur = dasha.get("current", {})
    if cur:
        lines.append(
            f"  Current Mahadasha: {cur.get('lord','?')} "
            f"({cur.get('start','?')} to {cur.get('end','?')})"
        )
    upcoming = dasha.get("upcoming", [])
    if upcoming:
        lines.append("  Upcoming dashas:")
        for d in upcoming[:3]:
            lines.append(f"    {d.get('lord','?')}: {d.get('start','?')} – {d.get('end','?')}")
    return "\n".join(lines)


def _format_muhurta(muhurta) -> str:
    if not muhurta:
        return ""
    lines = ["[LAYER 4 — MUHURTA: Best Time to Act]"]
    if isinstance(muhurta, dict):
        for k, v in muhurta.items():
            if v:
                lines.append(f"  {k}: {v}")
    else:
        lines.append(f"  {muhurta}")
    return "\n".join(lines)


def _format_tool_results(results: dict) -> str:
    """Format all tool results with 4-layer structure."""
    if not results:
        return ""
    parts = []

    # Layer 2 — Dasha (separate from kundali summary)
    if "dasha" in results and not results.get("dasha_error"):
        s = _format_dasha(results["dasha"])
        if s:
            parts.append(s)

    # Layer 3 — Gochar
    if "gochar" in results and not results.get("gochar_error"):
        s = _format_gochar(results["gochar"])
        if s:
            parts.append(s)

    # Layer 4 — Muhurta
    if "muhurta" in results and not results.get("muhurta_error"):
        s = _format_muhurta(results["muhurta"])
        if s:
            parts.append(s)

    # Panchang
    if "panchang" in results and not results.get("panchang_error"):
        p = results["panchang"]
        if isinstance(p, dict):
            lines = ["[PANCHANG: Aaj ka]"]
            for k in ["tithi", "nakshatra", "yoga", "karana", "vara", "rahukaal"]:
                if p.get(k):
                    lines.append(f"  {k}: {p[k]}")
            parts.append("\n".join(lines))

    return "\n\n".join(parts)


def _format_page_data(page_context: str, page_data: dict) -> str:
    if not page_data:
        return ""

    if page_context == "compatibility":
        kutas    = page_data.get("kutas", {})
        kuta_str = "\n".join([f"    {k}: {v}" for k, v in kutas.items()]) if kutas else ""
        doshas   = []
        if page_data.get("nadi_dosha"):   doshas.append("Nadi Dosha")
        if page_data.get("gana_mismatch"): doshas.append("Gana Mismatch")
        return f"""[Compatibility Report — jo user dekh raha hai]
  Score: {page_data.get('total_score','?')}/{page_data.get('out_of',36)}
  Verdict: {page_data.get('verdict','?')}
  Doshas: {', '.join(doshas) if doshas else 'Koi major dosha nahi'}
  Kuta Scores:
{kuta_str}
  Strong: {', '.join(page_data.get('strong_areas', []))}
  Weak: {', '.join(page_data.get('weak_areas', []))}"""

    elif page_context == "panchang":
        return f"""[Aaj ka Panchang — jo user dekh raha hai]
  Tithi: {page_data.get('tithi','?')}
  Nakshatra: {page_data.get('nakshatra','?')}
  Yoga: {page_data.get('yoga','?')}
  Karana: {page_data.get('karana','?')}
  Vara: {page_data.get('vara','?')}
  Rahukaal: {page_data.get('rahukaal','?')}
  Brahma Muhurta: {page_data.get('brahma_muhurta','?')}"""

    elif page_context == "sky":
        planets = page_data.get("planets", {})
        planet_lines = "\n".join([f"  {k}: {v}" for k, v in planets.items()]) if planets else ""
        return f"[Live Sky Data]\n{planet_lines}"

    else:
        try:
            return f"[Page Data — {page_context}]\n  {json.dumps(page_data, ensure_ascii=False)[:600]}"
        except Exception:
            return ""


def _format_rag_docs(docs: list) -> str:
    if not docs:
        return ""
    lines = ["[Vedic Texts — Reference]"]
    for i, d in enumerate(docs[:3], 1):
        src  = d.get("source", "?")
        text = d.get("text", "")[:600].strip()
        lines.append(f"\n  [Source {i}: {src}]\n  {text}")
    return "\n".join(lines)


def build_pass2_prompt(
    query: str,
    decision: dict,
    tool_results: dict,
    kundali_summary: Optional[dict],
    page_context: str,
    page_data: dict,
    rag_docs: list,
    language: str = "hi",
) -> str:
    """Build the final answer prompt for Pass 2."""

    query_type  = decision.get("query_type", "DEEP_VEDIC")
    intent      = decision.get("intent", "")
    lang_line   = _get_lang_instruction(decision)

    # Auto-analysis trigger
    if query == "__auto_analyze__":
        if page_context == "compatibility":
            actual_query = (
                "Is compatibility report ka complete analysis karo — "
                "score ka matlab, har dosha ki explanation, strong/weak areas ka impact, "
                "upay, aur vivah ke liye practical guidance."
            )
        elif page_context == "panchang":
            actual_query = (
                "Aaj ke panchang ka deep analysis karo — "
                "tithi, nakshatra, yoga ka mahatva aur aaj ke liye practical guidance."
            )
        else:
            actual_query = "Is report ka detailed analysis karo aur key insights do."
    else:
        actual_query = query

    # ── Conversational: short, warm, no data needed ──────────────────────────
    if query_type == "CONVERSATIONAL":
        return f"""{MASTER_PERSONA}

{lang_line}

User: {actual_query}

Short, warm jawab do — 1-2 sentences. Koi calculations ya books mat use karo."""

    # ── Build all data sections ───────────────────────────────────────────────
    kundali_section = _format_kundali(kundali_summary)
    calc_section    = _format_tool_results(tool_results)
    page_section    = _format_page_data(page_context, page_data)
    rag_section     = _format_rag_docs(rag_docs)

    sections = [MASTER_PERSONA, "", lang_line, ""]

    # Layer 1 — Kundali (always first if available)
    if kundali_section:
        sections.append(kundali_section)
        sections.append("")

    # Layers 2-4 — Calculation results (dasha, gochar, muhurta)
    if calc_section:
        sections.append(calc_section)
        sections.append("")

    # Page data (what user is currently viewing)
    if page_section:
        sections.append(page_section)
        sections.append("")

    # RAG docs (only for DEEP_VEDIC queries)
    if rag_section:
        sections.append(rag_section)
        sections.append("")

    sections.append(f"User ne poocha: {actual_query}")
    sections.append(f"Intent: {intent}")
    sections.append("")

    # Depth-specific instruction
    depth = decision.get("response_depth", "deep")
    if depth == "master":
        has_gochar = "gochar" in tool_results and not tool_results.get("gochar_error")
        has_dasha  = "dasha"  in tool_results and not tool_results.get("dasha_error")
        layers_available = []
        if kundali_summary:  layers_available.append("Kundali (Layer 1)")
        if has_dasha:        layers_available.append("Dasha (Layer 2)")
        if has_gochar:       layers_available.append("Gochar (Layer 3)")
        if "muhurta" in tool_results: layers_available.append("Muhurta (Layer 4)")

        sections.append(
            f"Available data: {', '.join(layers_available) if layers_available else 'general knowledge'}. "
            "Sab layers use karo jo available hain. "
            "Specific, insightful, aur actionable jawab do. "
            "Agar gochar + dasha dono favorable hain → HIGH confidence bolo. "
            "Agar sirf natal chart hai → MEDIUM confidence bolo aur honestly batao."
        )
    elif depth == "basic":
        sections.append("Short, clear, helpful jawab do. 2-3 sentences maximum.")
    else:
        sections.append("Detailed lekin focused jawab do. Unnecessary padding avoid karo.")

    return "\n".join(sections)
