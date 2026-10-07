package io.ticket.common.mail;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Keys {@code app.*} (DOC-34 §3.1): product name, public base URL and the domain part of
 * Message-ID.
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
    @NotBlank @DefaultValue("ticket") String name,
    @NotBlank @DefaultValue("http://localhost:8080") String baseUrl,
    @NotBlank @DefaultValue("ticket.localhost") String domain) {}
