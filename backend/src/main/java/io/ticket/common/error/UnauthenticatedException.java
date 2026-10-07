package io.ticket.common.error;

public class UnauthenticatedException extends DomainException {

  public UnauthenticatedException() {
    super(ErrorCode.UNAUTHENTICATED, null, null);
  }

  public UnauthenticatedException(ErrorCode code) {
    super(code, null, null);
  }
}
