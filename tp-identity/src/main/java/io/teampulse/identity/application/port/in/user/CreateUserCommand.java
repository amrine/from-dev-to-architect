package io.teampulse.identity.application.port.in.user;

import jakarta.validation.constraints.NotBlank;

public record CreateUserCommand(
    @NotBlank String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) { }
