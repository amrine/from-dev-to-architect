package io.teampulse.identity.api.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Stable failure categories for the public user lifecycle contract. */
@Getter
@AllArgsConstructor
public enum UserLifecycleErrorCode {
    USER_NOT_FOUND("User was not found"),
    TRANSITION_NOT_ALLOWED("User transition is not allowed"),
    EMAIL_ALREADY_USED("Email is already used in this organization"),
    INVALID_USER_DATA("User data is invalid"),
    CONCURRENT_MODIFICATION("User changed during the operation"),
    OPERATION_FAILED("User lifecycle operation failed");

    private final String value;
}
