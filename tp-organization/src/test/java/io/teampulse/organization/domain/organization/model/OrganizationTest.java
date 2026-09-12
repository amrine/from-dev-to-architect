package io.teampulse.organization.domain.organization.model;

import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-0908-00000ZA7B900";

    private static final String ADMIN_REFERENCE =
        "USR-2026-0908-00000ZA7B900";

    private static final String MANAGER_REFERENCE =
        "USR-2026-0908-00000ZA7B901";

    private static final String TIMEZONE = "Europe/Paris";

    @Nested
    class CreationTests {

        @Test
        void createsOrganizationInCreatingStatusWithoutResponsibleUsers() {
            Organization organization = createOrganization("TeamPulse");

            assertEquals(ORGANIZATION_REFERENCE, organization.getReference());
            assertEquals("TeamPulse", organization.getName());
            assertEquals(ZoneId.of(TIMEZONE), organization.getTimezone());
            assertNull(organization.getAdminReference());
            assertNull(organization.getManagerReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @Test
        void stripsSurroundingWhitespaceFromName() {
            Organization organization = createOrganization("  TeamPulse  ");

            assertEquals("TeamPulse", organization.getName());
        }

        @Test
        void preservesInternalWhitespaceAndCaseInName() {
            Organization organization = createOrganization("Team  Pulse France");

            assertEquals("Team  Pulse France", organization.getName());
        }

        @Test
        void acceptsNameAtMaximumLengthAfterNormalization() {
            String name = "A".repeat(200);

            Organization organization = createOrganization("  " + name + "  ");

            assertEquals(name, organization.getName());
        }
    }

    @Nested
    class ValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            "ORG-2026-0908-00000ZA7B90",
            "ORG-2026-0908-00000ZA7B9000",
            "org-2026-0908-00000ZA7B900",
            "USR-2026-0908-00000ZA7B900",
            " ORG-2026-0908-00000ZA7B900"
        })
        void rejectsInvalidOrganizationReference(String reference) {
            assertThrows(
                IllegalArgumentException.class,
                () -> Organization.create(reference, "TeamPulse", TIMEZONE)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void rejectsNullEmptyOrBlankName(String name) {
            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> createOrganization(name)
            );

            assertEquals(OrganizationErrorCode.INVALID_NAME, exception.getErrorCode());
        }

        @Test
        void rejectsNameLongerThanMaximumLengthAfterNormalization() {
            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> createOrganization("  " + "A".repeat(201) + "  ")
            );

            assertEquals(OrganizationErrorCode.INVALID_NAME, exception.getErrorCode());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {
            " ",
            "\t",
            "Unknown/Timezone",
            " Europe/Paris "
        })
        void rejectsInvalidTimezone(String timezone) {
            OrganizationException exception = assertThrows(
                OrganizationException.class,
                () -> Organization.create(
                    ORGANIZATION_REFERENCE,
                    "TeamPulse",
                    timezone
                )
            );

            assertEquals(
                OrganizationErrorCode.INVALID_TIMEZONE,
                exception.getErrorCode()
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "",
            " ",
            "USR-2026-0908-00000ZA7B90",
            "USR-2026-0908-00000ZA7B9000",
            "usr-2026-0908-00000ZA7B900",
            "ORG-2026-0908-00000ZA7B900",
            " USR-2026-0908-00000ZA7B900"
        })
        void rejectsInvalidAdministratorReference(String adminReference) {
            assertThrows(
                IllegalArgumentException.class,
                () -> Organization.restore(
                    ORGANIZATION_REFERENCE,
                    "TeamPulse",
                    TIMEZONE,
                    adminReference,
                    null,
                    OrganizationStatus.CREATING
                )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "",
            " ",
            "USR-2026-0908-00000ZA7B90",
            "USR-2026-0908-00000ZA7B9000",
            "usr-2026-0908-00000ZA7B900",
            "ORG-2026-0908-00000ZA7B900",
            "USR-2026-0908-00000ZA7B900 "
        })
        void rejectsInvalidManagerReference(String managerReference) {
            assertThrows(
                IllegalArgumentException.class,
                () -> Organization.restore(
                    ORGANIZATION_REFERENCE,
                    "TeamPulse",
                    TIMEZONE,
                    null,
                    managerReference,
                    OrganizationStatus.CREATING
                )
            );
        }

        @Test
        void rejectsNullRestoredStatus() {
            assertThrows(
                NullPointerException.class,
                () -> Organization.restore(
                    ORGANIZATION_REFERENCE,
                    "TeamPulse",
                    TIMEZONE,
                    null,
                    null,
                    null
                )
            );
        }
    }

    @Nested
    class RestorationTests {

        private static final List<RestoredState> VALID_STATES = List.of(
            new RestoredState(null, null, OrganizationStatus.CREATING),
            new RestoredState(ADMIN_REFERENCE, null, OrganizationStatus.CREATING),
            new RestoredState(null, MANAGER_REFERENCE, OrganizationStatus.CREATING),
            new RestoredState(ADMIN_REFERENCE, MANAGER_REFERENCE, OrganizationStatus.CREATING),
            new RestoredState(ADMIN_REFERENCE, MANAGER_REFERENCE, OrganizationStatus.ACTIVE),
            new RestoredState(ADMIN_REFERENCE, null, OrganizationStatus.SUSPENDED),
            new RestoredState(ADMIN_REFERENCE, MANAGER_REFERENCE, OrganizationStatus.SUSPENDED),
            new RestoredState(null, null, OrganizationStatus.ARCHIVED),
            new RestoredState(ADMIN_REFERENCE, null, OrganizationStatus.ARCHIVED),
            new RestoredState(null, MANAGER_REFERENCE, OrganizationStatus.ARCHIVED),
            new RestoredState(ADMIN_REFERENCE, MANAGER_REFERENCE, OrganizationStatus.ARCHIVED)
        );

        private static final List<RestoredState> INVALID_STATES = List.of(
            new RestoredState(null, null, OrganizationStatus.ACTIVE),
            new RestoredState(ADMIN_REFERENCE, null, OrganizationStatus.ACTIVE),
            new RestoredState(null, MANAGER_REFERENCE, OrganizationStatus.ACTIVE),
            new RestoredState(null, null, OrganizationStatus.SUSPENDED),
            new RestoredState(null, MANAGER_REFERENCE, OrganizationStatus.SUSPENDED)
        );

        @Test
        void restoresEveryStructurallyValidState() {
            for (RestoredState state : VALID_STATES) {
                Organization organization = assertDoesNotThrow(
                    () -> restoreOrganization(state),
                    state.toString()
                );

                assertEquals(state.adminReference(), organization.getAdminReference());
                assertEquals(state.managerReference(), organization.getManagerReference());
                assertEquals(state.status(), organization.getStatus());
            }
        }

        @Test
        void rejectsEveryStructurallyInvalidState() {
            for (RestoredState state : INVALID_STATES) {
                OrganizationException exception = assertThrows(
                    OrganizationException.class,
                    () -> restoreOrganization(state),
                    state.toString()
                );

                assertEquals(
                    OrganizationErrorCode.ACTIVATION_REQUIREMENTS_NOT_MET,
                    exception.getErrorCode()
                );
            }
        }

        @Test
        void restoresPersistedBusinessStateWithoutCreationWorkflow() {
            Organization organization = restoreOrganization(
                new RestoredState(
                    ADMIN_REFERENCE,
                    MANAGER_REFERENCE,
                    OrganizationStatus.ACTIVE
                )
            );

            assertEquals(ORGANIZATION_REFERENCE, organization.getReference());
            assertEquals("TeamPulse", organization.getName());
            assertEquals(ZoneId.of(TIMEZONE), organization.getTimezone());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
        }

        @Test
        void acceptsSameUserAsAdministratorAndManager() {
            Organization organization = Organization.restore(
                ORGANIZATION_REFERENCE,
                "TeamPulse",
                TIMEZONE,
                ADMIN_REFERENCE,
                ADMIN_REFERENCE,
                OrganizationStatus.ACTIVE
            );

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(ADMIN_REFERENCE, organization.getManagerReference());
        }

        private static Organization restoreOrganization(RestoredState state) {
            return Organization.restore(
                ORGANIZATION_REFERENCE,
                "TeamPulse",
                TIMEZONE,
                state.adminReference(),
                state.managerReference(),
                state.status()
            );
        }

        private record RestoredState(
            String adminReference,
            String managerReference,
            OrganizationStatus status
        ) {
        }
    }

    private static Organization createOrganization(String name) {
        return Organization.create(
            ORGANIZATION_REFERENCE,
            name,
            TIMEZONE
        );
    }
}
