package io.ticket.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.ticket.common.error.ErrorCode;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * ERR-01, ERR-12 and the parity rule of DOC-31 §9: both locales carry the same keys and
 * placeholders.
 */
class MessagesParityTest {

  private static Properties load(String locale) throws Exception {
    Properties p = new Properties();
    try (var in =
        new InputStreamReader(
            MessagesParityTest.class.getResourceAsStream("/messages_" + locale + ".properties"),
            StandardCharsets.UTF_8)) {
      p.load(in);
    }
    return p;
  }

  @Test
  void bothLocalesHaveTheSameKeysAndPlaceholders() throws Exception {
    Properties vi = load("vi");
    Properties en = load("en");
    assertThat(en.stringPropertyNames()).isEqualTo(vi.stringPropertyNames());
    Pattern placeholder = Pattern.compile("\\{(\\d+)}");
    for (String key : vi.stringPropertyNames()) {
      assertThat(placeholders(placeholder, en.getProperty(key)))
          .as(key)
          .isEqualTo(placeholders(placeholder, vi.getProperty(key)));
    }
  }

  @Test
  void everyErrorCodeHasATitleAndDetailInBothLocales() throws Exception {
    assertThat(ErrorCode.values()).hasSize(40);
    for (String locale : new String[] {"vi", "en"}) {
      Properties p = load(locale);
      for (ErrorCode code : ErrorCode.values()) {
        assertThat(p.getProperty("problem." + code.key() + ".title"))
            .as(locale + " " + code)
            .isNotBlank();
        assertThat(p.getProperty("problem." + code.key() + ".detail"))
            .as(locale + " " + code)
            .isNotBlank();
      }
    }
  }

  private static java.util.Set<String> placeholders(Pattern pattern, String text) {
    Matcher m = pattern.matcher(text);
    java.util.Set<String> found = new java.util.TreeSet<>();
    while (m.find()) found.add(m.group(1));
    return found;
  }
}
