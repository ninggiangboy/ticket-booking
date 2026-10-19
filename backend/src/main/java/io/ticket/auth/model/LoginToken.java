package io.ticket.auth.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Aggregate of {@code login_token} (DOC-15 §3). Only the SHA-256 of the token is stored, never the
 * raw token (NFR-07). The state is derived from the timestamps: used, superseded or expired.
 *
 * <p>{@code requested_ip} (an {@code inet} column) is not mapped: it is written by {@code
 * LoginTokenRepository.insert} and only counted in SQL. Rows are created by that query, not by
 * {@code save()}, so that {@code created_at} and {@code expires_at} come from the database clock
 * (DR-12).
 */
@Table("login_token")
public record LoginToken(
    @Id byte[] tokenHash,
    String email,
    String returnTo,
    String locale,
    Instant createdAt,
    Instant expiresAt,
    Instant usedAt,
    Instant supersededAt) {}
