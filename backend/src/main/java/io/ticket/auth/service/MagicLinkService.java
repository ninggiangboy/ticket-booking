package io.ticket.auth.service;

import io.ticket.common.error.ProviderUnavailableException;
import io.ticket.common.error.ValidationFailedException;
import io.ticket.common.i18n.SupportedLocales;
import io.ticket.common.mail.AppProperties;
import io.ticket.common.mail.EmailRenderer;
import io.ticket.common.mail.MailSender;
import io.ticket.common.mail.OutgoingEmail;
import io.ticket.common.mail.RenderedEmail;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/** Handles magic-link request, rate-limiting, and SMTP sending (DOC-19 §3, DOC-83 FL-01). */
@Service
public class MagicLinkService {

  private static final Logger log = LoggerFactory.getLogger(MagicLinkService.class);
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final MagicLinkTxService magicLinkTxService;
  private final MailSender mailSender;
  private final EmailRenderer emailRenderer;
  private final AppProperties appProperties;
  private final ReturnToSanitizer returnToSanitizer;

  public MagicLinkService(
      MagicLinkTxService magicLinkTxService,
      MailSender mailSender,
      EmailRenderer emailRenderer,
      AppProperties appProperties,
      ReturnToSanitizer returnToSanitizer) {
    this.magicLinkTxService = magicLinkTxService;
    this.mailSender = mailSender;
    this.emailRenderer = emailRenderer;
    this.appProperties = appProperties;
    this.returnToSanitizer = returnToSanitizer;
  }

  public enum MagicLinkStatus {
    SENT,
    THROTTLED
  }

  public record MagicLinkResult(MagicLinkStatus status) {
    public boolean isThrottled() {
      return status == MagicLinkStatus.THROTTLED;
    }
  }

  public MagicLinkResult requestMagicLink(
      String rawEmail, String rawReturnTo, String requestedLocale, String requestedIp) {
    if (rawEmail == null || rawEmail.isBlank()) {
      throw new ValidationFailedException("email", "invalid_email");
    }

    String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
    if (normalizedEmail.length() > 254 || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
      throw new ValidationFailedException("email", "invalid_email");
    }

    String sanitizedReturnTo = returnToSanitizer.sanitize(rawReturnTo);

    String locale =
        SupportedLocales.isSupported(requestedLocale)
            ? requestedLocale
            : (SupportedLocales.isSupported(LocaleContextHolder.getLocale().getLanguage())
                ? LocaleContextHolder.getLocale().getLanguage()
                : "vi");

    // T1 transaction (MagicLinkTxService): lock, rate limits, supersede old tokens, insert
    MagicLinkTxService.Issued txResult =
        magicLinkTxService.issue(normalizedEmail, sanitizedReturnTo, locale, requestedIp);

    if (txResult.throttled()) {
      log.info("magic_link.requested result=throttled");
      return new MagicLinkResult(MagicLinkStatus.THROTTLED);
    }

    // After T1 commit: send SMTP directly
    String messageId =
        "login-"
            + HexFormat.of().formatHex(txResult.tokenHash(), 0, 6)
            + "@"
            + appProperties.domain();
    String linkUrl = appProperties.baseUrl() + "/auth/callback?token=" + txResult.rawToken();
    Locale targetLocale = Locale.forLanguageTag(locale);

    RenderedEmail content =
        emailRenderer.render(
            "magic-link",
            targetLocale,
            Map.of("appName", appProperties.name(), "linkUrl", linkUrl, "expiresInMinutes", 15));

    OutgoingEmail outgoing = new OutgoingEmail(normalizedEmail, messageId, targetLocale, content);

    try {
      mailSender.send(outgoing);
      log.info("magic_link.requested result=sent");
    } catch (Exception e) {
      log.warn("magic_link.smtp_failed exception={}", e.getClass().getSimpleName());
      throw ProviderUnavailableException.email(5);
    }

    return new MagicLinkResult(MagicLinkStatus.SENT);
  }
}
