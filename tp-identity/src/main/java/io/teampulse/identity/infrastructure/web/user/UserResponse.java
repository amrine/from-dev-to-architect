package io.teampulse.identity.infrastructure.web.user;

public record UserResponse(
    String reference,
    String email,
    String firstName,
    String lastName,
    String status
) { }
