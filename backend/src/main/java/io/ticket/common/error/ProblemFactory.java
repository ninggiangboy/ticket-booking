package io.ticket.common.error;

import io.ticket.common.web.RequestIdFilter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Builds the RFC 9457 body of every error response (DOC-36 §5): standard members first, then
 * extensions.
 */
@Component
public class ProblemFactory {

  private final MessageSource messages;

  ProblemFactory(MessageSource messages) {
    this.messages = messages;
  }

  public Map<String, Object> body(ErrorCode code, Locale locale, Map<String, Object> extensions) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", code.type());
    body.put("title", messages.getMessage("problem." + code.key() + ".title", null, locale));
    body.put("status", code.status().value());
    body.put("detail", messages.getMessage("problem." + code.key() + ".detail", null, locale));
    body.put("code", code.name());
    body.put("requestId", MDC.get(RequestIdFilter.TRACE_ID_KEY));
    if (extensions != null) {
      body.putAll(extensions);
    }
    return body;
  }
}
