package io.ticket.common.mail;

import java.util.Locale;

/**
 * A rendered message with its envelope; {@code messageId} is the bare id without angle brackets.
 */
public record OutgoingEmail(String to, String messageId, Locale locale, RenderedEmail content) {}
