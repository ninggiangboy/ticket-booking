package io.ticket.support;

import io.ticket.TicketApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Whole application against real PostgreSQL, Redis, SeaweedFS and Mailpit, the images production
 * uses (DR-04). Containers are started once per JVM and shared by every subclass.
 */
@SpringBootTest(classes = TicketApplication.class)
public abstract class IntegrationTestBase {

  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:8.2-alpine").withExposedPorts(6379);

  static final GenericContainer<?> STORAGE =
      new GenericContainer<>("chrislusf/seaweedfs:4.48")
          .withCopyToContainer(
              Transferable.of(
                  "{\"identities\":[{\"name\":\"app\",\"credentials\":[{\"accessKey\":\"test\",\"secretKey\":\"test-secret\"}],\"actions\":[\"Admin\",\"Read\",\"Write\",\"List\"]}]}"),
              "/etc/seaweedfs/s3.json")
          .withCommand(
              "server", "-dir=/data", "-s3", "-s3.port=8333", "-s3.config=/etc/seaweedfs/s3.json")
          .withExposedPorts(8333)
          .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(90)));

  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.27")
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/livez").forPort(8025));

  static {
    POSTGRES.start();
    REDIS.start();
    STORAGE.start();
    MAILPIT.start();
  }

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add(
        "storage.s3.endpoint",
        () -> "http://" + STORAGE.getHost() + ":" + STORAGE.getMappedPort(8333));
    registry.add("storage.s3.access-key", () -> "test");
    registry.add("storage.s3.secret-key", () -> "test-secret");
    registry.add("spring.mail.host", MAILPIT::getHost);
    registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    registry.add("outbox.relay.enabled", () -> "false");
  }

  // ---- Mailpit helpers ----

  private static final HttpClient HTTP = HttpClient.newHttpClient();

  private static String mailpit(String method, String path) {
    try {
      var request =
          HttpRequest.newBuilder(
                  URI.create(
                      "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + path))
              .method(method, HttpRequest.BodyPublishers.noBody())
              .build();
      return HTTP.send(request, HttpResponse.BodyHandlers.ofString()).body();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** JSON of {@code GET /api/v1/messages}. */
  protected static String mailpitMessages() {
    return mailpit("GET", "/api/v1/messages");
  }

  /** JSON of one message, including its headers and bodies. */
  protected static String mailpitMessage(String id) {
    return mailpit("GET", "/api/v1/message/" + id);
  }

  protected static void clearMailpit() {
    mailpit("DELETE", "/api/v1/messages");
  }
}
