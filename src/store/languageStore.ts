import { create } from "zustand";
import { persist } from "zustand/middleware";

// ── Supported languages ───────────────────────────────────────────────────────
// Add more codes here after running `node scripts/translate.mjs`
// and importing the JSON in src/lib/i18n.ts

export type AppLanguage =
  | "EN" | "HI" | "SA"
  | "MR" | "BN" | "GU" | "TA" | "TE" | "KN" | "PA" | "ML" | "OR"
  ;

export interface LangMeta {
  code: AppLanguage;
  i18nCode: string;       // maps to i18next language code (lowercase)
  name: string;
  nativeName: string;
  apiLang: string;        // sent to backend for AI response language
  script: "devanagari" | "latin" | "other";
}

export const LANG_META: Record<AppLanguage, LangMeta> = {
  EN: { code: "EN", i18nCode: "en", name: "English",    nativeName: "English",     apiLang: "english",    script: "latin" },
  HI: { code: "HI", i18nCode: "hi", name: "Hindi",      nativeName: "हिंदी",       apiLang: "hindi",      script: "devanagari" },
  SA: { code: "SA", i18nCode: "sa", name: "Sanskrit",   nativeName: "संस्कृतम्",   apiLang: "sanskrit",   script: "devanagari" },
  MR: { code: "MR", i18nCode: "mr", name: "Marathi",    nativeName: "मराठी",       apiLang: "marathi",    script: "devanagari" },
  BN: { code: "BN", i18nCode: "bn", name: "Bengali",    nativeName: "বাংলা",       apiLang: "bengali",    script: "other" },
  GU: { code: "GU", i18nCode: "gu", name: "Gujarati",   nativeName: "ગુજરાતી",     apiLang: "gujarati",   script: "other" },
  TA: { code: "TA", i18nCode: "ta", name: "Tamil",      nativeName: "தமிழ்",       apiLang: "tamil",      script: "other" },
  TE: { code: "TE", i18nCode: "te", name: "Telugu",     nativeName: "తెలుగు",      apiLang: "telugu",     script: "other" },
  KN: { code: "KN", i18nCode: "kn", name: "Kannada",    nativeName: "ಕನ್ನಡ",       apiLang: "kannada",    script: "other" },
  PA: { code: "PA", i18nCode: "pa", name: "Punjabi",    nativeName: "ਪੰਜਾਬੀ",      apiLang: "punjabi",    script: "other" },
  ML: { code: "ML", i18nCode: "ml", name: "Malayalam",  nativeName: "മലയാളം",      apiLang: "malayalam",  script: "other" },
  OR: { code: "OR", i18nCode: "or", name: "Odia",       nativeName: "ଓଡ଼ିଆ",        apiLang: "odia",       script: "other" },
};

// Active language list (controls what shows in the switcher)
export const ACTIVE_LANGS: AppLanguage[] = ["EN", "HI", "SA", "MR", "BN", "GU", "TA", "TE", "KN", "PA", "ML", "OR"];

/** Maps UI lang → API lang param expected by backend */
export const LANG_TO_API: Record<AppLanguage, string> = Object.fromEntries(
  Object.entries(LANG_META).map(([k, v]) => [k, v.apiLang])
) as Record<AppLanguage, string>;

// ── Store ─────────────────────────────────────────────────────────────────────

interface LanguageState {
  lang: AppLanguage;
  setLang: (lang: AppLanguage) => void;
  cycleLang: () => void;
}

export const useLanguageStore = create<LanguageState>()(
  persist(
    (set, get) => ({
      lang: "EN",
      setLang: (lang) => set({ lang }),
      cycleLang: () => {
        const idx = ACTIVE_LANGS.indexOf(get().lang);
        set({ lang: ACTIVE_LANGS[(idx + 1) % ACTIVE_LANGS.length] });
      },
    }),
    { name: "brahm-language" }
  )
);
