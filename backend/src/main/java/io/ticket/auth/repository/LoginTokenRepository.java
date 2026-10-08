package io.ticket.auth.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL for the {@code login_token} table (DOC-15 §3, DOC-19 §3-§4). */
@Repository
public class LoginTokenRepository {

  private final JdbcClient jdbc;

  public LoginTokenRepository(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void lockEmail(String email) {
    jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:email, 0))")
        .param("email", email)
        .query()
        .singleRow();
  }

  public long countByEmailSince(String email, Instant since) {
    Long count =
        jdbc.sql("SELECT count(*) FROM login_token WHERE email = :email AND created_at > :since")
            .param("email", email)
            .param("since", since)
            .query(Long.class)
            .single();
    return count != null ? count : 0;
  }

  public long countByIpSince(String ip, Instant since) {
    if (ip == null || ip.isBlank()) {
      return 0;
    }
    Long count =
        jdbc.sql(
                "SELECT count(*) FROM login_token WHERE requested_ip = :ip::inet AND created_at > :since")
            .param("ip", ip)
            .param("since", since)
            .query(Long.class)
            .single();
    return count != null ? count : 0;
  }

  public void markSuperseded(String email) {
    jdbc.sql(
            """
            UPDATE login_token SET superseded_at = now()
             WHERE email = :email AND used_at IS NULL AND superseded_at IS NULL
            """)
        .param("email", email)
        .update();
  }

  public void insertToken(
      byte[] tokenHash,
      String email,
      String returnTo,
      String locale,
      String requestedIp,
      Instant expiresAt) {
    jdbc.sql(
            """
            INSERT INTO login_token (token_hash, email, return_to, locale, requested_ip, expires_at)
            VALUES (:tokenHash, :email, :returnTo, :locale, :ip::inet, :expiresAt)
            """)
        .param("tokenHash", tokenHash)
        .param("email", email)
        .param("returnTo", returnTo)
        .param("locale", locale)
        .param("ip", requestedIp)
        .param("expiresAt", expiresAt)
        .update();
  }

  public record ConsumedToken(String email, String returnTo, String locale) {}

  public Optional<ConsumedToken> consumeToken(byte[] tokenHash) {
    return jdbc.sql(
            """
            UPDATE login_token SET used_at = now()
             WHERE token_hash = :tokenHash AND used_at IS NULL AND superseded_at IS NULL AND expires_at > now()
            RETURNING email, return_to, locale
            """)
        .param("tokenHash", tokenHash)
        .query(
            (rs, rowNum) ->
                new ConsumedToken(
                    rs.getString("email"), rs.getString("return_to"), rs.getString("locale")))
        .optional();
  }
}
