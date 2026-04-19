"""
Prompt Builder — Pass 2 of two-pass reasoning.
Builds the final answer prompt using:
  - Pass 1 decision (intent, query_type, user_language_style)
  - Tool execution results (kundali, dasha, gochar, muhurta)
  - Kundali compressed summary
  - Page context + data
  - RAG docs (only when needed)
  - Conversation history (for follow-up context)

4-Layer Analysis for predictions:
  Layer 1: KUNDALI   — natal potential (what's possible)
  Layer 2: DASHA     — current timing (is this period favorable?)
  Layer 3: GOCHAR    — current transits (is the sky supporting it now?)
  Layer 4: MUHURTA   — best moment to act (specific date/time)
"""
import json
from datetime import datetime, timezone, timedelta
from typing import List, Dict, Optional

MASTER_PERSONA = """You are Brahm AI — a warm, wise Vedic astrology guide who talks like a trusted friend.

GOLDEN RULE — MEANING FIRST, NEVER DATA DUMP:
You are talking to a NORMAL PERSON, not an astrologer.
They don't care about "Budh in 10th house" or "Shukra in 9th house".
They care about: Will my career grow? When will I get married? Will I be rich?

ALWAYS lead with what it MEANS for their life.
Technical terms (planet names, house numbers) go in parentheses ONLY as brief supporting evidence.

GOOD example:
  "Your career is set to grow strongly in the next 2 years.
   You have natural leadership ability and will likely rise to a senior position.
   (Jupiter supports your career house, and your current dasha period favors professional growth.)"

BAD example (NEVER do this):
  "Guru aapke 10th house mein hai. Shukra 9th mein hai.
   Budh 7th house mein baith raha hai. Mangal 4th mein hai.
   Shani aapke 2nd house mein transit kar raha hai."

STRICT RULES:
1. NEVER start a sentence with a planet name or house number
2. NEVER list planet positions one after another
3. First say the IMPACT on their life, then briefly mention the reason in parentheses
4. Talk like you're explaining to your friend over chai — simple, warm, direct
5. Use "you/aap/tum" language — make it personal, not textbook

You have access to user's kundali data, gochar (transits), dasha, calculation results,
page data, Vedic book references, and past conversation memory.

For predictions, internally use all 4 layers (Kundali, Dasha, Gochar, Muhurta)
but DO NOT write these as separate labeled sections. Weave them naturally into your answer.

VARGA CHARTS — use the right divisional chart for the topic:
- Career: D-10, Marriage: D-9, Children: D-7, Education: D-24, Spirituality: D-20
- Health: D-6, Property: D-4, Wealth: D-2, Past karma: D-60
Use varga data internally but explain findings in simple human terms.

Style rules:
- Reply in the EXACT language the user writes (Hindi/English/Hinglish)
- Be compassionate but honest — false hope is worse than truth
- Max 300 words for most answers, up to 400 for deep analysis
- Never make up predictions if data is missing — say so honestly

OUTPUT FORMAT:
- Each thought on its OWN line with a blank line between
- NEVER put 2 sentences in one paragraph
- Headings: ## (only for 3+ section answers, use natural titles like "Career Outlook" not "LAYER 1 KUNDALI")
- Bold: **word** (key insights, not planet names)
- Bullets: - item (for 3+ related points)
- Quote: > text (one memorable insight per response)
- 💡 (ONE per response — most important takeaway)
- NEVER write walls of text

NEVER respond blank. No kundali data: use general Vedic wisdom.
Off-topic question: give a helpful answer, connect to Vedic angle if natural.
Unclear question: ask "Could you share a bit more detail?"
ALWAYS give something helpful — silence or error is never acceptable"""

# Language style instructions — mirror the user's exact writing style
LANG_STYLE = {
    "pure_hindi":   "पूरी तरह हिंदी में जवाब दो (देवनागरी)। कोई English words नहीं।",
    "pure_english": "Answer entirely in English. No Hindi or Roman-Hindi words.",
    "hinglish":     (
        "Reply in Hinglish — exactly as the user writes. "
        "Hindi words in Roman script (e.g. 'aapka', 'hoga', 'karo'). "
        "Technical terms in English are fine (e.g. 'transit', 'dasha'). "
        "Do NOT start with a formal 'Namaste'."
    ),
}

# Fallback for response_lang if style not detected — default English
LANG_FALLBACK = {
    "en": LANG_STYLE["pure_english"],
    "hi": LANG_STYLE["hinglish"],
    "sa": "संस्कृतभाषायाम् उत्तरं देहि।",
}


def _get_lang_instruction(decision: dict) -> str:
    style = decision.get("user_language_style", "")
    if style in LANG_STYLE:
        return LANG_STYLE[style]
    lang = decision.get("response_lang", "en")
    return LANG_FALLBACK.get(lang, LANG_STYLE["pure_english"])


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
    # Navamsha (D-9) — Soul, marriage, dharma potential
    if summary.get("navamsha_lagna") and summary["navamsha_lagna"] != "?":
        lines.append(f"  D-9 Lagna: {summary['navamsha_lagna']}")
    if summary.get("navamsha"):
        nav_parts = [f"{n}: {v}" for n, v in summary["navamsha"].items()]
        lines.append(f"  D-9 Planets: {', '.join(nav_parts)}")
    # Other Varga charts (D-2 through D-60)
    if summary.get("varga_charts"):
        for div_key, vc in summary["varga_charts"].items():
            lines.append(f"  {div_key} — {vc.get('name', div_key)}: Lagna={vc.get('lagna','?')}")
            if vc.get("planets"):
                lines.append(f"    Planets: {', '.join(vc['planets'])}")
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

    # Marriage (Vivah) analysis
    if "marriage" in results and not results.get("marriage_error"):
        m = results["marriage"]
        if isinstance(m, dict):
            lines = ["[VIVAH ANALYSIS — Classical Jyotish]"]
            lines.append(f"  Overall probability: {m.get('overall_probability','?')}")
            lines.append(f"  Current dasha: {m.get('current_dasha','?')}")
            lines.append(f"  Favorable windows: {m.get('favorable_windows','?')}")
            lines.append(f"  Next strong window: {m.get('next_strong_window','?')}")
            lines.append(f"  Estimated age range: {m.get('estimated_age_range','?')}")
            lines.append(f"  7th house: {m.get('7th_house','?')} | 7th lord: {m.get('7th_lord','?')}")
            if m.get("spouse_traits"):
                lines.append(f"  Spouse traits: {m['spouse_traits']}")
            if m.get("profession_hint"):
                lines.append(f"  Profession hint: {m['profession_hint']}")
            if m.get("active_yogas"):
                lines.append(f"  Active yogas: {m['active_yogas']}")
            if m.get("delay_factors"):
                lines.append(f"  Delay factors: {m['delay_factors']}")
            if m.get("karaka_strength"):
                lines.append(f"  Karaka: {m['karaka_strength']}")
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

    elif page_context == "marriage":
        result = page_data.get("result", page_data)
        timing  = result.get("marriage_timing", {})
        spouse  = result.get("spouse_profile", {})
        yogas   = result.get("marriage_yogas", [])
        delays  = result.get("delay_factors", [])
        current = timing.get("current_period") or {}
        favs    = timing.get("favorable_windows", [])
        present_yogas = [y["name"] for y in yogas if y.get("present")]
        delay_names   = [d["factor"] for d in delays]
        fav_str = "; ".join([f"{w.get('period','?')} ({w.get('probability','?')})" for w in favs[:3]])
        return f"""[Marriage Analysis — Vivah Jyotish]
  Overall probability: {timing.get('overall_probability','?')} ({timing.get('overall_probability_pct','?')}%)
  Estimated age range: {timing.get('estimated_age_range','?')}
  Current dasha: {current.get('period','?')} (score {current.get('score','?')})
  Favorable windows: {fav_str or 'None identified'}
  Next strong window: {timing.get('next_strong_window','?')}
  7th house sign: {spouse.get('7th_house_sign_en', spouse.get('7th_house_sign','?'))}
  7th lord: {spouse.get('7th_lord','?')} in house {spouse.get('7th_lord_house','?')}
  Spouse traits: {', '.join(spouse.get('traits',[])[:4])}
  Active yogas: {', '.join(present_yogas) or 'None'}
  Delay factors: {', '.join(delay_names) or 'None'}"""

    elif page_context == "kundali":
        # kundali page — kundali_raw is the full chart, already handled via compress_kundali
        # Just note the active tab/chart if present
        active_tab = page_data.get("active_tab", "")
        active_varga = page_data.get("active_varga", "")
        parts = []
        if active_tab:
            parts.append(f"Active tab: {active_tab}")
        if active_varga:
            parts.append(f"Active varga chart: {active_varga}")
        return f"[Kundali Page]\n  {' | '.join(parts)}" if parts else ""

    else:
        try:
            return f"[Page Data — {page_context}]\n  {json.dumps(page_data, ensure_ascii=False)[:600]}"
        except Exception:
            return ""


def _format_user_facts(facts: list) -> str:
    if not facts:
        return ""
    lines = ["[User ke baare mein — unke khud ke bataye facts]"]
    for f in facts[:8]:
        lines.append(f"  • {f}")
    return "\n".join(lines)


def _clean_source_name(source: str) -> str:
    """Same logic as rag_service — avoid circular import by duplicating small helper."""
    import re
    _OVERRIDES = {
        "bhagavadgita": "Bhagavad Gita", "sarit": "SARIT Sanskrit Corpus",
        "gretil": "GRETIL Sanskrit Texts", "suttacentral": "Sutta Central",
        "openphilology": "Open Philology", "wikisource": "Wikisource",
        "hf": "Sanskrit Texts", "brihat_parashara": "Brihat Parashara Hora Shastra",
        "bphs": "Brihat Parashara Hora Shastra", "jataka_parijata": "Jataka Parijata",
        "phaladeepika": "Phala Deepika", "saravali": "Saravali",
        "brihat_jataka": "Brihat Jataka", "mahabharata": "Mahabharata",
        "ramayana": "Ramayana", "wikipedia": "Wikipedia",
    }
    filename = source.split("/")[-1]
    name_raw = re.sub(r"\.(pdf|txt|htm|html|json|xml)$", "", filename, flags=re.I)
    name_raw = re.sub(r"[_-]?\d{4}[a-z]?$", "", name_raw)
    name_lower = name_raw.lower()
    for key, val in _OVERRIDES.items():
        if key in name_lower:
            return val
    folder = source.split("/")[0].lower()
    if folder in _OVERRIDES:
        return _OVERRIDES[folder]
    return re.sub(r"[_\-]+", " ", name_raw).strip().title() or source


def _format_rag_docs(docs: list) -> str:
    if not docs:
        return ""
    lines = ["[Vedic Texts — Reference]"]
    for i, d in enumerate(docs[:3], 1):
        src  = _clean_source_name(d.get("source", "?"))
        text = d.get("text", "")[:600].strip()
        lines.append(f"\n  [Source {i}: {src}]\n  {text}")
    return "\n".join(lines)


def _format_history(history: list) -> str:
    """Format last 6 conversation turns for Pass 2 context."""
    if not history:
        return ""
    relevant = history[-6:]
    lines = ["[Conversation History — last messages]"]
    for msg in relevant:
        role = "User" if msg.get("role") == "user" else "AI"
        content = msg.get("content", "")[:400].strip()
        if content:
            lines.append(f"  {role}: {content}")
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
    history: list = None,
    memory_section: str = "",
) -> str:
    """Build the final answer prompt for Pass 2."""

    query_type  = decision.get("query_type", "DEEP_VEDIC")
    intent      = decision.get("intent", "")
    lang_line   = _get_lang_instruction(decision)
    IST = timezone(timedelta(hours=5, minutes=30))
    today_str   = datetime.now(IST).strftime("%A, %d %B %Y")   # e.g. "Thursday, 26 March 2026"

    actual_query = query

    # ── Conversational / Small Talk: short, warm ─────────────────────────────
    if query_type in {"CONVERSATIONAL", "SMALL_TALK"}:
        history_section = _format_history(history or [])
        hist_block = f"\n{history_section}\n" if history_section else ""
        return f"""{MASTER_PERSONA}

{lang_line}

TODAY = {today_str}
{hist_block}
User: {actual_query}

Conversation history ke context mein jawab do. Ek-do sentence mein warm, friendly jawab do.
Agar user pehle kisi kaam ke baare mein baat kar raha tha (jaise kundali banana, koi question) toh uska reference do.
Koi calculations ya books mat use karo.

[MANDATORY] AKHIR mein:
[FOLLOWUPS: "question 1" | "question 2" | "question 3"]"""

    # ── General Knowledge: off-topic questions ───────────────────────────────
    if query_type == "GENERAL_KNOWLEDGE":
        history_section = _format_history(history or [])
        mem_section = memory_section or ""
        hist_block = f"\n{history_section}\n" if history_section else ""
        mem_block = f"\n{mem_section}\n" if mem_section else ""
        return f"""{MASTER_PERSONA}

{lang_line}

TODAY = {today_str}
{hist_block}{mem_block}
User ne poocha: {actual_query}

Yeh Vedic astrology se seedha related nahi hai — lekin tum helpful ho.
Apne general knowledge se answer do. Agar natural lage toh ek line mein Vedic/karmic angle add karo.
2-4 sentences. Warm tone. Kabhi "mujhe nahi pata" ya "main sirf jyotish jaanta hoon" mat kaho.

[MANDATORY] AKHIR mein:
[FOLLOWUPS: "question 1" | "question 2" | "question 3"]
3 short follow-up questions. User ki language. Max 7 words each."""

    # ── Build all data sections ───────────────────────────────────────────────
    history_section = _format_history(history or [])
    kundali_section = _format_kundali(kundali_summary)
    calc_section    = _format_tool_results(tool_results)
    page_section    = _format_page_data(page_context, page_data)
    rag_section     = _format_rag_docs(rag_docs)
    facts_section   = _format_user_facts(page_data.get("user_facts", []))

    sections = [
        MASTER_PERSONA,
        "",
        lang_line,
        "",
        f"TODAY = {today_str}  ← Yahi real current date hai. Agar user date/time pooche toh YAHI batao.",
        "",
    ]

    # Long-term memory (past relevant conversations) — inject first for context
    if memory_section:
        sections.append(memory_section)
        sections.append("")

    # Conversation history — critical for follow-up questions
    if history_section:
        sections.append(history_section)
        sections.append("")

    # User facts (personal context)
    if facts_section:
        sections.append(facts_section)
        sections.append("")

    # Layer 1 — Kundali
    if kundali_section:
        sections.append(kundali_section)
        sections.append("")

    # Layers 2-4 — Calculation results
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
        if kundali_summary:  layers_available.append("Kundali")
        if has_dasha:        layers_available.append("Dasha")
        if has_gochar:       layers_available.append("Gochar")
        if "muhurta" in tool_results: layers_available.append("Muhurta")
        varga_available = list(kundali_summary.get("varga_charts", {}).keys()) if kundali_summary else []
        varga_note = f" Varga charts available: {', '.join(varga_available)}." if varga_available else ""
        sections.append(
            f"Data available: {', '.join(layers_available) if layers_available else 'general knowledge'}.{varga_note}\n"
            "Use all available data INTERNALLY to form your answer, but DO NOT label sections as 'Kundali Analysis' or 'Dasha Period'.\n"
            "Write like you're talking to a friend — weave all insights into a natural, flowing answer.\n"
            "Start with the MOST IMPORTANT finding that impacts their life.\n"
            "Then explain WHY (briefly mention the astrological reason in parentheses).\n"
            "End with clear, practical advice they can act on.\n"
            "Max 350 words. Keep it warm and personal."
        )
    elif depth == "basic":
        sections.append(
            "STRICT: 2-3 sentences ONLY. Direct, warm answer. No jargon, no planet lists, no preamble."
        )
    else:
        sections.append(
            "Focused, warm jawab do. Meaning pehle, technical reason parentheses mein. Max 200 words. No padding."
        )

    # ── Follow-up loop (MANDATORY — always include) ──────────────────────────
    # These chips drive the infinite exploration experience like ChatGPT/Grok.
    # Rules:
    #  - ALWAYS include this tag — no exceptions, even for simple answers
    #  - 3 questions, each max 7 words, in user's language
    #  - Must be DIFFERENT from what was just asked — move the conversation forward
    #  - Must be SPECIFIC to user's chart/situation, not generic astrology questions
    #  - Vary the topics: timing | relationship | career | health | remedy | deeper analysis
    #  - For chart questions: next logical step after the answer
    #    e.g. asked about marriage → suggest: spouse qualities, children timing, 7th lord strength
    #    e.g. asked about career → suggest: business vs job, income peak years, ideal field
    #    e.g. asked about dasha → suggest: antardasha breakdown, gochar support, remedies
    #  - For panchang/horoscope: tomorrow, weekly overview, lucky window this month
    #  - For compatibility: specific dosha remedies, shaadi muhurta, children timing
    #
    sections.append(
        "\n[MANDATORY] Response ke AKHIR mein — HAMESHA yeh tag add karo — koi exception nahi:\n"
        "[FOLLOWUPS: \"question 1\" | \"question 2\" | \"question 3\"]\n"
        "3 questions. User ki language. Max 7 words each. "
        "Jo abhi discuss hua usse AAGE badhao — naya angle, naya topic. "
        "Sirf yahi ek line — koi aur text nahi."
    )

    return "\n".join(sections)
