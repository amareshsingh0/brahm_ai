"""
Query Router — Pass 1 of two-pass reasoning.
Gemini returns structured JSON decision about how to handle the query.
No hardcoded keyword rules — Gemini understands nuance and context.
"""
import re
import os
import json

OBVIOUS_GREETINGS = [
    r"^(hi+|hello+|hii+|hey+)\s*[!?.]*$",
    r"^(namaste|namaskar|pranam|jai shri|radhe radhe|hari om)\s*[!?.]*$",
    r"^(bye|alvida|good night|shubh ratri|shubh prabhat)\s*[!?.]*$",
    r"^(ok|okay|accha|theek hai|haan|nahi|ji|hmm|acha)\s*[!?.]*$",
    r"^(thanks|thank you|shukriya|dhanyawad|bahut shukriya)\s*[!?.]*$",
]

PASS1_PROMPT = """You are the decision engine for Brahm AI — a Vedic astrology AI assistant.

User query: "{query}"
Conversation history (last 6 messages): {history_summary}
Page context: {page_context}
Kundali already calculated: {has_kundali}
User profile birth data saved: {has_profile_birth_data}
Page data keys: {page_data_keys}

Return ONLY valid JSON, no explanation, no markdown:
{{
  "intent": "one line: what user actually wants",
  "query_type": "CONVERSATIONAL|SIMPLE_FACT|DEEP_VEDIC|CHART_ANALYSIS|REPORT_ANALYSIS|RECOMMENDATION|GENERAL_KNOWLEDGE|SMALL_TALK",
  "needs_calculation": true or false,
  "needs_birth_form": true or false,
  "calc_services": [],
  "needs_rag": true or false,
  "kundali_focus": [],
  "response_depth": "basic|deep|master",
  "response_lang": "hi|en|sa",
  "user_language_style": "pure_hindi|pure_english|hinglish",
  "birth_data": {{
    "date": "YYYY-MM-DD or null",
    "time": "HH:MM or null",
    "place": "city name or null",
    "name": "person name or null"
  }}
}}

Rules for birth_data extraction:
- Scan BOTH the current query AND conversation history for birth details
- date: parse any format (14/04/2003, April 14 2003, 14-4-2003) → YYYY-MM-DD
- time: parse any format (8:25 PM, 20:25, raat 8 baje) → HH:MM (24h)
- place: city name as-is (Delhi, Mumbai, etc.)
- If already seen in history and not overridden, carry it forward
- If not found anywhere, use null

Rules for query_type:
- CONVERSATIONAL: greetings, thanks, short replies, follow-up affirmations
- SMALL_TALK: casual chitchat, personal feelings, mood, "kya chal raha hai" etc.
- SIMPLE_FACT: basic planet/rashi/nakshatra info, no personal chart needed
- DEEP_VEDIC: scripture quote/shlok meaning, complex yoga theory — needs book search
- CHART_ANALYSIS: about user's personal life, future, dasha, career, health, marriage
- REPORT_ANALYSIS: explaining a report currently visible (compatibility score, panchang data)
- RECOMMENDATION: muhurta, auspicious date, remedy, what to do
- GENERAL_KNOWLEDGE: non-Vedic questions (news, sports, general life advice, science, etc.)
  → Brahm AI answers these from its general knowledge + links to Vedic wisdom if possible

Rules for needs_birth_form:
- true ONLY if ALL of these are true: user wants kundali/personal prediction AND birth_data has no date/place AND has_kundali is "no" AND has_profile_birth_data is "no"
- false if: has_kundali is "yes" — already have chart
- false if: has_profile_birth_data is "yes" — will use saved profile data directly, no form needed
- false if: birth_data extracted from conversation has date + place
- When in doubt → false (never ask for birth data that may already be saved)

Rules for needs_calculation:
- true if: CHART_ANALYSIS or RECOMMENDATION and (has_kundali is "yes" OR has_profile_birth_data is "yes" OR birth_data has date+place)
- true if: dasha timing, muhurta, panchang needed fresh
- false if: has_kundali is "yes" AND query is just asking to analyse existing chart (no recalculation needed)
- false if: CONVERSATIONAL/SIMPLE_FACT/SMALL_TALK/GENERAL_KNOWLEDGE
- false if: birth_data is incomplete AND has_profile_birth_data is "no" AND has_kundali is "no"
- false if: needs_birth_form is true

Rules for needs_rag:
- true ONLY if: user explicitly asks "shastra mein kya hai", scripture reference, shlok
- false for: 90% of queries — personal predictions, calculations, report analysis, conversation, general knowledge

Rules for calc_services (only fill if needs_calculation=true):
- "dasha": vimshottari dasha timeline — for timing questions
- "panchang": today's tithi/nakshatra/yoga/rahukaal
- "muhurta": auspicious timing for an event
- "kundali": full birth chart — for all personal chart questions
- "gochar": current transit positions

Rules for response_depth:
- basic: CONVERSATIONAL, SIMPLE_FACT, SMALL_TALK, GENERAL_KNOWLEDGE
- deep: DEEP_VEDIC, REPORT_ANALYSIS
- master: CHART_ANALYSIS, RECOMMENDATION (personal, needs full analysis)

Rules for user_language_style:
- pure_hindi: user wrote in Devanagari script (हिंदी में)
- pure_english: user wrote entirely in English with no Hindi words
- hinglish: user mixed Hindi+English (roman Hindi, e.g. "meri shaadi kb hogi")
  Note: most Indian users write hinglish — "kb", "kya", "meri", "btao" etc. = hinglish"""

# Safe CONVERSATIONAL fallback — always gives a response, never blank
_CONVERSATIONAL_FALLBACK = {
    "intent": "general query",
    "query_type": "CONVERSATIONAL",
    "needs_calculation": False,
    "needs_birth_form": False,
    "calc_services": [],
    "needs_rag": False,
    "kundali_focus": [],
    "response_depth": "basic",
    "response_lang": "en",
    "user_language_style": "pure_english",
    "birth_data": {"date": None, "time": None, "place": None, "name": None},
}


def get_pass1_decision(
    query: str,
    page_context: str = "general",
    kundali_summary: dict = None,
    page_data: dict = None,
    history: list = None,
) -> dict:
    """
    Pass 1: Gemini decides how to handle the query.
    Returns decision dict. Fast regex for obvious greetings, Gemini for everything else.
    Guaranteed to return a valid dict — never raises.
    """
    q = query.strip().lower()

    # Fast path for obvious greetings — saves Gemini API call
    for pattern in OBVIOUS_GREETINGS:
        if re.match(pattern, q):
            return {**_CONVERSATIONAL_FALLBACK, "intent": "greeting or small talk"}

    # Everything else: Gemini decides
    return _gemini_pass1(query, page_context, kundali_summary, page_data, history or [])


def _gemini_pass1(
    query: str,
    page_context: str,
    kundali_summary: dict,
    page_data: dict,
    history: list,
) -> dict:
    from google import genai
    from google.genai import types

    # Summarize last 6 messages for context
    history_summary = ""
    if history:
        for msg in history[-6:]:
            role = msg.get("role", "user")
            content = msg.get("content", "")[:200]
            history_summary += f"{role}: {content}\n"
    history_summary = history_summary.strip() or "none"

    has_profile = bool(page_data and page_data.get("user_birth_data"))
    prompt = PASS1_PROMPT.format(
        query=query,
        history_summary=history_summary,
        page_context=page_context,
        has_kundali="yes" if kundali_summary else "no",
        has_profile_birth_data="yes" if has_profile else "no",
        page_data_keys=list(page_data.keys()) if page_data else [],
    )

    api_key = os.environ.get("GEMINI_API_KEY", "")
    client = genai.Client(api_key=api_key)

    try:
        response = client.models.generate_content(
            model="models/gemini-2.5-flash",
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
            ),
        )
        text = response.text.strip()
        # Strip markdown code blocks if somehow present
        text = re.sub(r"^```json\s*", "", text)
        text = re.sub(r"\s*```$", "", text)
        decision = json.loads(text)
        # Validate required keys
        required = ["intent", "query_type", "needs_calculation", "needs_rag"]
        for key in required:
            if key not in decision:
                raise ValueError(f"Missing key: {key}")
        return decision
    except Exception:
        # Safe fallback — CONVERSATIONAL so Gemini always gives a helpful response
        return {**_CONVERSATIONAL_FALLBACK, "intent": query[:80]}
