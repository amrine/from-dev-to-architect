package io.teampulse.team.infrastructure.web.team;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.application.port.in.team.TeamResponsibleUsersUseCase;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Teams", description = "Tenant-scoped teams and memberships.")
@RequestMapping("/api/teams")
@AllArgsConstructor
public class TeamController {

    private final TeamLifecycleUseCase teamLifecycleUseCase;
    private final TeamResponsibleUsersUseCase responsibleUsersUseCase;
    private final TenantContextProvider tenantContextProvider;
    private final TeamMapper teamMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createTeam", summary = "Create an active team in the current tenant")
    public TeamResponse create(@Valid @RequestBody CreateTeamRequest request) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMapper.toResponse(teamLifecycleUseCase.create(tenantContext, teamMapper.toCommand(request)));
    }

    @PostMapping("/{teamReference}/suspension")
    @Operation(operationId = "suspendTeam", summary = "Suspend a team")
    public TeamResponse suspend(@PathVariable String teamReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMapper.toResponse(teamLifecycleUseCase.suspend(tenantContext, teamReference));
    }

    @PostMapping("/{teamReference}/reactivation")
    @Operation(
            operationId = "reactivateTeam",
            summary = "Reactivate a suspended team when its organization and responsible users are available")
    public TeamResponse reactivate(@PathVariable String teamReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMapper.toResponse(teamLifecycleUseCase.reactivate(tenantContext, teamReference));
    }

    @PostMapping("/{teamReference}/archive")
    @Operation(operationId = "archiveTeam", summary = "Archive a team")
    public TeamResponse archive(@PathVariable String teamReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMapper.toResponse(teamLifecycleUseCase.archive(tenantContext, teamReference));
    }

    @PutMapping("/{teamReference}/administrator")
    @Operation(
            operationId = "replaceTeamAdministrator",
            summary = "Replace an administrator in an active or suspended team")
    public TeamResponse replaceAdministrator(
            @PathVariable String teamReference, @Valid @RequestBody TeamResponsibleUserRequest request) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMapper.toResponse(responsibleUsersUseCase.replaceAdministrator(
                tenantContext, teamMapper.toResponsibleCommand(teamReference, request)));
    }

    @PutMapping("/{teamReference}/manager")
    @Operation(operationId = "replaceTeamManager", summary = "Replace a manager in an active or suspended team")
    public TeamResponse replaceManager(
            @PathVariable String teamReference, @Valid @RequestBody TeamResponsibleUserRequest request) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMapper.toResponse(responsibleUsersUseCase.replaceManager(
                tenantContext, teamMapper.toResponsibleCommand(teamReference, request)));
    }
}
