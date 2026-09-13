package io.teampulse.organization.infrastructure.persistence.mapper;

import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.time.ZoneId;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrganizationPersistenceMapperTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0908-00000ZA7B900";

    private static final String ADMIN_REFERENCE =
        "USR-2026-0908-00000ZA7B900";

    private static final String MANAGER_REFERENCE =
        "USR-2026-0908-00000ZA7B901";

    private static final String TIMEZONE = "Europe/Paris";

    private final OrganizationPersistenceMapper mapper =
        Mappers.getMapper(OrganizationPersistenceMapper.class);

    @Test
    void mapsDomainOrganizationToEntity() {
        Organization organization = organization(
            ADMIN_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.ACTIVE
        );

        OrganizationEntity entity = mapper.toEntity(organization);

        assertEquals(ORGANIZATION_REFERENCE, entity.getReference());
        assertEquals("TeamPulse", entity.getName());
        assertEquals(TIMEZONE, entity.getTimezone());
        assertEquals(ADMIN_REFERENCE, entity.getAdminReference());
        assertEquals(MANAGER_REFERENCE, entity.getManagerReference());
        assertEquals(OrganizationStatus.ACTIVE, entity.getStatus());
    }

    @Test
    void restoresDomainOrganizationFromEntity() {
        OrganizationEntity entity = new OrganizationEntity()
            .setReference(ORGANIZATION_REFERENCE)
            .setName("TeamPulse")
            .setTimezone(TIMEZONE)
            .setAdminReference(ADMIN_REFERENCE)
            .setManagerReference(null)
            .setStatus(OrganizationStatus.SUSPENDED);

        Organization organization = mapper.toDomain(entity);

        assertEquals(ORGANIZATION_REFERENCE, organization.getReference());
        assertEquals("TeamPulse", organization.getName());
        assertEquals(ZoneId.of(TIMEZONE), organization.getTimezone());
        assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
        assertNull(organization.getManagerReference());
        assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
    }

    @ParameterizedTest
    @MethodSource("organizationsWithOptionalResponsibleUsers")
    void propagatesAbsentResponsibleUsers(
        String adminReference,
        String managerReference,
        OrganizationStatus status
    ) {
        Organization organization = organization(
            adminReference,
            managerReference,
            status
        );

        OrganizationEntity entity = mapper.toEntity(organization);
        Organization restoredOrganization = mapper.toDomain(entity);

        assertEquals(adminReference, entity.getAdminReference());
        assertEquals(managerReference, entity.getManagerReference());
        assertEquals(status, entity.getStatus());
        assertEquals(
            adminReference,
            restoredOrganization.getAdminReference()
        );
        assertEquals(
            managerReference,
            restoredOrganization.getManagerReference()
        );
        assertEquals(status, restoredOrganization.getStatus());
    }

    @Test
    void clearsAbsentManagerWhenUpdatingEntity() {
        OrganizationEntity entity = new OrganizationEntity()
            .setReference(ORGANIZATION_REFERENCE)
            .setName("Previous name")
            .setTimezone("UTC")
            .setAdminReference(ADMIN_REFERENCE)
            .setManagerReference(MANAGER_REFERENCE)
            .setStatus(OrganizationStatus.ACTIVE);
        Organization organization = organization(
            ADMIN_REFERENCE,
            null,
            OrganizationStatus.SUSPENDED
        );

        mapper.updateEntity(entity, organization);

        assertEquals(ORGANIZATION_REFERENCE, entity.getReference());
        assertEquals("TeamPulse", entity.getName());
        assertEquals(TIMEZONE, entity.getTimezone());
        assertEquals(ADMIN_REFERENCE, entity.getAdminReference());
        assertNull(entity.getManagerReference());
        assertEquals(OrganizationStatus.SUSPENDED, entity.getStatus());
    }

    private static Stream<Arguments> organizationsWithOptionalResponsibleUsers() {
        return Stream.of(
            Arguments.of(null, null, OrganizationStatus.CREATING),
            Arguments.of(
                ADMIN_REFERENCE,
                null,
                OrganizationStatus.SUSPENDED
            ),
            Arguments.of(
                null,
                MANAGER_REFERENCE,
                OrganizationStatus.ARCHIVED
            )
        );
    }

    private static Organization organization(
        String adminReference,
        String managerReference,
        OrganizationStatus status
    ) {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            TIMEZONE,
            adminReference,
            managerReference,
            status
        );
    }
}
