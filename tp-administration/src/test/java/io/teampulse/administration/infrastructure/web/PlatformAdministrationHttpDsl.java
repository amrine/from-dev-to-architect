package io.teampulse.administration.infrastructure.web;

import org.openapitools.client.ApiClient;
import org.openapitools.client.api.PlatformProvisioningApi;
import org.openapitools.client.api.PlatformUserLifecycleApi;
import org.openapitools.client.model.PlatformProvisioningInput;
import org.openapitools.client.model.PlatformProvisioningResult;
import org.springframework.http.ResponseEntity;

/** Module-local DSL backed by the client generated from platform administration OpenAPI. */
final class PlatformAdministrationHttpDsl {

    private final PlatformProvisioningApi platformProvisioningApi;
    private final PlatformUserLifecycleApi platformUserLifecycleApi;

    private PlatformAdministrationHttpDsl(
        PlatformProvisioningApi platformProvisioningApi,
        PlatformUserLifecycleApi platformUserLifecycleApi
    ) {
        this.platformProvisioningApi = platformProvisioningApi;
        this.platformUserLifecycleApi = platformUserLifecycleApi;
    }

    static PlatformAdministrationHttpDsl runningAt(int port) {
        ApiClient apiClient = new ApiClient().setBasePath("http://localhost:" + port);
        return new PlatformAdministrationHttpDsl(
            new PlatformProvisioningApi(apiClient),
            new PlatformUserLifecycleApi(apiClient)
        );
    }

    ResponseEntity<PlatformProvisioningResult> createOrganizationAndInitialUser(
        String organizationName,
        String timezone,
        String email,
        String firstName,
        String lastName
    ) {
        return platformProvisioningApi.createOrganizationAndInitialUserWithHttpInfo(
            input(organizationName, timezone, email, firstName, lastName)
        );
    }

    ResponseEntity<PlatformProvisioningResult> inviteInitialUser(
        String organizationName,
        String timezone,
        String email,
        String firstName,
        String lastName
    ) {
        return platformProvisioningApi.inviteInitialUserWithHttpInfo(
            input(organizationName, timezone, email, firstName, lastName)
        );
    }

    ResponseEntity<Void> suspendUser(String userReference) {
        return platformUserLifecycleApi.suspendUserWithHttpInfo(userReference);
    }

    ResponseEntity<Void> deactivateUser(String userReference) {
        return platformUserLifecycleApi.deactivateUserWithHttpInfo(userReference);
    }

    private static PlatformProvisioningInput input(
        String organizationName,
        String timezone,
        String email,
        String firstName,
        String lastName
    ) {
        return new PlatformProvisioningInput()
            .organizationName(organizationName)
            .timezone(timezone)
            .email(email)
            .firstName(firstName)
            .lastName(lastName);
    }
}
