package io.ticket.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.ticket.auth.model.LoginToken;
import io.ticket.auth.repository.LoginTokenRepository;
import io.ticket.common.jdbc.JdbcConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/** {@code login_token} statements of DOC-19 §3.3 and §4.2 through the Spring Data repository. */
@Testcontainers
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcConfig.class, LoginTokenRepositoryIT.Config.class})
class LoginTokenRepositoryIT {

  @EnableJdbcRepositories(basePackageClasses = LoginTokenRepository.class)
  static class Config {
    @Bean
    JsonMapper jsonMapper() {
      return JsonMapper.builder().build();
    }
  }

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired LoginTokenRepository repository;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM login_token");
  }

  private void issue(byte hash, String email, String ip) {
    repository.insert(new byte[] {hash}, email, "/events", "vi", ip, 900);
  }

  @Test
  void insertedRowIsReadBackWithDatabaseDeadline() {
    issue((byte) 1, "alice@example.com", "203.0.113.9");

    LoginToken token = repository.findById(new byte[] {1}).orElseThrow();

    assertThat(token.email()).isEqualTo("alice@example.com");
    assertThat(token.returnTo()).isEqualTo("/events");
    assertThat(token.locale()).isEqualTo("vi");
    assertThat(token.expiresAt()).isAfter(token.createdAt());
    assertThat(token.usedAt()).isNull();
    assertThat(token.supersededAt()).isNull();
  }

  @Test
  void countsByEmailAndIpInsideTheWindow() {
    issue((byte) 1, "alice@example.com", "203.0.113.9");
    issue((byte) 2, "alice@example.com", "203.0.113.9");
    issue((byte) 3, "bob@example.com", null);
    jdbc.update(
        "UPDATE login_token SET created_at = now() - interval '20 minutes' WHERE token_hash = ?",
        new byte[] {2});

    assertThat(repository.countByEmailSince("alice@example.com", 900)).isEqualTo(1);
    assertThat(repository.countByEmailSince("alice@example.com", 3600)).isEqualTo(2);
    assertThat(repository.countByIpSince("203.0.113.9", 3600)).isEqualTo(2);
    assertThat(repository.countByIpSince("203.0.113.10", 3600)).isZero();
  }

  @Test
  void newerRequestSupersedesTheOlderOne() {
    issue((byte) 1, "alice@example.com", null);
    assertThat(repository.supersedeUnused("alice@example.com")).isEqualTo(1);
    issue((byte) 2, "alice@example.com", null);

    assertThat(repository.consume(new byte[] {1})).isEmpty();
    assertThat(repository.consume(new byte[] {2})).isPresent();
  }

  @Test
  void tokenIsSpentOnlyOnce() {
    issue((byte) 1, "alice@example.com", null);

    Optional<LoginToken> first = repository.consume(new byte[] {1});

    assertThat(first).isPresent();
    assertThat(first.get().email()).isEqualTo("alice@example.com");
    assertThat(first.get().usedAt()).isNotNull();
    assertThat(repository.consume(new byte[] {1})).isEmpty();
  }

  @Test
  void expiredTokenIsRejectedAndKeepsUsedAtNull() {
    issue((byte) 1, "alice@example.com", null);
    jdbc.update("UPDATE login_token SET expires_at = now() - interval '1 second'");

    assertThat(repository.consume(new byte[] {1})).isEmpty();
    assertThat(repository.findById(new byte[] {1}).orElseThrow().usedAt()).isNull();
  }

  @Test
  void unknownTokenIsRejected() {
    assertThat(repository.consume(new byte[] {9})).isEmpty();
  }

  @Test
  void lockEmailRunsInsideATransaction() {
    assertThat(repository.lockEmail("alice@example.com")).isEqualTo(1);
  }

  @Test
  void deletesRowsExpiredMoreThanADayAgo() {
    issue((byte) 1, "alice@example.com", null);
    issue((byte) 2, "bob@example.com", null);
    jdbc.update(
        "UPDATE login_token SET expires_at = now() - interval '25 hours' WHERE token_hash = ?",
        new byte[] {1});

    assertThat(repository.deleteExpired()).isEqualTo(1);
    assertThat(repository.findAll()).hasSize(1);
  }
}
