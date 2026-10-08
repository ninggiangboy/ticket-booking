package io.ticket.auth.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

/**
 * Validates and sanitizes {@code return_to} query parameter / field to prevent open redirect
 * vulnerabilities (DOC-19 §6).
 */
@Service
public class ReturnToSanitizer {

  private static final String DEFAULT_RETURN = "/";

  public String sanitize(String returnTo) {
    if (returnTo == null || returnTo.isEmpty()) {
      return DEFAULT_RETURN;
    }

    if (!returnTo.startsWith("/") || returnTo.startsWith("//") || returnTo.startsWith("/\\")) {
      return DEFAULT_RETURN;
    }

    if (returnTo.length() > 512) {
      return DEFAULT_RETURN;
    }

    if (containsControlChars(returnTo)) {
      return DEFAULT_RETURN;
    }

    try {
      String decoded = URLDecoder.decode(returnTo, StandardCharsets.UTF_8);
      if (containsControlChars(decoded)) {
        return DEFAULT_RETURN;
      }
    } catch (Exception e) {
      return DEFAULT_RETURN;
    }

    return returnTo;
  }

  private boolean containsControlChars(String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c < 0x20 || c == 0x7F) {
        return true;
      }
    }
    return false;
  }
}
