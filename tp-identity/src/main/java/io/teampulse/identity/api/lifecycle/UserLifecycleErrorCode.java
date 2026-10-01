package io.teampulse.identity.api.lifecycle;

/** Stable failure categories for the public user lifecycle contract. */
public enum UserLifecycleErrorCode {
    USER_NOT_FOUND,
    TRANSITION_NOT_ALLOWED,
    EMAIL_ALREADY_USED,
    INVALID_USER_DATA,
    CONCURRENT_MODIFICATION,
    OPERATION_FAILED
}
