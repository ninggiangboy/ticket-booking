package io.ticket.auth.controller;

import io.ticket.auth.dto.MagicLinkRequest;
import io.ticket.auth.dto.VerifyRequest;
import io.ticket.auth.dto.VerifyResponse;
import io.ticket.auth.service.LoginService;
import io.ticket.auth.service.MagicLinkService;
import io.ticket.common.error.ValidationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for magic-link requesting and token verification (DOC-19 §3-§4, DOC-83 FL-01-FL-02).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final MagicLinkService magicLinkService;
  private final LoginService loginService;
  private final boolean cookieSecure;

  public AuthController(
      MagicLinkService magicLinkService,
      LoginService loginService,
      @Value("${auth.cookie-secure:true}") boolean cookieSecure) {
    this.magicLinkService = magicLinkService;
    this.loginService = loginService;
    this.cookieSecure = cookieSecure;
  }

  @PostMapping("/magic-link")
  public ResponseEntity<Void> requestMagicLink(
      @RequestBody(required = false) MagicLinkRequest request,
      @RequestHeader(value = "X-Real-IP", required = false) String xRealIp,
      HttpServletRequest servletRequest) {

    if (request == null) {
      throw new ValidationFailedException("email", "invalid_email");
    }

    String ip = extractIp(servletRequest, xRealIp);
    var result =
        magicLinkService.requestMagicLink(
            request.email(), request.returnTo(), request.locale(), ip);

    if (result.isThrottled()) {
      return ResponseEntity.accepted().header("X-Magic-Link-Throttled", "1").build();
    }

    return ResponseEntity.accepted().build();
  }

  @PostMapping("/verify")
  public ResponseEntity<VerifyResponse> verifyMagicLink(
      @RequestBody(required = false) VerifyRequest request) {

    if (request == null || request.token() == null) {
      throw new ValidationFailedException("token", "required");
    }

    var result = loginService.verify(request.token());

    ResponseCookie cookie =
        ResponseCookie.from("tb_session", result.rawSessionId())
            .path("/")
            .maxAge(Duration.ofDays(30))
            .httpOnly(true)
            .sameSite("Lax")
            .secure(cookieSecure)
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new VerifyResponse(result.returnTo(), result.csrfToken()));
  }

  private static String extractIp(HttpServletRequest request, String xRealIp) {
    if (xRealIp != null && !xRealIp.isBlank()) {
      return xRealIp.trim();
    }
    return request.getRemoteAddr();
  }
}
