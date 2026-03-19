"""
Prompt Builder — Pass 2 of two-pass reasoning.
Builds the final answer prompt using:
  - Pass 1 decision (intent, query_type)
  - Tool execution results (calculations)
  - Kundali compressed summary
  - Page context + data
  - RAG docs (only when needed)
"""
import json
from typing import List, Dict, Optional

MASTER_PERSONA = """Tum Brahm AI ho — ek sampurna Vedic jyotishi aur aadhunik calculator ka sangam.

Tumhare paas yeh sab available hai:
- User ka kundali data (agar diya gaya)
- Fresh calculation results (agar run kiye)
- Page ka current data (jo user dekh raha hai)
- Vedic books ka context (sirf jab relevant)

Jawab dene ka structure (hamesha follow karo):
1. Seedha point — user kya jaanna chahta hai, woh pehle bolo
2. Astrological karan — specific graha, house, dasha ka naam lo
3. Timing — exact period ya year agar possible ho
4. Reality check — honest bolo, darawna nahi, hopeful bhi nahi agar sach nahi
5. Upay — sirf agar user ne manga ho ya critical situation ho

Style rules:
- Generic mat bolo — "Shani aapke 7th house mein hai" jaise specific bolo
- Sanskrit terms use karo lekin TURANT Hindi mein explain karo
- Compassionate lekin honest — jyotish mein false hope se bura kuch nahi
- Max 350 words — jab tak user ne detail na manga ho
- Kabhi false prediction nahi — agar data nahi toh honestly kaho"""

LANG_PREFIX = {
    "hi": "हिंदी में जवाब दो।",
    "en": "Answer in English.",
    "sa": "संस्कृतभाषायाम् उत्तरं देहि।",
}


def _format_kundali(summary: dict) -> str:
    if not summary:
        return ""
    lines = ["User ki Kundali Summary:"]
    lines.append(f"  Lagna: {summary.get('lagna','?')} ({summary.get('lagna_degree','?')}°)")
    lines.append(f"  Moon: {summary.get('moon_rashi','?')} — {summary.get('moon_house','?')}th house")
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


def _format_tool_results(results: dict) -> str:
    if not results:
        return ""
    lines = ["Calculation Results:"]
    for service, data in results.items():
        if "_error" in service:
            continue
        lines.append(f"\n  [{service.upper()}]")
        if isinstance(data, dict):
            for k, v in data.items():
                if v:
                    lines.append(f"    {k}: {v}")
        else:
            lines.append(f"    {data}")
    return "\n".join(lines)


def _format_page_data(page_context: str, page_data: dict) -> str:
    if not page_data:
        return ""

    if page_context == "compatibility":
        kutas = page_data.get("kutas", {})
        kuta_str = "\n".join([f"    {k}: {v}" for k, v in kutas.items()]) if kutas else ""
        doshas = []
        if page_data.get("nadi_dosha"): doshas.append("Nadi Dosha")
        if page_data.get("gana_mismatch"): doshas.append("Gana Mismatch")
        return f"""Compatibility Report (jo user dekh raha hai):
  Score: {page_data.get('total_score','?')}/{page_data.get('out_of',36)}
  Verdict: {page_data.get('verdict','?')}
  Doshas: {', '.join(doshas) if doshas else 'Koi major dosha nahi'}
  Kuta Scores:
{kuta_str}
  Strong: {', '.join(page_data.get('strong_areas', []))}
  Weak: {', '.join(page_data.get('weak_areas', []))}"""

    elif page_context == "panchang":
        return f"""Aaj ka Panchang (jo user dekh raha hai):
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
        return f"""Live Sky Data:
{planet_lines}"""

    else:
        try:
            return f"Page Data ({page_context}):\n  {json.dumps(page_data, ensure_ascii=False)[:600]}"
        except Exception:
            return ""


def _format_rag_docs(docs: list) -> str:
    if not docs:
        return ""
    lines = ["Vedic Texts se Reference:"]
    for i, d in enumerate(docs[:3], 1):
        src = d.get("source", "?")
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

    query_type = decision.get("query_type", "DEEP_VEDIC")
    intent = decision.get("intent", "")
    lang_line = LANG_PREFIX.get(language, LANG_PREFIX["hi"])

    # For auto-analysis trigger
    if query == "__auto_analyze__":
        if page_context == "compatibility":
            actual_query = "Is compatibility report ka complete analysis karo — score ka matlab, har dosha ki explanation, strong/weak areas ka impact, upay, aur vivah ke liye guidance."
        elif page_context == "panchang":
            actual_query = "Aaj ke panchang ka deep analysis karo — tithi, nakshatra, yoga ka mahatva aur aaj ke liye practical guidance."
        else:
            actual_query = "Is report ka detailed analysis karo aur key insights do."
    else:
        actual_query = query

    # Build all sections
    kundali_section = _format_kundali(kundali_summary)
    page_section = _format_page_data(page_context, page_data)
    calc_section = _format_tool_results(tool_results)
    rag_section = _format_rag_docs(rag_docs)

    # Conversational — short, no context needed
    if query_type == "CONVERSATIONAL":
        return f"""{MASTER_PERSONA}

{lang_line}

User: {actual_query}

Warm, short jawab do — 1-2 sentences. Pehli baar greet kar rahe ho toh naam batao.
Books ya calculations mat use karo."""

    # Build full prompt for everything else
    sections = [MASTER_PERSONA, "", lang_line, ""]

    if kundali_section:
        sections.append(kundali_section)
        sections.append("")

    if page_section:
        sections.append(page_section)
        sections.append("")

    if calc_section:
        sections.append(calc_section)
        sections.append("")

    if rag_section:
        sections.append(rag_section)
        sections.append("")

    sections.append(f"User ne poocha: {actual_query}")
    sections.append(f"Intent: {intent}")
    sections.append("")

    # Depth-specific instruction
    depth = decision.get("response_depth", "deep")
    if depth == "master":
        sections.append("Yeh ek personal astrological analysis hai. Sab available data use karo. Specific, insightful, aur actionable jawab do.")
    elif depth == "basic":
        sections.append("Short, clear, helpful jawab do.")
    else:
        sections.append("Detailed lekin focused jawab do.")

    return "\n".join(sections)
