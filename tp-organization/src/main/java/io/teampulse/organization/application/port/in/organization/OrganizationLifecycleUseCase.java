package io.teampulse.organization.application.port.in.organization;

import io.teampulse.organization.domain.organization.model.Organization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface OrganizationLifecycleUseCase {

    Organization create(
        @NotNull @Valid CreateOrganizationCommand command
    );

    Organization activate(
        @NotBlank String organizationReference
    );

    Organization suspend(
        @NotBlank String organizationReference
    );

    Organization reactivate(
        @NotBlank String organizationReference
    );

    Organization archive(
        @NotBlank String organizationReference
    );
}
