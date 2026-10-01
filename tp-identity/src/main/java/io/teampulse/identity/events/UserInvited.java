package io.teampulse.identity.events;

import jakarta.validation.constraints.NotBlank;

/**
 * Invitation fact containing only references to the user and its organization.
 */
public record UserInvited(
    @NotBlank String organizationReference,
    @NotBlank String userReference
) { }
