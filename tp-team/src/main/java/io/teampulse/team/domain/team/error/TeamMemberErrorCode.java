package io.teampulse.team.domain.team.error;

public enum TeamMemberErrorCode {
    NOT_FOUND,
    ALREADY_EXISTS,
    USER_NOT_FOUND,
    USER_NOT_AVAILABLE,
    USER_DIRECTORY_UNAVAILABLE,
    INVALID_STATUS_TRANSITION,
    CONCURRENT_MODIFICATION
}
