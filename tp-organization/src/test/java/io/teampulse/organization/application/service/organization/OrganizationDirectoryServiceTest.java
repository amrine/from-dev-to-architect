package io.teampulse.organization.application.service.organization;

import io.teampulse.organization.api.organization.OrganizationAvailability;
import io.teampulse.organization.api.organization.OrganizationDirectoryException;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationDirectoryServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-1309-00000ZA7B900";

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationDirectoryService service;

    @ParameterizedTest
    @MethodSource("availabilityMappings")
    void mapsEveryOrganizationStatus(
        OrganizationStatus status,
        OrganizationAvailability expectedAvailability
    ) {
        // GIVEN
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
            .thenReturn(Optional.of(organizationWithStatus(status)));

        // WHEN
        OrganizationAvailability availability = service.check(
            ORGANIZATION_REFERENCE
        );

        // THEN
        assertEquals(expectedAvailability, availability);
        verify(organizationRepository).findByReference(
            ORGANIZATION_REFERENCE
        );
    }

    static Stream<Arguments> availabilityMappings() {
        return Stream.of(
            Arguments.of(
                OrganizationStatus.ACTIVE,
                OrganizationAvailability.AVAILABLE
            ),
            Arguments.of(
                OrganizationStatus.CREATING,
                OrganizationAvailability.UNAVAILABLE
            ),
            Arguments.of(
                OrganizationStatus.SUSPENDED,
                OrganizationAvailability.UNAVAILABLE
            ),
            Arguments.of(
                OrganizationStatus.ARCHIVED,
                OrganizationAvailability.UNAVAILABLE
            )
        );
    }

    @Test
    void returnsNotFoundWhenOrganizationIsAbsent() {
        // GIVEN
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
            .thenReturn(Optional.empty());

        // WHEN
        OrganizationAvailability availability = service.check(
            ORGANIZATION_REFERENCE
        );

        // THEN
        assertEquals(OrganizationAvailability.NOT_FOUND, availability);
        verify(organizationRepository).findByReference(
            ORGANIZATION_REFERENCE
        );
    }

    @Test
    void wrapsRepositoryRuntimeExceptionAndPreservesCause() {
        // GIVEN
        RuntimeException repositoryFailure =
            new IllegalStateException("Repository unavailable");
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
            .thenThrow(repositoryFailure);

        // WHEN
        OrganizationDirectoryException exception = assertThrows(
            OrganizationDirectoryException.class,
            () -> service.check(ORGANIZATION_REFERENCE)
        );

        // THEN
        assertEquals(
            "Unable to check organization availability",
            exception.getMessage()
        );
        assertSame(repositoryFailure, exception.getCause());
    }

    @Test
    void wrapsInternalOrganizationExceptionWithoutExposingIt() {
        // GIVEN
        OrganizationException internalFailure = new OrganizationException(
            OrganizationErrorCode.NOT_FOUND,
            "Internal organization failure"
        );
        when(organizationRepository.findByReference(ORGANIZATION_REFERENCE))
            .thenThrow(internalFailure);

        // WHEN
        OrganizationDirectoryException exception = assertThrows(
            OrganizationDirectoryException.class,
            () -> service.check(ORGANIZATION_REFERENCE)
        );

        // THEN
        assertSame(internalFailure, exception.getCause());
    }

    private static Organization organizationWithStatus(
        OrganizationStatus status
    ) {
        return switch (status) {
            case CREATING, ARCHIVED -> Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                null,
                null,
                status
            );
            case ACTIVE -> Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                "USR-2026-1309-00000ZA7B901",
                "USR-2026-1309-00000ZA7B902",
                status
            );
            case SUSPENDED -> Organization.restore(
                ORGANIZATION_REFERENCE,
                "Team Pulse",
                "UTC",
                "USR-2026-1309-00000ZA7B901",
                null,
                status
            );
        };
    }
}
