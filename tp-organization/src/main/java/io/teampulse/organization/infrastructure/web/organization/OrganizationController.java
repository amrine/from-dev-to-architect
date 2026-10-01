package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.organization.application.port.in.organization.CreateOrganizationCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationLifecycleUseCase;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleUsersUseCase;
import io.teampulse.organization.domain.organization.model.Organization;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
@AllArgsConstructor
public class OrganizationController {

    private final OrganizationLifecycleUseCase organizationLifecycleUseCase;
    private final OrganizationResponsibleUsersUseCase responsibleUsersUseCase;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(
        @Valid @RequestBody CreateOrganizationRequest request
    ) {
        Organization organization = organizationLifecycleUseCase.create(
            new CreateOrganizationCommand(request.name(), request.timezone())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response(organization));
    }

    @PostMapping("/{organizationReference}/administrator")
    public OrganizationResponse assignAdministrator(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return response(responsibleUsersUseCase.assignAdministrator(
            command(organizationReference, request)
        ));
    }

    @PutMapping("/{organizationReference}/administrator")
    public OrganizationResponse replaceAdministrator(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return response(responsibleUsersUseCase.replaceAdministrator(
            command(organizationReference, request)
        ));
    }

    @PostMapping("/{organizationReference}/manager")
    public OrganizationResponse assignManager(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return response(responsibleUsersUseCase.assignManager(
            command(organizationReference, request)
        ));
    }

    @PutMapping("/{organizationReference}/manager")
    public OrganizationResponse replaceManager(
        @PathVariable String organizationReference,
        @Valid @RequestBody AssignOrganizationResponsibleUserRequest request
    ) {
        return response(responsibleUsersUseCase.replaceManager(
            command(organizationReference, request)
        ));
    }

    @DeleteMapping("/{organizationReference}/manager")
    public ResponseEntity<Void> removeManager(
        @PathVariable String organizationReference
    ) {
        responsibleUsersUseCase.removeManager(organizationReference);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{organizationReference}/suspension")
    public OrganizationResponse suspend(
        @PathVariable String organizationReference
    ) {
        return response(organizationLifecycleUseCase.suspend(organizationReference));
    }

    @PostMapping("/{organizationReference}/archive")
    public OrganizationResponse archive(
        @PathVariable String organizationReference
    ) {
        return response(organizationLifecycleUseCase.archive(organizationReference));
    }

    private static OrganizationResponsibleCommand command(
        String organizationReference,
        AssignOrganizationResponsibleUserRequest request
    ) {
        return new OrganizationResponsibleCommand(
            organizationReference,
            request.userReference()
        );
    }

    private static OrganizationResponse response(Organization organization) {
        return new OrganizationResponse(
            organization.getReference(),
            organization.getName(),
            organization.getTimezone().getId(),
            organization.getStatus().name(),
            organization.getAdminReference(),
            organization.getManagerReference()
        );
    }
}
