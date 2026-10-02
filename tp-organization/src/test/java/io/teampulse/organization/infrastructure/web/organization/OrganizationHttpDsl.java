package io.teampulse.organization.infrastructure.web.organization;

import org.openapitools.client.ApiClient;
import org.openapitools.client.api.OrganizationsApi;
import org.openapitools.client.model.CreateOrganizationInput;
import org.openapitools.client.model.OrganizationResponse;
import org.openapitools.client.model.ResponsibleUserInput;
import org.springframework.http.ResponseEntity;

/** Module-local DSL backed by the client generated from organization OpenAPI. */
final class OrganizationHttpDsl {

    private final OrganizationsApi organizationsApi;

    private OrganizationHttpDsl(OrganizationsApi organizationsApi) {
        this.organizationsApi = organizationsApi;
    }

    static OrganizationHttpDsl runningAt(int port) {
        ApiClient apiClient = new ApiClient()
            .setBasePath("http://localhost:" + port);
        return new OrganizationHttpDsl(new OrganizationsApi(apiClient));
    }

    ResponseEntity<OrganizationResponse> create(String name, String timezone) {
        return organizationsApi.createOrganizationWithHttpInfo(
            new CreateOrganizationInput()
                .name(name)
                .timezone(timezone)
        );
    }

    ResponseEntity<OrganizationResponse> assignAdministrator(
        String organizationReference,
        String userReference
    ) {
        return organizationsApi.assignOrganizationAdministratorWithHttpInfo(
            organizationReference,
            responsibleUser(userReference)
        );
    }

    ResponseEntity<OrganizationResponse> replaceAdministrator(
        String organizationReference,
        String userReference
    ) {
        return organizationsApi.replaceOrganizationAdministratorWithHttpInfo(
            organizationReference,
            responsibleUser(userReference)
        );
    }

    ResponseEntity<OrganizationResponse> assignManager(
        String organizationReference,
        String userReference
    ) {
        return organizationsApi.assignOrganizationManagerWithHttpInfo(
            organizationReference,
            responsibleUser(userReference)
        );
    }

    ResponseEntity<OrganizationResponse> replaceManager(
        String organizationReference,
        String userReference
    ) {
        return organizationsApi.replaceOrganizationManagerWithHttpInfo(
            organizationReference,
            responsibleUser(userReference)
        );
    }

    ResponseEntity<Void> removeManager(String organizationReference) {
        return organizationsApi.removeOrganizationManagerWithHttpInfo(organizationReference);
    }

    ResponseEntity<OrganizationResponse> suspend(String organizationReference) {
        return organizationsApi.suspendOrganizationWithHttpInfo(organizationReference);
    }

    ResponseEntity<OrganizationResponse> archive(String organizationReference) {
        return organizationsApi.archiveOrganizationWithHttpInfo(organizationReference);
    }

    private static ResponsibleUserInput responsibleUser(String userReference) {
        return new ResponsibleUserInput().userReference(userReference);
    }
}
