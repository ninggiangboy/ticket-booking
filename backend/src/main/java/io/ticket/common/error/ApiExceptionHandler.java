package io.ticket.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every exception into Problem Details with the logging rules of DOC-35 §5. */
@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
  private static final Pattern RULE = Pattern.compile("[a-z][a-z0-9_]*");

  private final ProblemFactory factory;

  ApiExceptionHandler(ProblemFactory factory) {
    this.factory = factory;
  }

  @ExceptionHandler(DomainException.class)
  ResponseEntity<Map<String, Object>> domain(DomainException e) {
    log(e.code(), e);
    Integer retry = e instanceof TransientException t ? t.retryAfterSeconds() : null;
    return respond(e.code(), e.extensions(), retry, null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException e) {
    List<FieldViolation> violations = new ArrayList<>();
    for (ObjectError error : e.getBindingResult().getAllErrors()) {
      String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
      violations.add(violation(field, error.getCode(), error.getDefaultMessage(), unwrap(error)));
    }
    return validation(violations);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<Map<String, Object>> invalidParameters(HandlerMethodValidationException e) {
    List<FieldViolation> violations = new ArrayList<>();
    e.getParameterValidationResults()
        .forEach(
            result -> {
              String name = result.getMethodParameter().getParameterName();
              result
                  .getResolvableErrors()
                  .forEach(
                      error -> {
                        String field = error instanceof FieldError fe ? fe.getField() : name;
                        violations.add(
                            violation(
                                field,
                                error.getCodes() == null ? null : lastCode(error.getCodes()),
                                error.getDefaultMessage(),
                                null));
                      });
            });
    return validation(violations);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Map<String, Object>> constraintViolations(ConstraintViolationException e) {
    List<FieldViolation> violations = new ArrayList<>();
    for (ConstraintViolation<?> v : e.getConstraintViolations()) {
      String path = v.getPropertyPath().toString();
      String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
      violations.add(
          violation(
              field,
              v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
              v.getMessage(),
              v.getConstraintDescriptor().getAttributes()));
    }
    return validation(violations);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<Map<String, Object>> badRequest(Exception e) {
    log(ErrorCode.BAD_REQUEST, e);
    return respond(ErrorCode.BAD_REQUEST, null, null, null);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<Map<String, Object>> noResource(NoResourceFoundException e) {
    return respond(ErrorCode.NOT_FOUND, null, null, null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Map<String, Object>> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
    HttpHeaders extra = new HttpHeaders();
    if (e.getSupportedHttpMethods() != null) {
      extra.setAllow(e.getSupportedHttpMethods());
    } else {
      extra.setAllow(java.util.Set.<HttpMethod>of());
    }
    return respond(ErrorCode.METHOD_NOT_ALLOWED, null, null, extra);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<Map<String, Object>> unsupportedMediaType(HttpMediaTypeNotSupportedException e) {
    return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null, null, null);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
    return respond(ErrorCode.PAYLOAD_TOO_LARGE, null, null, null);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Map<String, Object>> unauthenticated(AuthenticationException e) {
    log(ErrorCode.UNAUTHENTICATED, e);
    return respond(ErrorCode.UNAUTHENTICATED, null, null, null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException e) {
    log(ErrorCode.FORBIDDEN, e);
    return respond(ErrorCode.FORBIDDEN, null, null, null);
  }

  /**
   * Pool exhausted, query timeout or a lost connection: the client can retry, so 503 and not a
   * defect (DR-61).
   */
  @ExceptionHandler({
    DataAccessResourceFailureException.class,
    TransientDataAccessException.class,
    RejectedExecutionException.class
  })
  ResponseEntity<Map<String, Object>> overloaded(Exception e) {
    int retry = ThreadLocalRandom.current().nextInt(1, 4);
    log.warn("overloaded: dependency=postgres cause={}", e.getClass().getSimpleName());
    return respond(ErrorCode.OVERLOADED, Map.of("retryAfterSeconds", retry), retry, null);
  }

  @ExceptionHandler(Throwable.class)
  ResponseEntity<Map<String, Object>> unexpected(Throwable e) {
    log.error("unhandled error", e);
    return respond(ErrorCode.INTERNAL_ERROR, null, null, null);
  }

  // ---- helpers ----

  private ResponseEntity<Map<String, Object>> validation(List<FieldViolation> violations) {
    log(ErrorCode.VALIDATION_FAILED, null);
    return respond(ErrorCode.VALIDATION_FAILED, Map.of("errors", violations), null, null);
  }

  private ResponseEntity<Map<String, Object>> respond(
      ErrorCode code, Map<String, Object> extensions, Integer retryAfter, HttpHeaders extra) {
    Locale locale = LocaleContextHolder.getLocale();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    headers.set(HttpHeaders.CONTENT_LANGUAGE, locale.getLanguage());
    if (retryAfter != null) {
      headers.set(HttpHeaders.RETRY_AFTER, retryAfter.toString());
    }
    if (extra != null) {
      headers.putAll(extra);
    }
    return ResponseEntity.status(code.status())
        .headers(headers)
        .body(factory.body(code, locale, extensions));
  }

  /**
   * Business errors are INFO (WARN for auth failures), transient WARN, defects ERROR; no stack for
   * the first two.
   */
  private void log(ErrorCode code, Throwable cause) {
    switch (code.errorClass()) {
      case BUSINESS -> {
        boolean noisy =
            code == ErrorCode.UNAUTHENTICATED
                || code == ErrorCode.FORBIDDEN
                || code == ErrorCode.CSRF_TOKEN_INVALID
                || code == ErrorCode.QUEUE_REQUIRED
                || code == ErrorCode.IDEMPOTENCY_KEY_REUSED
                || code == ErrorCode.PAYLOAD_TOO_LARGE;
        if (noisy) {
          log.warn("{}", code);
        } else {
          log.info("{}", code);
        }
      }
      case TRANSIENT -> log.warn("{}", code);
      case DEFECT -> log.error("{}", code, cause);
    }
  }

  private static Map<String, Object> unwrap(ObjectError error) {
    try {
      ConstraintViolation<?> v = error.unwrap(ConstraintViolation.class);
      return v.getConstraintDescriptor().getAttributes();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String lastCode(String[] codes) {
    String first = codes[codes.length - 1];
    return first;
  }

  /**
   * The rule is the constraint message when it is already a snake_case token (custom constraints,
   * e.g. {@code @Email(message = "invalid_email")}); otherwise it comes from the annotation name
   * (DOC-35 §5.1).
   */
  private static FieldViolation violation(
      String field, String code, String message, Map<String, Object> attributes) {
    String rule;
    if (message != null && RULE.matcher(message).matches()) {
      rule = message;
    } else {
      rule = ruleFromAnnotation(code);
    }
    Map<String, Object> params = null;
    if (attributes != null) {
      params = new LinkedHashMap<>();
      for (String name : List.of("min", "max")) {
        Object value = attributes.get(name);
        if (value instanceof Number n && n.longValue() != Integer.MAX_VALUE && n.longValue() != 0) {
          params.put(name, n);
        }
      }
      if (params.isEmpty()) {
        params = null;
      }
    }
    return new FieldViolation(field, rule, params);
  }

  private static String ruleFromAnnotation(String code) {
    if (code == null) {
      return "invalid";
    }
    return switch (code) {
      case "NotBlank", "NotNull", "NotEmpty" -> "required";
      case "Size", "Length" -> "size";
      case "Pattern" -> "pattern";
      case "Min", "Max", "Positive", "PositiveOrZero", "Range" -> "out_of_range";
      case "Email" -> "invalid_email";
      default -> code.toLowerCase(Locale.ROOT);
    };
  }
}
