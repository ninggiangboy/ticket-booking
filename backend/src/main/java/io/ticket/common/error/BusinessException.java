package io.ticket.common.error;

import java.util.Map;

/**
 * Plain business error for codes that need no dedicated class; modules add their own subclasses
 * when they do.
 */
public class BusinessException extends DomainException {

  public BusinessException(ErrorCode code) {
    super(code, null, null);
  }

  public BusinessException(ErrorCode code, Map<String, Object> extensions) {
    super(code, null, extensions);
  }
}
