package io.ticket.notification.model;

import io.ticket.notification.OutboxKind;
import java.util.UUID;

/** A claimed outbox row. {@code attempts} already includes the attempt being made. */
public record OutboxMessage(UUID outboxId, OutboxKind kind, String payloadJson, int attempts) {}
