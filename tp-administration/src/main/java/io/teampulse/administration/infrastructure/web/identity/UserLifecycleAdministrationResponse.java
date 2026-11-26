package io.teampulse.administration.infrastructure.web.identity;

public record UserLifecycleAdministrationResponse(
    String organizationReference,
    String userReference
) { }
