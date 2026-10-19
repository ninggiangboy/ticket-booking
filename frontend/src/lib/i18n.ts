import i18next from "i18next";
import { DEFAULT_LOCALE, isLocale, type Locale } from "./locale";

export function currentLocale(): Locale {
  const language = i18next.resolvedLanguage ?? i18next.language;
  return isLocale(language) ? language : DEFAULT_LOCALE;
}

/**
 * Loads every namespace in use for `locale`, then switches. A failed load keeps the current
 * language (FL-04 E3) and rejects.
 */
export async function switchLanguage(locale: Locale): Promise<void> {
  await i18next.loadLanguages(locale);
  const namespaces = [i18next.options.ns ?? []].flat();
  if (namespaces.some((ns) => !i18next.hasResourceBundle(locale, ns))) {
    throw new Error(`could not load ${locale}`);
  }
  await i18next.changeLanguage(locale);
}
