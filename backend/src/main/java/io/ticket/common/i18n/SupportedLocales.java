package io.ticket.common.i18n;

import java.util.List;
import java.util.Locale;

/**
 * Locales the product supports; {@code vi} is both the default and the fallback (DR-10, DOC-31 §1).
 */
public final class SupportedLocales {

  public static final Locale DEFAULT = Locale.forLanguageTag("vi");
  public static final List<String> CODES = List.of("vi", "en");

  private SupportedLocales() {}

  public static boolean isSupported(String code) {
    return code != null && CODES.contains(code);
  }
}
