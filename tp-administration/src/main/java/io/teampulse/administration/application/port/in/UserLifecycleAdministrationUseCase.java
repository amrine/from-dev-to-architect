package io.teampulse.administration.application.port.in;

import io.teampulse.common.context.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface UserLifecycleAdministrationUseCase {

    void suspendUser(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );

    void deactivateUser(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );
}
