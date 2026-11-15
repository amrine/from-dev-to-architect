package io.teampulse.team.infrastructure.web.teammember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.team.application.port.in.teammember.TeamMembershipUseCase;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Teams", description = "Tenant-scoped teams and memberships.")
@RequestMapping("/api/teams/{teamReference}")
@AllArgsConstructor
public class TeamMemberController {

    private final TeamMembershipUseCase membershipUseCase;
    private final TenantContextProvider tenantContextProvider;
    private final TeamMemberMapper teamMemberMapper;

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "addTeamMember", summary = "Add an available user as an active team member")
    public TeamMemberResponse addMember(
            @PathVariable String teamReference, @Valid @RequestBody TeamMemberRequest request) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMemberMapper.toResponse(
                membershipUseCase.addMember(tenantContext, teamMemberMapper.toCommand(teamReference, request)));
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "inviteTeamMember",
            summary = "Invite an available or pending user to the team without activating the identity account")
    public TeamMemberResponse inviteMember(
            @PathVariable String teamReference, @Valid @RequestBody TeamMemberRequest request) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMemberMapper.toResponse(
                membershipUseCase.inviteMember(tenantContext, teamMemberMapper.toCommand(teamReference, request)));
    }

    @PostMapping("/members/{userReference}/activation")
    @Operation(operationId = "activateTeamMember", summary = "Activate an invited membership for an available user")
    public TeamMemberResponse activateMember(@PathVariable String teamReference, @PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMemberMapper.toResponse(membershipUseCase.activateMember(
                tenantContext, teamMemberMapper.toCommand(teamReference, userReference)));
    }

    @PostMapping("/members/{userReference}/suspension")
    @Operation(operationId = "suspendTeamMember", summary = "Suspend an active team membership")
    public TeamMemberResponse suspendMember(@PathVariable String teamReference, @PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMemberMapper.toResponse(membershipUseCase.suspendMember(
                tenantContext, teamMemberMapper.toCommand(teamReference, userReference)));
    }

    @PostMapping("/members/{userReference}/reactivation")
    @Operation(
            operationId = "reactivateTeamMember",
            summary = "Reactivate a suspended membership for an available user")
    public TeamMemberResponse reactivateMember(@PathVariable String teamReference, @PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMemberMapper.toResponse(membershipUseCase.reactivateMember(
                tenantContext, teamMemberMapper.toCommand(teamReference, userReference)));
    }

    @PostMapping("/members/{userReference}/removal")
    @Operation(operationId = "removeTeamMember", summary = "End a team membership while retaining its history")
    public TeamMemberResponse removeMember(@PathVariable String teamReference, @PathVariable String userReference) {
        TenantContext tenantContext = tenantContextProvider.current();
        return teamMemberMapper.toResponse(membershipUseCase.removeMember(
                tenantContext, teamMemberMapper.toCommand(teamReference, userReference)));
    }
}
