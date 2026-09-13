package io.teampulse.organization.domain.organization.model;

import io.teampulse.organization.domain.organization.error.OrganizationErrorCode;
import io.teampulse.organization.domain.organization.error.OrganizationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    private static final String OTHER_ADMIN_REFERENCE =
        "USR-2026-0908-00000ZA7B902";

    private static final String OTHER_MANAGER_REFERENCE =
        "USR-2026-0908-00000ZA7B903";

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

    @Nested
    class AdministratorAssignmentTests {

        @Test
        void assignsAndReplacesAdministratorWhileCreating() {
            Organization organization = createOrganization("TeamPulse");

            organization.assignAdministrator(ADMIN_REFERENCE);
            organization.assignAdministrator(OTHER_ADMIN_REFERENCE);

            assertEquals(OTHER_ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @Test
        void treatsSameAdministratorAssignmentAsNoOp() {
            Organization organization = createOrganization("TeamPulse");
            organization.assignAdministrator(ADMIN_REFERENCE);

            organization.assignAdministrator(ADMIN_REFERENCE);

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"ACTIVE", "SUSPENDED"}
        )
        void rejectsAssignmentOutsideCreatingWithoutMutation(
            OrganizationStatus status
        ) {
            Organization organization = organizationInStatus(status);

            assertInvalidStatusTransition(
                () -> organization.assignAdministrator(OTHER_ADMIN_REFERENCE)
            );

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(status, organization.getStatus());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "ORG-2026-0908-00000ZA7B900"})
        void rejectsInvalidReferenceWithoutMutation(String reference) {
            Organization organization = createOrganization("TeamPulse");
            organization.assignAdministrator(ADMIN_REFERENCE);

            assertThrows(
                IllegalArgumentException.class,
                () -> organization.assignAdministrator(reference)
            );

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }
    }

    @Nested
    class ManagerAssignmentTests {

        @Test
        void assignsAndReplacesManagerWhileCreating() {
            Organization organization = createOrganization("TeamPulse");

            organization.assignManager(MANAGER_REFERENCE);
            organization.assignManager(OTHER_MANAGER_REFERENCE);

            assertEquals(OTHER_MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @Test
        void treatsSameManagerAssignmentAsNoOpWhileCreating() {
            Organization organization = createOrganization("TeamPulse");
            organization.assignManager(MANAGER_REFERENCE);

            organization.assignManager(MANAGER_REFERENCE);

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @Test
        void assignsMissingManagerWhileSuspendedWithoutReactivating() {
            Organization organization = suspendedOrganizationWithoutManager();

            organization.assignManager(MANAGER_REFERENCE);

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
        }

        @Test
        void rejectsAssignmentWhenSuspendedManagerAlreadyExists() {
            Organization organization = organizationInStatus(
                OrganizationStatus.SUSPENDED
            );

            assertInvalidStatusTransition(
                () -> organization.assignManager(OTHER_MANAGER_REFERENCE)
            );

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
        }

        @Test
        void rejectsAssignmentWhileActive() {
            Organization organization = organizationInStatus(
                OrganizationStatus.ACTIVE
            );

            assertInvalidStatusTransition(
                () -> organization.assignManager(OTHER_MANAGER_REFERENCE)
            );

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "ORG-2026-0908-00000ZA7B900"})
        void rejectsInvalidReferenceWithoutMutation(String reference) {
            Organization organization = createOrganization("TeamPulse");
            organization.assignManager(MANAGER_REFERENCE);

            assertThrows(
                IllegalArgumentException.class,
                () -> organization.assignManager(reference)
            );

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }
    }

    @Nested
    class ActivationTests {

        @Test
        void activatesCreatingOrganizationWithBothResponsibleUsers() {
            Organization organization = creatingOrganizationWithResponsibleUsers();

            organization.activate();

            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
        }

        @Test
        void acceptsSameUserAsBothResponsibleUsersOnActivation() {
            Organization organization = createOrganization("TeamPulse");
            organization.assignAdministrator(ADMIN_REFERENCE);
            organization.assignManager(ADMIN_REFERENCE);

            organization.activate();

            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(ADMIN_REFERENCE, organization.getManagerReference());
        }

        @Test
        void rejectsActivationWithoutResponsibleUsers() {
            Organization organization = createOrganization("TeamPulse");

            assertActivationRequirementsNotMet(organization::activate);

            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @Test
        void rejectsActivationWithoutAdministrator() {
            Organization organization = createOrganization("TeamPulse");
            organization.assignManager(MANAGER_REFERENCE);

            assertActivationRequirementsNotMet(organization::activate);

            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
            assertNull(organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
        }

        @Test
        void rejectsActivationWithoutManager() {
            Organization organization = createOrganization("TeamPulse");
            organization.assignAdministrator(ADMIN_REFERENCE);

            assertActivationRequirementsNotMet(organization::activate);

            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertNull(organization.getManagerReference());
        }

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"ACTIVE", "SUSPENDED"}
        )
        void rejectsActivationOutsideCreating(OrganizationStatus status) {
            Organization organization = organizationInStatus(status);

            assertInvalidStatusTransition(organization::activate);

            assertEquals(status, organization.getStatus());
        }
    }

    @Nested
    class AdministratorReplacementTests {

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"ACTIVE", "SUSPENDED"}
        )
        void replacesAdministratorWithoutChangingStatus(OrganizationStatus status) {
            Organization organization = organizationInStatus(status);

            organization.replaceAdministrator(OTHER_ADMIN_REFERENCE);

            assertEquals(OTHER_ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(status, organization.getStatus());
        }

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"ACTIVE", "SUSPENDED"}
        )
        void treatsSameAdministratorReplacementAsNoOp(OrganizationStatus status) {
            Organization organization = organizationInStatus(status);

            organization.replaceAdministrator(ADMIN_REFERENCE);

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(status, organization.getStatus());
        }

        @Test
        void allowsAdministratorToBecomeCurrentManager() {
            Organization organization = organizationInStatus(
                OrganizationStatus.ACTIVE
            );

            organization.replaceAdministrator(MANAGER_REFERENCE);

            assertEquals(MANAGER_REFERENCE, organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
        }

        @Test
        void rejectsReplacementWhileCreating() {
            Organization organization = creatingOrganizationWithResponsibleUsers();

            assertInvalidStatusTransition(
                () -> organization.replaceAdministrator(OTHER_ADMIN_REFERENCE)
            );

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "ORG-2026-0908-00000ZA7B900"})
        void rejectsInvalidReferenceWithoutMutation(String reference) {
            Organization organization = organizationInStatus(
                OrganizationStatus.ACTIVE
            );

            assertThrows(
                IllegalArgumentException.class,
                () -> organization.replaceAdministrator(reference)
            );

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
        }
    }

    @Nested
    class ManagerReplacementTests {

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"ACTIVE", "SUSPENDED"}
        )
        void replacesExistingManagerWithoutChangingStatus(OrganizationStatus status) {
            Organization organization = organizationInStatus(status);

            organization.replaceManager(OTHER_MANAGER_REFERENCE);

            assertEquals(OTHER_MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(status, organization.getStatus());
        }

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"ACTIVE", "SUSPENDED"}
        )
        void treatsSameManagerReplacementAsNoOp(OrganizationStatus status) {
            Organization organization = organizationInStatus(status);

            organization.replaceManager(MANAGER_REFERENCE);

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(status, organization.getStatus());
        }

        @Test
        void allowsManagerToBecomeCurrentAdministrator() {
            Organization organization = organizationInStatus(
                OrganizationStatus.ACTIVE
            );

            organization.replaceManager(ADMIN_REFERENCE);

            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(ADMIN_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
        }

        @Test
        void rejectsReplacementWhenSuspendedManagerIsAbsent() {
            Organization organization = suspendedOrganizationWithoutManager();

            assertInvalidStatusTransition(
                () -> organization.replaceManager(OTHER_MANAGER_REFERENCE)
            );

            assertNull(organization.getManagerReference());
            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
        }

        @Test
        void rejectsReplacementWhileCreating() {
            Organization organization = creatingOrganizationWithResponsibleUsers();

            assertInvalidStatusTransition(
                () -> organization.replaceManager(OTHER_MANAGER_REFERENCE)
            );

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "ORG-2026-0908-00000ZA7B900"})
        void rejectsInvalidReferenceWithoutMutation(String reference) {
            Organization organization = organizationInStatus(
                OrganizationStatus.ACTIVE
            );

            assertThrows(
                IllegalArgumentException.class,
                () -> organization.replaceManager(reference)
            );

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
        }
    }

    @Nested
    class ManagerRemovalTests {

        @Test
        void removesManagerAndSuspendsActiveOrganization() {
            Organization organization = organizationInStatus(
                OrganizationStatus.ACTIVE
            );

            organization.removeManager();

            assertNull(organization.getManagerReference());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
        }

        @Test
        void removesManagerFromSuspendedOrganizationWithoutChangingStatus() {
            Organization organization = organizationInStatus(
                OrganizationStatus.SUSPENDED
            );

            organization.removeManager();

            assertNull(organization.getManagerReference());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
        }

        @Test
        void treatsMissingManagerRemovalAsNoOpWhileSuspended() {
            Organization organization = suspendedOrganizationWithoutManager();

            organization.removeManager();

            assertNull(organization.getManagerReference());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
        }

        @Test
        void rejectsManagerRemovalWhileCreating() {
            Organization organization = creatingOrganizationWithResponsibleUsers();

            assertInvalidStatusTransition(organization::removeManager);

            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            assertEquals(OrganizationStatus.CREATING, organization.getStatus());
        }
    }

    @Nested
    class SuspensionTests {

        @Test
        void suspendsActiveOrganizationAndPreservesResponsibleUsers() {
            Organization organization = organizationInStatus(
                OrganizationStatus.ACTIVE
            );

            organization.suspend();

            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
        }

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"CREATING", "SUSPENDED"}
        )
        void rejectsSuspensionOutsideActive(OrganizationStatus status) {
            Organization organization = organizationInStatus(status);

            assertInvalidStatusTransition(organization::suspend);

            assertEquals(status, organization.getStatus());
        }
    }

    @Nested
    class ReactivationTests {

        @Test
        void reactivatesSuspendedOrganizationWithBothResponsibleUsers() {
            Organization organization = organizationInStatus(
                OrganizationStatus.SUSPENDED
            );

            organization.reactivate();

            assertEquals(OrganizationStatus.ACTIVE, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
        }

        @Test
        void rejectsReactivationWithoutManager() {
            Organization organization = suspendedOrganizationWithoutManager();

            assertActivationRequirementsNotMet(organization::reactivate);

            assertEquals(OrganizationStatus.SUSPENDED, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertNull(organization.getManagerReference());
        }

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"CREATING", "ACTIVE"}
        )
        void rejectsReactivationOutsideSuspended(OrganizationStatus status) {
            Organization organization = organizationInStatus(status);

            assertInvalidStatusTransition(organization::reactivate);

            assertEquals(status, organization.getStatus());
        }
    }

    @Nested
    class ArchivalTests {

        @ParameterizedTest
        @EnumSource(
            value = OrganizationStatus.class,
            names = {"CREATING", "ACTIVE", "SUSPENDED"}
        )
        void archivesOrganizationAndPreservesResponsibleUsers(
            OrganizationStatus status
        ) {
            Organization organization = organizationInStatus(status);

            organization.archive();

            assertEquals(OrganizationStatus.ARCHIVED, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
        }

        @Test
        void archivesCreatingOrganizationWithoutResponsibleUsers() {
            Organization organization = createOrganization("TeamPulse");

            organization.archive();

            assertEquals(OrganizationStatus.ARCHIVED, organization.getStatus());
            assertNull(organization.getAdminReference());
            assertNull(organization.getManagerReference());
        }

        @Test
        void archivesCreatingOrganizationWithOnlyManager() {
            Organization organization = Organization.restore(
                ORGANIZATION_REFERENCE,
                "TeamPulse",
                TIMEZONE,
                null,
                MANAGER_REFERENCE,
                OrganizationStatus.CREATING
            );

            organization.archive();

            assertEquals(OrganizationStatus.ARCHIVED, organization.getStatus());
            assertNull(organization.getAdminReference());
            assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
        }

        @Test
        void archivesSuspendedOrganizationWithoutManager() {
            Organization organization = suspendedOrganizationWithoutManager();

            organization.archive();

            assertEquals(OrganizationStatus.ARCHIVED, organization.getStatus());
            assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
            assertNull(organization.getManagerReference());
        }
    }

    @Nested
    class ArchivedTerminalStateTests {

        @Test
        void rejectsEveryDomainOperationWithoutMutation() {
            Organization organization = organizationInStatus(
                OrganizationStatus.ARCHIVED
            );

            List<Executable> operations = List.of(
                () -> organization.assignAdministrator(null),
                () -> organization.assignManager("invalid-reference"),
                organization::activate,
                () -> organization.replaceAdministrator(OTHER_ADMIN_REFERENCE),
                () -> organization.replaceManager(OTHER_MANAGER_REFERENCE),
                organization::removeManager,
                organization::suspend,
                organization::reactivate,
                organization::archive
            );

            for (Executable operation : operations) {
                assertInvalidStatusTransition(operation);
                assertEquals(OrganizationStatus.ARCHIVED, organization.getStatus());
                assertEquals(ADMIN_REFERENCE, organization.getAdminReference());
                assertEquals(MANAGER_REFERENCE, organization.getManagerReference());
            }
        }
    }

    private static Organization createOrganization(String name) {
        return Organization.create(
            ORGANIZATION_REFERENCE,
            name,
            TIMEZONE
        );
    }

    private static Organization creatingOrganizationWithResponsibleUsers() {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            TIMEZONE,
            ADMIN_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.CREATING
        );
    }

    private static Organization suspendedOrganizationWithoutManager() {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            TIMEZONE,
            ADMIN_REFERENCE,
            null,
            OrganizationStatus.SUSPENDED
        );
    }

    private static Organization organizationInStatus(OrganizationStatus status) {
        return Organization.restore(
            ORGANIZATION_REFERENCE,
            "TeamPulse",
            TIMEZONE,
            ADMIN_REFERENCE,
            MANAGER_REFERENCE,
            status
        );
    }

    private static void assertInvalidStatusTransition(Executable operation) {
        OrganizationException exception = assertThrows(
            OrganizationException.class,
            operation
        );

        assertEquals(
            OrganizationErrorCode.INVALID_STATUS_TRANSITION,
            exception.getErrorCode()
        );
    }

    private static void assertActivationRequirementsNotMet(Executable operation) {
        OrganizationException exception = assertThrows(
            OrganizationException.class,
            operation
        );

        assertEquals(
            OrganizationErrorCode.ACTIVATION_REQUIREMENTS_NOT_MET,
            exception.getErrorCode()
        );
    }
}
