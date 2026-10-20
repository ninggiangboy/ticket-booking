package io.ticket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.ticket.auth.service.ReturnToSanitizer;
import org.junit.jupiter.api.Test;

class ReturnToSanitizerTest {

  private final ReturnToSanitizer sanitizer = new ReturnToSanitizer();

  @Test
  void testSanitizerRules() {
    assertThat(sanitizer.sanitize("/checkout/0199f3c2-7a10-7c4e-9b2a-3d6f1e8a5b01"))
        .isEqualTo("/checkout/0199f3c2-7a10-7c4e-9b2a-3d6f1e8a5b01");
    assertThat(sanitizer.sanitize("/events/0199f3a0?tab=map"))
        .isEqualTo("/events/0199f3a0?tab=map");
    assertThat(sanitizer.sanitize("//evil.example")).isEqualTo("/");
    assertThat(sanitizer.sanitize("/\\evil.example")).isEqualTo("/");
    assertThat(sanitizer.sanitize("https://evil.example/x")).isEqualTo("/");
    assertThat(sanitizer.sanitize("/" + "a".repeat(513))).isEqualTo("/");
    assertThat(sanitizer.sanitize(null)).isEqualTo("/");
    assertThat(sanitizer.sanitize("")).isEqualTo("/");
    assertThat(sanitizer.sanitize("/a%0d%0aSet-Cookie:x")).isEqualTo("/");
  }
}
