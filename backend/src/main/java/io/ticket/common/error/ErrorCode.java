package io.ticket.common.error;

import static io.ticket.common.error.ErrorClass.BUSINESS;
import static io.ticket.common.error.ErrorClass.DEFECT;
import static io.ticket.common.error.ErrorClass.TRANSIENT;

import java.util.Locale;
import org.springframework.http.HttpStatus;

/**
 * The 40 API error codes (DOC-35 §3). Each code has a fixed HTTP status and class, and two backend
 * messages: {@code problem.<code lower case>.title} and {@code problem.<code lower case>.detail}.
 */
public enum ErrorCode {
  IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, BUSINESS),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, BUSINESS),
  INVALID_SIGNATURE(HttpStatus.BAD_REQUEST, BUSINESS),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, BUSINESS),
  LOGIN_LINK_INVALID(HttpStatus.UNAUTHORIZED, BUSINESS),
  FORBIDDEN(HttpStatus.FORBIDDEN, BUSINESS),
  ORGANIZER_PROFILE_REQUIRED(HttpStatus.FORBIDDEN, BUSINESS),
  CSRF_TOKEN_INVALID(HttpStatus.FORBIDDEN, BUSINESS),
  NOT_FOUND(HttpStatus.NOT_FOUND, BUSINESS),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, BUSINESS),
  SEATS_UNAVAILABLE(HttpStatus.CONFLICT, BUSINESS),
  INSUFFICIENT_CAPACITY(HttpStatus.CONFLICT, BUSINESS),
  RESERVATION_NOT_ACTIVE(HttpStatus.CONFLICT, BUSINESS),
  REVISION_CONFLICT(HttpStatus.CONFLICT, BUSINESS),
  EVENT_NOT_ON_SALE(HttpStatus.CONFLICT, BUSINESS),
  ACTIVE_RESERVATION_EXISTS(HttpStatus.CONFLICT, BUSINESS),
  PAYMENT_WINDOW_TOO_SHORT(HttpStatus.CONFLICT, BUSINESS),
  PAYMENT_ALREADY_SUCCEEDED(HttpStatus.CONFLICT, BUSINESS),
  EVENT_STATE_CONFLICT(HttpStatus.CONFLICT, BUSINESS),
  STALE_EVENT_VERSION(HttpStatus.CONFLICT, BUSINESS),
  ORGANIZER_EXISTS(HttpStatus.CONFLICT, BUSINESS),
  TICKET_TYPE_IN_USE(HttpStatus.CONFLICT, BUSINESS),
  CAPACITY_BELOW_USED(HttpStatus.CONFLICT, BUSINESS),
  MAP_LOCKED_AFTER_SALE(HttpStatus.CONFLICT, BUSINESS),
  MAP_PUBLISH_BUSY(HttpStatus.CONFLICT, BUSINESS),
  MAP_ALREADY_EXISTS(HttpStatus.CONFLICT, BUSINESS),
  PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, BUSINESS),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, BUSINESS),
  VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, BUSINESS),
  IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_CONTENT, BUSINESS),
  PUBLISH_PRECONDITIONS_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, BUSINESS),
  MAP_VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, BUSINESS),
  TICKET_TYPE_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_CONTENT, BUSINESS),
  MEDIA_INVALID(HttpStatus.UNPROCESSABLE_CONTENT, BUSINESS),
  RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, TRANSIENT),
  QUEUE_REQUIRED(HttpStatus.TOO_MANY_REQUESTS, BUSINESS),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, DEFECT),
  OVERLOADED(HttpStatus.SERVICE_UNAVAILABLE, TRANSIENT),
  PAYMENT_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, TRANSIENT),
  EMAIL_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, TRANSIENT);

  private final HttpStatus status;
  private final ErrorClass errorClass;

  ErrorCode(HttpStatus status, ErrorClass errorClass) {
    this.status = status;
    this.errorClass = errorClass;
  }

  public HttpStatus status() {
    return status;
  }

  public ErrorClass errorClass() {
    return errorClass;
  }

  /** Lower-case form used in message keys, e.g. {@code seats_unavailable}. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * Identifier URI of the problem type (RFC 9457). It names the error and does not need to resolve.
   */
  public String type() {
    return "https://errors.ticket.dev/" + name();
  }
}
