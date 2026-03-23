/**
 * Brahm AI — Merge Missing Keys Script
 *
 * Translates ONLY the keys missing from existing locale files (hi, sa).
 * Merges new translations into the existing file without overwriting manual translations.
 *
 * Usage: node scripts/translate-merge.mjs
 */

import { readFileSync, writeFileSync, existsSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

// Load .env.local
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

const API_KEY =
  process.env.GOOGLE_TRANSLATE_KEY ||
  process.env.VITE_GOOGLE_TRANSLATE_KEY ||
  "";

if (!API_KEY) {
  console.error("❌  No API key found.");
  process.exit(1);
}

// Languages to merge (existing files that need new keys added)
const MERGE_LANGS = [
  { code: "hi", googleCode: "hi", name: "Hindi" },
  { code: "sa", googleCode: "hi", name: "Sanskrit" }, // use Hindi as proxy for sa
];

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

async function translateBatch(texts, targetLang) {
  const url = `https://translation.googleapis.com/language/translate/v2?key=${API_KEY}`;
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ q: texts, source: "en", target: targetLang, format: "text" }),
  });
  if (!response.ok) {
    const err = await response.text();
    throw new Error(`API error ${response.status}: ${err}`);
  }
  const data = await response.json();
  return data.data.translations.map((t) => t.translatedText);
}

async function main() {
  const enPath = resolve(ROOT, "src/locales/en.json");
  const enJson = JSON.parse(readFileSync(enPath, "utf-8"));
  const enFlat = flattenJson(enJson);

  for (const lang of MERGE_LANGS) {
    const outPath = resolve(ROOT, `src/locales/${lang.code}.json`);
    if (!existsSync(outPath)) {
      console.log(`⏭️  ${lang.name} file doesn't exist, skipping`);
      continue;
    }

    const existing = JSON.parse(readFileSync(outPath, "utf-8"));
    const existingFlat = flattenJson(existing);

    // Find missing keys
    const missingKeys = Object.keys(enFlat).filter((k) => !(k in existingFlat));
    if (missingKeys.length === 0) {
      console.log(`✅  ${lang.name} — no missing keys`);
      continue;
    }

    console.log(`🌐 ${lang.name} — ${missingKeys.length} missing keys, translating...`);

    const missingValues = missingKeys.map((k) => enFlat[k]);
    const maskedData = missingValues.map(maskPlaceholders);
    const maskedTexts = maskedData.map((d) => d.masked);

    try {
      const CHUNK = 100;
      const translated = [];
      for (let i = 0; i < maskedTexts.length; i += CHUNK) {
        const chunk = maskedTexts.slice(i, i + CHUNK);
        const result = await translateBatch(chunk, lang.googleCode);
        translated.push(...result);
      }

      const restored = translated.map((text, i) =>
        restorePlaceholders(text, maskedData[i].tokens)
      );

      // Merge: existing + new keys
      const mergedFlat = { ...existingFlat };
      missingKeys.forEach((k, i) => {
        mergedFlat[k] = restored[i];
      });

      const nestedJson = unflattenJson(mergedFlat);
      writeFileSync(outPath, JSON.stringify(nestedJson, null, 2) + "\n", "utf-8");
      console.log(`  ✅  Merged ${missingKeys.length} keys into ${lang.code}.json`);
    } catch (err) {
      console.log(`  ❌  Failed: ${err.message}`);
    }
  }

  console.log("\n✅  Done!");
}

main().catch(console.error);
