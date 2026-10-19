import i18next, { type BackendModule, type ResourceKey } from "i18next";
import ICU from "i18next-icu";
import { initReactI18next } from "react-i18next";
import { DEFAULT_LOCALE, SUPPORTED_LOCALES, type Locale } from "../lib/locale";

// i18next with ICU placeholders ({name}) and one lazily loaded JSON per locale and namespace
// (DOC-31 §3, DOC-40 §0). Keys are flat and carry their namespace ("auth.form.title"), so the
// key separator is off and a component lists the namespaces it reads.

export const NAMESPACES = ["common", "auth", "errors", "events", "tickets"] as const;
export type Namespace = (typeof NAMESPACES)[number];

/** Namespaces the shell needs before the first paint (header, error pages). */
const SHELL_NAMESPACES: Namespace[] = ["common", "errors"];

const catalogs = import.meta.glob<ResourceKey>("../locales/*/*.json", { import: "default" });

const lazyCatalogs: BackendModule = {
  type: "backend",
  init() {},
  read(language, namespace, callback) {
    const load = catalogs[`../locales/${language}/${namespace}.json`];
    if (!load) {
      callback(new Error(`no catalog ${language}/${namespace}`), false);
      return;
    }
    load().then(
      (resources) => callback(null, resources),
      (error: unknown) => callback(error as Error, false),
    );
  },
};

export async function initI18n(language: Locale): Promise<typeof i18next> {
  i18next.on("languageChanged", (lng) => {
    document.documentElement.lang = lng;
  });
  await i18next
    .use(ICU)
    .use(lazyCatalogs)
    .use(initReactI18next)
    .init({
      lng: language,
      fallbackLng: DEFAULT_LOCALE,
      supportedLngs: [...SUPPORTED_LOCALES],
      nonExplicitSupportedLngs: false,
      load: "currentOnly",
      ns: SHELL_NAMESPACES,
      defaultNS: "common",
      keySeparator: false,
      nsSeparator: false,
      interpolation: { escapeValue: false },
      react: { useSuspense: true, nsMode: "fallback" },
    });
  return i18next;
}
