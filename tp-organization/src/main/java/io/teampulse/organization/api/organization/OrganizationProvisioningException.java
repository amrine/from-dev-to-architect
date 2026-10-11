package io.teampulse.organization.api.organization;

import java.util.Objects;

/** Public organization provisioning failure without internal exception details. */
public class OrganizationProvisioningException extends RuntimeException {

    private final OrganizationProvisioningErrorCode errorCode;

    public OrganizationProvisioningException(
        OrganizationProvisioningErrorCode errorCode,
        String message
    ) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public OrganizationProvisioningErrorCode getErrorCode() {
        return errorCode;
    }
}
