package io.ticket.auth.repository;

import io.ticket.auth.model.LoginToken;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

/**
 * SQL of the {@code login_token} table (DOC-15 §3, DOC-19 §3.3 and §4.2). Time is compared with
 * {@code now()} in SQL (DR-12). A row is created by {@link #insert} and changed by conditional
 * UPDATEs; {@code save()} is not used (DOC-12 §3.2).
 */
public interface LoginTokenRepository extends ListCrudRepository<LoginToken, byte[]> {

  /** Serializes magic link requests of one email until the transaction ends (DR-97). */
  @Query("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended(:email, 0))) AS locked")
  int lockEmail(String email);

  @Query(
      """
      SELECT count(*) FROM login_token
       WHERE email = :email AND created_at > now() - make_interval(secs => :windowSeconds)
      """)
  int countByEmailSince(String email, int windowSeconds);

  @Query(
      """
      SELECT count(*) FROM login_token
       WHERE requested_ip = CAST(:ip AS inet)
         AND created_at > now() - make_interval(secs => :windowSeconds)
      """)
  int countByIpSince(String ip, int windowSeconds);

  /** Marks every unused token of the email as replaced by a newer request (FR-01.4). */
  @Modifying
  @Query(
      """
      UPDATE login_token SET superseded_at = now()
       WHERE email = :email AND used_at IS NULL AND superseded_at IS NULL
      """)
  int supersedeUnused(String email);

  @Modifying
  @Query(
      """
      INSERT INTO login_token (token_hash, email, return_to, locale, requested_ip, expires_at)
      VALUES (:tokenHash, :email, :returnTo, :locale, CAST(:ip AS inet),
              now() + make_interval(secs => :ttlSeconds))
      """)
  int insert(
      byte[] tokenHash, String email, String returnTo, String locale, String ip, int ttlSeconds);

  /**
   * Spends a token once. Empty means it is unknown, used, replaced or expired; the caller answers
   * 401 for all of them alike (DR-21).
   */
  @Query(
      """
      UPDATE login_token SET used_at = now()
       WHERE token_hash = :tokenHash AND used_at IS NULL AND superseded_at IS NULL
         AND expires_at > now()
      RETURNING token_hash, email, return_to, locale, created_at, expires_at, used_at, superseded_at
      """)
  Optional<LoginToken> consume(byte[] tokenHash);

  /** Retention: rows are removed 24 hours after they expire (DR-74). */
  @Modifying
  @Query("DELETE FROM login_token WHERE expires_at < now() - interval '24 hours'")
  int deleteExpired();
}
