package io.ticket.common.error;

import java.util.List;
import java.util.Map;

/** 422 {@code VALIDATION_FAILED} with per-field rules. */
public class ValidationFailedException extends DomainException {

  public ValidationFailedException(List<FieldViolation> errors) {
    super(ErrorCode.VALIDATION_FAILED, null, Map.of("errors", List.copyOf(errors)));
  }

  public ValidationFailedException(String field, String rule) {
    this(List.of(new FieldViolation(field, rule)));
  }
}
