import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "@/locales/en.json";
import hi from "@/locales/hi.json";
import sa from "@/locales/sa.json";

i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      hi: { translation: hi },
      sa: { translation: sa },
    },
    lng: (() => {
      // 1. User ne manually set kiya ho toh wahi use karo
      const stored = localStorage.getItem("brahm-language");
      if (stored) {
        const lang = JSON.parse(stored)?.state?.lang?.toLowerCase();
        if (lang && ["en", "hi", "sa"].includes(lang)) return lang;
      }
      // 2. Phone/browser ki language detect karo
      const browserLang = navigator.language.split("-")[0].toLowerCase();
      if (browserLang === "hi") return "hi";
      if (browserLang === "sa") return "sa";
      return "en";
    })(),
    fallbackLng: "en",
    interpolation: { escapeValue: false },
    initImmediate: false,
  });

export default i18n;
