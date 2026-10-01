package io.teampulse.team.infrastructure.web.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.application.port.in.team.TeamMemberCommand;
import io.teampulse.team.application.port.in.team.TeamMembershipUseCase;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.application.port.in.team.TeamResponsibleUsersUseCase;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@Profile({"local", "development"})
@AllArgsConstructor
public class TeamController {

    private final TeamLifecycleUseCase teamLifecycleUseCase;
    private final TeamResponsibleUsersUseCase responsibleUsersUseCase;
    private final TeamMembershipUseCase membershipUseCase;
    private final TenantContextProvider tenantContextProvider;

    @PostMapping
    public ResponseEntity<TeamResponse> create(
        @Valid @RequestBody CreateTeamRequest request
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        Team team = teamLifecycleUseCase.create(
            tenantContext,
            new CreateTeamCommand(
                request.name(),
                request.administratorReference(),
                request.managerReference()
            )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response(team));
    }

    @PostMapping("/{teamReference}/suspension")
    public TeamResponse suspend(@PathVariable String teamReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(teamLifecycleUseCase.suspend(tenantContext, teamReference));
    }

    @PostMapping("/{teamReference}/reactivation")
    public TeamResponse reactivate(@PathVariable String teamReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(teamLifecycleUseCase.reactivate(tenantContext, teamReference));
    }

    @PostMapping("/{teamReference}/archive")
    public TeamResponse archive(@PathVariable String teamReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(teamLifecycleUseCase.archive(tenantContext, teamReference));
    }

    @PutMapping("/{teamReference}/administrator")
    public TeamResponse replaceAdministrator(
        @PathVariable String teamReference,
        @Valid @RequestBody TeamResponsibleUserRequest request
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(responsibleUsersUseCase.replaceAdministrator(
            tenantContext,
            responsibleCommand(teamReference, request)
        ));
    }

    @PutMapping("/{teamReference}/manager")
    public TeamResponse replaceManager(
        @PathVariable String teamReference,
        @Valid @RequestBody TeamResponsibleUserRequest request
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(responsibleUsersUseCase.replaceManager(
            tenantContext,
            responsibleCommand(teamReference, request)
        ));
    }

    @PostMapping("/{teamReference}/members")
    public ResponseEntity<TeamMemberResponse> addMember(
        @PathVariable String teamReference,
        @Valid @RequestBody TeamMemberRequest request
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        TeamMember member = membershipUseCase.addMember(
            tenantContext,
            memberCommand(teamReference, request.userReference())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response(member));
    }

    @PostMapping("/{teamReference}/invitations")
    public ResponseEntity<TeamMemberResponse> inviteMember(
        @PathVariable String teamReference,
        @Valid @RequestBody TeamMemberRequest request
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        TeamMember member = membershipUseCase.inviteMember(
            tenantContext,
            memberCommand(teamReference, request.userReference())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response(member));
    }

    @PostMapping("/{teamReference}/members/{userReference}/activation")
    public TeamMemberResponse activateMember(
        @PathVariable String teamReference,
        @PathVariable String userReference
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(membershipUseCase.activateMember(
            tenantContext,
            memberCommand(teamReference, userReference)
        ));
    }

    @PostMapping("/{teamReference}/members/{userReference}/suspension")
    public TeamMemberResponse suspendMember(
        @PathVariable String teamReference,
        @PathVariable String userReference
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(membershipUseCase.suspendMember(
            tenantContext,
            memberCommand(teamReference, userReference)
        ));
    }

    @PostMapping("/{teamReference}/members/{userReference}/reactivation")
    public TeamMemberResponse reactivateMember(
        @PathVariable String teamReference,
        @PathVariable String userReference
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(membershipUseCase.reactivateMember(
            tenantContext,
            memberCommand(teamReference, userReference)
        ));
    }

    @PostMapping("/{teamReference}/members/{userReference}/removal")
    public TeamMemberResponse removeMember(
        @PathVariable String teamReference,
        @PathVariable String userReference
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        return response(membershipUseCase.removeMember(
            tenantContext,
            memberCommand(teamReference, userReference)
        ));
    }

    private static TeamResponsibleCommand responsibleCommand(
        String teamReference,
        TeamResponsibleUserRequest request
    ) {
        return new TeamResponsibleCommand(teamReference, request.userReference());
    }

    private static TeamMemberCommand memberCommand(String teamReference, String userReference) {
        return new TeamMemberCommand(teamReference, userReference);
    }

    private static TeamResponse response(Team team) {
        return new TeamResponse(
            team.getReference(),
            team.getName(),
            team.getStatus().name(),
            team.getAdminReference(),
            team.getManagerReference()
        );
    }

    private static TeamMemberResponse response(TeamMember member) {
        return new TeamMemberResponse(
            member.getUserReference(),
            member.getStatus().name(),
            member.getStartedAt(),
            member.getEndedAt()
        );
    }
}
