package io.ticket.common.mail;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

/**
 * Locale formatting for emails, matching the screens of DOC-40 §2 (DOC-51 §2.3). Done in Java, not
 * by MessageFormat.
 */
public final class MailFormats {

  private MailFormats() {}

  /**
   * {@code Thứ Bảy 14.11.2026 · 20:00}, with a {@code GMT+9} suffix when the event is outside the
   * platform zone.
   */
  public static String dateTime(Instant at, String eventZone, ZoneId platformZone, Locale locale) {
    ZoneId zone = ZoneId.of(eventZone);
    var text =
        DateTimeFormatter.ofPattern("EEEE dd.MM.yyyy · HH:mm", locale).format(at.atZone(zone));
    if (!zone.getRules().equals(platformZone.getRules())
        && !zone.getId().equals(platformZone.getId())) {
      text += " · " + DateTimeFormatter.ofPattern("O", locale).format(at.atZone(zone));
    }
    return text;
  }

  /** Whole VND: {@code 1.500.000 ₫} in vi, {@code ₫1,500,000} in en. */
  public static String vnd(long amount, Locale locale) {
    NumberFormat format = NumberFormat.getCurrencyInstance(locale);
    format.setCurrency(Currency.getInstance("VND"));
    format.setMaximumFractionDigits(0);
    return format.format(amount);
  }
}
