package io.ticket.common.i18n;

import java.util.List;
import java.util.Locale;

/**
 * Locale choice in the order of DOC-31 §2: account locale, cookie {@code tb_lang}, {@code
 * Accept-Language} by weight and language prefix, then {@code vi}.
 */
public final class LocaleSelector {

  private LocaleSelector() {}

  public static Locale select(String userLocale, String cookieLang, String acceptLanguage) {
    if (SupportedLocales.isSupported(userLocale)) {
      return Locale.forLanguageTag(userLocale);
    }
    if (SupportedLocales.isSupported(cookieLang)) {
      return Locale.forLanguageTag(cookieLang);
    }
    String fromHeader = fromAcceptLanguage(acceptLanguage);
    return fromHeader != null ? Locale.forLanguageTag(fromHeader) : SupportedLocales.DEFAULT;
  }

  private static String fromAcceptLanguage(String header) {
    if (header == null || header.isBlank()) {
      return null;
    }
    List<Locale.LanguageRange> ranges;
    try {
      ranges = Locale.LanguageRange.parse(header);
    } catch (IllegalArgumentException e) {
      return null;
    }
    for (Locale.LanguageRange range : ranges) {
      String tag = range.getRange();
      int dash = tag.indexOf('-');
      String language = dash < 0 ? tag : tag.substring(0, dash);
      if (SupportedLocales.isSupported(language)) {
        return language;
      }
    }
    return null;
  }
}
