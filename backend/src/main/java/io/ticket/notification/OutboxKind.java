package io.ticket.notification;

/**
 * Kinds of email that go through the outbox (DOC-15 §7). Magic links do not: they are sent directly
 * (DR-21).
 */
public enum OutboxKind {
  EMAIL_TICKETS("tickets"),
  EMAIL_EVENT_CHANGED("event-changed"),
  EMAIL_REFUND_PENDING("refund-pending");

  private final String template;

  OutboxKind(String template) {
    this.template = template;
  }

  /** Name of the {@code templates/email/<name>} pair used to render this kind. */
  public String template() {
    return template;
  }
}
