package io.ticket.common.mail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Keys {@code mail.*} (DOC-34 §3.3). {@code from} is the complete From header, display name
 * included.
 */
@Validated
@ConfigurationProperties("mail")
public record MailProperties(
    @NotBlank @DefaultValue("ticket <no-reply@ticket.localhost>") String from,
    @NotNull @DefaultValue Smtp smtp) {

  public record Smtp(@NotNull @DefaultValue("PT5S") Duration timeout) {}
}
