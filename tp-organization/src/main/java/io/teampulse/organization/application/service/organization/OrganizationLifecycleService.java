package io.teampulse.organization.application.service.organization;

import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycle;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleErrorCode;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleException;
import io.teampulse.organization.api.lifecycle.OrganizationLifecycleResult;
import io.teampulse.organization.api.lifecycle.OrganizationProvisioningCommand;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class OrganizationLifecycleService implements OrganizationLifecycle {

    private static final String ORGANIZATION_REFERENCE_PREFIX = "ORG";

    private final ReferenceFactory referenceFactory;
    private final OrganizationRepository organizationRepository;

    /**
     * {@inheritDoc}
     *
     * <p>Generates the functional reference and persists the creating state in one write
     * transaction. Business and technical failures are translated at this public boundary.</p>
     */
    @Override
    @Transactional
    public OrganizationLifecycleResult create(OrganizationProvisioningCommand command) {
        return mapPublicFailure(() -> toResult(organizationRepository.create(Organization.create(
            referenceFactory.generate(ORGANIZATION_REFERENCE_PREFIX),
            command.name(),
            command.timezone()
        ))));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Applies the domain suspension transition and persists it in one write transaction.
     * Business and technical failures are translated at this public boundary.</p>
     */
    @Override
    @Transactional
    public OrganizationLifecycleResult suspend(String organizationReference) {
        return mapPublicFailure(() -> update(organizationReference, Organization::suspend));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Applies the domain archival transition and persists it in one write transaction. Business
     * and technical failures are translated at this public boundary.</p>
     */
    @Override
    @Transactional
    public OrganizationLifecycleResult archive(String organizationReference) {
        return mapPublicFailure(() -> update(organizationReference, Organization::archive));
    }

    /**
     * Loads an organization, applies its domain transition, then persists the result.
     * No write occurs when lookup or domain validation rejects the operation.
     *
     * @param organizationReference organization to load
     * @param transition business operation to apply to the loaded model
     * @return a public snapshot of the persisted state
     * @throws OrganizationException if the organization is absent or the transition is forbidden
     */
    private OrganizationLifecycleResult update(String organizationReference, Consumer<Organization> transition) {
        Organization organization = organizationRepository.findByReference(organizationReference)
            .orElseThrow(() -> new OrganizationException(
                OrganizationErrorCode.NOT_FOUND, "Organization was not found"
            ));
        transition.accept(organization);
        return toResult(organizationRepository.update(organization));
    }

    /**
     * Copies business fields into the public lifecycle result.
     * The internal status is represented by its textual name; technical persistence
     * identity and auditing fields stay inside the module.
     *
     * @param organization persisted domain model
     * @return its public business snapshot
     */
    private static OrganizationLifecycleResult toResult(Organization organization) {
        return new OrganizationLifecycleResult(
            organization.getReference(),
            organization.getName(),
            organization.getTimezone().getId(),
            organization.getStatus().name(),
            organization.getAdminReference(),
            organization.getManagerReference()
        );
    }

    /**
     * Executes an operation behind the public lifecycle error boundary.
     * Known domain errors receive stable public categories. Unexpected runtime
     * failures become technical failures; original causes are preserved for diagnostics.
     *
     * @param <T> operation result type
     * @param operation synchronous business operation to execute
     * @return the operation result
     * @throws OrganizationLifecycleException when the operation fails
     */
    private <T> T mapPublicFailure(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (OrganizationLifecycleException exception) {
            throw exception;
        } catch (OrganizationException exception) {
            OrganizationLifecycleErrorCode publicCode = toPublicCode(exception.getErrorCode());
            if (publicCode == OrganizationLifecycleErrorCode.CONCURRENT_MODIFICATION) {
                log.warn("Organization lifecycle operation conflicted with a concurrent update", exception);
            } else if (publicCode == OrganizationLifecycleErrorCode.OPERATION_FAILED
                || publicCode == OrganizationLifecycleErrorCode.REFERENCE_GENERATION_FAILED) {
                log.error("Organization lifecycle operation failed", exception);
            }
            throw new OrganizationLifecycleException(publicCode, exception);
        } catch (RuntimeException exception) {
            log.error("Organization lifecycle operation failed", exception);
            throw new OrganizationLifecycleException(OrganizationLifecycleErrorCode.OPERATION_FAILED, exception);
        }
    }

    private OrganizationLifecycleErrorCode toPublicCode(OrganizationErrorCode errorCode) {
        return switch (errorCode) {
            case NOT_FOUND -> OrganizationLifecycleErrorCode.ORGANIZATION_NOT_FOUND;
            case INVALID_NAME -> OrganizationLifecycleErrorCode.INVALID_NAME;
            case INVALID_TIMEZONE -> OrganizationLifecycleErrorCode.INVALID_TIMEZONE;
            case INVALID_STATUS_TRANSITION -> OrganizationLifecycleErrorCode.TRANSITION_NOT_ALLOWED;
            case REFERENCE_GENERATION_FAILED -> OrganizationLifecycleErrorCode.REFERENCE_GENERATION_FAILED;
            case CONCURRENT_MODIFICATION -> OrganizationLifecycleErrorCode.CONCURRENT_MODIFICATION;
            case ACTIVATION_REQUIREMENTS_NOT_MET, LIFECYCLE_TRANSITION_DEFERRED,
                 ADMINISTRATOR_NOT_FOUND, ADMINISTRATOR_NOT_AVAILABLE,
                 MANAGER_NOT_FOUND, MANAGER_NOT_AVAILABLE, USER_DIRECTORY_UNAVAILABLE ->
                OrganizationLifecycleErrorCode.OPERATION_FAILED;
        };
    }
}
