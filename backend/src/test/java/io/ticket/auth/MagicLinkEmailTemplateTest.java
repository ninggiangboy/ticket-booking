package io.ticket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.ticket.common.i18n.I18nConfig;
import io.ticket.common.mail.EmailRenderer;
import io.ticket.common.mail.RenderedEmail;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = EmailRenderer.class)
@Import(I18nConfig.class)
class MagicLinkEmailTemplateTest {

  @Autowired private EmailRenderer emailRenderer;

  @Test
  void testMagicLinkTemplateVi() { // EML-01
    String token = "VGhpc0lzQUZha2VUb2tlbkZvckRvY3VtZW50YXRpb25Pbmx5MQ";
    String linkUrl = "http://localhost:8080/auth/callback?token=" + token;

    RenderedEmail rendered =
        emailRenderer.render(
            "magic-link",
            Locale.forLanguageTag("vi"),
            Map.of("appName", "ticket", "linkUrl", linkUrl, "expiresInMinutes", 15));

    assertThat(rendered.subject()).isEqualTo("Đường dẫn đăng nhập của bạn");
    assertThat(rendered.html()).contains(linkUrl);
    assertThat(rendered.text()).contains(linkUrl);
    assertThat(rendered.html()).doesNotContain("alice@example.com");
  }

  @Test
  void testMagicLinkTemplateEn() { // EML-02
    String token = "VGhpc0lzQUZha2VUb2tlbkZvckRvY3VtZW50YXRpb25Pbmx5MQ";
    String linkUrl = "http://localhost:8080/auth/callback?token=" + token;

    RenderedEmail rendered =
        emailRenderer.render(
            "magic-link",
            Locale.forLanguageTag("en"),
            Map.of("appName", "ticket", "linkUrl", linkUrl, "expiresInMinutes", 15));

    assertThat(rendered.subject()).isEqualTo("Your sign-in link");
    assertThat(rendered.html()).contains(linkUrl);
    assertThat(rendered.text()).contains(linkUrl);
    assertThat(rendered.html()).contains("Sign in");
  }
}
