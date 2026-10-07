package io.ticket.notification.service;

import io.ticket.common.mail.AppProperties;
import io.ticket.common.mail.MailFormats;
import io.ticket.notification.OutboxKind;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns an outbox payload into the variables of its template, with every number and date already
 * formatted.
 */
@Component
class MailModelFactory {

  private final JsonMapper mapper;
  private final AppProperties app;
  private final ZoneId platformZone;

  MailModelFactory(
      JsonMapper mapper,
      AppProperties app,
      @Value("${platform.timezone:Asia/Ho_Chi_Minh}") String platformZone) {
    this.mapper = mapper;
    this.app = app;
    this.platformZone = ZoneId.of(platformZone);
  }

  /** The recipient address of a payload. */
  String recipient(Map<String, Object> payload) {
    return (String) payload.get("to");
  }

  Map<String, Object> parse(String payloadJson) {
    return mapper.readValue(payloadJson, new TypeReference<>() {});
  }

  /**
   * Locale of the recipient, fixed when the row was written; unknown values fall back to Vietnamese
   * (DOC-27 §7.2).
   */
  Locale locale(Map<String, Object> payload) {
    return "en".equals(payload.get("locale"))
        ? Locale.forLanguageTag("en")
        : Locale.forLanguageTag("vi");
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> model(OutboxKind kind, Map<String, Object> payload, Locale locale) {
    return switch (kind) {
      case EMAIL_TICKETS -> {
        var event = (Map<String, Object>) payload.get("event");
        String eventName = (String) event.get("name");
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("subjectArgs", new Object[] {eventName});
        model.put("appName", app.name());
        model.put("eventName", eventName);
        model.put("venue", event.get("venue"));
        model.put(
            "startsAt",
            MailFormats.dateTime(
                Instant.parse((String) event.get("startsAt")),
                (String) event.get("timezone"),
                platformZone,
                locale));
        model.put("amount", MailFormats.vnd(((Number) payload.get("amount")).longValue(), locale));
        List<Map<String, Object>> tickets = new ArrayList<>();
        for (var t : (List<Map<String, Object>>) payload.get("tickets")) {
          Map<String, Object> ticket = new LinkedHashMap<>();
          ticket.put("code", t.get("code"));
          ticket.put("typeName", t.get("ticketTypeName"));
          ticket.put("seat", seatText((Map<String, Object>) t.get("label")));
          tickets.add(ticket);
        }
        model.put("tickets", tickets);
        model.put("ordersUrl", app.baseUrl() + payload.get("ordersPath"));
        yield model;
      }
      case EMAIL_EVENT_CHANGED, EMAIL_REFUND_PENDING ->
          throw new UnsupportedOperationException("no template for " + kind + " yet");
    };
  }

  /**
   * {@code Khán đài A · Hàng C · Ghế 9}-style text from a label of any shape (DOC-14 §4.2 shapes).
   */
  private static String seatText(Map<String, Object> label) {
    if (label == null) {
      return "";
    }
    List<String> parts = new ArrayList<>();
    for (String key : List.of("section", "row", "seat", "zone", "ticketType")) {
      Object value = label.get(key);
      if (value != null) {
        parts.add(value.toString());
      }
    }
    return String.join(" · ", parts);
  }
}
