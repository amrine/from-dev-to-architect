package io.teampulse.team.application.port.in.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.domain.team.model.Team;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface TeamLifecycleUseCase {

    Team create(@NotNull TenantContext tenantContext, @NotNull @Valid CreateTeamCommand command);

    Team suspend(@NotNull TenantContext tenantContext, @NotBlank String teamReference);

    Team reactivate(@NotNull TenantContext tenantContext, @NotBlank String teamReference);

    Team archive(@NotNull TenantContext tenantContext, @NotBlank String teamReference);
}
