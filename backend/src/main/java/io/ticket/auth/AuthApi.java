package io.ticket.auth;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Public API of the auth module exposed to other modules (DOC-19 §11). */
public interface AuthApi {

  /** Email and current locale of the given users; non-existent users are omitted. */
  Map<UUID, Recipient> recipientsOf(Collection<UUID> userIds);

  /** Organizer contact email: contact_email, or owner's email if null. */
  String organizerContactEmail(UUID organizerId);

  record Recipient(UUID userId, String email, String locale) {}
}
