package io.teampulse.organization.api.lifecycle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Synchronous public lifecycle commands; activation is deferred until W008. */
public interface OrganizationLifecycle {

    /**
     * Creates an organization in {@code CREATING}, with no responsible users.
     * The command is synchronous and does not activate the organization.
     *
     * @param command organization name and timezone to validate
     * @return the persisted organization's public business state
     * @throws OrganizationLifecycleException for invalid business data or a technical failure
     */
    OrganizationLifecycleResult create(@NotNull @Valid OrganizationProvisioningCommand command);

    /**
     * Suspends an active organization while preserving its responsible users.
     *
     * @param organizationReference functional reference of the organization
     * @return the persisted state after suspension
     * @throws OrganizationLifecycleException if the organization is absent, the
     *         transition is forbidden, or persistence fails
     */
    OrganizationLifecycleResult suspend(@NotBlank String organizationReference);

    /**
     * Archives a creating, active or suspended organization.
     * Archiving is terminal and retains existing responsible-user references.
     *
     * @param organizationReference functional reference of the organization
     * @return the persisted archived state
     * @throws OrganizationLifecycleException if the organization is absent, is
     *         already archived, or persistence fails
     */
    OrganizationLifecycleResult archive(@NotBlank String organizationReference);
}
