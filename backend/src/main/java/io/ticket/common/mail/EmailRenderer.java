package io.ticket.common.mail;

import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders {@code templates/email/<name>.html} and {@code .txt} with strings from {@code
 * messages_<locale>} ({@code email.<name>.*}); the subject is {@code email.<name>.subject} (DOC-27
 * §7.1).
 */
@Component
public class EmailRenderer {

  private final SpringTemplateEngine html;
  private final SpringTemplateEngine text;
  private final MessageSource messages;

  EmailRenderer(MessageSource messages) {
    this.messages = messages;
    this.html = engine(TemplateMode.HTML, ".html", messages);
    this.text = engine(TemplateMode.TEXT, ".txt", messages);
  }

  public RenderedEmail render(String template, Locale locale, Map<String, Object> model) {
    Context context = new Context(locale, model);
    String subject =
        messages.getMessage("email." + template + ".subject", subjectArgs(model), locale);
    return new RenderedEmail(
        subject,
        html.process("email/" + template, context),
        text.process("email/" + template, context));
  }

  private static Object[] subjectArgs(Map<String, Object> model) {
    Object args = model.get("subjectArgs");
    return args instanceof Object[] array ? array : new Object[0];
  }

  private static SpringTemplateEngine engine(
      TemplateMode mode, String suffix, MessageSource messages) {
    var resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(suffix);
    resolver.setTemplateMode(mode);
    resolver.setCharacterEncoding("UTF-8");
    resolver.setCacheable(true);
    var engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    engine.setTemplateEngineMessageSource(messages);
    return engine;
  }
}
