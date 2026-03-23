/**
 * Brahm AI — Auto Translation Script
 *
 * Uses Google Cloud Translation API (v2 Basic) to translate en.json
 * into all target Indian languages and writes them to src/locales/{lang}.json
 *
 * Usage:
 *   GOOGLE_TRANSLATE_KEY=your_api_key node scripts/translate.mjs
 *   OR set key in .env.local as VITE_GOOGLE_TRANSLATE_KEY
 *
 * Free tier: 500,000 characters/month — more than enough for our JSON files.
 */

import { readFileSync, writeFileSync, existsSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

// Load .env.local automatically
const __file = fileURLToPath(import.meta.url);
const __dir = dirname(__file);
const envPath = resolve(__dir, "../.env.local");
if (existsSync(envPath)) {
  const envContent = readFileSync(envPath, "utf-8");
  for (const line of envContent.split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eqIdx = trimmed.indexOf("=");
    if (eqIdx < 0) continue;
    const key = trimmed.slice(0, eqIdx).trim();
    const val = trimmed.slice(eqIdx + 1).trim();
    if (!process.env[key]) process.env[key] = val;
  }
}

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "..");

// ── Config ────────────────────────────────────────────────────────────────────

const API_KEY =
  process.env.GOOGLE_TRANSLATE_KEY ||
  process.env.VITE_GOOGLE_TRANSLATE_KEY ||
  "";

if (!API_KEY) {
  console.error(
    "❌  No API key found.\n" +
    "    Run: GOOGLE_TRANSLATE_KEY=your_key node scripts/translate.mjs\n" +
    "    Or add VITE_GOOGLE_TRANSLATE_KEY to .env.local"
  );
  process.exit(1);
}

// Target languages (Google Translate language codes)
// To add/remove a language, edit this list
const TARGET_LANGS = [
  { code: "hi", name: "Hindi",      googleCode: "hi" },
  { code: "mr", name: "Marathi",    googleCode: "mr" },
  { code: "bn", name: "Bengali",    googleCode: "bn" },
  { code: "gu", name: "Gujarati",   googleCode: "gu" },
  { code: "ta", name: "Tamil",      googleCode: "ta" },
  { code: "te", name: "Telugu",     googleCode: "te" },
  { code: "kn", name: "Kannada",    googleCode: "kn" },
  { code: "pa", name: "Punjabi",    googleCode: "pa" },
  { code: "ml", name: "Malayalam",  googleCode: "ml" },
  { code: "or", name: "Odia",       googleCode: "or" },
];

// If you already have a hand-crafted file, set to true to skip overwriting
const SKIP_EXISTING = {
  hi: true,   // hi.json is already manually translated — skip
  sa: true,   // sa.json Sanskrit — skip (Google Translate is bad at Sanskrit)
};

// MERGE_MODE: only translate keys that are missing in the existing file
// Set to true to avoid re-translating already translated keys (saves API quota)
const MERGE_MODE = true;

// ── Load source English JSON ───────────────────────────────────────────────

const EN_PATH = resolve(ROOT, "src/locales/en.json");
const sourceJson = JSON.parse(readFileSync(EN_PATH, "utf-8"));

// ── Google Translate API call ──────────────────────────────────────────────

/**
 * Translate an array of strings to a target language.
 * Batches them in a single API call (up to 128 strings / 5k chars per call).
 */
async function translateBatch(texts, targetLang) {
  const url = `https://translation.googleapis.com/language/translate/v2?key=${API_KEY}`;

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      q: texts,
      source: "en",
      target: targetLang,
      format: "text",
    }),
  });

  if (!response.ok) {
    const err = await response.text();
    throw new Error(`Google Translate API error ${response.status}: ${err}`);
  }

  const data = await response.json();
  return data.data.translations.map((t) => t.translatedText);
}

// ── Flatten / unflatten JSON ──────────────────────────────────────────────

/**
 * Flattens a nested object into { "a.b.c": "value" } pairs.
 * Keeps {{interpolation}} placeholders safe.
 */
function flattenJson(obj, prefix = "") {
  const result = {};
  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (typeof value === "object" && value !== null) {
      Object.assign(result, flattenJson(value, fullKey));
    } else {
      result[fullKey] = String(value);
    }
  }
  return result;
}

/**
 * Rebuilds nested object from flat key map.
 */
function unflattenJson(flat) {
  const result = {};
  for (const [key, value] of Object.entries(flat)) {
    const parts = key.split(".");
    let current = result;
    for (let i = 0; i < parts.length - 1; i++) {
      if (!current[parts[i]]) current[parts[i]] = {};
      current = current[parts[i]];
    }
    current[parts[parts.length - 1]] = value;
  }
  return result;
}

// ── Preserve {{variable}} placeholders ───────────────────────────────────

/**
 * Replace {{...}} with a placeholder Google Translate won't mangle.
 * We use PLACEHOLDER_N tokens and restore after translation.
 */
function maskPlaceholders(text) {
  const tokens = [];
  const masked = text.replace(/\{\{[^}]+\}\}/g, (match) => {
    const idx = tokens.length;
    tokens.push(match);
    return `__PH${idx}__`;
  });
  return { masked, tokens };
}

function restorePlaceholders(text, tokens) {
  return text.replace(/__PH(\d+)__/g, (_, idx) => tokens[Number(idx)] || _);
}

// ── Main ──────────────────────────────────────────────────────────────────

async function main() {
  const flat = flattenJson(sourceJson);
  const keys = Object.keys(flat);
  const values = Object.values(flat);

  // Mask placeholders in all values
  const maskedData = values.map(maskPlaceholders);
  const maskedTexts = maskedData.map((d) => d.masked);

  console.log(
    `📄 Source: ${keys.length} keys, ~${maskedTexts.join("").length} chars`
  );

  for (const lang of TARGET_LANGS) {
    const outPath = resolve(ROOT, `src/locales/${lang.code}.json`);

    if (SKIP_EXISTING[lang.code] && existsSync(outPath)) {
      console.log(`⏭️  ${lang.name} (${lang.code}) — skipping (already exists)`);
      continue;
    }

    // In MERGE_MODE, load existing translation and only translate missing keys
    let existingFlat = {};
    if (MERGE_MODE && existsSync(outPath)) {
      try {
        const existingJson = JSON.parse(readFileSync(outPath, "utf-8"));
        existingFlat = flattenJson(existingJson);
      } catch {
        // ignore parse errors, will re-translate all
      }
    }

    // Filter to only keys missing in the existing file
    const missingIndices = keys
      .map((k, i) => (existingFlat[k] === undefined ? i : -1))
      .filter((i) => i !== -1);

    if (missingIndices.length === 0) {
      console.log(`✅  ${lang.name} (${lang.code}) — already up to date, skipping`);
      continue;
    }

    process.stdout.write(`🌐 Translating ${missingIndices.length} new keys to ${lang.name}...`);

    try {
      const missingTexts = missingIndices.map((i) => maskedTexts[i]);
      const missingMaskedData = missingIndices.map((i) => maskedData[i]);

      // Split into chunks of 100 strings to stay under API limits
      const CHUNK = 100;
      const translated = [];
      for (let i = 0; i < missingTexts.length; i += CHUNK) {
        const chunk = missingTexts.slice(i, i + CHUNK);
        const result = await translateBatch(chunk, lang.googleCode);
        translated.push(...result);
      }

      // Restore placeholders
      const restored = translated.map((text, i) =>
        restorePlaceholders(text, missingMaskedData[i].tokens)
      );

      // Merge: start with existing translations, add new ones
      const mergedFlat = { ...existingFlat };
      missingIndices.forEach((keyIdx, i) => {
        mergedFlat[keys[keyIdx]] = restored[i];
      });

      const nestedJson = unflattenJson(mergedFlat);
      writeFileSync(outPath, JSON.stringify(nestedJson, null, 2) + "\n", "utf-8");
      console.log(` ✅  Saved to src/locales/${lang.code}.json`);
    } catch (err) {
      console.log(` ❌  Failed: ${err.message}`);
    }
  }

  console.log("\n✅  Done! Add new language codes to src/lib/i18n.ts");
  console.log("    Example: import mr from '@/locales/mr.json';");
  console.log("    Then add: mr: { translation: mr }  in resources.");
}

main().catch(console.error);
