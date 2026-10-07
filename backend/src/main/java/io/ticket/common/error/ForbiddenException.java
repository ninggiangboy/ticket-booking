package io.ticket.common.error;

public class ForbiddenException extends DomainException {

  public ForbiddenException() {
    super(ErrorCode.FORBIDDEN, null, null);
  }

  public ForbiddenException(ErrorCode code) {
    super(code, null, null);
  }
}
