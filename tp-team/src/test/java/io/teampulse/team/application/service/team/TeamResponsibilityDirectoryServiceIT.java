package io.teampulse.team.application.service.team;

import io.teampulse.common.context.TenantContext;
import io.teampulse.team.AbstractIntegrationTest;
import io.teampulse.team.api.TeamLifecycleState;
import io.teampulse.team.api.TeamResponsibilityDirectory;
import io.teampulse.team.api.TeamResponsibilitySnapshot;
import io.teampulse.team.application.port.out.team.TeamMemberRepository;
import io.teampulse.team.application.port.out.team.TeamRepository;
import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamMemberRepository;
import io.teampulse.team.infrastructure.persistence.repository.JpaTeamRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamResponsibilityDirectoryServiceIT extends AbstractIntegrationTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B950";
    private static final String OTHER_ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B951";
    private static final String USER_REFERENCE = "USR-2026-0916-00000ZA7B950";
    private static final String OTHER_USER_REFERENCE = "USR-2026-0916-00000ZA7B951";
    private static final Instant STARTED_AT = Instant.parse("2026-09-16T08:30:00Z");

    @Inject
    private TeamResponsibilityDirectory responsibilityDirectory;

    @Inject
    private TeamRepository teamRepository;

    @Inject
    private TeamMemberRepository teamMemberRepository;

    @Inject
    private JpaTeamRepository jpaTeamRepository;

    @Inject
    private JpaTeamMemberRepository jpaTeamMemberRepository;

    @BeforeEach
    void cleanDatabase() {
        inTransactionTemplate(() -> {
            jpaTeamMemberRepository.deleteAllInBatch();
            jpaTeamRepository.deleteAllInBatch();
        });
    }

    @Test
    void returnsTenantScopedTeamResponsibilitiesAndExcludesMemberships() {
        Team active = teamRepository.create(team(
            "TEM-2026-0916-00000ZA7B950",
            ORGANIZATION_REFERENCE,
            USER_REFERENCE,
            USER_REFERENCE
        ));

        Team suspended = teamRepository.create(team(
            "TEM-2026-0916-00000ZA7B951",
            ORGANIZATION_REFERENCE,
            USER_REFERENCE,
            OTHER_USER_REFERENCE
        ));
        suspended.suspend();
        teamRepository.update(suspended);

        Team archived = teamRepository.create(team(
            "TEM-2026-0916-00000ZA7B952",
            ORGANIZATION_REFERENCE,
            OTHER_USER_REFERENCE,
            USER_REFERENCE
        ));
        archived.archive();
        teamRepository.update(archived);

        teamRepository.create(team(
            "TEM-2026-0916-00000ZA7B953",
            OTHER_ORGANIZATION_REFERENCE,
            USER_REFERENCE,
            OTHER_USER_REFERENCE
        ));

        Team membershipOnly = teamRepository.create(team(
            "TEM-2026-0916-00000ZA7B954",
            ORGANIZATION_REFERENCE,
            OTHER_USER_REFERENCE,
            OTHER_USER_REFERENCE
        ));
        teamMemberRepository.create(TeamMember.add(
            ORGANIZATION_REFERENCE,
            membershipOnly.getId(),
            USER_REFERENCE,
            STARTED_AT
        ));

        List<TeamResponsibilitySnapshot> responsibilities =
            responsibilityDirectory.findByResponsibleUser(
                new TenantContext(ORGANIZATION_REFERENCE),
                USER_REFERENCE
            );

        assertEquals(List.of(
            new TeamResponsibilitySnapshot(
                "TEM-2026-0916-00000ZA7B950",
                TeamLifecycleState.ACTIVE
            ),
            new TeamResponsibilitySnapshot(
                "TEM-2026-0916-00000ZA7B951",
                TeamLifecycleState.SUSPENDED
            ),
            new TeamResponsibilitySnapshot(
                "TEM-2026-0916-00000ZA7B952",
                TeamLifecycleState.ARCHIVED
            )
        ), responsibilities);
    }

    private static Team team(
        String teamReference,
        String organizationReference,
        String administratorReference,
        String managerReference
    ) {
        return Team.create(
            teamReference,
            organizationReference,
            "Team " + teamReference,
            administratorReference,
            managerReference
        );
    }
}
