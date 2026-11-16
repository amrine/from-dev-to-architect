package io.teampulse.team.application.port.in;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.application.port.in.team.TeamResponsibleUsersUseCase;
import io.teampulse.team.application.port.in.teammember.TeamMemberCommand;
import io.teampulse.team.application.port.in.teammember.TeamMembershipUseCase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TenantedInputPortsArchitectureTest {

    @Test
    void portsStartWithTenantContext() {
        Set<Class<?>> ports = Set.of(
            TeamLifecycleUseCase.class,
            TeamMembershipUseCase.class,
            TeamResponsibleUsersUseCase.class
        );

        ports.forEach(port -> Arrays.stream(port.getDeclaredMethods()).forEach(method ->
            assertEquals(TenantContext.class, method.getParameterTypes()[0],
                port.getSimpleName() + "." + method.getName())));
    }

    @Test
    void commandsDoNotCarryOrganizationReference() {
        Set<Class<?>> commands = Set.of(
            CreateTeamCommand.class,
            TeamMemberCommand.class,
            TeamResponsibleCommand.class
        );

        commands.forEach(command -> assertFalse(
            Arrays.stream(command.getRecordComponents())
                .anyMatch(component -> component.getName().equals("organizationReference")),
            command.getSimpleName() + " must not carry organizationReference"));
    }
}
