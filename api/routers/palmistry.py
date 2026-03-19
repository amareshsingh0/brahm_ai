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


@router.post("/palmistry/analyze")
async def analyze_palm(file: UploadFile = File(...)):
    if not file.content_type or not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="File must be an image (JPEG, PNG, WebP)")

    api_key = os.environ.get("GEMINI_API_KEY", "")
    if not api_key:
        raise HTTPException(status_code=503, detail="GEMINI_API_KEY not set in server environment.")

    image_bytes = await file.read()
    if len(image_bytes) > 20 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="Image too large. Please use an image under 20MB.")

    mime = file.content_type
    if mime not in ("image/jpeg", "image/png", "image/gif", "image/webp"):
        mime = "image/jpeg"

    try:
        from google import genai
        from google.genai import types

        client = genai.Client(api_key=api_key)

        response = client.models.generate_content(
            model="models/gemini-2.5-flash",
            contents=[
                types.Content(parts=[
                    types.Part(
                        inline_data=types.Blob(mime_type=mime, data=image_bytes)
                    ),
                    types.Part(text=SYSTEM_PROMPT + "\n\n" + ANALYSIS_PROMPT),
                ])
            ],
        )
        raw = response.text.strip()

    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Vision analysis failed: {str(e)}")

    # Strip markdown code fences if present
    raw = re.sub(r"^```(?:json)?\s*", "", raw)
    raw = re.sub(r"\s*```$", "", raw)

    try:
        result = json.loads(raw)
    except json.JSONDecodeError:
        m = re.search(r"\{.*\}", raw, re.DOTALL)
        if m:
            try:
                result = json.loads(m.group())
            except Exception:
                raise HTTPException(status_code=502, detail="Could not parse reading. Please try again.")
        else:
            raise HTTPException(status_code=502, detail="Could not parse reading. Please try again.")

    return result
