package io.ticket.auth.service;

import io.ticket.auth.repository.AppUserRepository;
import io.ticket.auth.repository.LoginTokenRepository;
import io.ticket.auth.repository.SessionRepository;
import io.ticket.common.error.ErrorCode;
import io.ticket.common.error.UnauthenticatedException;
import io.ticket.common.error.ValidationFailedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consumes magic-link tokens and creates sessions (DOC-19 §4, DOC-83 FL-02). */
@Service
public class LoginService {

  private static final Logger log = LoggerFactory.getLogger(LoginService.class);
  private final SecureRandom secureRandom = new SecureRandom();

  private final LoginTokenRepository loginTokenRepository;
  private final AppUserRepository appUserRepository;
  private final SessionRepository sessionRepository;
  private final ReturnToSanitizer returnToSanitizer;

  public LoginService(
      LoginTokenRepository loginTokenRepository,
      AppUserRepository appUserRepository,
      SessionRepository sessionRepository,
      ReturnToSanitizer returnToSanitizer) {
    this.loginTokenRepository = loginTokenRepository;
    this.appUserRepository = appUserRepository;
    this.sessionRepository = sessionRepository;
    this.returnToSanitizer = returnToSanitizer;
  }

  public record LoginResult(String rawSessionId, String returnTo, String csrfToken) {}

  /** One transaction: consume the token, upsert the user, insert the session (DOC-19 §4). */
  @Transactional
  public LoginResult verify(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new ValidationFailedException("token", "required");
    }

    byte[] tokenHash = sha256(rawToken);

    var consumedOpt = loginTokenRepository.consume(tokenHash);
    if (consumedOpt.isEmpty()) {
      log.info("login.invalid");
      throw new UnauthenticatedException(ErrorCode.LOGIN_LINK_INVALID);
    }

    var consumed = consumedOpt.get();
    var userRecord = appUserRepository.upsertUserOnLogin(consumed.email(), consumed.locale());

    byte[] sessionBytes = new byte[32];
    secureRandom.nextBytes(sessionBytes);
    String rawSessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(sessionBytes);
    byte[] sessionHash = sha256(rawSessionId);

    byte[] csrfBytes = new byte[32];
    secureRandom.nextBytes(csrfBytes);
    String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(csrfBytes);

    sessionRepository.insertSession(sessionHash, userRecord.userId(), csrfToken);

    String sanitizedReturnTo = returnToSanitizer.sanitize(consumed.returnTo());

    return new LoginResult(rawSessionId, sanitizedReturnTo, csrfToken);
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
