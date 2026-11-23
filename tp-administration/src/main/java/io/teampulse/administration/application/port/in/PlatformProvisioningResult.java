package io.teampulse.administration.application.port.in;

import java.util.Objects;

/** Public organization and user references after creating or inviting an initial user. */
public record PlatformProvisioningResult(
    String organizationReference,
    String userReference
) {

    public PlatformProvisioningResult {
        Objects.requireNonNull(organizationReference, "organizationReference must not be null");
        Objects.requireNonNull(userReference, "userReference must not be null");
        if (organizationReference.isBlank() || userReference.isBlank()) {
            throw new IllegalArgumentException("provisioned references must not be blank");
        }
    }
}
