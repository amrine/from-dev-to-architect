package io.teampulse.organization.api.organization;

/** Stable provisioning failures for consumers outside tp-organization. */
public enum OrganizationProvisioningErrorCode {
    INVALID_NAME,
    INVALID_TIMEZONE,
    REFERENCE_GENERATION_FAILED,
    CONCURRENT_MODIFICATION,
    OPERATION_FAILED
}
