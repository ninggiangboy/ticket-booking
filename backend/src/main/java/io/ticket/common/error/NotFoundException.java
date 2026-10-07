package io.ticket.common.error;

/**
 * The resource does not exist or belongs to someone else; the two are indistinguishable by design
 * (DR-23).
 */
public class NotFoundException extends DomainException {

  public NotFoundException() {
    super(ErrorCode.NOT_FOUND, null, null);
  }
}
