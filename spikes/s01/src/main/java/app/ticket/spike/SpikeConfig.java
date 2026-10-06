package app.ticket.spike;

import java.sql.SQLException;
import java.util.List;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SpikeConfig {

  @Bean
  JdbcCustomConversions jdbcCustomConversions() {
    return new JdbcCustomConversions(List.of(new JsonDocToPg(), new PgToJsonDoc()));
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
  }

  @WritingConverter
  static class JsonDocToPg implements Converter<JsonDoc, PGobject> {
    @Override
    public PGobject convert(JsonDoc source) {
      try {
        PGobject o = new PGobject();
        o.setType("jsonb");
        o.setValue(source.value());
        return o;
      } catch (SQLException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @ReadingConverter
  static class PgToJsonDoc implements Converter<PGobject, JsonDoc> {
    @Override
    public JsonDoc convert(PGobject source) {
      return new JsonDoc(source.getValue());
    }
  }
}
