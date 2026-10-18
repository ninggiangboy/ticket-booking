package io.ticket.auth.repository;

import java.time.Instant;
import java.util.UUID;

/**
 * One valid session row together with the account fields the caller needs (DOC-19 §5.1): the
 * session filter turns it into {@code CurrentUser} and keeps it as the cache entry (DOC-19 §5.2,
 * same fields). {@code organizerId} is null for a buyer without an organizer profile (DR-23).
 */
public record SessionView(
    UUID userId, String csrfToken, String locale, UUID organizerId, Instant lastSeenAt) {}
