package io.teampulse.organization.api.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Stable failures exposed by the public lifecycle contract. */
@Getter
@AllArgsConstructor
public enum OrganizationLifecycleErrorCode {
    ORGANIZATION_NOT_FOUND("Organization was not found"),
    INVALID_NAME("Organization name is invalid"),
    INVALID_TIMEZONE("Organization timezone is invalid"),
    TRANSITION_NOT_ALLOWED("Organization transition is not allowed"),
    REFERENCE_GENERATION_FAILED("Organization could not be created"),
    CONCURRENT_MODIFICATION("Organization was modified concurrently"),
    OPERATION_FAILED("Organization lifecycle operation failed");

    private final String message;
}
