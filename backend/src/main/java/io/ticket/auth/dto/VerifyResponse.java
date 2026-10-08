package io.ticket.auth.dto;

public record VerifyResponse(String returnTo, String csrfToken) {}
