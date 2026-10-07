package io.ticket.notification.job;

import io.ticket.notification.service.OutboxRelayService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ticks every {@code outbox.relay.interval}; {@code fixedDelay} so a slow pass never overlaps
 * itself (DOC-27 §5.2).
 */
@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
class OutboxRelay {

  private final OutboxRelayService relay;

  OutboxRelay(OutboxRelayService relay) {
    this.relay = relay;
  }

  @Scheduled(fixedDelayString = "${outbox.relay.interval:PT1S}")
  void tick() {
    relay.runOnce();
  }
}
