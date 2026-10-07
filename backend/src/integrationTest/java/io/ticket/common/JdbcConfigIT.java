package io.ticket.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.ticket.common.jdbc.JdbcConfig;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * DOC-12 §3.2: jsonb round trip through the Map converters, enum as text, ID assigned before
 * insert.
 */
@Testcontainers
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcConfig.class, JdbcConfigIT.Mapper.class})
class JdbcConfigIT {

  enum Phase {
    OPEN,
    CLOSED
  }

  @Table("jdbc_probe")
  record Probe(@Id UUID id, Phase phase, Map<String, Object> payload) {}

  static class Mapper {
    @Bean
    JsonMapper jsonMapper() {
      return JsonMapper.builder().build();
    }
  }

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

  @Autowired JdbcAggregateTemplate template;
  @Autowired JdbcTemplate jdbc;

  @Test
  void jsonbAndEnumRoundTripWithAssignedId() {
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS jdbc_probe (id uuid PRIMARY KEY, phase text NOT NULL, payload jsonb)");
    UUID id = UUID.randomUUID();
    template.insert(new Probe(id, Phase.OPEN, Map.of("seat", "A1", "n", 2)));

    Probe loaded = template.findById(id, Probe.class);
    assertThat(loaded.phase()).isEqualTo(Phase.OPEN);
    assertThat(loaded.payload()).containsEntry("seat", "A1").containsEntry("n", 2);
    assertThat(jdbc.queryForObject("SELECT pg_typeof(payload)::text FROM jdbc_probe", String.class))
        .isEqualTo("jsonb");
    assertThat(jdbc.queryForObject("SELECT phase FROM jdbc_probe", String.class)).isEqualTo("OPEN");
  }
}
