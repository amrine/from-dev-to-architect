package io.teampulse.organization.application.port.out.organization;

import io.teampulse.organization.domain.organization.model.Organization;

import java.util.Optional;

public interface OrganizationRepository {

    Organization create(Organization organization);

    Organization update(Organization organization);

    Optional<Organization> findByReference(String organizationReference);
}
