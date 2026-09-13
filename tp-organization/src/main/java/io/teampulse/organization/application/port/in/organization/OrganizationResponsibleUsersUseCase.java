package io.teampulse.organization.application.port.in.organization;

import io.teampulse.organization.domain.organization.model.Organization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface OrganizationResponsibleUsersUseCase {

    Organization assignAdministrator(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    Organization assignManager(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    Organization replaceAdministrator(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    Organization replaceManager(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    Organization removeManager(
        @NotBlank String organizationReference
    );
}
