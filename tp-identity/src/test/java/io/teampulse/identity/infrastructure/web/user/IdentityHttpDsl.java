package io.teampulse.identity.infrastructure.web.user;

import org.openapitools.client.ApiClient;
import org.openapitools.client.api.UsersApi;
import org.openapitools.client.model.UserInput;
import org.openapitools.client.model.UserResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

/** Module-local DSL backed by the client generated from identity's OpenAPI contract. */
final class IdentityHttpDsl {

    private final UsersApi usersApi;

    private IdentityHttpDsl(UsersApi usersApi) {
        this.usersApi = usersApi;
    }

    static IdentityHttpDsl runningAt(int port) {
        ApiClient apiClient = new ApiClient()
            .setBasePath("http://localhost:" + port);
        return new IdentityHttpDsl(new UsersApi(apiClient));
    }

    ResponseEntity<UserResponse> createUser(String email, String firstName, String lastName) {
        return usersApi.createUserWithHttpInfo(input(email, firstName, lastName));
    }

    ResponseEntity<UserResponse> inviteUser(String email, String firstName, String lastName) {
        return usersApi.inviteUserWithHttpInfo(input(email, firstName, lastName));
    }

    ResponseEntity<List<UserResponse>> listUsers() {
        return usersApi.listUsersWithHttpInfo();
    }

    private static UserInput input(String email, String firstName, String lastName) {
        return new UserInput()
            .email(email)
            .firstName(firstName)
            .lastName(lastName);
    }
}
