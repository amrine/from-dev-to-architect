package io.teampulse.organization.infrastructure.web.organization;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.organization.AbstractOrganizationMvcIT;
import io.teampulse.organization.fixture.DeterministicUserDirectory;
import io.teampulse.organization.api.lifecycle.OrganizationProvisioningCommand;
import io.teampulse.organization.application.port.out.organization.OrganizationRepository;
import io.teampulse.organization.domain.organization.model.Organization;
import io.teampulse.organization.domain.organization.model.OrganizationStatus;
import io.teampulse.organization.infrastructure.persistence.entity.OrganizationEntity;
import io.teampulse.organization.infrastructure.persistence.repository.JpaOrganizationRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationControllerIT extends AbstractOrganizationMvcIT {

    private static final String REFERENCE = "ORG-2026-1409-00000ZA7B940";
    private static final String ADMINISTRATOR = "USR-2026-1409-00000ZA7B930";
    private static final String MANAGER = "USR-2026-1409-00000ZA7B931";
    private static final String REPLACEMENT = "USR-2026-1409-00000ZA7B932";

    @Value("${teampulse.test.product-version}")
    private String expectedApiVersion;

    @Value("${teampulse.test.openapi-file}")
    private String generatedOpenApiFile;

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
    void createsAndPersistsAnOrganizationInCreatingStatus() throws Exception {
        var result = mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrganizationProvisioningCommand(" Acme ", "Europe/Paris"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Acme"))
            .andExpect(jsonPath("$.timezone").value("Europe/Paris"))
            .andExpect(jsonPath("$.status").value("CREATING"))
            .andReturn();
        var response = objectMapper.readValue(result.getResponse().getContentAsString(), OrganizationResponse.class);
        var persisted = persisted(response.reference());
        assertEquals(OrganizationStatus.CREATING, persisted.getStatus());
        assertNull(persisted.getAdminReference());
        assertNull(persisted.getManagerReference());
        assertEquals("Acme", persisted.getName());
        assertEquals("Europe/Paris", persisted.getTimezone());
    }

    @Test
    void readsUnassignedResponsibilities() throws Exception {
        organizationRepository.create(Organization.create(REFERENCE, "Acme", "UTC"));
        mockMvc.perform(get("/api/organizations/{reference}/responsibilities", REFERENCE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationReference").value(REFERENCE))
            .andExpect(jsonPath("$.status").value("CREATING"))
            .andExpect(jsonPath("$.administratorReference").isEmpty())
            .andExpect(jsonPath("$.managerReference").isEmpty());
        assertEquals(OrganizationStatus.CREATING, persisted(REFERENCE).getStatus());
        assertEquals(1L, jpaOrganizationRepository.count());
    }

    @ParameterizedTest
    @EnumSource(OrganizationStatus.class)
    void readsAssignedResponsibilitiesWithoutChangingPersistedState(OrganizationStatus organizationStatus) throws Exception {
        store(organizationStatus, ADMINISTRATOR, MANAGER);
        var before = persisted(REFERENCE);
        mockMvc.perform(get("/api/organizations/{reference}/responsibilities", REFERENCE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationReference").value(REFERENCE))
            .andExpect(jsonPath("$.status").value(organizationStatus.name()))
            .andExpect(jsonPath("$.administratorReference").value(ADMINISTRATOR))
            .andExpect(jsonPath("$.managerReference").value(MANAGER));
        var after = persisted(REFERENCE);
        assertEquals(organizationStatus, after.getStatus());
        assertEquals(before.getAdminReference(), after.getAdminReference());
        assertEquals(before.getManagerReference(), after.getManagerReference());
        assertEquals(before.getVersion(), after.getVersion());
        assertEquals(1L, jpaOrganizationRepository.count());
    }

    @Test
    void returnsNotFoundForAbsentResponsibilitiesWithoutWriting() throws Exception {
        mockMvc.perform(get("/api/organizations/{reference}/responsibilities", REFERENCE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Organization was not found"));
        assertEquals(0L, jpaOrganizationRepository.count());
    }

    @Test
    void mapsInvalidOrganizationDataToASanitizedLocalBusinessError() throws Exception {
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrganizationProvisioningCommand("Acme", "private-invalid-zone"))))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("INVALID_ORGANIZATION_TIMEZONE"))
            .andExpect(jsonPath("$.message").value("Organization timezone is invalid"));
        assertEquals(0L, jpaOrganizationRepository.count());
    }

    @Test
    void mapsInvalidRequestStructureThroughTheSharedTransportAdvice() throws Exception {
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OrganizationProvisioningCommand(" ", "UTC"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").value("Request validation failed"));
        assertEquals(0L, jpaOrganizationRepository.count());
    }

    @Test
    void rejectsMalformedJsonWithoutWriting() throws Exception {
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
        assertEquals(0L, jpaOrganizationRepository.count());
    }

    @Test
    void assignsAnAvailableAdministratorButRefusesTheFinalAvailableManager() throws Exception {
        organizationRepository.create(Organization.create(REFERENCE, "Acme", "UTC"));
        userDirectory.setAvailability(REFERENCE, ADMINISTRATOR, UserAvailability.AVAILABLE);
        userDirectory.setAvailability(REFERENCE, MANAGER, UserAvailability.AVAILABLE);
        mockMvc.perform(post("/api/organizations/{reference}/administrator", REFERENCE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(ADMINISTRATOR))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CREATING"));
        mockMvc.perform(post("/api/organizations/{reference}/manager", REFERENCE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(MANAGER))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_LIFECYCLE_DEFERRED"))
            .andExpect(jsonPath("$.message").value("Completing organization responsibilities is deferred"));
        assertEquals(OrganizationStatus.CREATING, persisted(REFERENCE).getStatus());
        assertEquals(ADMINISTRATOR, persisted(REFERENCE).getAdminReference());
        assertNull(persisted(REFERENCE).getManagerReference());
    }

    @Test
    void assignsAManagerWhileTheAdministratorIsUnassigned() throws Exception {
        organizationRepository.create(Organization.create(REFERENCE, "Acme", "UTC"));
        userDirectory.setAvailability(REFERENCE, MANAGER, UserAvailability.AVAILABLE);
        mockMvc.perform(post("/api/organizations/{reference}/manager", REFERENCE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(MANAGER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CREATING"))
            .andExpect(jsonPath("$.managerReference").value(MANAGER));
        assertEquals(MANAGER, persisted(REFERENCE).getManagerReference());
        assertNull(persisted(REFERENCE).getAdminReference());
        assertEquals(OrganizationStatus.CREATING, persisted(REFERENCE).getStatus());
    }

    @ParameterizedTest
    @CsvSource({"CREATING,administrator,PENDING,409,ORGANIZATION_ADMINISTRATOR_NOT_AVAILABLE",
        "CREATING,administrator,UNAVAILABLE,409,ORGANIZATION_ADMINISTRATOR_NOT_AVAILABLE",
        "CREATING,administrator,NOT_FOUND,404,ORGANIZATION_ADMINISTRATOR_NOT_FOUND",
        "CREATING,manager,PENDING,409,ORGANIZATION_MANAGER_NOT_AVAILABLE",
        "CREATING,manager,UNAVAILABLE,409,ORGANIZATION_MANAGER_NOT_AVAILABLE",
        "CREATING,manager,NOT_FOUND,404,ORGANIZATION_MANAGER_NOT_FOUND",
        "SUSPENDED,manager,PENDING,409,ORGANIZATION_MANAGER_NOT_AVAILABLE"})
    void refusesAnUnavailableOrAbsentResponsibleWithoutWriting(
        OrganizationStatus initialStatus, String role, UserAvailability availability,
        int expectedStatus, String errorCode
    ) throws Exception {
        store(initialStatus, initialStatus == OrganizationStatus.SUSPENDED ? ADMINISTRATOR : null, null);
        userDirectory.setAvailability(REFERENCE, REPLACEMENT, availability);
        var before = persisted(REFERENCE);
        mockMvc.perform(post("/api/organizations/{reference}/{role}", REFERENCE, role)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(REPLACEMENT))))
            .andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.code").value(errorCode));
        var after = persisted(REFERENCE);
        assertEquals(before.getVersion(), after.getVersion());
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getAdminReference(), after.getAdminReference());
        assertEquals(before.getManagerReference(), after.getManagerReference());
    }

    @Test
    void refusesFinalManagerAssignmentInASuspendedOrganization() throws Exception {
        String role = "manager";
        store(OrganizationStatus.SUSPENDED, ADMINISTRATOR, null);
        userDirectory.setAvailability(REFERENCE, ADMINISTRATOR, UserAvailability.AVAILABLE);
        userDirectory.setAvailability(REFERENCE, MANAGER, UserAvailability.AVAILABLE);
        var before = persisted(REFERENCE);
        mockMvc.perform(post("/api/organizations/{reference}/{role}", REFERENCE, role)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(
                    MANAGER))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_LIFECYCLE_DEFERRED"));
        assertEquals(OrganizationStatus.SUSPENDED, persisted(REFERENCE).getStatus());
        assertEquals(before.getVersion(), persisted(REFERENCE).getVersion());
        assertEquals(before.getAdminReference(), persisted(REFERENCE).getAdminReference());
        assertEquals(before.getManagerReference(), persisted(REFERENCE).getManagerReference());
    }

    @ParameterizedTest
    @CsvSource({"administrator", "manager"})
    void replacesAnOperationalResponsibleWithoutChangingStatus(String role) throws Exception {
        store(OrganizationStatus.ACTIVE, ADMINISTRATOR, MANAGER);
        userDirectory.setAvailability(REFERENCE, REPLACEMENT, UserAvailability.AVAILABLE);
        mockMvc.perform(put("/api/organizations/{reference}/{role}", REFERENCE, role)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(REPLACEMENT))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$." + role + "Reference").value(REPLACEMENT));
        var stored = persisted(REFERENCE);
        assertEquals(OrganizationStatus.ACTIVE, stored.getStatus());
        assertEquals(role.equals("administrator") ? REPLACEMENT : ADMINISTRATOR, stored.getAdminReference());
        assertEquals(role.equals("manager") ? REPLACEMENT : MANAGER, stored.getManagerReference());
    }

    @ParameterizedTest
    @CsvSource({"administrator,ORGANIZATION_ADMINISTRATOR_NOT_AVAILABLE", "manager,ORGANIZATION_MANAGER_NOT_AVAILABLE"})
    void rejectsReplacementByANonOperationalUserWithoutMutation(String role, String errorCode) throws Exception {
        store(OrganizationStatus.ACTIVE, ADMINISTRATOR, MANAGER);
        userDirectory.setAvailability(REFERENCE, REPLACEMENT, UserAvailability.PENDING);
        var before = persisted(REFERENCE);
        mockMvc.perform(put("/api/organizations/{reference}/{role}", REFERENCE, role)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(REPLACEMENT))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(errorCode));
        assertEquals(before.getVersion(), persisted(REFERENCE).getVersion());
        assertEquals(ADMINISTRATOR, persisted(REFERENCE).getAdminReference());
        assertEquals(MANAGER, persisted(REFERENCE).getManagerReference());
    }

    @ParameterizedTest
    @EnumSource(value = OrganizationStatus.class, names = {"ACTIVE", "SUSPENDED"})
    void removesManagerAndPersistsTheDomainStatus(OrganizationStatus initialStatus) throws Exception {
        store(initialStatus, ADMINISTRATOR, MANAGER);
        mockMvc.perform(delete("/api/organizations/{reference}/manager", REFERENCE))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        var stored = persisted(REFERENCE);
        assertNull(stored.getManagerReference());
        assertEquals(ADMINISTRATOR, stored.getAdminReference());
        assertEquals(initialStatus == OrganizationStatus.ACTIVE ? OrganizationStatus.SUSPENDED : initialStatus, stored.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrganizationStatus.class, names = {"CREATING", "ARCHIVED"})
    void refusesManagerRemovalInAnUnsupportedState(OrganizationStatus initialStatus) throws Exception {
        store(initialStatus, ADMINISTRATOR, MANAGER);
        var before = persisted(REFERENCE);
        mockMvc.perform(delete("/api/organizations/{reference}/manager", REFERENCE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_TRANSITION_NOT_ALLOWED"));
        assertEquals(initialStatus, persisted(REFERENCE).getStatus());
        assertEquals(MANAGER, persisted(REFERENCE).getManagerReference());
        assertEquals(before.getVersion(), persisted(REFERENCE).getVersion());
    }

    @Test
    void suspendsAnActiveOrganization() throws Exception {
        store(OrganizationStatus.ACTIVE, ADMINISTRATOR, MANAGER);
        mockMvc.perform(post("/api/organizations/{reference}/suspension", REFERENCE))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));
        assertEquals(OrganizationStatus.SUSPENDED, persisted(REFERENCE).getStatus());
        assertEquals(ADMINISTRATOR, persisted(REFERENCE).getAdminReference());
        assertEquals(MANAGER, persisted(REFERENCE).getManagerReference());
    }

    @Test
    void archivesACreatingOrganization() throws Exception {
        organizationRepository.create(Organization.create(REFERENCE, "Acme", "UTC"));
        mockMvc.perform(post("/api/organizations/{reference}/archive", REFERENCE))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        assertEquals(OrganizationStatus.ARCHIVED, persisted(REFERENCE).getStatus());
    }

    @Test
    void refusesSuspensionOfACreatingOrganizationWithoutChangingItsState() throws Exception {
        organizationRepository.create(Organization.create(REFERENCE, "Acme", "UTC"));
        mockMvc.perform(post("/api/organizations/{reference}/suspension", REFERENCE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ORGANIZATION_TRANSITION_NOT_ALLOWED"));
        assertEquals(OrganizationStatus.CREATING, persisted(REFERENCE).getStatus());
    }

    @Test
    void returnsNotFoundForAnAbsentOrganizationThroughThePublicLifecycle() throws Exception {
        mockMvc.perform(post("/api/organizations/{reference}/archive", REFERENCE))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
        assertEquals(0L, jpaOrganizationRepository.count());
    }

    @ParameterizedTest
    @CsvSource({"administrator", "manager"})
    void rejectsBlankResponsibleReferencesBeforeMutation(String role) throws Exception {
        store(OrganizationStatus.ACTIVE, ADMINISTRATOR, MANAGER);
        var before = persisted(REFERENCE);
        mockMvc.perform(put("/api/organizations/{reference}/{role}", REFERENCE, role)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AssignOrganizationResponsibleUserRequest(" "))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertEquals(before.getVersion(), persisted(REFERENCE).getVersion());
    }

    @Test
    void documentsApiMetadataAndSuccessfulHttpStatuses() throws Exception {
        var result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode contract = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(expectedApiVersion, contract.path("info").path("version").asString());
        assertEquals("/", contract.path("servers").get(0).path("url").asString());
        assertEquals("#/components/schemas/OrganizationProvisioningCommand", contract.path("paths")
            .path("/api/organizations").path("post").path("requestBody")
            .path("content").path("application/json").path("schema").path("$ref").asString());
        assertNotNull(contract.path("paths").path("/api/organizations").path("post").path("responses").get("201"));
        assertNotNull(contract.path("paths").path("/api/organizations/{organizationReference}/manager")
            .path("delete").path("responses").get("204"));
        JsonNode responsibilitiesSchema = contract.path("paths")
            .path("/api/organizations/{organizationReference}/responsibilities").path("get");
        assertEquals("getOrganizationResponsibilities", responsibilitiesSchema.path("operationId").asString());
        assertEquals("#/components/schemas/OrganizationResponsibilities", responsibilitiesSchema
            .path("responses").path("200").path("content").path("*/*").path("schema").path("$ref").asString());
    }

    @Test
    void exportsTheMavenProductVersionInTheGeneratedContract() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(generatedOpenApiFile));
        var contract = yaml.getObject();

        assertNotNull(contract);
        assertEquals("TeamPulse Organization API", contract.getProperty("info.title"));
        assertEquals(expectedApiVersion, contract.getProperty("info.version"));
        assertEquals("/", contract.getProperty("servers[0].url"));
        assertEquals("#/components/schemas/OrganizationProvisioningCommand", contract.getProperty(
            "paths./api/organizations.post.requestBody.content.application/json.schema.$ref"));
    }

    private void store(OrganizationStatus organizationStatus, String administrator, String manager) {
        organizationRepository.create(Organization.restore(REFERENCE, "Acme", "UTC", administrator, manager, organizationStatus));
    }

    private OrganizationEntity persisted(String reference) {
        return jpaOrganizationRepository.findByReference(reference).orElseThrow();
    }
}
