package io.ticket.common.i18n;

import io.ticket.common.security.CurrentUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Resolves the request locale for Problem Details and anything else that uses {@code
 * LocaleContextHolder}.
 */
public class AppLocaleResolver implements LocaleResolver {

  public static final String LANGUAGE_COOKIE = "tb_lang";

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    String userLocale =
        request.getAttribute(CurrentUser.REQUEST_ATTRIBUTE) instanceof CurrentUser user
            ? user.locale()
            : null;
    return LocaleSelector.select(userLocale, cookie(request), request.getHeader("Accept-Language"));
  }

  @Override
  public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    throw new UnsupportedOperationException("the locale is derived from the request");
  }

  private static String cookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (LANGUAGE_COOKIE.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
