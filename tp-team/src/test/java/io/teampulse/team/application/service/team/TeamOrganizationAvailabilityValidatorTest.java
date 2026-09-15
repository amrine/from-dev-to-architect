package io.teampulse.team.application.service.team;

import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectory;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.team.domain.team.error.TeamErrorCode;
import io.teampulse.team.domain.team.error.TeamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamOrganizationAvailabilityValidatorTest {

    private static final String ORGANIZATION_REFERENCE = "ORG-2026-1309-00000ZA7B900";

    @Mock(mockMaker = MockMakers.PROXY)
    private OrganizationDirectory organizationDirectory;

    @InjectMocks
    private TeamOrganizationAvailabilityValidator validator;

    @ParameterizedTest
    @MethodSource("availabilityMappings")
    void mapsOrganizationAvailability(OrganizationAvailability availability, TeamErrorCode expectedErrorCode) {
        when(organizationDirectory.check(ORGANIZATION_REFERENCE)).thenReturn(availability);

        if (expectedErrorCode == null) {
            validator.validateAvailable(ORGANIZATION_REFERENCE);
        } else {
            TeamException exception =
                    assertThrows(TeamException.class, () -> validator.validateAvailable(ORGANIZATION_REFERENCE));

            assertEquals(expectedErrorCode, exception.getErrorCode());
        }

        verify(organizationDirectory).check(ORGANIZATION_REFERENCE);
    }

    @Test
    void preservesTheCauseWhenTheOrganizationDirectoryCannotBeReached() {
        OrganizationDirectoryException directoryException =
                new OrganizationDirectoryException(new IllegalStateException("Organization unavailable"));
        when(organizationDirectory.check(ORGANIZATION_REFERENCE)).thenThrow(directoryException);

        TeamException exception =
                assertThrows(TeamException.class, () -> validator.validateAvailable(ORGANIZATION_REFERENCE));

        assertEquals(TeamErrorCode.ORGANIZATION_DIRECTORY_UNAVAILABLE, exception.getErrorCode());
        assertSame(directoryException, exception.getCause());
    }

    private static Stream<Arguments> availabilityMappings() {
        return Stream.of(
                Arguments.of(OrganizationAvailability.AVAILABLE, null),
                Arguments.of(OrganizationAvailability.UNAVAILABLE, TeamErrorCode.ORGANIZATION_UNAVAILABLE),
                Arguments.of(OrganizationAvailability.NOT_FOUND, TeamErrorCode.ORGANIZATION_NOT_FOUND));
    }
}
