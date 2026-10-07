package io.ticket.notification.repository;

import io.ticket.notification.OutboxKind;
import io.ticket.notification.entity.OutboxMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * SQL of the {@code outbox} table (DOC-15 §7). Every state change is a conditional UPDATE that
 * returns a row count; {@code status} is never changed through {@code save()} (DOC-12 §3.2).
 */
@Repository
public class OutboxRepository {

  private final JdbcClient jdbc;
  private final JdbcTemplate template;

  OutboxRepository(JdbcClient jdbc, JdbcTemplate template) {
    this.jdbc = jdbc;
    this.template = template;
  }

  public UUID insert(OutboxKind kind, String payloadJson) {
    return jdbc.sql(
            "INSERT INTO outbox (kind, payload) VALUES (:kind, :payload::jsonb) RETURNING outbox_id")
        .param("kind", kind.name())
        .param("payload", payloadJson)
        .query(UUID.class)
        .single();
  }

  public void insertAll(OutboxKind kind, List<String> payloadJsons, int batchSize) {
    template.batchUpdate(
        "INSERT INTO outbox (kind, payload) VALUES (?, ?::jsonb)",
        payloadJsons,
        batchSize,
        (ps, json) -> {
          ps.setString(1, kind.name());
          ps.setString(2, json);
        });
  }

  /**
   * Takes due rows and pushes {@code next_attempt_at} out by the lease so nobody else takes them
   * (DR-53).
   */
  public List<OutboxMessage> claim(int limit, double leaseSeconds) {
    return jdbc.sql(
            """
            UPDATE outbox
               SET attempts = attempts + 1, next_attempt_at = now() + make_interval(secs => :lease)
             WHERE outbox_id IN (
               SELECT outbox_id FROM outbox
                WHERE status = 'PENDING' AND next_attempt_at <= now()
                ORDER BY next_attempt_at LIMIT :limit
                FOR UPDATE SKIP LOCKED)
            RETURNING outbox_id, kind, payload::text AS payload, attempts
            """)
        .param("lease", leaseSeconds)
        .param("limit", limit)
        .query(
            (rs, i) ->
                new OutboxMessage(
                    rs.getObject("outbox_id", UUID.class),
                    OutboxKind.valueOf(rs.getString("kind")),
                    rs.getString("payload"),
                    rs.getInt("attempts")))
        .list();
  }

  public int markSent(UUID id) {
    return jdbc.sql(
            """
            UPDATE outbox SET status = 'SENT', sent_at = now(), last_error = NULL
             WHERE outbox_id = :id AND status = 'PENDING'
            """)
        .param("id", id)
        .update();
  }

  /** Schedules the next attempt with exponential backoff; 0 rows means the attempts are used up. */
  public int markRetry(
      UUID id, String error, double baseSeconds, double maxSeconds, int maxAttempts) {
    return jdbc.sql(
            """
            UPDATE outbox
               SET next_attempt_at = now() + make_interval(secs => LEAST(:base * power(2, attempts - 1), :max)),
                   last_error = :error
             WHERE outbox_id = :id AND status = 'PENDING' AND attempts < :maxAttempts
            """)
        .param("base", baseSeconds)
        .param("max", maxSeconds)
        .param("error", error)
        .param("id", id)
        .param("maxAttempts", maxAttempts)
        .update();
  }

  public int markFailed(UUID id, String error) {
    return jdbc.sql(
            """
            UPDATE outbox SET status = 'FAILED', last_error = :error
             WHERE outbox_id = :id AND status = 'PENDING'
            """)
        .param("id", id)
        .param("error", error)
        .update();
  }

  /** Hands back rows the relay ran out of time for, so their attempt is not wasted (DR-104). */
  public int giveBack(List<UUID> ids) {
    if (ids.isEmpty()) {
      return 0;
    }
    return jdbc.sql(
            """
            UPDATE outbox SET attempts = attempts - 1, next_attempt_at = now()
             WHERE outbox_id = ANY(:ids) AND status = 'PENDING'
            """)
        .param("ids", ids.toArray(UUID[]::new))
        .update();
  }
}
