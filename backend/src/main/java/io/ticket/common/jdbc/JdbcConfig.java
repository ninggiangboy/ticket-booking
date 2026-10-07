package io.ticket.common.jdbc;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Data JDBC conversions (DOC-12 §3.2 rule 5): {@code jsonb} columns map to {@code
 * Map<String, Object>} through {@link PGobject}; enums map to {@code text} out of the box.
 * Registering a writing converter also makes Spring Data treat the Map as a simple value instead of
 * a one-to-many association.
 */
@Configuration
public class JdbcConfig {

  @Bean
  JdbcCustomConversions jdbcCustomConversions(JsonMapper mapper) {
    return new JdbcCustomConversions(List.of(new MapToJsonb(mapper), new JsonbToMap(mapper)));
  }

  @WritingConverter
  static class MapToJsonb implements Converter<Map<String, Object>, PGobject> {
    private final JsonMapper mapper;

    MapToJsonb(JsonMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public PGobject convert(Map<String, Object> source) {
      try {
        PGobject object = new PGobject();
        object.setType("jsonb");
        object.setValue(mapper.writeValueAsString(source));
        return object;
      } catch (SQLException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @ReadingConverter
  static class JsonbToMap implements Converter<PGobject, Map<String, Object>> {
    private final JsonMapper mapper;

    JsonbToMap(JsonMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public Map<String, Object> convert(PGobject source) {
      String value = source.getValue();
      return value == null ? null : mapper.readValue(value, new TypeReference<>() {});
    }
  }
}
