package io.ticket.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes a Problem Details response from code that runs outside MVC: servlet filters and security
 * handlers.
 */
@Component
public class ProblemResponseWriter {

  private final ProblemFactory factory;
  private final LocaleResolver localeResolver;
  private final JsonMapper mapper;

  ProblemResponseWriter(ProblemFactory factory, LocaleResolver localeResolver, JsonMapper mapper) {
    this.factory = factory;
    this.localeResolver = localeResolver;
    this.mapper = mapper;
  }

  public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
      throws IOException {
    write(request, response, code, null);
  }

  public void write(
      HttpServletRequest request,
      HttpServletResponse response,
      ErrorCode code,
      Integer retryAfterSeconds)
      throws IOException {
    Locale locale = localeResolver.resolveLocale(request);
    Map<String, Object> extensions = new HashMap<>();
    if (retryAfterSeconds != null) {
      extensions.put("retryAfterSeconds", retryAfterSeconds);
      response.setHeader("Retry-After", Integer.toString(retryAfterSeconds));
    }
    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Content-Language", locale.getLanguage());
    mapper.writeValue(response.getWriter(), factory.body(code, locale, extensions));
  }
}
