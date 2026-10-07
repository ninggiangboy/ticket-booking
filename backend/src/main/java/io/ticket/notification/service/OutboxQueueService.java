package io.ticket.notification.service;

import io.ticket.notification.config.OutboxProperties;
import io.ticket.notification.entity.OutboxMessage;
import io.ticket.notification.repository.OutboxRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The short transactions of the relay: claim, then record each outcome. Sending happens between
 * them, outside any.
 */
@Service
class OutboxQueueService {

  private final OutboxRepository outbox;
  private final OutboxProperties.Relay relay;

  OutboxQueueService(OutboxRepository outbox, OutboxProperties properties) {
    this.outbox = outbox;
    this.relay = properties.relay();
  }

  @Transactional
  List<OutboxMessage> claim() {
    return outbox.claim(relay.batchSize(), relay.lease().toMillis() / 1000.0);
  }

  @Transactional
  void sent(UUID id) {
    outbox.markSent(id);
  }

  /** Backoff while attempts remain, otherwise FAILED. Returns true when the row is now FAILED. */
  @Transactional
  boolean failed(UUID id, String error, boolean permanent) {
    if (!permanent
        && outbox.markRetry(
                id,
                error,
                relay.backoffBase().toMillis() / 1000.0,
                relay.backoffMax().toMillis() / 1000.0,
                relay.maxAttempts())
            > 0) {
      return false;
    }
    outbox.markFailed(id, error);
    return true;
  }

  @Transactional
  void giveBack(List<UUID> ids) {
    outbox.giveBack(ids);
  }
}
