package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.team.AbstractIntegrationTest;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.application.port.in.team.TeamResponsibleUsersUseCase;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamResponsibleUsersServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B900";
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";
    private static final String REPLACEMENT_REFERENCE = "USR-2026-0916-00000ZA7B902";

    @Inject
    private TeamResponsibleUsersUseCase responsibleUsers;

    @Inject
    private TeamRepository teamRepository;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @MockitoBean
    private UserDirectory userDirectory;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(() -> {
            jpaTeamMemberRepository.deleteAllInBatch();
            jpaTeamRepository.deleteAllInBatch();
        });
        when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.NOT_FOUND);
    }

    @Test
    void replacesAnAvailableManagerAndPersistsTheTeamWithoutChangingItsStatus() {
        teamRepository.create(activeTeam());
        when(userDirectory.check(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE)).thenReturn(UserAvailability.AVAILABLE);

        Team updatedTeam = responsibleUsers.replaceManager(
                new TenantContext(ORGANIZATION_REFERENCE),
                new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE));

        assertEquals(TeamStatus.ACTIVE, updatedTeam.getStatus());
        assertEquals(REPLACEMENT_REFERENCE, updatedTeam.getManagerReference());
        TeamEntity storedEntity = jpaTeamRepository
                .findByOrganizationReferenceAndReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE)
                .orElseThrow();
        assertEquals(TeamStatus.ACTIVE, storedEntity.getStatus());
        assertEquals(REPLACEMENT_REFERENCE, storedEntity.getManagerReference());
        verify(userDirectory).check(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);
    }

    @ParameterizedTest
    @MethodSource("tenantRequiredOperations")
    void rejectsNullTenantContextForEveryResponsibleUserOperation(String operation) {
        assertThrows(ConstraintViolationException.class, () -> {
            switch (operation) {
                case "administrator" -> responsibleUsers.replaceAdministrator(
                        null, new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE));
                case "manager" -> responsibleUsers.replaceManager(
                        null, new TeamResponsibleCommand(TEAM_REFERENCE, REPLACEMENT_REFERENCE));
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            }
        });

        verifyNoInteractions(userDirectory);
    }

    private static Team activeTeam() {
        return Team.create(
                TEAM_REFERENCE, ORGANIZATION_REFERENCE, "TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE);
    }

    private static Stream<String> tenantRequiredOperations() {
        return Stream.of("administrator", "manager");
    }
}
