package io.teampulse.team.api;

import java.util.Objects;

/** Signals that the public team responsibility query failed technically. */
public class TeamResponsibilityDirectoryException extends RuntimeException {

    private static final String MESSAGE = "Unable to check team responsibilities";

    public TeamResponsibilityDirectoryException(Throwable cause) {
        super(MESSAGE, Objects.requireNonNull(cause, "cause must not be null"));
    }
}
