package io.teampulse.administration.application.port.in;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Platform-admin operations for initial user provisioning and user lifecycle changes. */
public interface UserLifecycleAdministrationUseCase {

    PlatformProvisioningResult createInitialUser(
        @NotNull TenantContext tenantContext,
        @NotNull @Valid UserProvisioningCommand initialUser
    );

    PlatformProvisioningResult inviteInitialUser(
        @NotNull TenantContext tenantContext,
        @NotNull @Valid UserProvisioningCommand initialUser
    );

    void suspendUser(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );

    void deactivateUser(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );
}
