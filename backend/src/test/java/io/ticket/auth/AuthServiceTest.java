package io.ticket.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import io.ticket.auth.repository.AppUserRepository;
import io.ticket.auth.repository.LoginTokenRepository;
import io.ticket.auth.repository.SessionRepository;
import io.ticket.auth.service.LoginService;
import io.ticket.auth.service.MagicLinkService;
import io.ticket.auth.service.ReturnToSanitizer;
import io.ticket.common.error.ErrorCode;
import io.ticket.common.error.ProviderUnavailableException;
import io.ticket.common.error.UnauthenticatedException;
import io.ticket.common.error.ValidationFailedException;
import io.ticket.common.mail.AppProperties;
import io.ticket.common.mail.EmailDeliveryException;
import io.ticket.common.mail.EmailRenderer;
import io.ticket.common.mail.MailSender;
import io.ticket.common.mail.RenderedEmail;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private LoginTokenRepository loginTokenRepository;
  @Mock private AppUserRepository appUserRepository;
  @Mock private SessionRepository sessionRepository;
  @Mock private MailSender mailSender;
  @Mock private EmailRenderer emailRenderer;
  @Mock private TransactionTemplate transactionTemplate;

  private final AppProperties appProperties =
      new AppProperties("ticket", "http://localhost:8080", "ticket.localhost");
  private final ReturnToSanitizer returnToSanitizer = new ReturnToSanitizer();

  private MagicLinkService magicLinkService;
  private LoginService loginService;

  @BeforeEach
  void setUp() {
    // Make transactionTemplate execute callback directly
    org.mockito.Mockito.lenient()
        .doAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(new SimpleTransactionStatus());
            })
        .when(transactionTemplate)
        .execute(any());

    magicLinkService =
        new MagicLinkService(
            loginTokenRepository,
            mailSender,
            emailRenderer,
            appProperties,
            returnToSanitizer,
            transactionTemplate);

    loginService =
        new LoginService(
            loginTokenRepository,
            appUserRepository,
            sessionRepository,
            returnToSanitizer,
            transactionTemplate);
  }

  @Test
  void requestMagicLinkNormalizesEmailAndSendsMail() { // AU-01, FLA-01, FLA-08
    given(emailRenderer.render(any(), any(), any()))
        .willReturn(new RenderedEmail("Subject", "<p>html</p>", "text"));

    var result =
        magicLinkService.requestMagicLink(" Alice@Example.Com ", "/checkout", "vi", "127.0.0.1");

    assertThat(result.isThrottled()).isFalse();

    verify(loginTokenRepository).markSuperseded("alice@example.com");
    verify(loginTokenRepository)
        .insertToken(
            any(), eq("alice@example.com"), eq("/checkout"), eq("vi"), eq("127.0.0.1"), any());
    verify(mailSender).send(any());
  }

  @Test
  void requestMagicLinkEmailRateLimitReached() { // AU-05, FLA-03
    given(loginTokenRepository.countByEmailSince(eq("alice@example.com"), any())).willReturn(3L);

    var result =
        magicLinkService.requestMagicLink("alice@example.com", "/checkout", "vi", "127.0.0.1");

    assertThat(result.isThrottled()).isTrue();
    verify(loginTokenRepository).lockEmail("alice@example.com");
  }

  @Test
  void requestMagicLinkIpRateLimitReached() { // AU-05, FLA-04
    given(loginTokenRepository.countByEmailSince(eq("bob@example.com"), any())).willReturn(0L);
    given(loginTokenRepository.countByIpSince(eq("203.0.113.9"), any())).willReturn(10L);

    var result =
        magicLinkService.requestMagicLink("bob@example.com", "/checkout", "vi", "203.0.113.9");

    assertThat(result.isThrottled()).isTrue();
  }

  @Test
  void requestMagicLinkInvalidEmailThrowsValidationException() { // FLA-05
    assertThatThrownBy(
            () -> magicLinkService.requestMagicLink("invalid_email", "/", "vi", "127.0.0.1"))
        .isInstanceOf(ValidationFailedException.class);
  }

  @Test
  void requestMagicLinkSmtpFailureThrowsProviderUnavailable() { // AU-06, FLA-06
    given(emailRenderer.render(any(), any(), any()))
        .willReturn(new RenderedEmail("Subject", "<p>html</p>", "text"));
    doAnswer(
            inv -> {
              throw new EmailDeliveryException("SMTP error", false, null);
            })
        .when(mailSender)
        .send(any());

    assertThatThrownBy(
            () -> magicLinkService.requestMagicLink("alice@example.com", "/", "vi", "127.0.0.1"))
        .isInstanceOf(ProviderUnavailableException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.EMAIL_PROVIDER_UNAVAILABLE);
  }

  @Test
  void verifySuccessCreatesUserAndSession() { // FLA-10, FLA-14
    given(loginTokenRepository.consumeToken(any()))
        .willReturn(
            Optional.of(
                new LoginTokenRepository.ConsumedToken("alice@example.com", "/checkout", "vi")));
    UUID userId = UUID.randomUUID();
    given(appUserRepository.upsertUserOnLogin("alice@example.com", "vi"))
        .willReturn(new AppUserRepository.UserRecord(userId, "vi"));

    var result = loginService.verify("some_raw_token_string_43_chars_long_1234567890");

    assertThat(result.rawSessionId()).isNotBlank();
    assertThat(result.csrfToken()).isNotBlank();
    assertThat(result.returnTo()).isEqualTo("/checkout");

    verify(sessionRepository).insertSession(any(), eq(userId), eq(result.csrfToken()));
  }

  @Test
  void verifyInvalidTokenThrowsUnauthenticated() { // FLA-11, FLA-12, FLA-13
    given(loginTokenRepository.consumeToken(any())).willReturn(Optional.empty());

    assertThatThrownBy(() -> loginService.verify("invalid_or_expired_token"))
        .isInstanceOf(UnauthenticatedException.class)
        .extracting("code")
        .isEqualTo(ErrorCode.LOGIN_LINK_INVALID);
  }

  @Test
  void verifyEmptyTokenThrowsValidationException() {
    assertThatThrownBy(() -> loginService.verify("")).isInstanceOf(ValidationFailedException.class);
  }
}
