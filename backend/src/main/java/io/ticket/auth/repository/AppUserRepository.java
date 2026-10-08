package io.ticket.auth.repository;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL for the {@code app_user} table (DOC-14 §4, DOC-19 §4). */
@Repository
public class AppUserRepository {

  private final JdbcClient jdbc;

  public AppUserRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public record UserRecord(UUID userId, String locale) {}

  public UserRecord upsertUserOnLogin(String email, String locale) {
    return jdbc.sql(
            """
            INSERT INTO app_user (email, locale, last_login_at) VALUES (:email, :locale, now())
            ON CONFLICT (email) DO UPDATE SET last_login_at = now()
            RETURNING user_id, locale
            """)
        .param("email", email)
        .param("locale", locale)
        .query(
            (rs, rowNum) ->
                new UserRecord(rs.getObject("user_id", UUID.class), rs.getString("locale")))
        .single();
  }
}
