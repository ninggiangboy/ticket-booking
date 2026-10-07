package io.ticket.common.mail;

/** Subject plus the two bodies of a multipart/alternative message (DOC-27 §7.1). */
public record RenderedEmail(String subject, String html, String text) {}
