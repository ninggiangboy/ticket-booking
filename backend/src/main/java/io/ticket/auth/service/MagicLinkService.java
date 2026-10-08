package io.ticket.auth.service;

import io.ticket.auth.repository.LoginTokenRepository;
import io.ticket.common.error.ProviderUnavailableException;
import io.ticket.common.error.ValidationFailedException;
import io.ticket.common.i18n.SupportedLocales;
import io.ticket.common.mail.AppProperties;
import io.ticket.common.mail.EmailRenderer;
import io.ticket.common.mail.MailSender;
import io.ticket.common.mail.OutgoingEmail;
import io.ticket.common.mail.RenderedEmail;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Handles magic-link request, rate-limiting, and SMTP sending (DOC-19 §3, DOC-83 FL-01). */
@Service
public class MagicLinkService {

  private static final Logger log = LoggerFactory.getLogger(MagicLinkService.class);
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private final SecureRandom secureRandom = new SecureRandom();

  private final LoginTokenRepository loginTokenRepository;
  private final MailSender mailSender;
  private final EmailRenderer emailRenderer;
  private final AppProperties appProperties;
  private final ReturnToSanitizer returnToSanitizer;
  private final TransactionTemplate transactionTemplate;

  public MagicLinkService(
      LoginTokenRepository loginTokenRepository,
      MailSender mailSender,
      EmailRenderer emailRenderer,
      AppProperties appProperties,
      ReturnToSanitizer returnToSanitizer,
      TransactionTemplate transactionTemplate) {
    this.loginTokenRepository = loginTokenRepository;
    this.mailSender = mailSender;
    this.emailRenderer = emailRenderer;
    this.appProperties = appProperties;
    this.returnToSanitizer = returnToSanitizer;
    this.transactionTemplate = transactionTemplate;
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

  private record TxResult(
      boolean throttled, String rawToken, byte[] tokenHash, String email, String locale) {}

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

    // T1 transaction: lock, check rate limits, supersede old tokens, insert new token, commit
    TxResult txResult =
        transactionTemplate.execute(
            status -> {
              loginTokenRepository.lockEmail(normalizedEmail);
              Instant now = Instant.now();
              long emailCount =
                  loginTokenRepository.countByEmailSince(
                      normalizedEmail, now.minus(Duration.ofMinutes(15)));
              long ipCount =
                  loginTokenRepository.countByIpSince(requestedIp, now.minus(Duration.ofHours(1)));

              if (emailCount >= 3 || ipCount >= 10) {
                return new TxResult(true, null, null, normalizedEmail, locale);
              }

              loginTokenRepository.markSuperseded(normalizedEmail);

              byte[] randomBytes = new byte[32];
              secureRandom.nextBytes(randomBytes);
              String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
              byte[] tokenHash = sha256(rawToken);

              Instant expiresAt = now.plus(Duration.ofMinutes(15));
              loginTokenRepository.insertToken(
                  tokenHash, normalizedEmail, sanitizedReturnTo, locale, requestedIp, expiresAt);

              return new TxResult(false, rawToken, tokenHash, normalizedEmail, locale);
            });

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
    Locale targetLocale = Locale.forLanguageTag(txResult.locale());

    RenderedEmail content =
        emailRenderer.render(
            "magic-link",
            targetLocale,
            Map.of("appName", appProperties.name(), "linkUrl", linkUrl, "expiresInMinutes", 15));

    OutgoingEmail outgoing = new OutgoingEmail(txResult.email(), messageId, targetLocale, content);

    try {
      mailSender.send(outgoing);
      log.info("magic_link.requested result=sent");
    } catch (Exception e) {
      log.warn("magic_link.smtp_failed exception={}", e.getClass().getSimpleName());
      throw ProviderUnavailableException.email(5);
    }

    return new MagicLinkResult(MagicLinkStatus.SENT);
  }

  private static byte[] sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
