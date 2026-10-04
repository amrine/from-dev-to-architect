package io.teampulse.common.error;

/**
 * Stable, framework-independent error data returned by TeamPulse HTTP APIs.
 *
 * @param code public error code
 * @param message safe message intended for API clients
 */
public record ApiError(String code, String message) {

    public ApiError {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
