import { EN_TEXT } from "./i18n-en.mjs";

export const LANGUAGE_STORAGE_KEY = "agenda-sacramental-language";

export function resolveLanguage({ requested, saved, browser = "es" } = {}) {
  if (requested === "en" || requested === "es") return requested;
  if (saved === "en" || saved === "es") return saved;
  return browser.toLowerCase().startsWith("en") ? "en" : "es";
}

export function currentLanguage() {
  let saved;
  try { saved = globalThis.localStorage?.getItem(LANGUAGE_STORAGE_KEY); } catch { /* Private browsing. */ }
  return resolveLanguage({
    requested: new URLSearchParams(globalThis.location?.search || "").get("lang"),
    saved,
    browser: globalThis.navigator?.language || "es"
  });
}

export function translate(text, language = currentLanguage()) {
  return language === "en" ? EN_TEXT[text] ?? text : text;
}
