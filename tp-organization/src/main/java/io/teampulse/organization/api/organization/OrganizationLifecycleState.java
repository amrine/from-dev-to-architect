package io.teampulse.organization.api.organization;

/** Public lifecycle state exposed without leaking the organization domain model. */
public enum OrganizationLifecycleState {
    CREATING,
    ACTIVE,
    SUSPENDED,
    ARCHIVED
}
