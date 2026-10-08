package io.ticket.auth.repository;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL for the {@code session} table (DOC-15 §4, DOC-19 §4-§5). */
@Repository
public class SessionRepository {

  private final JdbcClient jdbc;

  public SessionRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void insertSession(byte[] sessionHash, UUID userId, String csrfToken) {
    jdbc.sql(
            """
            INSERT INTO session (session_hash, user_id, csrf_token) VALUES (:sessionHash, :userId, :csrfToken)
            """)
        .param("sessionHash", sessionHash)
        .param("userId", userId)
        .param("csrfToken", csrfToken)
        .update();
  }
}
