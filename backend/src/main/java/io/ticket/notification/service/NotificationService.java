package io.ticket.notification.service;

import io.ticket.notification.NotificationApi;
import io.ticket.notification.OutboxKind;
import io.ticket.notification.config.OutboxProperties;
import io.ticket.notification.repository.OutboxRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes outbox rows in the caller's transaction: calling it outside one is a programming error
 * (ADR-0006).
 */
@Service
class NotificationService implements NotificationApi {

  private final OutboxRepository outbox;
  private final JsonMapper mapper;
  private final OutboxProperties properties;

  NotificationService(OutboxRepository outbox, JsonMapper mapper, OutboxProperties properties) {
    this.outbox = outbox;
    this.mapper = mapper;
    this.properties = properties;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public UUID enqueue(OutboxKind kind, Map<String, Object> payload) {
    return outbox.insert(kind, mapper.writeValueAsString(payload));
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void enqueueAll(OutboxKind kind, List<Map<String, Object>> payloads) {
    List<String> json = payloads.stream().map(mapper::writeValueAsString).toList();
    outbox.insertAll(kind, json, properties.enqueueBatchSize());
  }
}
