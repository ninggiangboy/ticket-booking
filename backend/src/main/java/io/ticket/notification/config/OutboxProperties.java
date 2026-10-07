package io.ticket.notification.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Keys {@code outbox.*} (DOC-34 §3.3). */
@Validated
@ConfigurationProperties("outbox")
public record OutboxProperties(
    @NotNull @DefaultValue Relay relay, @Min(1) @DefaultValue("500") int enqueueBatchSize) {

  public record Relay(
      @DefaultValue("true") boolean enabled,
      @NotNull @DefaultValue("PT1S") Duration interval,
      @Min(1) @DefaultValue("50") int batchSize,
      @NotNull @DefaultValue("PT60S") Duration lease,
      @NotNull @DefaultValue("PT10S") Duration backoffBase,
      @NotNull @DefaultValue("PT1H") Duration backoffMax,
      @Min(1) @DefaultValue("12") int maxAttempts,
      @NotNull @DefaultValue("PT40S") Duration batchBudget) {}
}
