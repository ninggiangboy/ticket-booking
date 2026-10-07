package io.ticket.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.ticket.common.i18n.LocaleSelector;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** The table of DOC-31 §2. */
class LocaleSelectorTest {

  private static Locale pick(String user, String cookie, String header) {
    return LocaleSelector.select(user, cookie, header);
  }

  @Test
  void accountLocaleWinsOverCookieAndHeader() {
    assertThat(pick("en", "vi", "vi")).isEqualTo(Locale.forLanguageTag("en"));
  }

  @Test
  void cookieWinsOverHeader() {
    assertThat(pick(null, "vi", "en-US,en;q=0.9")).isEqualTo(Locale.forLanguageTag("vi"));
  }

  @Test
  void headerIsMatchedByLanguagePrefixAndWeight() {
    assertThat(pick(null, null, "en-US,en;q=0.9")).isEqualTo(Locale.forLanguageTag("en"));
    assertThat(pick(null, null, "fr;q=0.9,vi-VN;q=0.8")).isEqualTo(Locale.forLanguageTag("vi"));
    assertThat(pick(null, null, "en;q=0.5,vi;q=0.9")).isEqualTo(Locale.forLanguageTag("vi"));
  }

  @Test
  void unsupportedOrMissingFallsBackToVietnamese() {
    assertThat(pick(null, null, "fr-FR,fr;q=0.9")).isEqualTo(Locale.forLanguageTag("vi"));
    assertThat(pick(null, "de", "en")).isEqualTo(Locale.forLanguageTag("en"));
    assertThat(pick(null, null, null)).isEqualTo(Locale.forLanguageTag("vi"));
    assertThat(pick(null, null, ";;;garbage")).isEqualTo(Locale.forLanguageTag("vi"));
  }
}
