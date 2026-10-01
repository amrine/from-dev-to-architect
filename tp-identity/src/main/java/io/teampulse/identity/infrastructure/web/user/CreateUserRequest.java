package io.teampulse.identity.infrastructure.web.user;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @NotBlank String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) { }
