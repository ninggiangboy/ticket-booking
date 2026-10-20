package io.ticket.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.ticket.auth.controller.AuthController;
import io.ticket.auth.service.LoginService;
import io.ticket.auth.service.MagicLinkService;
import io.ticket.common.config.SecurityConfig;
import io.ticket.common.error.ErrorCode;
import io.ticket.common.error.ProblemFactory;
import io.ticket.common.error.ProblemResponseWriter;
import io.ticket.common.error.ProviderUnavailableException;
import io.ticket.common.error.UnauthenticatedException;
import io.ticket.common.error.ValidationFailedException;
import io.ticket.common.i18n.I18nConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({
  AuthController.class,
  SecurityConfig.class,
  I18nConfig.class,
  ProblemFactory.class,
  ProblemResponseWriter.class
})
class AuthControllerTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private MagicLinkService magicLinkService;

  @MockitoBean private LoginService loginService;

  @Test
  void requestMagicLinkSuccess() throws Exception {
    given(magicLinkService.requestMagicLink(eq("alice@example.com"), any(), any(), any()))
        .willReturn(new MagicLinkService.MagicLinkResult(MagicLinkService.MagicLinkStatus.SENT));

    mvc.perform(
            post("/api/v1/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.com\"}"))
        .andExpect(status().isAccepted())
        .andExpect(header().doesNotExist("X-Magic-Link-Throttled"));
  }

  @Test
  void requestMagicLinkThrottled() throws Exception {
    given(magicLinkService.requestMagicLink(eq("alice@example.com"), any(), any(), any()))
        .willReturn(
            new MagicLinkService.MagicLinkResult(MagicLinkService.MagicLinkStatus.THROTTLED));

    mvc.perform(
            post("/api/v1/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.com\"}"))
        .andExpect(status().isAccepted())
        .andExpect(header().string("X-Magic-Link-Throttled", "1"));
  }

  @Test
  void requestMagicLinkInvalidEmail() throws Exception {
    given(magicLinkService.requestMagicLink(eq("abc"), any(), any(), any()))
        .willThrow(new ValidationFailedException("email", "invalid_email"));

    mvc.perform(
            post("/api/v1/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"abc\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("email"))
        .andExpect(jsonPath("$.errors[0].rule").value("invalid_email"));
  }

  @Test
  void requestMagicLinkSmtpFailed() throws Exception {
    given(magicLinkService.requestMagicLink(eq("alice@example.com"), any(), any(), any()))
        .willThrow(ProviderUnavailableException.email(5));

    mvc.perform(
            post("/api/v1/auth/magic-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@example.com\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "5"))
        .andExpect(jsonPath("$.code").value("EMAIL_PROVIDER_UNAVAILABLE"));
  }

  @Test
  void verifySuccess() throws Exception {
    given(loginService.verify("valid_token_string"))
        .willReturn(new LoginService.LoginResult("raw_session_id", "/checkout", "csrf_123"));

    mvc.perform(
            post("/api/v1/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"valid_token_string\"}"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    org.hamcrest.Matchers.containsString("tb_session=raw_session_id")))
        .andExpect(jsonPath("$.returnTo").value("/checkout"))
        .andExpect(jsonPath("$.csrfToken").value("csrf_123"));
  }

  @Test
  void verifyInvalidToken() throws Exception { // AU-19
    given(loginService.verify("1234567890"))
        .willThrow(new UnauthenticatedException(ErrorCode.LOGIN_LINK_INVALID));

    mvc.perform(
            post("/api/v1/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"1234567890\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_LINK_INVALID"));
  }

  @Test
  void verifyMissingToken() throws Exception { // AU-19
    mvc.perform(post("/api/v1/auth/verify").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("token"))
        .andExpect(jsonPath("$.errors[0].rule").value("required"));
  }
}
