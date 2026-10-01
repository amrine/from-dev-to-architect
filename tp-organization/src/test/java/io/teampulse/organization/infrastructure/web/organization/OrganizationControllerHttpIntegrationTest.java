package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.common.error.ApiError;
import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.organization.AbstractOrganizationHttpIntegrationTest;
import io.teampulse.organization.OrganizationTestApplication.DeterministicUserDirectory;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OrganizationControllerHttpIntegrationTest extends AbstractOrganizationHttpIntegrationTest {

    private static final String ADMINISTRATOR_REFERENCE =
        "USR-2026-1409-00000ZA7B930";
    private static final String MANAGER_REFERENCE =
        "USR-2026-1409-00000ZA7B931";

    @Value("${local.server.port}")
    private int port;

    @Inject
    private DeterministicUserDirectory userDirectory;

    @Inject
    private OrganizationRepository organizationRepository;

    @Inject
    private JpaOrganizationRepository jpaOrganizationRepository;

    @BeforeEach
    void resetExternalUserDirectoryStub() {
        userDirectory.reset();
    }

    @Test
    void createsAndPersistsAnOrganizationInCreatingStatus() {
        var response = api().create("Acme", "Europe/Paris");
        var organization = response.getBody();

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(organization);
        assertEquals("CREATING", organization.getStatus().getValue());

        OrganizationEntity persisted = persisted(organization.getReference());
        assertEquals(OrganizationStatus.CREATING, persisted.getStatus());
        assertNull(persisted.getAdminReference());
        assertNull(persisted.getManagerReference());
    }

    @Test
    void mapsInvalidOrganizationDataToASanitizedLocalBusinessError() {
        ApiError error = restTestClient.post()
            .uri("/api/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "name": "Acme", "timezone": "private-invalid-zone" }
                """)
            .exchange()
            .expectStatus().isEqualTo(422)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("INVALID_ORGANIZATION_TIMEZONE", error.code());
        assertEquals("Organization timezone is invalid", error.message());
        assertEquals(0L, jpaOrganizationRepository.count());
    }

    @Test
    void mapsInvalidRequestStructureThroughTheSharedTransportAdvice() {
        ApiError error = restTestClient.post()
            .uri("/api/organizations")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "name": " ", "timezone": "UTC" }
                """)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("VALIDATION_FAILED", error.code());
        assertEquals("Request validation failed", error.message());
        assertEquals(0L, jpaOrganizationRepository.count());
    }

    @Test
    void assignsAnAvailableAdministratorButRefusesTheFinalAvailableManager() {
        var created = api().create("Acme", "UTC").getBody();
        assertNotNull(created);
        String organizationReference = created.getReference();
        userDirectory.setAvailability(
            organizationReference,
            ADMINISTRATOR_REFERENCE,
            UserAvailability.AVAILABLE
        );
        var assignedAdministrator = api().assignAdministrator(
            organizationReference,
            ADMINISTRATOR_REFERENCE
        );
        assertEquals(200, assignedAdministrator.getStatusCode().value());
        assertEquals("CREATING", assignedAdministrator.getBody().getStatus().getValue());

        userDirectory.setAvailability(
            organizationReference,
            MANAGER_REFERENCE,
            UserAvailability.AVAILABLE
        );
        ApiError error = restTestClient.post()
            .uri("/api/organizations/{organizationReference}/manager", organizationReference)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "userReference": "USR-2026-1409-00000ZA7B931" }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("ORGANIZATION_LIFECYCLE_DEFERRED", error.code());
        assertEquals(
            "Completing organization responsibilities is deferred",
            error.message()
        );
        OrganizationEntity persisted = persisted(organizationReference);
        assertEquals(OrganizationStatus.CREATING, persisted.getStatus());
        assertEquals(ADMINISTRATOR_REFERENCE, persisted.getAdminReference());
        assertNull(persisted.getManagerReference());
    }

    @Test
    void refusesUnavailableResponsibleWithoutPersistingTheAssignment() {
        var created = api().create("Acme", "UTC").getBody();
        assertNotNull(created);
        String organizationReference = created.getReference();
        userDirectory.setAvailability(
            organizationReference,
            ADMINISTRATOR_REFERENCE,
            UserAvailability.UNAVAILABLE
        );

        ApiError error = restTestClient.post()
            .uri("/api/organizations/{organizationReference}/administrator", organizationReference)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "userReference": "USR-2026-1409-00000ZA7B930" }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("ORGANIZATION_ADMINISTRATOR_NOT_AVAILABLE", error.code());
        OrganizationEntity persisted = persisted(organizationReference);
        assertEquals(OrganizationStatus.CREATING, persisted.getStatus());
        assertNull(persisted.getAdminReference());
    }

    @Test
    void refusesToReactivateSuspendedOrganizationByAssigningItsMissingManager() {
        String organizationReference = "ORG-2026-1409-00000ZA7B940";
        organizationRepository.create(Organization.restore(
            organizationReference,
            "Suspended organization",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            null,
            OrganizationStatus.SUSPENDED
        ));
        userDirectory.setAvailability(
            organizationReference,
            ADMINISTRATOR_REFERENCE,
            UserAvailability.AVAILABLE
        );
        userDirectory.setAvailability(
            organizationReference,
            MANAGER_REFERENCE,
            UserAvailability.AVAILABLE
        );

        ApiError error = restTestClient.post()
            .uri("/api/organizations/{organizationReference}/manager", organizationReference)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                { "userReference": "USR-2026-1409-00000ZA7B931" }
                """)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(ApiError.class)
            .returnResult()
            .getResponseBody();

        assertNotNull(error);
        assertEquals("ORGANIZATION_LIFECYCLE_DEFERRED", error.code());
        OrganizationEntity persisted = persisted(organizationReference);
        assertEquals(OrganizationStatus.SUSPENDED, persisted.getStatus());
        assertNull(persisted.getManagerReference());
    }

    @Test
    void suspendsAndArchivesUsingTheOrganizationLifecycleApi() {
        String activeReference = "ORG-2026-1409-00000ZA7B941";
        organizationRepository.create(Organization.restore(
            activeReference,
            "Active organization",
            "UTC",
            ADMINISTRATOR_REFERENCE,
            MANAGER_REFERENCE,
            OrganizationStatus.ACTIVE
        ));

        var suspended = api().suspend(activeReference);

        assertEquals(200, suspended.getStatusCode().value());
        assertEquals("SUSPENDED", suspended.getBody().getStatus().getValue());
        assertEquals(OrganizationStatus.SUSPENDED, persisted(activeReference).getStatus());

        String creatingReference = "ORG-2026-1409-00000ZA7B942";
        organizationRepository.create(Organization.create(
            creatingReference,
            "Creating organization",
            "UTC"
        ));
        var archived = api().archive(creatingReference);

        assertEquals(200, archived.getStatusCode().value());
        assertEquals("ARCHIVED", archived.getBody().getStatus().getValue());
        assertEquals(OrganizationStatus.ARCHIVED, persisted(creatingReference).getStatus());
    }

    private OrganizationHttpDsl api() {
        return OrganizationHttpDsl.runningAt(port);
    }

    private OrganizationEntity persisted(String organizationReference) {
        return jpaOrganizationRepository.findByReference(organizationReference)
            .orElseThrow();
    }
}
