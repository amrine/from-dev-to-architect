package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.api.organization.OrganizationLifecycleState;
import io.teampulse.organization.api.organization.OrganizationProvisioning;
import io.teampulse.organization.api.organization.OrganizationProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationProvisioningErrorCode;
import io.teampulse.organization.api.organization.OrganizationProvisioningException;
import io.teampulse.organization.api.organization.OrganizationProvisioningResult;
import io.teampulse.organization.application.port.in.organization.CreateOrganizationCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationLifecycleUseCase;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
public class OrganizationProvisioningContractService implements OrganizationProvisioning {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        OrganizationProvisioningContractService.class
    );

    private final OrganizationLifecycleUseCase organizationLifecycleUseCase;

    @Override
    public OrganizationProvisioningResult createOrganization(
        OrganizationProvisioningCommand command
    ) {
        try {
            Organization organization = organizationLifecycleUseCase.create(
                new CreateOrganizationCommand(command.name(), command.timezone())
            );
            return new OrganizationProvisioningResult(
                organization.getReference(),
                toPublicState(organization)
            );
        } catch (OrganizationException exception) {
            OrganizationProvisioningErrorCode publicCode = toPublicCode(exception);
            if (publicCode == OrganizationProvisioningErrorCode.REFERENCE_GENERATION_FAILED
                || publicCode == OrganizationProvisioningErrorCode.CONCURRENT_MODIFICATION
                || publicCode == OrganizationProvisioningErrorCode.OPERATION_FAILED) {
                LOGGER.error("Organization provisioning failed", exception);
            }
            throw new OrganizationProvisioningException(
                publicCode,
                safeMessage(publicCode)
            );
        }
    }

    private static OrganizationLifecycleState toPublicState(Organization organization) {
        return switch (organization.getStatus()) {
            case CREATING -> OrganizationLifecycleState.CREATING;
            case ACTIVE -> OrganizationLifecycleState.ACTIVE;
            case SUSPENDED -> OrganizationLifecycleState.SUSPENDED;
            case ARCHIVED -> OrganizationLifecycleState.ARCHIVED;
        };
    }

    private static OrganizationProvisioningErrorCode toPublicCode(
        OrganizationException exception
    ) {
        return switch (exception.getErrorCode()) {
            case INVALID_NAME -> OrganizationProvisioningErrorCode.INVALID_NAME;
            case INVALID_TIMEZONE -> OrganizationProvisioningErrorCode.INVALID_TIMEZONE;
            case REFERENCE_GENERATION_FAILED ->
                OrganizationProvisioningErrorCode.REFERENCE_GENERATION_FAILED;
            case CONCURRENT_MODIFICATION ->
                OrganizationProvisioningErrorCode.CONCURRENT_MODIFICATION;
            default -> OrganizationProvisioningErrorCode.OPERATION_FAILED;
        };
    }

    private static String safeMessage(OrganizationProvisioningErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_NAME -> "Organization name is invalid";
            case INVALID_TIMEZONE -> "Organization timezone is invalid";
            case REFERENCE_GENERATION_FAILED -> "Organization could not be created";
            case CONCURRENT_MODIFICATION -> "Organization changed during provisioning";
            case OPERATION_FAILED -> "Organization provisioning failed";
        };
    }
}
