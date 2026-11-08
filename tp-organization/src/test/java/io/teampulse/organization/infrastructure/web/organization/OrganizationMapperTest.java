package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.organization.api.lifecycle.OrganizationLifecycleResult;
import io.teampulse.organization.application.port.in.organization.OrganizationResponsibleCommand;
import io.teampulse.organization.domain.organization.model.Organization;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrganizationMapperTest {

    private final OrganizationMapper mapper = Mappers.getMapper(OrganizationMapper.class);

    @Test
    void combinesRouteAndResponsiblePayload() {
        assertEquals(new OrganizationResponsibleCommand("organization", "user"),
            mapper.toCommand("organization", new AssignOrganizationResponsibleUserRequest("user")));
    }

    @Test
    void mapsPartialSourcesAccordingToTheMappingContract() {
        assertEquals(new OrganizationResponsibleCommand("organization", null), mapper.toCommand("organization", null));
        assertEquals(new OrganizationResponsibleCommand(null, "user"),
            mapper.toCommand(null, new AssignOrganizationResponsibleUserRequest("user")));
    }

    @Test
    void returnsNullForAbsentSources() {
        assertNull(mapper.toCommand(null, null));
        assertNull(mapper.toResponse((Organization) null));
        assertNull(mapper.toResponse((OrganizationLifecycleResult) null));
    }
}
