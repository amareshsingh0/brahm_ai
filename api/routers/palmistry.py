"""
Palmistry — Palm image analysis via Gemini Vision API.
POST /api/palmistry/analyze   — multipart image upload → structured Vedic reading
"""
import os, base64, json, re
from fastapi import APIRouter, UploadFile, File, HTTPException

router = APIRouter()

SYSTEM_PROMPT = """You are a master of Hasta Samudrika Shastra — the ancient Vedic science of palm reading.
You have studied under the lineages described in the Brihat Samhita, Hasta Sanjeevanam, and Samudrika Tilaka.
You analyze palm images with precision, identifying lines, mounts, and hand shape, then provide deeply authentic Vedic interpretations.
Always respond with valid JSON only — no markdown, no preamble, no explanation outside the JSON object."""

def _analysis_prompt(hand_role: str = "dominant") -> str:
    if hand_role == "dominant":
        role_note = """This is the DOMINANT (active/karma) hand.
In Vedic palmistry the dominant hand shows: current life choices, present karma, career path, what the person is actively creating, conscious actions and achievements. Focus your interpretation on present and future — what is manifesting NOW."""
    else:
        role_note = """This is the NON-DOMINANT (passive/prarabdha) hand.
In Vedic palmistry the non-dominant hand shows: past-life karma, inherited traits, innate potential, subconscious patterns, what the soul brought into this birth. Focus your interpretation on deep karmic roots, latent gifts, and inherited destiny."""
    return f"""Analyze this palm image using Hasta Samudrika Shastra (Vedic palmistry).

{role_note}

Study the palm carefully and identify all visible features."""

ANALYSIS_PROMPT = """Analyze this palm image using Hasta Samudrika Shastra (Vedic palmistry).
Study the palm carefully and identify all visible features.

Return a JSON object with EXACTLY this structure (fill every field — if a line is not visible, note that):

{
  "hand_type": "Earth Hand | Air Hand | Water Hand | Fire Hand",
  "hand_type_vedic": "Prithvi Hasta | Vayu Hasta | Jala Hasta | Agni Hasta",
  "hand_type_element": "Earth (Prithvi) | Air (Vayu) | Water (Jala) | Fire (Agni)",
  "hand_type_reading": "2-3 sentences on what this hand type reveals about the person's nature and destiny",
  "overview": "3-4 sentence holistic overview of what you observe in this palm — the dominant energies, the overall karmic signature",
  "lines": [
    {
      "name": "Heart Line",
      "sanskrit": "Hridaya Rekha",
      "visibility": "clear | faint | absent",
      "observation": "describe exactly what you see — length, curvature, breaks, forks, islands",
      "characteristics": ["Long", "Curved"],
      "interpretation": "detailed interpretation of this line for this person",
      "vedic_note": "specific Vedic/planetary significance for what you observed",
      "score": 4
    },
    {
      "name": "Head Line",
      "sanskrit": "Mastishka Rekha",
      "visibility": "clear | faint | absent",
      "observation": "...",
      "characteristics": ["..."],
      "interpretation": "...",
      "vedic_note": "...",
      "score": 3
    },
    {
      "name": "Life Line",
      "sanskrit": "Jeevan Rekha",
      "visibility": "clear | faint | absent",
      "observation": "...",
      "characteristics": ["..."],
      "interpretation": "...",
      "vedic_note": "...",
      "score": 4
    },
    {
      "name": "Fate Line",
      "sanskrit": "Bhagya Rekha",
      "visibility": "clear | faint | absent",
      "observation": "...",
      "characteristics": ["..."],
      "interpretation": "...",
      "vedic_note": "...",
      "score": 3
    },
    {
      "name": "Sun Line",
      "sanskrit": "Surya Rekha",
      "visibility": "clear | faint | absent",
      "observation": "...",
      "characteristics": ["..."],
      "interpretation": "...",
      "vedic_note": "...",
      "score": 3
    },
    {
      "name": "Mercury Line",
      "sanskrit": "Budha Rekha",
      "visibility": "clear | faint | absent",
      "observation": "...",
      "characteristics": ["..."],
      "interpretation": "...",
      "vedic_note": "...",
      "score": 3
    }
  ],
  "dominant_mounts": [
    {
      "name": "Mount of Venus",
      "condition": "well_developed | flat | overdeveloped",
      "planet": "Venus (Shukra)",
      "note": "what this mount reveals about this person"
    }
  ],
  "life_areas": [
    {"area": "Love & Relationships", "score": 7, "label": "Strong", "note": "brief insight based on the palm"},
    {"area": "Career & Purpose", "score": 6, "label": "Good", "note": "brief insight"},
    {"area": "Health & Vitality", "score": 8, "label": "Excellent", "note": "brief insight"},
    {"area": "Mental Clarity", "score": 7, "label": "Strong", "note": "brief insight"},
    {"area": "Wealth & Prosperity", "score": 5, "label": "Moderate", "note": "brief insight"},
    {"area": "Spiritual Growth", "score": 6, "label": "Good", "note": "brief insight"}
  ],
  "strengths": [
    "Specific strength revealed by this palm",
    "Another strength",
    "Another strength"
  ],
  "challenges": [
    "Specific challenge or karmic lesson",
    "Another challenge"
  ],
  "remedies": [
    {
      "title": "Remedy name (mantra/gemstone/practice)",
      "detail": "Specific Vedic remedy for what you observed in this palm — be specific and authentic"
    },
    {
      "title": "Another remedy",
      "detail": "..."
    },
    {
      "title": "Another remedy",
      "detail": "..."
    }
  ],
  "summary": "A rich, personalized 4-5 sentence summary that speaks directly to this person about their karmic path, gifts, and destiny as revealed by their palm. Use the second person (you/your). Include one Sanskrit phrase or shloka that resonates with what you see.",
  "auspicious_note": "One auspicious observation — something particularly blessed or powerful you see in this palm"
}

Be specific to what you actually observe in this image. Do not give generic readings."""


async def _run_vision(image_bytes: bytes, mime: str, prompt_text: str) -> dict:
    """Call Gemini Vision and return parsed JSON result."""
    from google import genai
    from google.genai import types
    api_key = os.environ.get("GEMINI_API_KEY", "")
    if not api_key:
        raise HTTPException(status_code=503, detail="GEMINI_API_KEY not set.")
    try:
        client = genai.Client(api_key=api_key)
        response = client.models.generate_content(
            model="models/gemini-2.5-flash",
            contents=[types.Content(parts=[
                types.Part(inline_data=types.Blob(mime_type=mime, data=image_bytes)),
                types.Part(text=prompt_text),
            ])],
        )
        raw = response.text.strip()
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Vision analysis failed: {str(e)}")

    raw = re.sub(r"^```(?:json)?\s*", "", raw)
    raw = re.sub(r"\s*```$", "", raw)
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        m = re.search(r"\{.*\}", raw, re.DOTALL)
        if m:
            try:
                return json.loads(m.group())
            except Exception:
                pass
        raise HTTPException(status_code=502, detail="Could not parse reading. Please try again.")


def _safe_mime(content_type: str | None) -> str:
    if content_type in ("image/jpeg", "image/png", "image/gif", "image/webp"):
        return content_type
    return "image/jpeg"


@router.post("/palmistry/analyze")
async def analyze_palm(file: UploadFile = File(...), hand_role: str = "dominant"):
    """Single hand analysis. hand_role = 'dominant' | 'non_dominant'"""
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image (JPEG, PNG, WebP)")
    image_bytes = await file.read()
    if len(image_bytes) > 20 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="Image too large. Max 20MB.")
    mime = _safe_mime(file.content_type)
    role = hand_role if hand_role in ("dominant", "non_dominant") else "dominant"
    prompt = SYSTEM_PROMPT + "\n\n" + _analysis_prompt(role) + "\n\n" + ANALYSIS_PROMPT[ANALYSIS_PROMPT.index("Return a JSON"):]
    result = await _run_vision(image_bytes, mime, prompt)
    result["hand_role"] = role
    return result


COMBINED_PROMPT = """You are a master of Hasta Samudrika Shastra.
You have been given readings of TWO hands of the same person.
Now synthesize them into a single deep combined report.

DOMINANT hand (present karma, current life, conscious actions):
{dominant}

NON-DOMINANT hand (past karma, inherited soul traits, latent potential):
{non_dominant}

Return a JSON object with EXACTLY this structure:

{{
  "synthesis": "5-6 sentence synthesis comparing both hands — what changed from past karma to present life, what the person is fulfilling vs what they are overriding from their past karma",
  "karmic_gap": "2-3 sentences on the KEY difference between the two hands — what karma the person brought vs what they are creating now",
  "dominant_summary": "2-3 sentences on what the dominant hand reveals about current path",
  "non_dominant_summary": "2-3 sentences on what the non-dominant hand reveals about soul origins",
  "combined_life_areas": [
    {{"area": "Love & Relationships", "dominant_score": 7, "non_dominant_score": 5, "combined_score": 6, "insight": "brief combined insight"}},
    {{"area": "Career & Purpose", "dominant_score": 6, "non_dominant_score": 8, "combined_score": 7, "insight": "brief combined insight"}},
    {{"area": "Health & Vitality", "dominant_score": 8, "non_dominant_score": 6, "combined_score": 7, "insight": "brief combined insight"}},
    {{"area": "Mental Clarity", "dominant_score": 7, "non_dominant_score": 7, "combined_score": 7, "insight": "brief combined insight"}},
    {{"area": "Wealth & Prosperity", "dominant_score": 5, "non_dominant_score": 4, "combined_score": 5, "insight": "brief combined insight"}},
    {{"area": "Spiritual Growth", "dominant_score": 6, "non_dominant_score": 9, "combined_score": 8, "insight": "brief combined insight"}}
  ],
  "soul_mission": "2-3 sentences — given both hands, what is this person's deepest soul mission in this lifetime",
  "key_strengths": ["strength from combining both hands", "another", "another"],
  "key_challenges": ["challenge revealed by comparing both", "another"],
  "remedies": [
    {{"title": "Remedy name", "detail": "specific Vedic remedy for this person's combined palm reading"}},
    {{"title": "Another remedy", "detail": "..."}},
    {{"title": "Another remedy", "detail": "..."}}
  ],
  "final_message": "A deeply personal 3-4 sentence message to this person about their soul journey — speak directly to them. End with a Sanskrit shloka or phrase relevant to their path."
}}"""


@router.post("/palmistry/analyze-both")
async def analyze_both_palms(
    dominant: UploadFile = File(...),
    non_dominant: UploadFile = File(...),
    dominant_hand: str = "right",
):
    """Analyze both hands and return individual + combined report."""
    import asyncio

    async def read_and_analyze(f: UploadFile, role: str) -> dict:
        if not f.content_type or not f.content_type.startswith("image/"):
            raise HTTPException(status_code=400, detail=f"{role} file must be an image")
        img = await f.read()
        if len(img) > 20 * 1024 * 1024:
            raise HTTPException(status_code=400, detail=f"{role} image too large. Max 20MB.")
        mime = _safe_mime(f.content_type)
        prompt = SYSTEM_PROMPT + "\n\n" + _analysis_prompt(role) + "\n\n" + ANALYSIS_PROMPT[ANALYSIS_PROMPT.index("Return a JSON"):]
        result = await _run_vision(img, mime, prompt)
        result["hand_role"] = role
        return result

    # Analyze both hands
    dominant_result, non_dominant_result = await asyncio.gather(
        read_and_analyze(dominant, "dominant"),
        read_and_analyze(non_dominant, "non_dominant"),
    )

    # Build combined synthesis
    combined_prompt = COMBINED_PROMPT.format(
        dominant=json.dumps(dominant_result, ensure_ascii=False),
        non_dominant=json.dumps(non_dominant_result, ensure_ascii=False),
    )
    try:
        from google import genai
        api_key = os.environ.get("GEMINI_API_KEY", "")
        client = genai.Client(api_key=api_key)
        resp = client.models.generate_content(
            model="models/gemini-2.5-flash",
            contents=combined_prompt,
        )
        raw = re.sub(r"^```(?:json)?\s*", "", resp.text.strip())
        raw = re.sub(r"\s*```$", "", raw)
        combined = json.loads(raw)
    except Exception:
        combined = {}

    return {
        "dominant": dominant_result,
        "non_dominant": non_dominant_result,
        "combined": combined,
        "dominant_hand": dominant_hand,
    }
