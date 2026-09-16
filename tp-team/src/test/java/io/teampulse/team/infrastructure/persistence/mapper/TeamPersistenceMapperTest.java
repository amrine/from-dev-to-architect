package io.teampulse.team.infrastructure.persistence.mapper;

import io.teampulse.team.domain.team.model.Team;
import io.teampulse.team.domain.team.model.TeamStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TeamPersistenceMapperTest {

    private static final Long TEAM_ID = 1L;
    private static final String TEAM_REFERENCE = "TEM-2026-0916-00000ZA7B900";
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B900";
    private static final String ADMIN_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final String MANAGER_REFERENCE = "USR-2026-0916-00000ZA7B901";
    private static final Instant CREATED_AT = Instant.parse("2026-09-16T10:15:30Z");
    private static final Instant MODIFIED_AT = Instant.parse("2026-09-16T11:15:30Z");

    private final TeamPersistenceMapper mapper = Mappers.getMapper(TeamPersistenceMapper.class);

    @Test
    void mapsNewDomainTeamToEntity() {
        Team team = Team.create(
                TEAM_REFERENCE, ORGANIZATION_REFERENCE, " TeamPulse Engineering ", ADMIN_REFERENCE, MANAGER_REFERENCE);

        TeamEntity entity = mapper.toEntity(team);

        assertNull(entity.getId());
        assertEquals(TEAM_REFERENCE, entity.getReference());
        assertEquals(ORGANIZATION_REFERENCE, entity.getOrganizationReference());
        assertEquals("TeamPulse Engineering", entity.getName());
        assertEquals(ADMIN_REFERENCE, entity.getAdminReference());
        assertEquals(MANAGER_REFERENCE, entity.getManagerReference());
        assertEquals(TeamStatus.ACTIVE, entity.getStatus());
    }

    @Test
    void restoresDomainTeamFromEntity() {
        TeamEntity entity =
                teamEntity("TeamPulse Engineering", ADMIN_REFERENCE, MANAGER_REFERENCE, TeamStatus.SUSPENDED);

        Team team = mapper.toDomain(entity);

        assertEquals(TEAM_ID, team.getId());
        assertEquals(TEAM_REFERENCE, team.getReference());
        assertEquals(ORGANIZATION_REFERENCE, team.getOrganizationReference());
        assertEquals("TeamPulse Engineering", team.getName());
        assertEquals(ADMIN_REFERENCE, team.getAdminReference());
        assertEquals(MANAGER_REFERENCE, team.getManagerReference());
        assertEquals(TeamStatus.SUSPENDED, team.getStatus());
    }

    @Test
    void updatesExistingEntityWithoutReplacingItsTechnicalState() {
        TeamEntity entity = teamEntity("Previous name", ADMIN_REFERENCE, MANAGER_REFERENCE, TeamStatus.ACTIVE);
        setTechnicalState(entity);
        Team team = Team.restore(
                TEAM_ID,
                TEAM_REFERENCE,
                ORGANIZATION_REFERENCE,
                "TeamPulse Engineering",
                MANAGER_REFERENCE,
                ADMIN_REFERENCE,
                TeamStatus.SUSPENDED);

        TeamEntity updatedEntity = entity;
        mapper.updateEntity(updatedEntity, team);

        assertSame(entity, updatedEntity);
        assertEquals(TEAM_ID, updatedEntity.getId());
        assertEquals(3L, updatedEntity.getVersion());
        assertEquals("creator", updatedEntity.getCreatedBy());
        assertEquals(CREATED_AT, updatedEntity.getCreatedAt());
        assertEquals("modifier", updatedEntity.getModifiedBy());
        assertEquals(MODIFIED_AT, updatedEntity.getModifiedAt());
        assertEquals("TeamPulse Engineering", updatedEntity.getName());
        assertEquals(MANAGER_REFERENCE, updatedEntity.getAdminReference());
        assertEquals(ADMIN_REFERENCE, updatedEntity.getManagerReference());
        assertEquals(TeamStatus.SUSPENDED, updatedEntity.getStatus());
    }

    private static TeamEntity teamEntity(
            String name, String adminReference, String managerReference, TeamStatus status) {
        TeamEntity entity = new TeamEntity()
                .setReference(TEAM_REFERENCE)
                .setOrganizationReference(ORGANIZATION_REFERENCE)
                .setName(name)
                .setAdminReference(adminReference)
                .setManagerReference(managerReference)
                .setStatus(status);
        ReflectionTestUtils.setField(entity, "id", TEAM_ID);
        return entity;
    }

    private static void setTechnicalState(TeamEntity entity) {
        ReflectionTestUtils.setField(entity, "version", 3L);
        ReflectionTestUtils.setField(entity, "createdBy", "creator");
        ReflectionTestUtils.setField(entity, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(entity, "modifiedBy", "modifier");
        ReflectionTestUtils.setField(entity, "modifiedAt", MODIFIED_AT);
    }
}
