package io.teampulse.identity.api.lifecycle;

import io.teampulse.common.context.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Synchronous identity contract for lifecycle changes orchestrated by the
 * platform administration module.
 */
public interface UserLifecycle {

    /**
     * Creates a user in the requested tenant without exposing identity's
     * internal domain model.
     */
    UserLifecycleResult create(
        @NotNull TenantContext tenantContext,
        @NotNull @Valid UserProvisioningCommand command
    );

    /**
     * Creates an invitation in the requested tenant without accepting it or
     * activating the account.
     */
    UserLifecycleResult invite(
        @NotNull TenantContext tenantContext,
        @NotNull @Valid UserProvisioningCommand command
    );

    /**
     * Suspends a user after the caller has checked cross-module responsibilities.
     *
     * @throws UserLifecycleException when the user is missing, cannot be
     *         transitioned, or a technical operation fails
     */
    void suspend(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );

    /**
     * Deactivates a user after the caller has checked cross-module responsibilities.
     *
     * @throws UserLifecycleException when the user is missing, cannot be
     *         transitioned, or a technical operation fails
     */
    void deactivate(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );
}
