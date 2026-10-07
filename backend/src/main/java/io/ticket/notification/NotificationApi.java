package io.ticket.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Queues emails in the caller's transaction (DOC-27 §5.1). The payload must hold everything needed
 * to render.
 */
public interface NotificationApi {

  /** Inserts one PENDING row; must run inside an open transaction. */
  UUID enqueue(OutboxKind kind, Map<String, Object> payload);

  /** Inserts many rows with JDBC batches (schedule change, event cancellation). */
  void enqueueAll(OutboxKind kind, List<Map<String, Object>> payloads);
}
