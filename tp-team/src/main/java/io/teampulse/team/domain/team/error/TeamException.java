package io.teampulse.team.domain.team.error;

import lombok.Getter;

import java.util.Objects;

public class TeamException extends RuntimeException {

    @Getter
    private final TeamErrorCode errorCode;

    public TeamException(TeamErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public TeamException(TeamErrorCode errorCode, String message, Throwable cause) {
        super(Objects.requireNonNull(message, "message must not be null"), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
