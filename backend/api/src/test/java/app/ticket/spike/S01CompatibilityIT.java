package app.ticket.spike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.ticket.TicketApplication;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.stripe.net.Webhook;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.IsSimpleOp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Testcontainers
@SpringBootTest(classes = TicketApplication.class)
@AutoConfigureMockMvc
class S01CompatibilityIT {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Container
  static GenericContainer<?> seaweed =
      new GenericContainer<>("chrislusf/seaweedfs:4.48")
          .withCopyToContainer(
              org.testcontainers.images.builder.Transferable.of(
                  "{\"identities\":[{\"name\":\"app\",\"credentials\":[{\"accessKey\":\"AKIATEST\",\"secretKey\":\"secrettest\"}],\"actions\":[\"Admin\",\"Read\",\"Write\",\"List\"]}]}"),
              "/etc/seaweedfs/s3.json")
          .withCommand("server", "-dir=/data", "-s3", "-s3.port=8333", "-s3.config=/etc/seaweedfs/s3.json")
          .withExposedPorts(8333)
          .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(90)));

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired JdbcAggregateTemplate template;
  @Autowired SpikeItemRepository repo;

  // #2 Spring Boot 4 + Security: the context starts and CSRF is enforced on writes.
  @Test
  void securityStartsAndCsrfIsEnforced() throws Exception {
    mvc.perform(get("/spike/ping")).andExpect(status().isOk());
    mvc.perform(post("/spike/write")).andExpect(status().isForbidden());
    mvc.perform(post("/spike/write").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk());
  }

  // #3 Spring Data JDBC against PostgreSQL 18.
  @Test
  void dataJdbcInsertWithAssignedIdJsonbAndConditionalUpdate() {
    UUID id = UUID.randomUUID();
    template.insert(
        new SpikeItem(
            id,
            SpikeItem.Status.HELD,
            new JsonDoc("{\"a\": 1}"),
            Set.of(new SpikeItem.SpikeLine("A1"), new SpikeItem.SpikeLine("A2"))));

    SpikeItem loaded = repo.findById(id).orElseThrow();
    assertThat(loaded.status()).isEqualTo(SpikeItem.Status.HELD);
    assertThat(loaded.payload().value()).contains("\"a\"");
    assertThat(loaded.lines()).hasSize(2);
    assertThat(jdbc.queryForObject("select pg_typeof(payload)::text from spike_item where id = ?", String.class, id))
        .isEqualTo("jsonb");

    assertThat(repo.transition(id, SpikeItem.Status.HELD, SpikeItem.Status.CONFIRMED)).isEqualTo(1);
    assertThat(repo.transition(id, SpikeItem.Status.HELD, SpikeItem.Status.EXPIRED)).isZero();
    assertThat(repo.findById(id).orElseThrow().status()).isEqualTo(SpikeItem.Status.CONFIRMED);
  }

  // #4 Flyway on PostgreSQL 18: uuidv7(), partial index, immutability trigger.
  @Test
  void flywayMigrationAppliedUuidv7PartialIndexAndTrigger() {
    String version =
        jdbc.queryForObject("select current_setting('server_version_num')::int / 10000", String.class);
    assertThat(version).isEqualTo("18");
    UUID id = jdbc.queryForObject("insert into spike_ledger(amount) values (5) returning id", UUID.class);
    assertThat(id.version()).isEqualTo(7);
    assertThat(
            jdbc.queryForObject(
                "select indexdef from pg_indexes where indexname = 'spike_item_held_idx'", String.class))
        .contains("WHERE (status = 'HELD'");
    assertThatThrownBy(() -> jdbc.update("update spike_ledger set amount = 6 where id = ?", id))
        .hasMessageContaining("immutable");
  }

  // #5 springdoc: OpenAPI 3.1 document.
  @Test
  void springdocServesOpenApi31() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.1")))
        .andExpect(jsonPath("$.paths['/spike/ping']").exists());
    String doc = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
    java.nio.file.Files.writeString(java.nio.file.Path.of("build/openapi-s01.json"), doc);
  }

  // #6 stripe-java: webhook signature verification (offline).
  @Test
  void stripeWebhookSignatureVerifies() throws Exception {
    String payload =
        "{\"id\":\"evt_1\",\"object\":\"event\",\"api_version\":\"2025-01-01\",\"type\":\"payment_intent.succeeded\","
            + "\"data\":{\"object\":{\"id\":\"pi_1\",\"object\":\"payment_intent\"}}}";
    String secret = "whsec_test";
    long ts = System.currentTimeMillis() / 1000;
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] sig = mac.doFinal((ts + "." + payload).getBytes(StandardCharsets.UTF_8));
    String header = "t=" + ts + ",v1=" + java.util.HexFormat.of().formatHex(sig);
    var event = Webhook.constructEvent(payload, header, secret);
    assertThat(event.getType()).isEqualTo("payment_intent.succeeded");
    assertThat(event.getId()).isEqualTo("evt_1");
  }

  // #7 AWS SDK v2 S3 with SeaweedFS.
  @Test
  void s3RoundTripAgainstSeaweedFs() {
    try (S3Client s3 =
        S3Client.builder()
            .endpointOverride(URI.create("http://" + seaweed.getHost() + ":" + seaweed.getMappedPort(8333)))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATEST", "secrettest")))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .httpClient(UrlConnectionHttpClient.create())
            .build()) {
      s3.createBucket(CreateBucketRequest.builder().bucket("media").build());
      s3.putObject(PutObjectRequest.builder().bucket("media").key("a.txt").build(), RequestBody.fromString("hello"));
      String body = s3.getObjectAsBytes(GetObjectRequest.builder().bucket("media").key("a.txt").build()).asUtf8String();
      assertThat(body).isEqualTo("hello");
      s3.deleteObject(DeleteObjectRequest.builder().bucket("media").key("a.txt").build());
      assertThatThrownBy(() -> s3.getObject(GetObjectRequest.builder().bucket("media").key("a.txt").build()))
          .isInstanceOf(software.amazon.awssdk.services.s3.model.NoSuchKeyException.class);
    }
  }

  // #8 Spring Modulith.
  @Test
  void modulithVerifies() {
    ApplicationModules.of(TicketApplication.class).verify();
  }

  // #9 ArchUnit reads JDK 25 bytecode.
  @Test
  void archUnitReadsClassesCompiledByJdk25() {
    var classes =
        new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("app.ticket");
    assertThat(classes.size()).isGreaterThan(3);
    Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .layer("Spike")
        .definedBy("app.ticket.spike..")
        .whereLayer("Spike")
        .mayNotBeAccessedByAnyLayer()
        .check(classes);
  }

  // #11 JTS, JCS, networknt.
  @Test
  void jtsJcsAndJsonSchemaWork() throws Exception {
    GeometryFactory gf = new GeometryFactory();
    Polygon bowtie =
        gf.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0), new Coordinate(2, 2), new Coordinate(2, 0), new Coordinate(0, 2), new Coordinate(0, 0)
            });
    assertThat(new IsSimpleOp(bowtie.getBoundary()).isSimple()).isFalse();
    Polygon square =
        gf.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0), new Coordinate(0, 2), new Coordinate(2, 2), new Coordinate(2, 0), new Coordinate(0, 0)
            });
    assertThat(org.locationtech.jts.geom.prep.PreparedGeometryFactory.prepare(square).contains(gf.createPoint(new Coordinate(1, 1))))
        .isTrue();

    String canonical =
        new org.erdtman.jcs.JsonCanonicalizer("{\"b\": 2, \"a\": [1, 0.50]}").getEncodedString();
    assertThat(canonical).isEqualTo("{\"a\":[1,0.5],\"b\":2}");

    Schema schema =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema("{\"type\":\"object\",\"required\":[\"id\"],\"properties\":{\"id\":{\"type\":\"string\"}}}");
    var mapper = new tools.jackson.databind.ObjectMapper();
    assertThat(schema.validate(mapper.readTree("{\"id\":\"x\"}"))).isEmpty();
    assertThat(schema.validate(mapper.readTree("{}"))).isNotEmpty();
  }
}
