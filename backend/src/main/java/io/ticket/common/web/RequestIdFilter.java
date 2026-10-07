package io.ticket.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before everything else so that even rejected requests carry the headers: takes {@code
 * X-Request-Id} from nginx (or makes one), puts it in MDC as {@code trace_id}, echoes it, and
 * stamps {@code X-Server-Time} (DR-09, DR-66).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String SERVER_TIME_HEADER = "X-Server-Time";
  public static final String TRACE_ID_KEY = "trace_id";

  private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String id = request.getHeader(REQUEST_ID_HEADER);
    if (id == null || !VALID_ID.matcher(id).matches()) {
      id = UUID.randomUUID().toString().replace("-", "");
    }
    MDC.put(TRACE_ID_KEY, id);
    response.setHeader(REQUEST_ID_HEADER, id);
    response.setHeader(SERVER_TIME_HEADER, Long.toString(System.currentTimeMillis()));
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(TRACE_ID_KEY);
    }
  }
}
