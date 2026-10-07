package io.ticket.common.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Infrastructure that is temporarily unavailable. Always 503 or 429 and carries {@code
 * Retry-After}.
 */
public class TransientException extends DomainException {

  private final Integer retryAfterSeconds;

  public TransientException(ErrorCode code, Integer retryAfterSeconds) {
    super(code, null, withRetry(retryAfterSeconds));
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /** Seconds until a retry makes sense; {@code null} means no {@code Retry-After} header. */
  public Integer retryAfterSeconds() {
    return retryAfterSeconds;
  }

  private static Map<String, Object> withRetry(Integer retryAfterSeconds) {
    Map<String, Object> ext = new HashMap<>();
    if (retryAfterSeconds != null) {
      ext.put("retryAfterSeconds", retryAfterSeconds);
    }
    return ext;
  }
}
