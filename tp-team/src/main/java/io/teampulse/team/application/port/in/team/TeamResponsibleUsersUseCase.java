package io.teampulse.team.application.port.in.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.domain.team.model.Team;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface TeamResponsibleUsersUseCase {

    Team replaceAdministrator(@NotNull TenantContext tenantContext, @NotNull @Valid TeamResponsibleCommand command);

    Team replaceManager(@NotNull TenantContext tenantContext, @NotNull @Valid TeamResponsibleCommand command);
}
