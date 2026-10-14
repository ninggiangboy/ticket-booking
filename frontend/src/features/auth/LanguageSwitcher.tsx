import { useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { SUPPORTED_LOCALES, type Locale } from "../../lib/locale";
import { changeLocale } from "./localeSync";
import styles from "./LanguageSwitcher.module.css";

/** `vi | en` toggle in every header (DOC-39 §7.19, FL-04). Switching never reloads the page. */
export function LanguageSwitcher() {
  const { t, i18n } = useTranslation("common");
  const queryClient = useQueryClient();
  const active = i18n.resolvedLanguage;
  const names: Record<Locale, string> = { vi: t("common.lang.vi"), en: t("common.lang.en") };

  const select = (locale: Locale) => {
    if (locale === active) return;
    changeLocale(locale, queryClient).catch((error: unknown) => {
      // catalog failed to load: keep the current language (FL-04 E3)
      console.error(error);
    });
  };

  return (
    <div className={styles.group} role="group" aria-label={t("common.lang.label")}>
      {SUPPORTED_LOCALES.map((locale) => (
        <button
          key={locale}
          type="button"
          lang={locale}
          className={styles.option}
          aria-pressed={locale === active}
          aria-label={names[locale]}
          onClick={() => select(locale)}
        >
          {locale}
        </button>
      ))}
    </div>
  );
}
