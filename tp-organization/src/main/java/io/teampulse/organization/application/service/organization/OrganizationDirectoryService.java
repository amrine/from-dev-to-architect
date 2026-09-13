package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
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

    @Override
    public OrganizationAvailability check(String organizationReference) {
        Optional<Organization> organization;
        try {
            organization = organizationRepository.findByReference(
                organizationReference
            );
        } catch (RuntimeException exception) {
            throw new OrganizationDirectoryException(exception);
        }

        return organization
            .map(OrganizationDirectoryService::toAvailability)
            .orElse(OrganizationAvailability.NOT_FOUND);
    }

    private static OrganizationAvailability toAvailability(
        Organization organization
    ) {
        return organization.getStatus() == OrganizationStatus.ACTIVE
            ? OrganizationAvailability.AVAILABLE
            : OrganizationAvailability.UNAVAILABLE;
    }
}
