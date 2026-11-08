package io.teampulse.organization.application.port.out.organization;

import io.teampulse.organization.domain.organization.model.Organization;

import java.util.Optional;

public interface OrganizationRepository {

    /**
     * Persists a new organization's business state.
     * The adapter owns technical identity, audit data and constraint translation.
     *
     * @param organization new organization to persist
     * @return the business state restored from persistence
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         for a recognized organization reference collision
     */
    Organization create(Organization organization);

    /**
     * Updates an existing organization identified by its functional reference.
     *
     * @param organization business state to persist
     * @return the persisted business state
     * @throws io.teampulse.organization.domain.organization.error.OrganizationException
     *         if the organization is absent or a recognized persistence conflict occurs
     */
    Organization update(Organization organization);

    /**
     * Looks up an organization by its globally unique functional reference.
     *
     * @param organizationReference functional organization reference
     * @return the restored organization, or empty when absent
     */
    Optional<Organization> findByReference(String organizationReference);
}
