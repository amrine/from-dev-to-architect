package io.teampulse.team.domain.team.error;

import lombok.Getter;

import java.util.Objects;

public class TeamMemberException extends RuntimeException {

    @Getter
    private final TeamMemberErrorCode errorCode;

    public TeamMemberException(TeamMemberErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public TeamMemberException(TeamMemberErrorCode errorCode, String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
