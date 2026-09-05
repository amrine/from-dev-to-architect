package io.teampulse.identity.api.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Public identity contract for checking a user's availability within an organization.
 */
public interface UserDirectory {

    /**
     * Checks the availability of a user in the requested organization.
     *
     * @param organizationReference organization owning the user
     * @param userReference user to check
     * @return the user's availability without exposing identity internals
     * @throws UserDirectoryException when the identity module cannot perform the check
     */
    UserAvailability check(
        @NotBlank String organizationReference,
        @NotBlank String userReference
    );
}
