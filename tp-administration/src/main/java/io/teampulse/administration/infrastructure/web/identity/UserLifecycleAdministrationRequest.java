package io.teampulse.administration.infrastructure.web.identity;

import jakarta.validation.constraints.NotBlank;

public record UserLifecycleAdministrationRequest(
    @NotBlank String email,
    @NotBlank String firstName,
    @NotBlank String lastName
) { }
