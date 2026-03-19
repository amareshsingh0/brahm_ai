import { create } from "zustand";
import { persist } from "zustand/middleware";

export type AppLanguage = "EN" | "HI" | "SA";

export const LANG_LABELS: Record<AppLanguage, string> = {
  EN: "English",
  HI: "हिन्दी",
  SA: "संस्कृतम्",
};

/** Maps UI lang → API lang param expected by backend */
export const LANG_TO_API: Record<AppLanguage, string> = {
  EN: "english",
  HI: "hindi",
  SA: "sanskrit",
};

const CYCLE: AppLanguage[] = ["EN", "HI", "SA"];

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
        const idx = CYCLE.indexOf(get().lang);
        set({ lang: CYCLE[(idx + 1) % CYCLE.length] });
      },
    }),
    { name: "brahm-language" }
  )
);
