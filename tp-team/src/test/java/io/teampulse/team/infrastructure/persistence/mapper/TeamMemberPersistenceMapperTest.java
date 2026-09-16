package io.teampulse.team.infrastructure.persistence.mapper;

import io.teampulse.team.domain.team.model.TeamMember;
import io.teampulse.team.domain.team.model.TeamMemberStatus;
import io.teampulse.team.infrastructure.persistence.entity.TeamMemberEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TeamMemberPersistenceMapperTest {

    private static final Long MEMBER_ID = 2L;
    private static final Long TEAM_ID = 1L;
    private static final String ORGANIZATION_REFERENCE = "ORG-2026-0916-00000ZA7B900";
    private static final String USER_REFERENCE = "USR-2026-0916-00000ZA7B900";
    private static final Instant STARTED_AT = Instant.parse("2026-09-16T10:15:30Z");
    private static final Instant ENDED_AT = Instant.parse("2026-09-16T11:15:30Z");
    private static final Instant CREATED_AT = Instant.parse("2026-09-16T09:15:30Z");
    private static final Instant MODIFIED_AT = Instant.parse("2026-09-16T12:15:30Z");

    private final TeamMemberPersistenceMapper mapper = Mappers.getMapper(TeamMemberPersistenceMapper.class);

    @Test
    void mapsNewDomainMembershipToEntity() {
        TeamMember teamMember = TeamMember.add(ORGANIZATION_REFERENCE, TEAM_ID, USER_REFERENCE, STARTED_AT);

        TeamMemberEntity entity = mapper.toEntity(teamMember);

        assertNull(entity.getId());
        assertEquals(ORGANIZATION_REFERENCE, entity.getOrganizationReference());
        assertEquals(TEAM_ID, entity.getTeamId());
        assertEquals(USER_REFERENCE, entity.getUserReference());
        assertEquals(TeamMemberStatus.ACTIVE, entity.getStatus());
        assertEquals(STARTED_AT, entity.getStartedAt());
        assertNull(entity.getEndedAt());
    }

    @Test
    void restoresDomainMembershipFromEntity() {
        TeamMemberEntity entity = teamMemberEntity(TeamMemberStatus.REMOVED, STARTED_AT, ENDED_AT);

        TeamMember teamMember = mapper.toDomain(entity);

        assertEquals(MEMBER_ID, teamMember.getId());
        assertEquals(ORGANIZATION_REFERENCE, teamMember.getOrganizationReference());
        assertEquals(TEAM_ID, teamMember.getTeamId());
        assertEquals(USER_REFERENCE, teamMember.getUserReference());
        assertEquals(TeamMemberStatus.REMOVED, teamMember.getStatus());
        assertEquals(STARTED_AT, teamMember.getStartedAt());
        assertEquals(ENDED_AT, teamMember.getEndedAt());
    }

    @Test
    void updatesExistingEntityWithoutReplacingItsTechnicalState() {
        TeamMemberEntity entity = teamMemberEntity(TeamMemberStatus.INVITED, null, null);
        setTechnicalState(entity);
        TeamMember teamMember = TeamMember.restore(
                MEMBER_ID,
                ORGANIZATION_REFERENCE,
                TEAM_ID,
                USER_REFERENCE,
                TeamMemberStatus.REMOVED,
                STARTED_AT,
                ENDED_AT);

        TeamMemberEntity updatedEntity = entity;
        mapper.updateEntity(updatedEntity, teamMember);

        assertSame(entity, updatedEntity);
        assertEquals(MEMBER_ID, updatedEntity.getId());
        assertEquals(4L, updatedEntity.getVersion());
        assertEquals("creator", updatedEntity.getCreatedBy());
        assertEquals(CREATED_AT, updatedEntity.getCreatedAt());
        assertEquals("modifier", updatedEntity.getModifiedBy());
        assertEquals(MODIFIED_AT, updatedEntity.getModifiedAt());
        assertEquals(TeamMemberStatus.REMOVED, updatedEntity.getStatus());
        assertEquals(STARTED_AT, updatedEntity.getStartedAt());
        assertEquals(ENDED_AT, updatedEntity.getEndedAt());
    }

    private static TeamMemberEntity teamMemberEntity(TeamMemberStatus status, Instant startedAt, Instant endedAt) {
        TeamMemberEntity entity = new TeamMemberEntity()
                .setOrganizationReference(ORGANIZATION_REFERENCE)
                .setTeamId(TEAM_ID)
                .setUserReference(USER_REFERENCE)
                .setStatus(status)
                .setStartedAt(startedAt)
                .setEndedAt(endedAt);
        ReflectionTestUtils.setField(entity, "id", MEMBER_ID);
        return entity;
    }

    private static void setTechnicalState(TeamMemberEntity entity) {
        ReflectionTestUtils.setField(entity, "version", 4L);
        ReflectionTestUtils.setField(entity, "createdBy", "creator");
        ReflectionTestUtils.setField(entity, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(entity, "modifiedBy", "modifier");
        ReflectionTestUtils.setField(entity, "modifiedAt", MODIFIED_AT);
    }
}
