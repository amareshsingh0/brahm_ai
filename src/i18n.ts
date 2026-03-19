import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import enTranslations from "./locales/en/translation.json";
import hiTranslations from "./locales/hi/translation.json";
import saTranslations from "./locales/sa/translation.json";

const resources = {
  en: { translation: enTranslations },
  hi: { translation: hiTranslations },
  sa: { translation: saTranslations },
};

i18n.use(initReactI18next).init({
  resources,
  lng: "en", // default language
  fallbackLng: "en",
  interpolation: { escapeValue: false },
});

export default i18n;
