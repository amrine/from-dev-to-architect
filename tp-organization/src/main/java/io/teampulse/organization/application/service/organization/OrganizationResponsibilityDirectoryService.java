package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilityDirectoryException;
import io.teampulse.organization.api.organization.OrganizationResponsibilitySnapshot;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class OrganizationResponsibilityDirectoryService
    implements OrganizationResponsibilityDirectory {

    private final OrganizationRepository organizationRepository;

    @Override
    public Optional<OrganizationResponsibilitySnapshot> findByOrganizationReference(
        String organizationReference
    ) {
        try {
            return organizationRepository.findByReference(organizationReference)
                .map(OrganizationResponsibilityDirectoryService::snapshot);
        } catch (RuntimeException exception) {
            throw new OrganizationResponsibilityDirectoryException(exception);
        }
    }

    private static OrganizationResponsibilitySnapshot snapshot(Organization organization) {
        return new OrganizationResponsibilitySnapshot(
            organization.getReference(),
            toPublicState(organization),
            organization.getAdminReference(),
            organization.getManagerReference()
        );
    }

    private static OrganizationLifecycleState toPublicState(Organization organization) {
        return switch (organization.getStatus()) {
            case CREATING -> OrganizationLifecycleState.CREATING;
            case ACTIVE -> OrganizationLifecycleState.ACTIVE;
            case SUSPENDED -> OrganizationLifecycleState.SUSPENDED;
            case ARCHIVED -> OrganizationLifecycleState.ARCHIVED;
        };
    }
}
