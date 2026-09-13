package io.teampulse.organization.application.service.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleUsersUseCase;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class OrganizationResponsibleUsersService
    implements OrganizationResponsibleUsersUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationResponsibleUsersValidator responsibleUsersValidator;

    @Override
    @Transactional
    public Organization assignAdministrator(OrganizationResponsibleCommand command) {
        Organization organization = findOrganization(command.organizationReference());
        UserAvailability availability =
            responsibleUsersValidator.validateAssignableAdministrator(
                organization.getReference(),
                command.responsibleReference()
            );

        organization.assignAdministrator(command.responsibleReference());
        activateWhenResponsibleUsersAreAvailable(organization, availability);

        return organizationRepository.update(organization);
    }

    @Override
    @Transactional
    public Organization assignManager(OrganizationResponsibleCommand command) {
        Organization organization = findOrganization(command.organizationReference());
        UserAvailability availability =
            responsibleUsersValidator.validateAssignableManager(
                organization.getReference(),
                command.responsibleReference()
            );

        organization.assignManager(command.responsibleReference());
        activateWhenResponsibleUsersAreAvailable(organization, availability);

        return organizationRepository.update(organization);
    }

    @Override
    @Transactional
    public Organization replaceAdministrator(OrganizationResponsibleCommand command) {
        Organization organization = findOrganization(command.organizationReference());
        String newReference = command.responsibleReference();

        if (Objects.equals(organization.getAdminReference(), newReference)) {
            organization.replaceAdministrator(newReference);
            return organization;
        }

        responsibleUsersValidator.validateOperationalAdministrator(
            organization.getReference(),
            newReference
        );
        organization.replaceAdministrator(newReference);

        return organizationRepository.update(organization);
    }

    @Override
    @Transactional
    public Organization replaceManager(OrganizationResponsibleCommand command) {
        Organization organization = findOrganization(command.organizationReference());
        String newReference = command.responsibleReference();

        if (Objects.equals(organization.getManagerReference(), newReference)) {
            organization.replaceManager(newReference);
            return organization;
        }

        responsibleUsersValidator.validateOperationalManager(
            organization.getReference(),
            newReference
        );
        organization.replaceManager(newReference);

        return organizationRepository.update(organization);
    }

    @Override
    @Transactional
    public Organization removeManager(String organizationReference) {
        Organization organization = findOrganization(organizationReference);
        organization.removeManager();

        return organizationRepository.update(organization);
    }

    private void activateWhenResponsibleUsersAreAvailable(
        Organization organization,
        UserAvailability assignedUserAvailability
    ) {
        if (assignedUserAvailability != UserAvailability.AVAILABLE
            || organization.getAdminReference() == null
            || organization.getManagerReference() == null) {
            return;
        }

        if (organization.getStatus() != OrganizationStatus.CREATING
            && organization.getStatus() != OrganizationStatus.SUSPENDED) {
            return;
        }

        boolean bothAvailable = responsibleUsersValidator
            .areResponsibleUsersAvailable(
                organization.getReference(),
                organization.getAdminReference(),
                organization.getManagerReference()
            );
        if (!bothAvailable) {
            return;
        }

        if (organization.getStatus() == OrganizationStatus.CREATING) {
            organization.activate();
        } else {
            organization.reactivate();
        }
    }

    private Organization findOrganization(String organizationReference) {
        return organizationRepository.findByReference(organizationReference)
            .orElseThrow(() -> new OrganizationException(
                OrganizationErrorCode.NOT_FOUND,
                "Organization was not found: " + organizationReference
            ));
    }
}
