package io.ticket.common.i18n;

import java.nio.charset.StandardCharsets;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

@Configuration
public class I18nConfig {

  /**
   * Missing keys fail loudly instead of leaking a key to users; Vietnamese is the fallback (DOC-31
   * §5).
   */
  @Bean
  MessageSource messageSource() {
    var source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:messages");
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());
    source.setFallbackToSystemLocale(false);
    source.setDefaultLocale(SupportedLocales.DEFAULT);
    source.setUseCodeAsDefaultMessage(false);
    return source;
  }

  /** The bean name {@code localeResolver} is what DispatcherServlet looks up. */
  @Bean
  LocaleResolver localeResolver() {
    return new AppLocaleResolver();
  }
}
