package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.organization.api.lifecycle.OrganizationLifecycle;
import io.teampulse.organization.api.lifecycle.OrganizationProvisioningCommand;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationResponsibilities;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleUsersUseCase;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@Tag(name = "Organizations", description = "Organization lifecycle and responsibilities.")
@RequestMapping("/api/organizations")
@AllArgsConstructor
public class OrganizationController {

    private final OrganizationLifecycle organizationLifecycle;
    private final OrganizationDirectory organizationDirectory;
    private final OrganizationMapper organizationMapper;
    private final OrganizationResponsibleUsersUseCase responsibleUsersUseCase;

    /**
     * Reads the organization's current responsibilities through its public directory.
     *
     * @param organizationReference target organization from the platform route
     * @return the current status and assigned responsible-user references
     * @throws OrganizationException if the organization does not exist
     */
    @GetMapping("/{organizationReference}/responsibilities")
    @Operation(operationId = "getOrganizationResponsibilities")
    public OrganizationResponsibilities responsibilities(
        @PathVariable String organizationReference
    ) {
        return organizationDirectory.findByOrganizationReference(organizationReference)
            .orElseThrow(() -> new OrganizationException(
                OrganizationErrorCode.NOT_FOUND, "Organization was not found"
            ));
    }

    /**
     * Creates a platform organization in {@code CREATING}.
     *
     * @param command public provisioning command containing the organization name and timezone
     * @return HTTP 201 with the persisted public organization state
     */
    @PostMapping
    @Operation(operationId = "createOrganization")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(
        @Valid @RequestBody OrganizationProvisioningCommand command
    ) {
        return organizationMapper.toResponse(organizationLifecycle.create(command));
    }

    /**
     * Assigns an administrator candidate while applying the T05 activation guard.
     *
     * @param organizationReference target organization
     * @param request candidate user reference
     * @return the organization state after assignment
     */
    @PostMapping("/{organizationReference}/administrator")
    @Operation(operationId = "assignOrganizationAdministrator")
    public OrganizationResponse assignAdministrator(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return organizationMapper.toResponse(responsibleUsersUseCase.assignAdministrator(
            organizationMapper.toCommand(organizationReference, request)
        ));
    }

    /**
     * Replaces an existing operational administrator without changing organization status.
     *
     * @param organizationReference target organization
     * @param request new administrator reference
     * @return the organization state after replacement
     */
    @PutMapping("/{organizationReference}/administrator")
    @Operation(operationId = "replaceOrganizationAdministrator")
    public OrganizationResponse replaceAdministrator(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return organizationMapper.toResponse(responsibleUsersUseCase.replaceAdministrator(
            organizationMapper.toCommand(organizationReference, request)
        ));
    }

    /**
     * Assigns a manager candidate while refusing a final assignment that would activate the
     * organization.
     *
     * @param organizationReference target organization
     * @param request candidate user reference
     * @return the organization state after assignment
     */
    @PostMapping("/{organizationReference}/manager")
    @Operation(operationId = "assignOrganizationManager")
    public OrganizationResponse assignManager(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return organizationMapper.toResponse(responsibleUsersUseCase.assignManager(
            organizationMapper.toCommand(organizationReference, request)
        ));
    }

    /**
     * Replaces an existing operational manager without changing organization status.
     *
     * @param organizationReference target organization
     * @param request new manager reference
     * @return the organization state after replacement
     */
    @PutMapping("/{organizationReference}/manager")
    @Operation(operationId = "replaceOrganizationManager")
    public OrganizationResponse replaceManager(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return organizationMapper.toResponse(responsibleUsersUseCase.replaceManager(
            organizationMapper.toCommand(organizationReference, request)
        ));
    }

    /**
     * Removes the manager; an active organization becomes suspended.
     * Responds with HTTP 204 and no body after the operation completes.
     *
     * @param organizationReference target organization
     */
    @DeleteMapping("/{organizationReference}/manager")
    @Operation(operationId = "removeOrganizationManager")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeManager(
        @PathVariable String organizationReference
    ) {
        responsibleUsersUseCase.removeManager(organizationReference);
    }

    /**
     * Suspends an active organization through its public lifecycle contract.
     *
     * @param organizationReference target organization
     * @return the public organization state after suspension
     */
    @PostMapping("/{organizationReference}/suspension")
    @Operation(operationId = "suspendOrganization")
    public OrganizationResponse suspend(
        @PathVariable String organizationReference
    ) {
        return organizationMapper.toResponse(organizationLifecycle.suspend(organizationReference));
    }

    /**
     * Archives an organization through its public lifecycle contract.
     *
     * @param organizationReference target organization
     * @return the public archived organization state
     */
    @PostMapping("/{organizationReference}/archive")
    @Operation(operationId = "archiveOrganization")
    public OrganizationResponse archive(
        @PathVariable String organizationReference
    ) {
        return organizationMapper.toResponse(organizationLifecycle.archive(organizationReference));
    }

}
