package io.ticket.common.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * A business or infrastructure error with a stable {@link ErrorCode}. Services throw subclasses;
 * {@link ApiExceptionHandler} turns them into Problem Details. Services never build HTTP responses
 * (DOC-12 §4).
 */
public abstract class DomainException extends RuntimeException {

  private final ErrorCode code;
  private final Map<String, Object> extensions;

  protected DomainException(ErrorCode code, String message, Map<String, Object> extensions) {
    super(message == null ? code.name() : message);
    this.code = code;
    this.extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
  }

  public ErrorCode code() {
    return code;
  }

  public HttpStatus status() {
    return code.status();
  }

  public ErrorClass errorClass() {
    return code.errorClass();
  }

  /** Members added to the Problem Details body next to the standard ones (DOC-35 §4). */
  public Map<String, Object> extensions() {
    return extensions;
  }
}
