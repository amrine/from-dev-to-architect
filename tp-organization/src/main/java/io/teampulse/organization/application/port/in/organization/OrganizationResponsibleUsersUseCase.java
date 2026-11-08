package io.teampulse.organization.application.port.in.organization;

import io.teampulse.organization.domain.organization.model.Organization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface OrganizationResponsibleUsersUseCase {

    /**
     * Assigns an administrator candidate to an organization being created.
     * The application requires an available user within that organization and refuses a final
     * available assignment that would complete a deferred activation.
     *
     * @param command organization and candidate user references
     * @return the organization's updated state, or its unchanged state for a no-op
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         if the organization, user availability, or lifecycle does not allow the operation
     */
    Organization assignAdministrator(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    /**
     * Assigns a manager candidate while creating an organization, or fills an
     * empty manager role on a suspended organization. Only available candidates are accepted.
     * A final available assignment
     * that would complete activation or reactivation is refused before mutation.
     *
     * @param command organization and candidate user references
     * @return the organization's updated state, or its unchanged state for a no-op
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         if the organization, user availability, or lifecycle does not allow the operation
     */
    Organization assignManager(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    /**
     * Replaces the administrator of an active or suspended organization with an
     * available user. Reusing the current reference is a no-op; the status is retained.
     *
     * @param command organization and candidate user references
     * @return the organization's updated state, or its unchanged state for a no-op
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         if the organization, user availability, or lifecycle does not allow the operation
     */
    Organization replaceAdministrator(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    /**
     * Replaces an existing manager of an active or suspended organization with an
     * available user. Reusing the current reference is a no-op; the status is retained.
     *
     * @param command organization and candidate user references
     * @return the organization's updated state, or its unchanged state for a no-op
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         if the organization, user availability, or lifecycle does not allow the operation
     */
    Organization replaceManager(
        @NotNull @Valid OrganizationResponsibleCommand command
    );

    /**
     * Removes the manager of an active or suspended organization.
     * An active organization becomes suspended; removing an already absent manager
     * from a suspended organization is a no-op.
     *
     * @param organizationReference functional reference of the organization
     * @return the persisted state without a manager
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         if the organization is absent or its status forbids removal
     */
    Organization removeManager(
        @NotBlank String organizationReference
    );
}
