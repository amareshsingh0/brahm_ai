import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "@/locales/en.json";

export const SUPPORTED_LANGUAGES = [
  { code: "en", name: "English",    nativeName: "English",    flag: "🇬🇧" },
  { code: "hi", name: "Hindi",      nativeName: "हिंदी",      flag: "🇮🇳" },
  { code: "mr", name: "Marathi",    nativeName: "मराठी",      flag: "🇮🇳" },
  { code: "bn", name: "Bengali",    nativeName: "বাংলা",      flag: "🇮🇳" },
  { code: "gu", name: "Gujarati",   nativeName: "ગુજરાતી",    flag: "🇮🇳" },
  { code: "ta", name: "Tamil",      nativeName: "தமிழ்",      flag: "🇮🇳" },
  { code: "te", name: "Telugu",     nativeName: "తెలుగు",     flag: "🇮🇳" },
  { code: "kn", name: "Kannada",    nativeName: "ಕನ್ನಡ",      flag: "🇮🇳" },
  { code: "pa", name: "Punjabi",    nativeName: "ਪੰਜਾਬੀ",     flag: "🇮🇳" },
  { code: "ml", name: "Malayalam",  nativeName: "മലയാളം",     flag: "🇮🇳" },
  { code: "or", name: "Odia",       nativeName: "ଓଡ଼ିଆ",      flag: "🇮🇳" },
] as const;

export type LangCode = typeof SUPPORTED_LANGUAGES[number]["code"];

// ── Dynamic loader — only loads the requested language chunk ──────────────────
const LOADERS: Record<string, () => Promise<unknown>> = {
  hi: () => import("@/locales/hi.json"),
  sa: () => import("@/locales/sa.json"),
  mr: () => import("@/locales/mr.json"),
  bn: () => import("@/locales/bn.json"),
  gu: () => import("@/locales/gu.json"),
  ta: () => import("@/locales/ta.json"),
  te: () => import("@/locales/te.json"),
  kn: () => import("@/locales/kn.json"),
  pa: () => import("@/locales/pa.json"),
  ml: () => import("@/locales/ml.json"),
  or: () => import("@/locales/or.json"),
};

export async function loadLanguage(lang: string): Promise<void> {
  if (lang === "en" || i18n.hasResourceBundle(lang, "translation")) return;
  const loader = LOADERS[lang];
  if (!loader) return;
  const mod = await loader() as { default?: Record<string, unknown> } | Record<string, unknown>;
  const data = (mod as { default?: Record<string, unknown> }).default ?? mod;
  i18n.addResourceBundle(lang, "translation", data, true, true);
}

// ── Detect language ───────────────────────────────────────────────────────────
function detectLanguage(): string {
  try {
    const stored = localStorage.getItem("brahm-language");
    if (stored) {
      const lang = JSON.parse(stored)?.state?.lang?.toLowerCase();
      if (lang && SUPPORTED_LANGUAGES.some((l) => l.code === lang)) return lang;
    }
  } catch {}
  const browserLang = navigator.language.split("-")[0].toLowerCase();
  if (SUPPORTED_LANGUAGES.some((l) => l.code === browserLang)) return browserLang;
  return "en";
}

// ── Init — only English bundled, others loaded on demand ─────────────────────
const detectedLang = detectLanguage();

i18n.use(initReactI18next).init({
  resources: { en: { translation: en } },
  lng: "en", // start with en, switch after async load below
  fallbackLng: "en",
  interpolation: { escapeValue: false },
  initImmediate: false,
});

// Load user's language async (non-blocking — en shows first, then switches)
if (detectedLang !== "en") {
  loadLanguage(detectedLang).then(() => {
    i18n.changeLanguage(detectedLang);
  });
}

export default i18n;
