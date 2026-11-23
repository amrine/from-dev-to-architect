package io.teampulse.administration.application.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlatformAdministrationErrorCode {
    ORGANIZATION_NOT_FOUND("Organization was not found"),
    ORGANIZATION_LOOKUP_UNAVAILABLE("Organization could not be checked"),
    USER_DATA_INVALID("User data is invalid"),
    USER_EMAIL_ALREADY_USED("Email is already used in this organization"),
    USER_NOT_FOUND("User was not found"),
    USER_TRANSITION_NOT_ALLOWED("User transition is not allowed"),
    USER_LIFECYCLE_FAILED("User lifecycle operation failed"),
    USER_HAS_ACTIVE_RESPONSIBILITIES("User has active responsibilities"),
    RESPONSIBILITY_CHECK_UNAVAILABLE("User responsibilities could not be checked");

    private final String message;
}
