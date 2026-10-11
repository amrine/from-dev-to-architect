package io.teampulse.identity.infrastructure.web.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(name = "UserResponse")
public record UserResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String reference,
    @Schema(format = "email", requiredMode = Schema.RequiredMode.REQUIRED)
    String email,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String firstName,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    String lastName,
    @Schema(
        allowableValues = {"INVITED", "CREATING", "ACTIVE", "SUSPENDED", "DEACTIVATED"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String status
) {

    public UserResponse {
        Objects.requireNonNull(reference, "reference must not be null");
        if (reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(firstName, "firstName must not be null");
        Objects.requireNonNull(lastName, "lastName must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
