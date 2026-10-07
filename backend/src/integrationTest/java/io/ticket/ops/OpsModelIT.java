package io.ticket.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Schema and canonical statements of DOC-15 (OM-xx) and the account tables of DOC-14 (DM-xx), on
 * postgres:18.
 */
@Testcontainers
class OpsModelIT {

  @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .locations("classpath:db/migration")
        .validateOnMigrate(true)
        .load()
        .migrate();
  }

  @BeforeEach
  void clean() throws SQLException {
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      s.execute(
          "TRUNCATE login_token, idempotency_key, stripe_event, outbox, session, organizer, app_user");
    }
  }

  private static Connection connect() throws SQLException {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  private static long count(String sql, Object... params) throws SQLException {
    try (Connection c = connect();
        PreparedStatement ps = c.prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private static void exec(String sql) throws SQLException {
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      s.execute(sql);
    }
  }

  // ---- schema ----

  @Test
  void everyP1TableExists() throws SQLException {
    List<String> names = new ArrayList<>();
    try (Connection c = connect();
        Statement s = c.createStatement();
        ResultSet rs =
            s.executeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
      while (rs.next()) names.add(rs.getString(1));
    }
    assertThat(names)
        .contains(
            "app_user",
            "organizer",
            "login_token",
            "session",
            "idempotency_key",
            "stripe_event",
            "outbox",
            "flyway_schema_history");
  }

  @Test
  void checkConstraintsRejectBadValues() {
    assertThatThrownBy(() -> exec("INSERT INTO app_user (email, locale) VALUES ('a@x.test', 'fr')"))
        .hasMessageContaining("app_user_locale_check");
    assertThatThrownBy(
            () ->
                exec(
                    "INSERT INTO outbox (kind, payload) VALUES ('EMAIL_MAGIC_LINK', '{}')")) // OM-11
        .hasMessageContaining("outbox_kind_check");
    assertThatThrownBy(
            () ->
                exec(
                    "INSERT INTO outbox (kind, payload, status) VALUES ('EMAIL_TICKETS', '{}', 'DONE')"))
        .hasMessageContaining("outbox_status_check");
    assertThatThrownBy(
            () ->
                exec(
                    "INSERT INTO idempotency_key (user_id, idem_key, operation, request_hash)"
                        + " VALUES (uuidv7(), uuidv7(), 'PAY', '\\x00')"))
        .hasMessageContaining("idempotency_key_operation_check");
  }

  @Test
  void organizerNameLengthAndOneProfilePerUser() throws SQLException {
    exec(
        "INSERT INTO app_user (user_id, email) VALUES ('0199f3b8-5d14-7a30-8c91-6e2b0f4d7a12', 'o@x.test')");
    assertThatThrownBy(
            () ->
                exec(
                    "INSERT INTO organizer (owner_user_id, name) VALUES"
                        + " ('0199f3b8-5d14-7a30-8c91-6e2b0f4d7a12', '')"))
        .hasMessageContaining("organizer_name_check");
    exec(
        "INSERT INTO organizer (owner_user_id, name) VALUES ('0199f3b8-5d14-7a30-8c91-6e2b0f4d7a12', 'Nhà hát')");
    assertThatThrownBy(
            () ->
                exec(
                    "INSERT INTO organizer (owner_user_id, name) VALUES"
                        + " ('0199f3b8-5d14-7a30-8c91-6e2b0f4d7a12', 'Hai')"))
        .hasMessageContaining("organizer_owner_user_id_key");
  }

  @Test
  void emailIsUniqueAndPrimaryKeysAreUuidV7() throws SQLException {
    exec("INSERT INTO app_user (email) VALUES ('dup@x.test')");
    assertThatThrownBy(() -> exec("INSERT INTO app_user (email) VALUES ('dup@x.test')"))
        .hasMessageContaining("app_user_email_uq");
    assertThat(count("SELECT count(*) FROM app_user WHERE substring(user_id::text, 15, 1) = '7'"))
        .isEqualTo(1);
  }

  @Test
  void forbidUpdateFunctionBlocksWritesOnAnyTableItIsAttachedTo() throws SQLException {
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      s.execute("CREATE TEMP TABLE frozen (id int)");
      s.execute(
          "CREATE TRIGGER t BEFORE UPDATE OR DELETE ON frozen"
              + " FOR EACH ROW EXECUTE FUNCTION forbid_update()");
      s.execute("INSERT INTO frozen VALUES (1)");
      assertThatThrownBy(() -> s.execute("UPDATE frozen SET id = 2"))
          .hasMessageContaining("frozen is immutable");
      assertThatThrownBy(() -> s.execute("DELETE FROM frozen"))
          .hasMessageContaining("frozen is immutable");
    }
  }

  // ---- OM-02, OM-03, OM-04: login_token ----

  private static final String CONSUME =
      "UPDATE login_token SET used_at = now()"
          + " WHERE token_hash = ? AND used_at IS NULL AND superseded_at IS NULL AND expires_at > now()"
          + " RETURNING email, return_to, locale";

  private static void insertToken(String email, byte[] hash) throws SQLException {
    try (Connection c = connect();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO login_token (token_hash, email, locale, expires_at)"
                    + " VALUES (?, ?, 'vi', now() + interval '15 minutes')")) {
      ps.setBytes(1, hash);
      ps.setString(2, email);
      ps.executeUpdate();
    }
  }

  @Test
  void fiftyParallelConsumersOfOneTokenYieldExactlyOneWinner() throws Exception { // OM-02
    byte[] hash = new byte[32];
    hash[0] = 1;
    insertToken("race@x.test", hash);
    int threads = 50;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger winners = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      futures.add(
          pool.submit(
              () -> {
                try (Connection c = connect();
                    PreparedStatement ps = c.prepareStatement(CONSUME)) {
                  c.setAutoCommit(false);
                  ps.setBytes(1, hash);
                  start.await();
                  try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) winners.incrementAndGet();
                  }
                  c.commit();
                }
                return null;
              }));
    }
    start.countDown();
    for (Future<?> f : futures) f.get();
    pool.shutdown();
    assertThat(winners.get()).isEqualTo(1);
  }

  @Test
  void supersededTokenCannotBeConsumed() throws SQLException { // OM-03
    byte[] first = new byte[32];
    first[0] = 1;
    byte[] second = new byte[32];
    second[0] = 2;
    insertToken("s@x.test", first);
    exec(
        "UPDATE login_token SET superseded_at = now()"
            + " WHERE email = 's@x.test' AND used_at IS NULL AND superseded_at IS NULL");
    insertToken("s@x.test", second);
    assertThat(consume(first)).isFalse();
    assertThat(consume(second)).isTrue();
  }

  @Test
  void expiredTokenCannotBeConsumedAndKeepsUsedAtNull() throws SQLException {
    byte[] hash = new byte[32];
    hash[0] = 9;
    try (Connection c = connect();
        PreparedStatement ps =
            c.prepareStatement(
                "INSERT INTO login_token (token_hash, email, locale, created_at, expires_at)"
                    + " VALUES (?, 'old@x.test', 'vi', now() - interval '20 minutes',"
                    + " now() - interval '5 minutes')")) {
      ps.setBytes(1, hash);
      ps.executeUpdate();
    }
    assertThat(consume(hash)).isFalse();
    assertThat(count("SELECT count(*) FROM login_token WHERE used_at IS NULL")).isEqualTo(1);
  }

  private static boolean consume(byte[] hash) throws SQLException {
    try (Connection c = connect();
        PreparedStatement ps = c.prepareStatement(CONSUME)) {
      ps.setBytes(1, hash);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  @Test
  void emailRateLimitCountSeesFourTokensAndUsesTheEmailIndex() throws SQLException { // OM-04
    for (int i = 0; i < 4; i++) {
      byte[] hash = new byte[32];
      hash[0] = (byte) (20 + i);
      insertToken("limit@x.test", hash);
    }
    assertThat(
            count(
                "SELECT count(*) FROM login_token"
                    + " WHERE email = ? AND created_at > now() - interval '15 minutes'",
                "limit@x.test"))
        .isEqualTo(4);
    exec(
        "INSERT INTO login_token (token_hash, email, locale, expires_at, created_at)"
            + " SELECT sha256(i::text::bytea), 'bulk' || i || '@x.test', 'vi', now() + interval '1 hour', now()"
            + " FROM generate_series(1, 2000) i");
    exec("ANALYZE login_token");
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      s.execute("SET enable_seqscan = off");
      StringBuilder plan = new StringBuilder();
      try (ResultSet rs =
          s.executeQuery(
              "EXPLAIN SELECT count(*) FROM login_token WHERE email = 'limit@x.test'"
                  + " AND created_at > now() - interval '15 minutes'")) {
        while (rs.next()) plan.append(rs.getString(1)).append('\n');
      }
      assertThat(plan.toString()).contains("login_token_email_idx");
    }
  }

  // ---- OM-05, OM-06, OM-07: idempotency_key and stripe_event ----

  @Test
  void idempotencyKeyInsertWaitsForTheFirstTransactionThenDoesNothing() throws Exception { // OM-05
    UUID user = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    String insert =
        "INSERT INTO idempotency_key (user_id, idem_key, operation, request_hash)"
            + " VALUES (?, ?, 'HOLD', '\\x01') ON CONFLICT (user_id, idem_key) DO NOTHING";
    try (Connection first = connect();
        Connection second = connect()) {
      first.setAutoCommit(false);
      second.setAutoCommit(false);
      try (PreparedStatement a = first.prepareStatement(insert);
          PreparedStatement b = second.prepareStatement(insert)) {
        a.setObject(1, user);
        a.setObject(2, key);
        assertThat(a.executeUpdate()).isEqualTo(1);
        b.setObject(1, user);
        b.setObject(2, key);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<Integer> pending =
            pool.submit((java.util.concurrent.Callable<Integer>) b::executeUpdate);
        Thread.sleep(300);
        assertThat(pending.isDone()).isFalse();
        first.commit();
        assertThat(pending.get()).isZero();
        second.commit();
        pool.shutdown();
      }
    }
    assertThat(count("SELECT count(*) FROM idempotency_key")).isEqualTo(1);
  }

  @Test
  void rolledBackIdempotencyKeyLeavesNoTrace() throws SQLException { // OM-06
    UUID user = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    String insert =
        "INSERT INTO idempotency_key (user_id, idem_key, operation, request_hash)"
            + " VALUES (?, ?, 'HOLD', '\\x01') ON CONFLICT (user_id, idem_key) DO NOTHING";
    try (Connection c = connect();
        PreparedStatement ps = c.prepareStatement(insert)) {
      c.setAutoCommit(false);
      ps.setObject(1, user);
      ps.setObject(2, key);
      assertThat(ps.executeUpdate()).isEqualTo(1);
      c.rollback();
    }
    try (Connection c = connect();
        PreparedStatement ps = c.prepareStatement(insert)) {
      ps.setObject(1, user);
      ps.setObject(2, key);
      assertThat(ps.executeUpdate()).isEqualTo(1);
    }
  }

  @Test
  void stripeEventInsertedTwiceIsADuplicateTheSecondTime() throws SQLException { // OM-07
    String insert =
        "INSERT INTO stripe_event (stripe_event_id, type, payment_intent_id, outcome)"
            + " VALUES ('evt_1', 'payment_intent.succeeded', 'pi_1', 'CONFIRMED')"
            + " ON CONFLICT (stripe_event_id) DO NOTHING";
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      assertThat(s.executeUpdate(insert)).isEqualTo(1);
      assertThat(s.executeUpdate(insert)).isZero();
    }
  }

  // ---- OM-08, OM-09, OM-10: outbox claim ----

  private static final String CLAIM =
      "UPDATE outbox SET attempts = attempts + 1, next_attempt_at = now() + interval '60 seconds'"
          + " WHERE outbox_id IN (SELECT outbox_id FROM outbox"
          + " WHERE status = 'PENDING' AND next_attempt_at <= now()"
          + " ORDER BY next_attempt_at LIMIT 50 FOR UPDATE SKIP LOCKED)"
          + " RETURNING outbox_id, kind, payload, attempts";

  @Test
  void parallelRelaysClaimEveryRowExactlyOnce() throws Exception { // OM-08
    exec(
        "INSERT INTO outbox (kind, payload, next_attempt_at)"
            + " SELECT 'EMAIL_TICKETS', '{}', now() - interval '1 second' FROM generate_series(1, 200)");
    int relays = 100;
    ExecutorService pool = Executors.newFixedThreadPool(relays);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();
    for (int i = 0; i < relays; i++) {
      futures.add(
          pool.submit(
              () -> {
                try (Connection c = connect();
                    Statement s = c.createStatement()) {
                  start.await();
                  int claimed = 0;
                  try (ResultSet rs = s.executeQuery(CLAIM)) {
                    while (rs.next()) claimed++;
                  }
                  return claimed;
                }
              }));
    }
    start.countDown();
    int total = 0;
    for (Future<Integer> f : futures) total += f.get();
    pool.shutdown();
    assertThat(total).isEqualTo(200);
    assertThat(count("SELECT sum(attempts) FROM outbox")).isEqualTo(200);
    assertThat(count("SELECT count(*) FROM outbox WHERE attempts <> 1")).isZero();
  }

  @Test
  void aClaimedRowIsTakenAgainOnceItsLeaseHasPassed() throws SQLException { // OM-09
    exec("INSERT INTO outbox (kind, payload) VALUES ('EMAIL_TICKETS', '{}')");
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      assertThat(rows(s.executeQuery(CLAIM))).isEqualTo(1);
      assertThat(rows(s.executeQuery(CLAIM))).isZero();
      s.execute("UPDATE outbox SET next_attempt_at = now() - interval '1 second'"); // lease elapsed
      assertThat(rows(s.executeQuery(CLAIM))).isEqualTo(1);
    }
    assertThat(count("SELECT attempts FROM outbox")).isEqualTo(2);
  }

  @Test
  void failedRowsDropOutOfTheDueIndex() throws SQLException { // OM-10
    exec(
        "INSERT INTO outbox (kind, payload, attempts, status)"
            + " VALUES ('EMAIL_TICKETS', '{}', 12, 'FAILED')");
    try (Connection c = connect();
        Statement s = c.createStatement()) {
      assertThat(rows(s.executeQuery(CLAIM))).isZero();
    }
  }

  private static int rows(ResultSet rs) throws SQLException {
    int n = 0;
    while (rs.next()) n++;
    return n;
  }
}
