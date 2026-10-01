package io.teampulse.administration.application.port.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface PlatformProvisioningUseCase {

    PlatformProvisioningResult createOrganizationAndInitialUser(
        @NotNull @Valid PlatformProvisioningCommand command
    );

    PlatformProvisioningResult inviteInitialUser(
        @NotNull @Valid PlatformProvisioningCommand command
    );
}
