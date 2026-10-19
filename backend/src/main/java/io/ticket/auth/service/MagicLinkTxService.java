package io.ticket.auth.service;

import io.ticket.auth.repository.LoginTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Short transaction of a magic link request; SMTP runs after it commits (DOC-19 §3). */
@Service
public class MagicLinkTxService {

  private static final int EMAIL_LIMIT = 3;
  private static final int IP_LIMIT = 10;
  private static final int EMAIL_WINDOW_SECONDS = (int) Duration.ofMinutes(15).toSeconds();
  private static final int IP_WINDOW_SECONDS = (int) Duration.ofHours(1).toSeconds();
  private static final int TTL_SECONDS = (int) Duration.ofMinutes(15).toSeconds();

  private final SecureRandom secureRandom = new SecureRandom();
  private final LoginTokenRepository loginTokenRepository;

  public MagicLinkTxService(LoginTokenRepository loginTokenRepository) {
    this.loginTokenRepository = loginTokenRepository;
  }

  /** Outcome of {@link #issue}; token fields are null when throttled. */
  public record Issued(boolean throttled, String rawToken, byte[] tokenHash) {}

  @Transactional
  public Issued issue(String email, String returnTo, String locale, String requestedIp) {
    loginTokenRepository.lockEmail(email);
    int emailCount = loginTokenRepository.countByEmailSince(email, EMAIL_WINDOW_SECONDS);
    int ipCount = loginTokenRepository.countByIpSince(requestedIp, IP_WINDOW_SECONDS);
    if (emailCount >= EMAIL_LIMIT || ipCount >= IP_LIMIT) {
      return new Issued(true, null, null);
    }

    loginTokenRepository.supersedeUnused(email);

    byte[] randomBytes = new byte[32];
    secureRandom.nextBytes(randomBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    byte[] tokenHash = sha256(rawToken);

    loginTokenRepository.insert(tokenHash, email, returnTo, locale, requestedIp, TTL_SECONDS);
    return new Issued(false, rawToken, tokenHash);
  }

  private static byte[] sha256(String input) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
