package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.team.AbstractTeamJpaIT;
import io.teampulse.team.application.port.in.team.TeamResponsibleCommand;
import io.teampulse.team.application.port.in.team.TeamResponsibleUsersUseCase;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamResponsibleUsersServiceIT extends AbstractTeamJpaIT {

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
    void cleanDatabaseAndConfigureDirectory() {
        inTransactionTemplate(() -> {
            jpaTeamMemberRepository.deleteAllInBatch();
            jpaTeamRepository.deleteAllInBatch();
        });
        when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.NOT_FOUND);
    }

    @Nested
    class ReplaceAdministrator {

        @Test
        void replacesAndPersistsAnAvailableAdministratorWithoutChangingTeamStatus() {
            teamRepository.create(activeTeam());
            when(userDirectory.check(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE))
                    .thenReturn(UserAvailability.AVAILABLE);

            Team updatedTeam = responsibleUsers.replaceAdministrator(tenantContext(), command(REPLACEMENT_REFERENCE));

            assertEquals(TeamStatus.ACTIVE, updatedTeam.getStatus());
            assertEquals(REPLACEMENT_REFERENCE, updatedTeam.getAdminReference());
            TeamEntity storedTeam = storedTeam();
            assertEquals(TeamStatus.ACTIVE, storedTeam.getStatus());
            assertEquals(REPLACEMENT_REFERENCE, storedTeam.getAdminReference());
            assertEquals(MANAGER_REFERENCE, storedTeam.getManagerReference());
            verify(userDirectory).check(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);
        }

        @Test
        void preservesTheAdministratorWhenTheDirectoryRejectsTheReplacement() {
            teamRepository.create(activeTeam());

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> responsibleUsers.replaceAdministrator(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertEquals(TeamErrorCode.ADMINISTRATOR_NOT_FOUND, exception.getErrorCode());
            assertEquals(ADMIN_REFERENCE, storedTeam().getAdminReference());
        }
    }

    @Nested
    class ReplaceManager {

        @Test
        void replacesAndPersistsAnAvailableManagerWithoutChangingTeamStatus() {
            teamRepository.create(activeTeam());
            when(userDirectory.check(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE))
                    .thenReturn(UserAvailability.AVAILABLE);

            Team updatedTeam = responsibleUsers.replaceManager(tenantContext(), command(REPLACEMENT_REFERENCE));

            assertEquals(TeamStatus.ACTIVE, updatedTeam.getStatus());
            assertEquals(REPLACEMENT_REFERENCE, updatedTeam.getManagerReference());
            TeamEntity storedTeam = storedTeam();
            assertEquals(TeamStatus.ACTIVE, storedTeam.getStatus());
            assertEquals(REPLACEMENT_REFERENCE, storedTeam.getManagerReference());
            assertEquals(ADMIN_REFERENCE, storedTeam.getAdminReference());
            verify(userDirectory).check(ORGANIZATION_REFERENCE, REPLACEMENT_REFERENCE);
        }

        @Test
        void preservesTheManagerWhenTheDirectoryRejectsTheReplacement() {
            teamRepository.create(activeTeam());

            TeamException exception = assertThrows(
                    TeamException.class,
                    () -> responsibleUsers.replaceManager(tenantContext(), command(REPLACEMENT_REFERENCE)));

            assertEquals(TeamErrorCode.MANAGER_NOT_FOUND, exception.getErrorCode());
            assertEquals(MANAGER_REFERENCE, storedTeam().getManagerReference());
        }
    }

    @Nested
    class TenantContextValidation {

        @ParameterizedTest
        @MethodSource("responsibleUserOperations")
        void rejectsANullTenantContextForEveryOperation(String operation) {
            assertThrows(ConstraintViolationException.class, () -> {
                switch (operation) {
                    case "administrator" -> responsibleUsers.replaceAdministrator(null, command(REPLACEMENT_REFERENCE));
                    case "manager" -> responsibleUsers.replaceManager(null, command(REPLACEMENT_REFERENCE));
                    default -> throw new IllegalArgumentException("Unknown operation: " + operation);
                }
            });

            verifyNoInteractions(userDirectory);
        }

        private static Stream<String> responsibleUserOperations() {
            return Stream.of("administrator", "manager");
        }
    }

    private TeamEntity storedTeam() {
        return jpaTeamRepository
                .findByOrganizationReferenceAndReference(ORGANIZATION_REFERENCE, TEAM_REFERENCE)
                .orElseThrow();
    }

    private static TenantContext tenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private static TeamResponsibleCommand command(String responsibleReference) {
        return new TeamResponsibleCommand(TEAM_REFERENCE, responsibleReference);
    }

    private static Team activeTeam() {
        return Team.create(
                TEAM_REFERENCE, ORGANIZATION_REFERENCE, "TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE);
    }
}
