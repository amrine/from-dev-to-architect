package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.organization.api.organization.OrganizationResponsibilities;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class OrganizationDirectoryService implements OrganizationDirectory {

    private final OrganizationRepository organizationRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Maps the persisted status to availability within a read-only transaction. Technical lookup
     * failures retain their original cause.</p>
     */
    @Override
    public OrganizationAvailability check(String organizationReference) {
        try {
            return organizationRepository.findByReference(organizationReference)
                .map(OrganizationDirectoryService::toAvailability)
                .orElse(OrganizationAvailability.NOT_FOUND);
        } catch (RuntimeException exception) {
            throw new OrganizationDirectoryException(exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the current responsibilities within a read-only transaction. Technical lookup
     * failures retain their original cause.</p>
     */
    @Override
    public Optional<OrganizationResponsibilities> findByOrganizationReference(String organizationReference) {
        try {
            return organizationRepository.findByReference(organizationReference)
                .map(OrganizationDirectoryService::responsibilities);
        } catch (RuntimeException exception) {
            throw new OrganizationDirectoryException(exception);
        }
    }

    /**
     * Copies the status and responsible-user references into the public responsibilities read model.
     * Unassigned roles remain null and no persistence fields are exposed.
     *
     * @param organization domain state returned by persistence
     * @return the public organization responsibilities
     */
    private static OrganizationResponsibilities responsibilities(Organization organization) {
        return new OrganizationResponsibilities(
            organization.getReference(),
            organization.getStatus().name(),
            organization.getAdminReference(),
            organization.getManagerReference()
        );
    }

    /**
     * Exposes only active organizations as available to other modules.
     * Other persisted states are unavailable; absence is handled by the caller.
     *
     * @param organization existing organization whose status is inspected
     * @return availability derived from the current business status
     */
    private static OrganizationAvailability toAvailability(
        Organization organization
    ) {
        return organization.getStatus() == OrganizationStatus.ACTIVE
            ? OrganizationAvailability.AVAILABLE
            : OrganizationAvailability.UNAVAILABLE;
    }
}
