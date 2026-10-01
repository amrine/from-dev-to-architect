package io.teampulse.administration.infrastructure.web;

public record PlatformProvisioningResponse(
    String organizationReference,
    String userReference
) { }
