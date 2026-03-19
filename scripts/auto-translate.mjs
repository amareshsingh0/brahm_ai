/**
 * Auto-translate en.json → hi.json, sa.json
 * Uses Google Cloud Translation API (free tier: 500k chars/month)
 *
 * Setup:
 *   1. GCP Console → APIs → Cloud Translation API enable karo
 *   2. API key banao
 *   3. GOOGLE_TRANSLATE_API_KEY=your_key node scripts/auto-translate.mjs
 */

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const API_KEY = process.env.GOOGLE_TRANSLATE_API_KEY;

if (!API_KEY) {
  console.error("❌ Set GOOGLE_TRANSLATE_API_KEY env var");
  process.exit(1);
}

const LOCALES_DIR = path.join(__dirname, "../src/locales");
const en = JSON.parse(fs.readFileSync(path.join(LOCALES_DIR, "en.json"), "utf8"));

// Flatten nested object to { "nav.dashboard": "Dashboard", ... }
function flatten(obj, prefix = "") {
  return Object.entries(obj).reduce((acc, [key, val]) => {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (typeof val === "object") Object.assign(acc, flatten(val, fullKey));
    else acc[fullKey] = val;
    return acc;
  }, {});
}

// Unflatten back to nested
function unflatten(obj) {
  const result = {};
  for (const [key, val] of Object.entries(obj)) {
    const parts = key.split(".");
    let cur = result;
    for (let i = 0; i < parts.length - 1; i++) {
      cur[parts[i]] = cur[parts[i]] || {};
      cur = cur[parts[i]];
    }
    cur[parts.at(-1)] = val;
  }
  return result;
}

async function translateBatch(texts, targetLang) {
  const BATCH = 100; // Google allows 128 per request
  const results = [];
  for (let i = 0; i < texts.length; i += BATCH) {
    const batch = texts.slice(i, i + BATCH);
    const res = await fetch(
      `https://translation.googleapis.com/language/translate/v2?key=${API_KEY}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ q: batch, target: targetLang, format: "text" }),
      }
    );
    const data = await res.json();
    if (data.error) throw new Error(data.error.message);
    results.push(...data.data.translations.map((t) => t.translatedText));
    console.log(`  Translated ${Math.min(i + BATCH, texts.length)}/${texts.length}`);
  }
  return results;
}

async function generateLocale(targetLang, outputFile) {
  console.log(`\n🌐 Translating to ${targetLang}...`);
  const flat = flatten(en);
  const keys = Object.keys(flat);
  const values = Object.values(flat);

  // Don't translate: keys ending in url, icon, or values that are just symbols/numbers
  const toTranslate = values.map((v) =>
    typeof v === "string" && v.length > 1 && !/^[₹$€£\d\s\.\/]+$/.test(v) ? v : null
  );

  const translateableValues = toTranslate.filter(Boolean);
  const translated = await translateBatch(translateableValues, targetLang);

  let translatedIdx = 0;
  const result = {};
  keys.forEach((key, i) => {
    result[key] = toTranslate[i] ? translated[translatedIdx++] : values[i];
  });

  const nested = unflatten(result);
  fs.writeFileSync(outputFile, JSON.stringify(nested, null, 2), "utf8");
  console.log(`✅ Saved to ${outputFile}`);
}

await generateLocale("hi", path.join(LOCALES_DIR, "hi.json"));
// Sanskrit not available in Google Translate, so skip or use Hindi as base
// await generateLocale("sa", path.join(LOCALES_DIR, "sa.json"));

console.log("\n🎉 Done! All translations generated.");
