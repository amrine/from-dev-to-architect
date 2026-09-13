package io.teampulse.organization.application.service.organization;

import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.organization.application.port.in.organization.CreateOrganizationCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationLifecycleUseCase;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class OrganizationLifecycleService implements OrganizationLifecycleUseCase {

    private static final String ORGANIZATION_REFERENCE_PREFIX = "ORG";

    private final ReferenceFactory referenceFactory;
    private final OrganizationRepository organizationRepository;
    private final OrganizationResponsibleUsersValidator responsibleUsersValidator;

    @Override
    @Transactional
    public Organization create(CreateOrganizationCommand command) {
        String organizationReference = referenceFactory.generate(
            ORGANIZATION_REFERENCE_PREFIX
        );
        Organization organization = Organization.create(
            organizationReference,
            command.name(),
            command.timezone()
        );

        return organizationRepository.create(organization);
    }

    @Override
    @Transactional
    public Organization activate(String organizationReference) {
        Organization organization = findOrganization(organizationReference);
        requireStatus(
            organization,
            OrganizationStatus.CREATING,
            "activate"
        );
        requireResponsibleReferences(organization);
        responsibleUsersValidator.validateOperationalResponsibleUsers(
            organization.getReference(),
            organization.getAdminReference(),
            organization.getManagerReference()
        );
        organization.activate();

        return organizationRepository.update(organization);
    }

    @Override
    @Transactional
    public Organization suspend(String organizationReference) {
        Organization organization = findOrganization(organizationReference);
        organization.suspend();

        return organizationRepository.update(organization);
    }

    @Override
    @Transactional
    public Organization reactivate(String organizationReference) {
        Organization organization = findOrganization(organizationReference);
        requireStatus(
            organization,
            OrganizationStatus.SUSPENDED,
            "reactivate"
        );
        requireResponsibleReferences(organization);
        responsibleUsersValidator.validateOperationalResponsibleUsers(
            organization.getReference(),
            organization.getAdminReference(),
            organization.getManagerReference()
        );
        organization.reactivate();

        return organizationRepository.update(organization);
    }

    @Override
    @Transactional
    public Organization archive(String organizationReference) {
        Organization organization = findOrganization(organizationReference);
        organization.archive();

        return organizationRepository.update(organization);
    }

    private Organization findOrganization(String organizationReference) {
        return organizationRepository.findByReference(organizationReference)
            .orElseThrow(() -> new OrganizationException(
                OrganizationErrorCode.NOT_FOUND,
                "Organization was not found: " + organizationReference
            ));
    }

    private static void requireStatus(
        Organization organization,
        OrganizationStatus expectedStatus,
        String operation
    ) {
        if (organization.getStatus() != expectedStatus) {
            throw new OrganizationException(
                OrganizationErrorCode.INVALID_STATUS_TRANSITION,
                "Operation %s is not allowed from status %s"
                    .formatted(operation, organization.getStatus())
            );
        }
    }

    private static void requireResponsibleReferences(Organization organization) {
        if (organization.getAdminReference() == null
            || organization.getManagerReference() == null) {
            throw new OrganizationException(
                OrganizationErrorCode.ACTIVATION_REQUIREMENTS_NOT_MET,
                "Organization requires an administrator and a manager"
            );
        }
    }
}
