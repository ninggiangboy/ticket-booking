package app.ticket.spike;

/** Raw jsonb document, read and written through {@code PGobject}. */
public record JsonDoc(String value) {}
