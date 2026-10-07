package io.ticket.common.error;

import java.util.Map;

/**
 * One entry of {@code errors[]} in a 422 response: JSON path, snake_case rule, optional parameters
 * (DR-25).
 */
public record FieldViolation(String field, String rule, Map<String, Object> params) {

  public FieldViolation(String field, String rule) {
    this(field, rule, null);
  }
}
