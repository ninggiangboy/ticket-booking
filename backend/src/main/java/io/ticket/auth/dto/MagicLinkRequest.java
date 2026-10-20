package io.ticket.auth.dto;

public record MagicLinkRequest(String email, String returnTo, String locale) {}
