import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "@/locales/en.json";
import hi from "@/locales/hi.json";
import sa from "@/locales/sa.json";
import mr from "@/locales/mr.json";
import bn from "@/locales/bn.json";
import gu from "@/locales/gu.json";
import ta from "@/locales/ta.json";
import te from "@/locales/te.json";
import kn from "@/locales/kn.json";
import pa from "@/locales/pa.json";
import ml from "@/locales/ml.json";
import or from "@/locales/or.json";

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

// ── Detect language ───────────────────────────────────────────────────────────

function detectLanguage(): string {
  // 1. User manually set a preference
  try {
    const stored = localStorage.getItem("brahm-language");
    if (stored) {
      const lang = JSON.parse(stored)?.state?.lang?.toLowerCase();
      if (lang && SUPPORTED_LANGUAGES.some((l) => l.code === lang)) return lang;
    }
  } catch {}

  // 2. Browser/phone language
  const browserLang = navigator.language.split("-")[0].toLowerCase();
  if (SUPPORTED_LANGUAGES.some((l) => l.code === browserLang)) return browserLang;

  return "en";
}

// ── Init i18next ──────────────────────────────────────────────────────────────

i18n.use(initReactI18next).init({
  resources: {
    en: { translation: en },
    hi: { translation: hi },
    sa: { translation: sa },
    mr: { translation: mr },
    bn: { translation: bn },
    gu: { translation: gu },
    ta: { translation: ta },
    te: { translation: te },
    kn: { translation: kn },
    pa: { translation: pa },
    ml: { translation: ml },
    or: { translation: or },
  },
  lng: detectLanguage(),
  fallbackLng: "en",
  interpolation: { escapeValue: false },
  initImmediate: false,
});

export default i18n;
