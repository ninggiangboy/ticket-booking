package io.ticket.common.error;

/** Out of permits, connections or queue space (DR-61). */
public class OverloadedException extends TransientException {

  public OverloadedException(int retryAfterSeconds) {
    super(ErrorCode.OVERLOADED, retryAfterSeconds);
  }
}
