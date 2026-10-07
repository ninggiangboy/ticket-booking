package io.ticket.common.security;

import io.ticket.common.error.ErrorCode;
import io.ticket.common.error.ForbiddenException;
import java.util.UUID;

/** The authenticated caller of the current request, set by the session filter (DOC-19 §5.1). */
public record CurrentUser(
    UUID userId, byte[] sessionHash, String locale, UUID organizerId, String csrfToken) {

  /** Request attribute under which the session filter publishes the caller. */
  public static final String REQUEST_ATTRIBUTE = CurrentUser.class.getName();

  public UUID requireOrganizer() {
    if (organizerId == null) {
      throw new ForbiddenException(ErrorCode.ORGANIZER_PROFILE_REQUIRED);
    }
    return organizerId;
  }
}
