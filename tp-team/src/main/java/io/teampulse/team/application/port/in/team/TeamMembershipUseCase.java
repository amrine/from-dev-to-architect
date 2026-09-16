package io.teampulse.team.application.port.in.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.domain.team.model.TeamMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface TeamMembershipUseCase {

    TeamMember addMember(@NotNull TenantContext tenantContext, @NotNull @Valid TeamMemberCommand command);

    TeamMember inviteMember(@NotNull TenantContext tenantContext, @NotNull @Valid TeamMemberCommand command);

    TeamMember activateMember(@NotNull TenantContext tenantContext, @NotNull @Valid TeamMemberCommand command);

    TeamMember suspendMember(@NotNull TenantContext tenantContext, @NotNull @Valid TeamMemberCommand command);

    TeamMember reactivateMember(@NotNull TenantContext tenantContext, @NotNull @Valid TeamMemberCommand command);

    TeamMember removeMember(@NotNull TenantContext tenantContext, @NotNull @Valid TeamMemberCommand command);
}
