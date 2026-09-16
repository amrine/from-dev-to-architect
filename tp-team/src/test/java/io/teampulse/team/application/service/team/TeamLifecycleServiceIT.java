package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFormat;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.team.AbstractIntegrationTest;
import io.teampulse.team.application.port.in.team.CreateTeamCommand;
import io.teampulse.team.application.port.in.team.TeamLifecycleUseCase;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import io.teampulse.testsupport.transaction.TransactionManagerProbe;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TeamLifecycleServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";

    @Inject
    private TeamLifecycleUseCase teamLifecycle;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @MockitoBean
    private OrganizationDirectory organizationDirectory;

    @MockitoBean
    private UserDirectory userDirectory;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(() -> {
            jpaTeamMemberRepository.deleteAllInBatch();
            jpaTeamRepository.deleteAllInBatch();
        });
        when(organizationDirectory.check(anyString())).thenReturn(OrganizationAvailability.AVAILABLE);
        when(userDirectory.check(anyString(), anyString())).thenReturn(UserAvailability.AVAILABLE);
    }

    @Test
    void createsAPersistedTeamInsideAnActiveReadWriteTransaction() {
        Team team = teamLifecycle.create(tenantContext(), command(" TeamPulse Engineering "));
        TransactionManagerProbe.TransactionObservation observation = transactionProbe.observation();

        assertTrue(ReferenceFormat.matches(team.getReference(), "TEM"));
        assertEquals("TeamPulse Engineering", team.getName());
        assertEquals(TeamStatus.ACTIVE, team.getStatus());
        TeamEntity storedEntity = jpaTeamRepository
                .findByOrganizationReferenceAndReference(ORGANIZATION_REFERENCE, team.getReference())
                .orElseThrow();
        assertEquals(team.getId(), storedEntity.getId());
        assertEquals(ADMIN_REFERENCE, storedEntity.getAdminReference());
        assertEquals(MANAGER_REFERENCE, storedEntity.getManagerReference());
        verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
        verify(userDirectory).check(ORGANIZATION_REFERENCE, ADMIN_REFERENCE);
        verify(userDirectory).check(ORGANIZATION_REFERENCE, MANAGER_REFERENCE);

        assertTrue(observation.name().endsWith("TeamLifecycleService.create"));
        assertFalse(observation.readOnly());
        assertTrue(observation.committed());
        assertFalse(observation.rolledBack());
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void rejectsInvalidCommandsBeforeCallingDirectoriesOrPersisting(
            TenantContext tenantContext, CreateTeamCommand command) {
        assertThrows(ConstraintViolationException.class, () -> teamLifecycle.create(tenantContext, command));

        assertEquals(0L, jpaTeamRepository.count());
        verifyNoInteractions(organizationDirectory, userDirectory);
    }

    @Test
    void rejectsAnUnavailableOrganizationWithoutPersisting() {
        when(organizationDirectory.check(ORGANIZATION_REFERENCE)).thenReturn(OrganizationAvailability.UNAVAILABLE);

        TeamException exception = assertThrows(
                TeamException.class, () -> teamLifecycle.create(tenantContext(), command("TeamPulse Engineering")));

        assertEquals(TeamErrorCode.ORGANIZATION_UNAVAILABLE, exception.getErrorCode());
        assertEquals(0L, jpaTeamRepository.count());
        verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
        verifyNoInteractions(userDirectory);
    }

    private static Stream<Arguments> invalidCommands() {
        return Stream.of(
                Arguments.of(null, command("TeamPulse Engineering")),
                Arguments.of(tenantContext(), null),
                Arguments.of(tenantContext(), command(" ")));
    }

    private static TenantContext tenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private static CreateTeamCommand command(String name) {
        return new CreateTeamCommand(name, ADMIN_REFERENCE, MANAGER_REFERENCE);
    }
}
